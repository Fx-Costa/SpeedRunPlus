package com.fx.srp.listeners;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.Speedrun;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

/**
 * Listens for world-related events relevant to SRP gameplay and delegates
 * handling to {@link GameManager}.
 *
 * <p>This listener currently handles events such as entity deaths, specifically
 * the Ender Dragon, to determine if a speedrun has been completed.</p>
 */
@AllArgsConstructor
@SuppressWarnings("unused")
public class WorldEventListener implements Listener {

    private final GameManager gameManager;

    /**
     * Handles {@link PlayerTeleportEvent} for determining when a run is completed.
     *
     * <p>This ensures that teleport events in the end fountain triggers run completion</p>
     *
     * @param event the player teleport event triggered in the speedrun end world
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World world = event.getFrom().getWorld();
        Block sourceBlock = event.getFrom().getBlock();

        // Ensure the event was in the speedrun end world
        Optional<Speedrunner> runner = gameManager.getSpeedrunner(player);
        if (runner.isEmpty() || !runner.get().getWorldSet().getEnd().getName().equals(world.getName())) return;

        // Cancel all teleport events in the speedrun end world
        event.setCancelled(true);

        // Ensure the event was fired from the end portal
        if (!sourceBlock.getType().equals(Material.END_PORTAL)) return;

        // Determine which run this player participates in
        Optional<Speedrun> run = gameManager.getActiveRun(player);
        if (run.isEmpty()) return; // Not in a speedrun

        Speedrun speedrun = run.get();

        // Only process if the run is actually running
        if (speedrun.getState() != Speedrun.State.RUNNING) return;

        gameManager.completeRun(speedrun, player);
    }
}
