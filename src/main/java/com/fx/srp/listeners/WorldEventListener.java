package com.fx.srp.listeners;

import com.fx.srp.config.ConfigHandler;
import com.fx.srp.managers.GameManager;
import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.Speedrun;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Comparator;
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

    private final ConfigHandler configHandler = ConfigHandler.getInstance();

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

        // Ensure the event was fired from the end portal
        if (!sourceBlock.getType().equals(Material.END_PORTAL)) return;

        // Cancel the teleport event
        event.setCancelled(true);

        // Determine which run this player participates in
        Optional<Speedrun> run = gameManager.getActiveRun(player);
        if (run.isEmpty()) return; // Not in a speedrun

        Speedrun speedrun = run.get();

        // Only process if the run is actually running
        if (speedrun.getState() != Speedrun.State.RUNNING) return;

        gameManager.completeRun(speedrun, player);
    }

    /**
     * Handles {@link EntitySpawnEvent} for assisted triangulation.
     *
     * <p>Ensures that when an ender signal is spawned in the speedrun overworld, assisted triangulation is triggered.
     * </p>
     *
     * @param event the ender signal spawn event triggered by an ender eye throw in the speedrun overworld
     */
    @EventHandler
    public void onEyeThrow(EntitySpawnEvent event) {
        // Ensure the event is caused by an ender signal spawning
        if (!(event.getEntity() instanceof EnderSignal)) {
            return;
        }

        // Only if assisted triangulation is enabled
        if (!configHandler.isAssistedTriangulation()) return;

        // Ender signal info and flight data
        EnderSignal eye = (EnderSignal) event.getEntity();
        World world = eye.getWorld();
        Location spawnLocation = eye.getLocation();
        Location targetLocation = eye.getTargetLocation();

        // Infer the responsible player by finding the nearest player (within 1 block) to the EnderSignal spawn
        Player player = spawnLocation.getNearbyPlayers(1).stream()
                .min(Comparator.comparingDouble(p -> p.getLocation().distance(spawnLocation)))
                .orElse(null);

        // Ensure that the responsible player could be inferred
        if (player == null) return;

        // Determine which run this player participates in
        Optional<Speedrun> run = gameManager.getActiveRun(player);
        if (run.isEmpty()) return; // Not in a speedrun

        Speedrun speedrun = run.get();

        // Only process if the run is actually running
        if (speedrun.getState() != Speedrun.State.RUNNING) return;

        // Ensure the speedrunner is present
        Optional<Speedrunner> runner = gameManager.getSpeedrunner(player);
        if (runner.isEmpty()) return;

        Speedrunner speedrunner = runner.get();

        // Ensure the event was in their speedrun overworld
        if (!speedrunner.getWorldSet().getOverworld().getName().equals(world.getName())) return;

        // Build eye throw
        EyeThrow eyeThrow = new EyeThrow(player, spawnLocation, targetLocation, System.currentTimeMillis());

        // Trigger triangulation
        gameManager.assistedTriangulation(speedrunner, eyeThrow);
    }
}

