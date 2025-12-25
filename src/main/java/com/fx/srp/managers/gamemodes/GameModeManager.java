package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.Speedrun;
import com.fx.srp.util.ui.TimerUtil;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Abstract base class for managing game mode logic in SRP.
 *
 * <p>This class provides common functionality for all game modes, such as:
 * <ul>
 *     <li>Run initialization and state management</li>
 *     <li>Countdown timers before starting runs</li>
 *     <li>World creation and reset for players</li>
 *     <li>Run finishing and cleanup</li>
 *     <li>Scheduling repeated tick updates and run timeouts</li>
 * </ul>
 * </p>
 *
 * @param <T> the specific type of {@link Speedrun} this manager handles
 */
@AllArgsConstructor
public abstract class GameModeManager<T extends Speedrun> implements IGameModeManager {

    protected final ConfigHandler configHandler = ConfigHandler.getInstance();
    protected final SpeedRunPlus plugin;

    protected final GameManager gameManager;
    protected final WorldManager worldManager;

    @Override
    public void abort(@NonNull Speedrun run, CommandSender sender, String reason) {
        // Notify each participant
        run.getSpeedrunners().forEach((speedrunner) -> {
            String message = ChatColor.RED + "Your run has been aborted" +
                    (sender instanceof Player ? " by: " + ChatColor.WHITE + sender.getName() : "") +
                    (reason != null ? ChatColor.RED + ", reason: " + ChatColor.WHITE + reason : "!");

            speedrunner.getPlayer().sendMessage(message);
        });

        finishRun(run, 0);
    }

    /* ==========================================================
     *                COMMON INITIALIZATION
     * ========================================================== */
    /**
     * Initializes a new run and setting the state to {@link Speedrun.State#CREATING_WORLDS}.
     *
     * @param run the run to initialize
     */
    protected void initializeRun(T run) {
        run.setState(Speedrun.State.CREATING_WORLDS);
    }

    /* ==========================================================
     *                COMMON COUNTDOWN LOGIC
     * ========================================================== */
    /**
     * Starts a countdown before a run begins.
     *
     * <p>After a short delay, the run will transition to the running state,
     * the stopwatch will start, and players will be un-frozen.</p>
     *
     * @param run the run to start
     * @param players the collection of {@link Speedrunner}s participating
     */
    protected void startCountdown(T run, Collection<Speedrunner> players) {
        // Delay slightly to let the world load
        Bukkit.getScheduler().runTaskLater(plugin, () -> start(run, players), 20L);
    }

    private void start(T run, Collection<Speedrunner> players) {
        // Create the timer and update the game state
        run.setState(Speedrun.State.COUNTDOWN);
        run.initializeTimers();

        new BukkitRunnable() {
            /* default */ int seconds = configHandler.getTimerCountdown();

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
                run.setState(Speedrun.State.RUNNING);

                // Schedule timeout
                scheduleTimeoutTask(
                        run,
                        () -> abort(run, null, "Times up!")
                );

                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /* ==========================================================
     *                COMMON RESET LOGIC
     * ========================================================== */
    /**
     * Resets a player's world for a run by:
     * <ul>
     *     <li>Freezing the player</li>
     *     <li>Creating new worlds for the player</li>
     *     <li>Teleporting and restoring player state</li>
     *     <li>Deleting old worlds and assigning new ones</li>
     * </ul>
     *
     * @param speedrunner the player to reset
     * @param seed optional seed for world generation
     * @param afterWorldDeletion callback to run after old worlds are deleted
     */
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
    /**
     * Finishes a run, performing cleanup tasks including:
     * <ul>
     *     <li>Stopping the stopwatch and canceling scheduled tasks</li>
     *     <li>Freezing and restoring player states</li>
     *     <li>Deleting worlds via {@link WorldManager}</li>
     *     <li>Unregistering the run from {@link GameManager}</li>
     * </ul>
     *
     * @param run the run to finish
     */
    protected void finishRun(Speedrun run, int delayTicks) {
        run.setState(Speedrun.State.FINISHED);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Perform cleanup
            cleanupAfterRun(run, () ->
                    worldManager.deleteWorldsForPlayers(run.getSpeedrunners(), () -> {})
            );

            // Remove from global speedrun registry
            gameManager.unregisterRun(run);

        }, delayTicks);
    }

    private void cleanupAfterRun(Speedrun run, Runnable onWorldsDeleted) {
        // stop stopwatch + cancel update tasks
        run.getStopWatch().stop();
        cancelTasks(run);

        // Update speedrun state
        run.setState(Speedrun.State.CLEANING);

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
    /**
     * Schedules a task to run after the maximum allowed run time has elapsed.
     *
     * <p>If the run is still running, the provided {@code timeoutHandler} is executed.</p>
     *
     * @param run the run to monitor
     * @param timeoutHandler the action to execute on timeout
     */
    protected void scheduleTimeoutTask(T run, Runnable timeoutHandler) {
        long ticks = configHandler.getMaxRunTime() / 50L;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (run.getState() == Speedrun.State.RUNNING) {
                timeoutHandler.run();
            }
        }, ticks);

        run.setTimeoutTask(task);
    }

    /**
     * Schedules a repeating task that executes every few ticks to update run-specific logic.
     *
     * <p>Typically used for updating player timers, HUDs, or other periodic tasks.</p>
     *
     * @param run the run associated with the task
     * @param tickAction the action to execute each tick
     */
    protected void scheduleRepeatingTickTask(T run, Consumer<T> tickAction) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (run.getState() == Speedrun.State.RUNNING) {
                tickAction.accept(run);
            }},
            0L,
            5L
        );

        run.setTimerUpdateTask(task);
    }

    /**
     * Cancels any scheduled timer update or timeout tasks associated with a run.
     *
     * @param run the run whose tasks should be canceled
     */
    protected void cancelTasks(Speedrun run) {
        if (run.getTimerUpdateTask() != null) run.getTimerUpdateTask().cancel(); run.setTimerUpdateTask(null);
        if (run.getTimeoutTask() != null) run.getTimeoutTask().cancel(); run.setTimeoutTask(null);
    }
}