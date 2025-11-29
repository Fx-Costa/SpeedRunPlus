package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.AbstractSpeedrun;
import com.fx.srp.util.ui.TimerUtil;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public abstract class AbstractGameModeManager<T extends AbstractSpeedrun> implements IGameModeManager<T> {

    protected final ConfigHandler configHandler = ConfigHandler.getInstance();
    protected final SpeedRunPlus plugin;

    protected final GameManager gameManager;
    protected final WorldManager worldManager;

    /* ==========================================================
     *                COMMON INITIALIZATION
     * ========================================================== */
    protected void initializeRun(T run) {
        run.setState(AbstractSpeedrun.State.CREATING_WORLDS);
    }

    /* ==========================================================
     *                COMMON COUNTDOWN LOGIC
     * ========================================================== */
    protected void startCountdown(T run, Collection<Speedrunner> players) {
        // Delay slightly to let the world load
        Bukkit.getScheduler().runTaskLater(plugin, () -> start(run, players), 20L);
    }

    private void start(T run, Collection<Speedrunner> players) {
        // Create the timer and update the game state
        run.setState(AbstractSpeedrun.State.COUNTDOWN);
        run.initializeTimers();

        new BukkitRunnable() {
            int seconds = configHandler.getTimerCountdown();

            @Override
            public void run() {
                // All players must stay online
                if (players.stream().anyMatch(p -> !p.getPlayer().isOnline())) {
                    cancel();
                    return;
                }

                // Countdown text (title)
                if (seconds > 0) {
                    for (Speedrunner p : players) {
                        p.getPlayer().sendTitle(
                                ChatColor.YELLOW + "Starting in " + seconds + "...",
                                "",
                                0, 20, 0
                        );
                    }
                    seconds--;
                    return;
                }

                // Start stopwatch
                run.getStopWatch().reset();
                run.getStopWatch().start();

                // === START ===
                // Start text (title) and unfreeze
                players.forEach(p -> {
                    p.getPlayer().sendTitle(ChatColor.GREEN + "GO!", "", 0, 40, 20);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> p.getPlayer().resetTitle(), 40L);
                    p.unfreeze();
                });

                // Begin timer HUD
                scheduleRepeatingTickTask(
                        run,
                        r -> players.forEach(p -> TimerUtil.updateTimer(p.getPlayer(), r.getStopWatch()))
                );

                // Update speedrun state
                run.setState(AbstractSpeedrun.State.RUNNING);

                // Schedule timeout
                scheduleTimeoutTask(
                        run,
                        () -> stop(run, null)
                );

                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /* ==========================================================
     *                COMMON RESET LOGIC
     * ========================================================== */
    protected void recreateWorldsForReset(
            Speedrunner speedrunner,
            Long seed,
            Runnable afterWorldDeletion
    ) {
        Player player = speedrunner.getPlayer();
        UUID uuid = player.getUniqueId();

        // Freeze player during reset
        speedrunner.freeze();

        // Create new worlds
        worldManager.createWorldsForPlayers(List.of(player), seed, sets -> {
            WorldManager.WorldSet newWorldSet = sets.get(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Teleport- and reset state of player
                player.teleport(newWorldSet.getSpawn());
                speedrunner.resetState();

                // Unfreeze player & display title
                speedrunner.unfreeze();
                player.sendTitle(ChatColor.GREEN + "GO!", "", 0, 40, 20);
                Bukkit.getScheduler().runTaskLater(plugin, player::resetTitle, 40L);

                TimerUtil.createTimer(List.of(player), speedrunner.getStopWatch());

                // Delete old worlds
                worldManager.deleteWorldsForPlayers(List.of(speedrunner), () -> {
                    // Assign new worlds
                    speedrunner.setWorldSet(newWorldSet);

                    // Callback
                    afterWorldDeletion.run();
                });
            });
        });
    }

    /* ==========================================================
     *                COMMON STOP LOGIC
     * ========================================================== */
    protected void finishRun(T run) {
        run.setState(AbstractSpeedrun.State.FINISHED);

        // Perform cleanup
        cleanupAfterRun(run, () -> worldManager.deleteWorldsForPlayers(run.getSpeedrunners(), () -> {}));

        // Remove from global speedrun registry
        gameManager.unregisterRun(run);
    }

    private void cleanupAfterRun(T run, Runnable onWorldsDeleted) {
        // stop stopwatch + cancel update tasks
        run.getStopWatch().stop();
        cancelTasks(run);

        // Update speedrun state
        run.setState(AbstractSpeedrun.State.CLEANING);

        // Freeze all speedrunners
        List<Speedrunner> speedRunners = run.getSpeedrunners();
        speedRunners.forEach(Speedrunner::freeze);

        // Callback for managers to delete worlds
        onWorldsDeleted.run();

        // Restore player states
        Bukkit.getScheduler().runTask(plugin, () -> speedRunners.forEach(speedRunner -> {
            speedRunner.restoreState();
            speedRunner.unfreeze();
        }));
    }

    /* ==========================================================
     *                COMMON TIMER LOGIC
     * ========================================================== */
    protected void scheduleTimeoutTask(T run, Runnable timeoutHandler) {
        long ticks = configHandler.getMaxRunTime() / 50L;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (run.getState() == AbstractSpeedrun.State.RUNNING) {
                timeoutHandler.run();
            }
        }, ticks);

        run.setTimeoutTask(task);
    }

    protected void scheduleRepeatingTickTask(T run, Consumer<T> tickAction) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (run.getState() == AbstractSpeedrun.State.RUNNING) {
                tickAction.accept(run);
            }},
            0L,
            5L
        );

        run.setTimerUpdateTask(task);
    }

    protected void cancelTasks(T run) {
        if (run.getTimerUpdateTask() != null) run.getTimerUpdateTask().cancel(); run.setTimerUpdateTask(null);
        if (run.getTimeoutTask() != null) run.getTimeoutTask().cancel(); run.setTimeoutTask(null);
    }
}