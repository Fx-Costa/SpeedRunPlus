package com.fx.srp.listeners;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.run.Speedrun;
import lombok.AllArgsConstructor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

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
     * Handles {@link EntityDeathEvent} for the Ender Dragon.
     *
     * <p>This allows speedruns that depend on killing the Ender Dragon to be
     * automatically completed when the dragon dies.</p>
     *
     * @param event the entity death event triggered in the world
     */
    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Determine which run this player participates in
        Optional<Speedrun> run = gameManager.getActiveRun(killer);
        if (run.isEmpty()) return; // Not in a speedrun

        Speedrun speedrun = run.get();

        // Only process if the run is actually running
        if (speedrun.getState() != Speedrun.State.RUNNING) return;

        // Trigger completion logic on the run manager
        gameManager.completeRun(speedrun, killer);
    }
}

