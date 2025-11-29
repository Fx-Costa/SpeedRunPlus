package com.fx.srp.util.ui;

import com.fx.srp.util.time.TimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;

import java.util.List;

/**
 * Utility class for creating and updating scoreboard-based timers for players.
 * <p>
 * This class allows displaying a live timer on a player's sidebar using
 * {@link Scoreboard} and {@link Team}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimerUtil {

    private static final String TIMER_OBJECTIVE_ID = "SRP_TIMER";
    private static final String TIMER_OBJECTIVE_CRITERIA = "dummy";
    private static final String TIMER_TITLE = "Timer";
    private static final String TEAM_ID = "SRP_TEAM";
    private static final String TEAM_SIDEBAR_ANCHOR = "§a";
    private static final String TEAM_TIMER_ANCHOR = "§f";

    /**
     * Creates a timer for multiple players.
     *
     * @param players   the list of {@code Player}s to add timers for
     * @param stopwatch the {@link StopWatch} used to track the timer
     */
    public static void createTimer(List<Player> players, StopWatch stopwatch) {
        players.forEach(player -> createTimer(player, stopwatch));
    }

    // Create a time objective on a given player's scoreboard
    private static void createTimer(Player player, StopWatch stopWatch) {
        if (player == null || !player.isOnline()) return;

        // Get the player's scoreboard and the timer within it, exit prematurely if it already exists
        Scoreboard scoreboard = player.getScoreboard();
        Objective timer = scoreboard.getObjective(TIMER_OBJECTIVE_ID);
        if (timer != null) return;

        // Create the timer
        timer = scoreboard.registerNewObjective(TIMER_OBJECTIVE_ID, TIMER_OBJECTIVE_CRITERIA, TIMER_TITLE);
        timer.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Use a team to present the timer
        Team team = scoreboard.getTeam(TEAM_ID);
        if (team == null) team = scoreboard.registerNewTeam(TEAM_ID);
        team.addEntry(TEAM_SIDEBAR_ANCHOR); // Anchoring the team to the sidebar
        team.setPrefix("");
        team.setSuffix(TEAM_TIMER_ANCHOR + new TimeFormatter(stopWatch).withHours().withSuperscriptMs().format());

        // Set the (team) timer
        timer.getScore(TEAM_SIDEBAR_ANCHOR).setScore(0);
    }

    /**
     * Updates an existing timer for a player.
     * <p>
     * If the player is {@code null}, offline, or does not have a team with the timer ID, this method does nothing.
     *
     * @param player    the {@code Player} whose timer should be updated
     * @param stopWatch the {@link StopWatch} used to track the timer
     */
    public static void updateTimer(Player player, StopWatch stopWatch) {
        if (player == null || !player.isOnline()) return;

        // Get the player's scoreboard and their team, exit prematurely if it does not already exist
        Team team = player.getScoreboard().getTeam(TEAM_ID);
        if (team == null) return;

        // Update the timer
        team.setSuffix(TEAM_TIMER_ANCHOR +  new TimeFormatter(stopWatch).withHours().withSuperscriptMs().format());
    }
}

