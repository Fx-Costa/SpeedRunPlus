package com.fx.srp.managers;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.managers.gamemodes.SoloManager;
import com.fx.srp.managers.gamemodes.BattleManager;
import com.fx.srp.managers.gamemodes.CoopManager;
import com.fx.srp.commands.GameMode;
import com.fx.srp.managers.util.AfkManager;
import com.fx.srp.managers.util.LeaderboardManager;
import com.fx.srp.managers.util.SeedManager;
import com.fx.srp.managers.util.TriangulationManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.Speedrun;
import com.fx.srp.model.run.SoloSpeedrun;
import com.fx.srp.model.run.BattleSpeedrun;
import com.fx.srp.model.run.CoopSpeedrun;
import com.fx.srp.model.seed.SeedCategory;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central manager for SRP gameplay, responsible for coordinating active runs,
 * player management, event handling, and auxiliary utilities.
 *
 * <p>This class integrates:</p>
 * <ul>
 *     <li>Game mode managers ({@link SoloManager}, {@link BattleManager})</li>
 *     <li>AFK monitoring via {@link AfkManager}</li>
 *     <li>Leaderboard management via {@link LeaderboardManager}</li>
 *     <li>World management via {@link WorldManager}</li>
 * </ul>
 */
public class GameManager {

    private final ActiveRunRegistry runRegistry = ActiveRunRegistry.getINSTANCE();

    // Game modes
    private final SoloManager soloManager;
    private final BattleManager battleManager;
    private final CoopManager coopManager;

    // Utilities
    private final SeedManager seedManager;
    private final AfkManager afkManager;
    private final LeaderboardManager leaderboardManager;
    private final TriangulationManager triangulationManager;

    /**
     * Constructs a new {@link GameManager} and initializes all sub-managers
     * and utilities.
     *
     * @param plugin the main {@link SpeedRunPlus} plugin instance
     */
    public GameManager(SpeedRunPlus plugin) {
        // Utilities
        this.afkManager = new AfkManager(plugin);
        this.leaderboardManager = new LeaderboardManager(plugin);
        this.seedManager = new SeedManager(plugin);
        this.triangulationManager = new TriangulationManager();
        WorldManager worldManager = new WorldManager(plugin, seedManager);

        // Game mode managers
        this.soloManager = new SoloManager(plugin, this, worldManager);
        this.battleManager = new BattleManager(plugin, this, worldManager);
        this.coopManager = new CoopManager(plugin, this, worldManager);

        // Bind managers to their game modes
        GameMode.SOLO.bindManager(soloManager);
        GameMode.BATTLE.bindManager(battleManager);
        GameMode.COOP.bindManager(coopManager);
    }

    /* ==========================================================
     *                      Run Management
     * ========================================================== */
    /**
     * Retrieves the active run a player is currently participating in.
     *
     * @param player the player
     * @return an {@link Optional} containing the {@link Speedrun}, or empty if not in a run
     */
    public Optional<Speedrun> getActiveRun(Player player) {
        if (player == null) return Optional.empty();
        return Optional.ofNullable(runRegistry.getActiveRun(player.getUniqueId()));
    }

    /**
     * Checks whether a player is currently in any active speedrun.
     *
     * @param player the player
     * @return {@code true} if the player is in a run, {@code false} otherwise
     */
    public boolean isInRun(Player player) {
        return runRegistry.isPlayerInAnyRun(player.getUniqueId());
    }

    /**
     * Registers a new speedrun for all associated players.
     *
     * <p>Starts AFK monitoring as a side effect.</p>
     *
     * @param run the {@link Speedrun} to register
     */
    public void registerRun(Speedrun run) {
        run.getSpeedrunners().forEach(player ->
                runRegistry.addRun(player.getPlayer().getUniqueId(), run)
        );

        // Start AFK monitoring once we have at least one active run
        startAfkMonitoring();
    }

    /**
     * Unregisters a speedrun and removes all participating players from the registry.
     *
     * <p>Stops AFK monitoring as a side effect.</p>
     *
     * @param run the {@link Speedrun} to unregister
     */
    public void unregisterRun(Speedrun run) {
        run.getSpeedrunners().forEach(player ->
                runRegistry.removeRun(player.getPlayer().getUniqueId())
        );

        // Stop AFK monitoring when there are no active runs
        if (runRegistry.getAllRuns().isEmpty()) {
            afkManager.stopAfkChecker();
        }
    }

