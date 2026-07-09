package com.fx.srp.managers.util;

import com.fx.srp.config.ConfigHandler;
import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.triangulation.AssistedProbabilisticTriangulation;
import com.fx.srp.util.triangulation.DeterministicTriangulation;
import com.fx.srp.util.triangulation.ProbabilisticTriangulation;
import com.fx.srp.util.triangulation.TriangulationStrategy;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;

/**
 * Manages the assisted triangulation.
 * <p>
 * The {@code TriangulationManager} is responsible for determining the underlying algorithm used to
 * triangulate the stronghold with various levels of assistance and variance.
 * <p>
 */
@NoArgsConstructor
public class TriangulationManager {

    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    /**
     * Perform assisted triangulation.
     *
     * <p> Records each eye throw, and provides feedback to the player based on the amount of throws made. On the
     * second eye throw, the stronghold coordinates in overworld and nether coordinates are given to the player.
     * </p>
     *
     * @param speedrunner the speedrunner whose performing the triangulation
     */
    public void tryTriangulation(Speedrunner speedrunner) {
        // Get the triangulation strategy to use for triangulation
        TriangulationStrategy triangulationStrategy = getTriangulationStrategy();
        if (triangulationStrategy == null) return;

        // Get the eye throws
        List<EyeThrow> eyeThrows = speedrunner.getEyeThrows();

        // Determine whether enough info has been recorded for performing triangulation based on the strategy
        if (!triangulationStrategy.isReady(eyeThrows)) return;

        Player player = speedrunner.getPlayer();

        // Trigger triangulation on the second eye thrown and on any subsequent eye throw
        TriangulationResult triangulationResult = triangulationStrategy.triangulate(eyeThrows);

        // On failed triangulation
        if (triangulationResult == null){
            player.sendMessage(ChatColor.RED + "Triangulation failed! Try moving more blocks between throws!");
            return;
        }

        // On successful triangulation
        Vector overworld = triangulationResult.getOverworld();
        Vector nether = triangulationResult.getNether();
        Double confidence = triangulationResult.getConfidence();
        sendTriangulationMessage(player, overworld, nether, confidence);
    }

    /**
     * Restart assisted triangulation for a given speedrunner
     */
    public void resetTriangulation(Speedrunner speedrunner) {
        if (!configHandler.isAssistedTriangulation()) return;

        speedrunner.resetEyeThrows();
        speedrunner.getPlayer().sendMessage(
                ChatColor.YELLOW + "Triangulation has reset! Throw an Eye of Ender to start triangulation!"
        );
    }

    /**
     * Send a help message on the first eye throw
     */
    public void sendFirstEyeMessage(Speedrunner speedrunner){
        if (!configHandler.isAssistedTriangulation()) return;

        TriangulationStrategy triangulationStrategy = getTriangulationStrategy();
        if (triangulationStrategy == null) return;

        // Send the help message
        speedrunner.getPlayer().sendMessage(ChatColor.YELLOW + triangulationStrategy.getFirstEyeMessage());
    }

    /**
     * Send a help message on subsequent eye throws after the second
     */
    public void sendRecalculationMessage(Speedrunner speedrunner){
        // Send the help message
        speedrunner.getPlayer().sendMessage(ChatColor.YELLOW + "Recalculating the stronghold location...");
    }

    private TriangulationStrategy getTriangulationStrategy() {
        // Get the configured triangulation strategy
        String strategy = configHandler.getAssistedTriangulationStrategy();

        // Triangulate using the specified strategy
        switch (strategy.toUpperCase(Locale.ROOT)) {
            // Deterministic triangulation (no variance)
            case "DETERMINISTIC": return new DeterministicTriangulation();

            // Assisted Probabilistic-based triangulation
            case "ASSISTED-PROBABILISTIC": return new AssistedProbabilisticTriangulation();

            // Probabilistic-based triangulation
            case "PROBABILISTIC": return new ProbabilisticTriangulation();

            // No strategy is set
            default: return null;
        }
    }

    private void sendTriangulationMessage(Player player, Vector overworld, Vector nether, Double confidence) {
        ChatColor yellow = ChatColor.YELLOW;
        ChatColor bold = ChatColor.BOLD;
        ChatColor reset = ChatColor.RESET;
        ChatColor green = ChatColor.GREEN;
        ChatColor gold = ChatColor.GOLD;
        ChatColor darkRed = ChatColor.DARK_RED;
        ChatColor red = ChatColor.RED;

        // Overworld
        String overworldMsg = String.format(
                "    Overworld -> X: %s %.0f %s, Z: %s %.0f",
                green, overworld.getX(), yellow, green, overworld.getZ()
        );

        // Nether
        String netherMsg = String.format(
                "    Nether -> X: %s %.0f %s, Z: %s %.0f",
                green, nether.getX(), yellow, green, nether.getZ()
        );

        StringBuilder message = new StringBuilder();
        message.append(yellow).append("\nStronghold located:\n")
                .append(overworldMsg).append("\n")
                .append(yellow).append(netherMsg);

        // Confidence (only if present)
        if (confidence != null) {
            ChatColor confidenceColor =
                    confidence > 94 ? green:
                    confidence > 84 ? yellow:
                    confidence > 70 ? gold:
                    confidence > 60 ? darkRed:
                    red;

            String confidenceFlavor =
                    confidence > 98 ? bold + "Perfect" + reset:
                    confidence > 94 ? "Excellent":
                    confidence > 84 ? "Good":
                    confidence > 70 ? "OK":
                    confidence > 60 ? "Uncertain":
                    "Bad";

            String confidenceMsg = String.format(
                    "    Confidence: %s %.0f%% (%s)",
                    confidenceColor, confidence, confidenceFlavor
            );
            message.append("\n").append(yellow).append(confidenceMsg);
        }

        player.sendMessage(message.toString());
    }
}
