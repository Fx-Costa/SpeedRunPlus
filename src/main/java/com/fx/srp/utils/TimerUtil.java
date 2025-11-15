package com.fx.srp.utils;

import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class TimerUtil {

    private static final String TIMER_OBJECTIVE_ID = "SRP_TIMER";
    private static final String TIMER_OBJECTIVE_CRITERIA = "dummy";
    private static final String TIMER_TITLE = "Timer";
    private static final String TEAM_ID = "SRP_TEAM";
    private static final String TEAM_SIDEBAR_ANCHOR = "§a";
    private static final String TEAM_TIMER_ANCHOR = "§f";

    // Create a time objective on a given player's scoreboard
    public static void createTimer(Player player, StopWatch stopWatch) {
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

    public static long getMilliseconds(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long ms = stopWatch.getTime() % 1000L;
        return (ms / 10L);  // 2 Digits
    }

    public static long getSeconds(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long sec = stopWatch.getTime() / 1000L;
        return sec % 60;
    }

    public static long getMinutes(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long sec = stopWatch.getTime() / 1000L;
        return sec % 3600 / 60;
    }

    public static long getHours(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long sec = stopWatch.getTime() / 1000L;
        return sec / 3600;
    }

    public static long getMilliseconds(long milliseconds) {
        long ms = milliseconds % 1000L;
        return ms / 10L; // 2 digits
    }

    public static long getSeconds(long milliseconds) {
        long sec = milliseconds / 1000L;
        return sec % 60;
    }

    public static long getMinutes(long milliseconds) {
        long sec = milliseconds / 1000L;
        return (sec % 3600) / 60;
    }

    public static long getHours(long milliseconds) {
        long sec = milliseconds / 1000L;
        return sec / 3600;
    }
}

