package com.fx.srp.model.run;

import com.fx.srp.model.player.Speedrunner;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import java.util.List;

public interface ISpeedrun {
    // State of the run
    enum State { WAITING, CREATING_WORLDS, COUNTDOWN, RUNNING, FINISHED, CLEANING }
    State getState();
    void setState(State state);

    // Retrieve all players in this run
    List<Speedrunner> getSpeedrunners();

    // Called when a player respawns
    void onPlayerRespawn(Speedrunner speedrunner, PlayerRespawnEvent event);

    // Called when a player leaves or disconnects
    void onPlayerLeave(Player player);

    // Initialize timers for the run
    void initializeTimers();

    // Stopwatch for the tune
    StopWatch getStopWatch();

    // Seed for the run
    Long getSeed();
    void setSeed(Long seed);
}
