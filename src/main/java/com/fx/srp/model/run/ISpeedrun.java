package com.fx.srp.model.run;

import com.fx.srp.model.player.Speedrunner;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import java.util.List;

/**
 * Interface representing a generic speedrun.
 * <p>
 * Provides methods for managing the state, participants, timers, and events
 * during a speedrun.
 * </p>
 */
public interface ISpeedrun {
    // State of the run
    enum State { WAITING, CREATING_WORLDS, COUNTDOWN, RUNNING, FINISHED, CLEANING }

    /**
     * Gets the current state of the speedrun.
     *
     * @return the {@code State} of this run
     */
    State getState();

    /**
     * Sets the current state of the speedrun.
     *
     * @param state the {@code State} to set
     */
    void setState(State state);

    /**
     * Returns a list of all participants in this run.
     *
     * @return a {@code List<Speedrunner>} representing all speedrunners
     */
    List<Speedrunner> getSpeedrunners();

    /**
     * Called when a player respawns during the run.
     *
     * @param speedrunner the {@code Speedrunner} who respawned
     * @param event the {@code PlayerRespawnEvent} associated with the respawn
     */
    void onPlayerRespawn(Speedrunner speedrunner, PlayerRespawnEvent event);

    /**
     * Called when a player leaves or disconnects during the run.
     *
     * @param player the {@code Player} who left
     */
    void onPlayerLeave(Player player);

    /**
     * Initializes any timers or HUD elements for the run.
     */
    void initializeTimers();

    /**
     * Returns the stopwatch tracking the elapsed time for this run.
     *
     * @return the {@code StopWatch} for the run
     */
    StopWatch getStopWatch();

    /**
     * Returns the seed used for world generation in this run.
     *
     * @return the seed as a {@code Long}, or {@code null} if not set
     */
    Long getSeed();

    /**
     * Sets the seed used for world generation in this run.
     *
     * @param seed the seed as a {@code Long}, may be {@code null}
     */
    void setSeed(Long seed);
}
