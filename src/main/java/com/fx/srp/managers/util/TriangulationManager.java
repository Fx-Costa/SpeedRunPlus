package com.fx.srp.managers.util;

import com.fx.srp.config.ConfigHandler;
import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.triangulation.DeterministicTriangulation;
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
     * @param eyeThrow  the {@code EyeThrow} with data on the ender eye
     */
    public void assistedTriangulation(Speedrunner speedrunner, EyeThrow eyeThrow) {
        int triangulationTriggerAmount = 2;
        int initialEyeThrowCount = speedrunner.getEyeThrows().size();

        // Record the eye throw
        speedrunner.addEyeThrow(eyeThrow);

        Player player = speedrunner.getPlayer();
        List<EyeThrow> eyeThrows = speedrunner.getEyeThrows();
        int eyeThrowCount = eyeThrows.size();

        // Trigger feedback on the first eye throw
        if (eyeThrowCount < triangulationTriggerAmount){
            player.sendMessage(ChatColor.YELLOW +
                    "1st Eye of Ender thrown! Throw another to triangulate the stronghold."
            );
            return;
        }

        // On throws after the second one
        if (initialEyeThrowCount == triangulationTriggerAmount){
            player.sendMessage(ChatColor.YELLOW + "Recalculating the stronghold location...");
        }

        // Trigger triangulation on the second eye thrown and on any subsequent eye throw
        TriangulationResult triangulationResult = triangulate(eyeThrows);

        // On failed triangulation
        if (triangulationResult == null){
            player.sendMessage(ChatColor.RED + "Triangulation failed! Move more blocks between throws!");
            return;
        }

        // On successful triangulation
        Vector overworld = triangulationResult.getOverworld();
        Vector nether = triangulationResult.getNether();
        ChatColor green = ChatColor.GREEN;
        ChatColor yellow = ChatColor.YELLOW;
        String overworldMsg = String.format("    Overworld -> X: %s %.0f %s, Z: %s %.0f",
                green, overworld.getX(), yellow, green, overworld.getZ()
        );
        String netherMsg = String.format("    Nether -> X: %s %.0f %s, Z: %s %.0f",
                green, nether.getX(), yellow, green, nether.getZ()
        );
        player.sendMessage(yellow + "Stronghold located:\n" + overworldMsg + "\n" + yellow + netherMsg);
    }

    private TriangulationResult triangulate(List<EyeThrow> eyeThrows){
        // Get the configured triangulation strategy
        String strategy = configHandler.getAssistedTriangulationStrategy();

        // Triangulate using the specified strategy
        switch (strategy.toUpperCase(Locale.ROOT)) {
            // Deterministic triangulation (no variance)
            case "DETERMINISTIC": return new DeterministicTriangulation().triangulate(eyeThrows);

            // Fallback to deterministic
            default: return new DeterministicTriangulation().triangulate(eyeThrows);
        }
    }
}
