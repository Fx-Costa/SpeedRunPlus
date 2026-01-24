package com.fx.srp.managers.util;

import com.fx.srp.config.ConfigHandler;
import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.triangulation.DeterministicTriangulation;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
public class TriangulationManager {

    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public void assistedTriangulation(Speedrunner speedrunner, Projectile projectile) {
        Player player = speedrunner.getPlayer();
        List<EyeThrow> eyeThrows = speedrunner.getEyeThrows();
        int eyeThrowCount = eyeThrows.size();

        // Record the eye throw
        speedrunner.addEyeThrow(projectile);

        // Trigger feedback on the first eye throw
        if (eyeThrowCount < 2){
            player.sendMessage(ChatColor.YELLOW +
                    "1st Eye of Ender thrown! Throw another to triangulate the stronghold."
            );
            return;
        }

        // Trigger triangulation on the second eye thrown and on any subsequent eye throw
        if (eyeThrowCount == 2){
            player.sendMessage(ChatColor.YELLOW + "2nd Eye of Ender thrown! Triangulating the stronghold...");
        }
        else {
            player.sendMessage(ChatColor.YELLOW +
                    "Another Eye of Ender thrown! Recalculating the stronghold location..."
            );
        }

        // Perform triangulation
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
        String overworldMsg = String.format(
                "Overworld -> X: %s %.1f, Z: %s %.1f", green, overworld.getX(), green, overworld.getZ()
        );
        String netherMsg = String.format(
                "Nether -> X: %s %.1f, Z: %s %.1f", green, nether.getX(), green, nether.getZ()
        );
        player.sendMessage(ChatColor.YELLOW + "Stronghold located!\n" + overworldMsg + "\n" + netherMsg);
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
