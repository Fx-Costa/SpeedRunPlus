package com.fx.srp.managers;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.managers.gamemodes.BattleManager;
import com.fx.srp.managers.gamemodes.SoloManager;
import com.fx.srp.managers.util.AfkManager;
import com.fx.srp.managers.util.LeaderboardManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.BattleSpeedrun;
import com.fx.srp.model.run.ISpeedrun;
import com.fx.srp.model.run.SoloSpeedrun;
import lombok.Getter;
import org.bukkit.Bukkit;
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
    @Getter private final SoloManager soloManager;
    @Getter private final BattleManager battleManager;

    // Utilities
    private final AfkManager afkManager;
    private final LeaderboardManager leaderboardManager;

    /**
     * Constructs a new {@link GameManager} and initializes all sub-managers
     * and utilities.
     *
     * @param plugin the main {@link SpeedRunPlus} plugin instance
     */
    public GameManager(SpeedRunPlus plugin) {
        this.afkManager = new AfkManager(plugin);
        this.leaderboardManager =  new LeaderboardManager(plugin);

        WorldManager worldManager = new WorldManager(plugin);

        this.soloManager = new SoloManager(plugin, this, worldManager);
        this.battleManager = new BattleManager(plugin, this, worldManager);
    }

    /* ==========================================================
     *                      Run Management
     * ========================================================== */
    /**
     * Retrieves the active run a player is currently participating in.
     *
     * @param player the player
     * @return an {@link Optional} containing the {@link ISpeedrun}, or empty if not in a run
     */
    public Optional<ISpeedrun> getActiveRun(Player player) {
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
     * @param run the {@link ISpeedrun} to register
     */
    public void registerRun(ISpeedrun run) {
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
     * @param run the {@link ISpeedrun} to unregister
     */
    public void unregisterRun(ISpeedrun run) {
        run.getSpeedrunners().forEach(player ->
                runRegistry.removeRun(player.getPlayer().getUniqueId())
        );

        // Stop AFK monitoring when there are no active runs
        if (runRegistry.getAllRuns().isEmpty()) {
            afkManager.stopAfkChecker();
        }
    }

    /**
     * Finishes a speedrun for a specific player and updates the leaderboard.
     *
     * <p>Delegates to the appropriate manager depending on the run type
     * (e.g.: {@link BattleSpeedrun}).</p>
     *
     * @param run the {@link ISpeedrun} to finish
     * @param player the player responsible for finishing the run (nullable)
     */
    public void finishRun(ISpeedrun run, Player player) {
        if (run instanceof SoloSpeedrun) soloManager.stop((SoloSpeedrun) run, player);
        if (run instanceof BattleSpeedrun) battleManager.stop((BattleSpeedrun) run, player);

        // Persist changes to the leaderboard
        if (player != null) leaderboardManager.finishRun(player, run.getStopWatch().getTime());
    }

    /**
     * Aborts and finishes all active runs without specifying a finishing player.
     */
    public void stopAllRuns() {
        // Abort all runs
        ActiveRunRegistry.getINSTANCE().getAllRuns().forEach(run -> finishRun(run, null));
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
                        finishRun(run, null)
                )
        );
    }
}