    /**
     * Finishes a speedrun awarding a win to a player and updates the leaderboard.
     *
     * <p>Delegates to the appropriate manager depending on the run type
     * (e.g.: {@link BattleSpeedrun}).</p>
     *
     * @param run the {@link Speedrun} to finish
     * @param player the winner
     */
    public void completeRun(Speedrun run, @NonNull Player player) {
        if (run instanceof SoloSpeedrun) soloManager.stop(player);
        if (run instanceof BattleSpeedrun) battleManager.stop(player);
        if (run instanceof CoopSpeedrun) coopManager.stop(player);

        // Persist changes to the leaderboard
        leaderboardManager.finishRun(player, run.getStopWatch().getTime());
    }

    /**
     * Abort all active runs.
     */
    public void abortAllRuns() {
        // Abort all runs
        ActiveRunRegistry.getINSTANCE().getAllRuns().forEach(run -> abortRun(run, null, null));
    }

    /**
     * Abort a player's active run.
     *
     * @param player the player's run to abort (including all other players in the same run)
     */
    public void abortRun(@NonNull Player player) {
        getActiveRun(player).ifPresent(run -> abortRun(run, player, null));
    }

    /**
     * Abort an active run.
     *
     * @param run the {@code Speedrun} to abort
     */
    public void abortRun(@NonNull Speedrun run, CommandSender sender, String reason) {
        // Delegate to the appropriate manager for mode-specific cleanup
        run.getGameMode().getManager().abort(run, sender, reason);

        run.getSpeedrunners().forEach(runner ->
                runRegistry.removeRun(runner.getPlayer().getUniqueId())
        );
    }

