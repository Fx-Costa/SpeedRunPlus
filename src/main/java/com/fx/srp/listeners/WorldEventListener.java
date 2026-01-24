package com.fx.srp.listeners;

import com.fx.srp.config.ConfigHandler;
import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.Speedrun;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
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

    /**
     * Handles {@link ProjectileLaunchEvent} for assisted triangulation.
     *
     * <p>Ensures that when ender eyes are thrown in the speedrun overworld, assisted triangulation is triggered</p>
     *
     * @param event the projectile launch event triggered by an ender eye throw in the speedrun overworld     */
    @EventHandler
    public void onEyeThrow(ProjectileLaunchEvent event) {
        // Only if assisted triangulation is enabled
        if (!configHandler.isAssistedTriangulation()) return;

        // Ensure the event is caused by an eye of ender being thrown
        if (!(event.getEntity() instanceof EnderSignal)) return;

        // Ensure the event is caused by a player
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) return;

        World world = event.getEntity().getWorld();
        Player player = (Player) projectile.getShooter();

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

        // Ensure the event was in the speedrun overworld
        if (speedrunner.getWorldSet().getOverworld().getName().equals(world.getName())) return;

        // Trigger triangulation
        gameManager.assistedTriangulation(speedrunner, projectile);
    }
}

