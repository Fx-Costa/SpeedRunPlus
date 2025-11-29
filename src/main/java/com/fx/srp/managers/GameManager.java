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

public class GameManager {

    private final ActiveRunRegistry runRegistry = ActiveRunRegistry.getInstance();

    // Game modes
    @Getter private final SoloManager soloManager;
    @Getter private final BattleManager battleManager;

    // Utilities
    private final AfkManager afkManager;
    private final LeaderboardManager leaderboardManager;

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
    public Optional<ISpeedrun> getActiveRun(Player p) {
        return Optional.ofNullable(runRegistry.getActiveRun(p.getUniqueId()));
    }

    public boolean isInRun(Player p) {
        return runRegistry.isPlayerInAnyRun(p.getUniqueId());
    }

    public void registerRun(ISpeedrun run) {
        run.getSpeedrunners().forEach(player ->
                runRegistry.addRun(player.getPlayer().getUniqueId(), run)
        );

        // Start AFK monitoring once we have at least one active run
        startAfkMonitoring();
    }

    public void unregisterRun(ISpeedrun run) {
        run.getSpeedrunners().forEach(player ->
                runRegistry.removeRun(player.getPlayer().getUniqueId())
        );

        // Stop AFK monitoring when there are no active runs
        if (runRegistry.getAllRuns().isEmpty()) {
            afkManager.stopAfkChecker();
        }
    }

    public void finishRun(ISpeedrun run, Player player) {
        if (run instanceof SoloSpeedrun) soloManager.stop((SoloSpeedrun) run, player);
        if (run instanceof BattleSpeedrun) battleManager.stop((BattleSpeedrun) run, player);

        // Persist changes to the leaderboard
        if (player != null) leaderboardManager.finishRun(player, run.getStopWatch().getTime());
    }

    public void stopAllRuns() {
        // Abort all runs
        ActiveRunRegistry.getInstance().getAllRuns().forEach(run -> finishRun(run, null));
    }

    /* ==========================================================
     *                    Player management
     * ========================================================== */
    public List<Player> getAllPlayersInRuns() {
        return runRegistry.getAllPlayersInRuns().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Optional<Speedrunner> getSpeedrunner(Player player) {
        return getActiveRun(player).flatMap(run -> run.getSpeedrunners().stream()
                .filter(runner -> runner.getPlayer().equals(player))
                .findFirst());
    }

    /* ==========================================================
     *                    Event management
     * ========================================================== */
    public void handlePlayerMove(Player player, PlayerMoveEvent event) {
        getSpeedrunner(player).ifPresent(runner -> {
            if (runner.isFrozen()) {
                event.setTo(event.getFrom());
                event.setCancelled(true);
            }
            afkManager.updateActivity(player);
        });
    }

    public void handlePlayerInteract(Player player, PlayerInteractEvent event) {
        getSpeedrunner(player).ifPresent(runner -> {
            if (runner.isFrozen()) {
                event.setCancelled(true);
            }
            afkManager.updateActivity(player);
        });
    }

    public void handlePlayerQuit(Player player) {
        getActiveRun(player).ifPresent(run -> run.onPlayerLeave(player));
    }

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
    public void startAfkMonitoring() {
        afkManager.startAfkChecker(
                this::getAllPlayersInRuns,
                player -> getActiveRun(player).ifPresent(run ->
                        finishRun(run, null)
                )
        );
    }
}