    /* ==========================================================
     *                    Player management
     * ========================================================== */
    /**
     * Returns a list of all {@link Player}s currently participating in any active run.
     *
     * @return list of players
     */
    public List<Player> getAllPlayersInRuns() {
        return runRegistry.getAllPlayersInRuns().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all {@link Player}s currently participating in a given active run.
     *
     * @return list of players
     */
    public List<Player> getAllPlayersInRun(Speedrun run) {
        return runRegistry.getAllPlayersInRun(run).stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the {@link Speedrunner} object representing a player in their active run.
     *
     * @param player the player
     * @return an {@link Optional} containing the {@link Speedrunner}, or empty if not in a run
     */
    public Optional<Speedrunner> getSpeedrunner(Player player) {
        return getActiveRun(player).flatMap(run -> run.getSpeedrunners().stream()
                .filter(runner -> runner.getPlayer().equals(player))
                .findFirst());
    }

    /* ==========================================================
     *                    Event management
     * ========================================================== */
    /**
     * Assisted triangulation
     */
    public void assistedTriangulation(Speedrunner speedrunner, EyeThrow eyeThrow) {
        // Perform the assisted triangulation
        triangulationManager.assistedTriangulation(speedrunner, eyeThrow);
    }

    /**
     * Handles player movement events.
     *
     * <p>Freezes movement if the player is marked as frozen and updates AFK activity.</p>
     *
     * @param player the player who moved
     * @param event the {@link PlayerMoveEvent} triggered
     */
    public void handlePlayerMove(Player player, PlayerMoveEvent event) {
        getSpeedrunner(player).ifPresent(runner -> {
            if (runner.isFrozen()) {
                event.setTo(event.getFrom());
                event.setCancelled(true);
            }
            afkManager.updateActivity(player);
        });
    }

    /**
     * Handles player interaction events.
     *
     * <p>Cancels interactions if the player is frozen and updates AFK activity.</p>
     *
     * @param player the player who interacted
     * @param event the {@link PlayerInteractEvent} triggered
     */
    public void handlePlayerInteract(Player player, PlayerInteractEvent event) {
        getSpeedrunner(player).ifPresent(runner -> {
            if (runner.isFrozen()) {
                event.setCancelled(true);
            }
            afkManager.updateActivity(player);
        });
    }

    /**
     * Handles player quit events by notifying the active run they left.
     *
     * @param player the player who quit
     */
    public void handlePlayerQuit(Player player) {
        getActiveRun(player).ifPresent(run -> run.onPlayerLeave(player));
    }

    /**
     * Handles player respawn events.
     *
     * <p>Delegates to the active run to manage respawning.</p>
     *
     * @param event the {@link PlayerRespawnEvent} triggered
     */
    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        getSpeedrunner(event.getPlayer()).ifPresent(speedrunner ->
                getActiveRun(event.getPlayer()).ifPresent(run ->
                        run.onPlayerRespawn(speedrunner, event)
                )
        );
    }

    /* ==========================================================
     *                      AFK Monitoring
     * ========================================================== */
    private void startAfkMonitoring() {
        afkManager.startAfkChecker(
                this::getAllPlayersInRuns,
                player -> getActiveRun(player).ifPresent(run ->
                        abortRun(run, null, "AFK")
                )
        );
    }

    /* ==========================================================
     *                     Utilities
     * ========================================================== */
    /**
     * Unload the podium
     */
    public void unloadPodium(){
        leaderboardManager.unloadPodium();
    }

    /**
     * Load the podium
     */
    public void loadPodium() {
        leaderboardManager.loadPodium();
    }

    /**
     * Add filtered seeds to the seed files
     */
    public void addSeed(SeedCategory.SeedType seedType, Integer amount, CommandSender sender) {
        if (seedType == null || amount == null) return;
        seedManager.addSeedAsync(seedType, amount, sender);
    }

    /**
     * Send a help message to the given {@link CommandSender}
     *
     * @param sender the {@link CommandSender} to send command help
     */
    public void sendHelpMessage(CommandSender sender) {
        ChatColor green = ChatColor.GREEN;
        ChatColor yellow = ChatColor.YELLOW;
        ChatColor white = ChatColor.WHITE;

        sender.sendMessage(green + "===== SpeedRunPlus Help =====");
        sender.sendMessage(yellow + "/srp help" + white + " - Show this help message");
        sender.sendMessage("");
        sender.sendMessage(yellow + "/srp solo start" + white + " - Start a solo speedrun");
        sender.sendMessage(yellow + "/srp solo reset" + white + " - Reset your solo speedrun");
        sender.sendMessage(yellow + "/srp solo stop" + white + " - Stop your solo speedrun");
        sender.sendMessage("");
        sender.sendMessage(yellow + "/srp battle request <player>" + white + " - Challenge a player to a battle");
        sender.sendMessage(yellow + "/srp battle accept" + white + " - Accept a request to battle");
        sender.sendMessage(yellow + "/srp battle decline" + white + " - Decline a request to battle");
        sender.sendMessage(yellow + "/srp battle surrender" + white + " - Surrender the battle speedrun");
        sender.sendMessage("");
        sender.sendMessage(yellow + "/srp coop request <player>" + white + " - Request a player to a coop speedrun");
        sender.sendMessage(yellow + "/srp coop accept" + white + " - Accept a request to a coop speedrun");
        sender.sendMessage(yellow + "/srp coop decline" + white + " - Decline a request to coop speedrun");
        sender.sendMessage(yellow + "/srp coop stop" + white + " - Stop the coop speedrun");
        sender.sendMessage(green + "===========================");
    }

    /**
     * Send an admin help message to the given {@link CommandSender}
     *
     * @param sender the {@link CommandSender} to send admin command help
     */
    public void sendAdminHelpMessage(CommandSender sender) {
        ChatColor red = ChatColor.RED;
        ChatColor white = ChatColor.WHITE;

        sender.sendMessage(red + "===== SpeedRunPlus Help =====");
        sender.sendMessage(red + "/srp admin help" + white + " - Show this help message");
        sender.sendMessage("");
        sender.sendMessage(red + "/srp admin stop" + white + " - Stop all speedruns");
        sender.sendMessage(red + "/srp admin stop <player>" + white + " - Stop another player's speedrun");
        sender.sendMessage("");
        sender.sendMessage(red + "/srp admin podium <load|unload>" + white + " - Load/Unload the podium");
        sender.sendMessage("");
        sender.sendMessage(red + "/srp admin seed <type> <amount>" + white + " - Add new filtered seeds");
        sender.sendMessage(red + "===========================");
    }
}
