package com.fx.srp.util.ui;

import com.fx.srp.util.time.TimeFormatter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

public class TimerUtil {

    private static final String TIMER_OBJECTIVE_ID = "SRP_TIMER";
    private static final String TIMER_OBJECTIVE_CRITERIA = "dummy";
    private static final String TIMER_TITLE = "Timer";
    private static final String TEAM_ID = "SRP_TEAM";
    private static final String TEAM_SIDEBAR_ANCHOR = "§a";
    private static final String TEAM_TIMER_ANCHOR = "§f";

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
        team.setSuffix(TEAM_TIMER_ANCHOR + new TimeFormatter(stopWatch).includeHours().superscriptMs().format());

        // Set the (team) timer
        timer.getScore(TEAM_SIDEBAR_ANCHOR).setScore(0);
    }

    // Refresh the timer objective on a given player's scoreboard
    public static void updateTimer(Player player, StopWatch stopWatch) {
        if (player == null || !player.isOnline()) return;

        // Get the player's scoreboard and their team, exit prematurely if it does not already exist
        Team team = player.getScoreboard().getTeam(TEAM_ID);
        if (team == null) return;

        // Update the timer
        team.setSuffix(TEAM_TIMER_ANCHOR +  new TimeFormatter(stopWatch).includeHours().superscriptMs().format());
    }
}

