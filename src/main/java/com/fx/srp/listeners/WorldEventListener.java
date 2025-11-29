package com.fx.srp.listeners;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.run.AbstractSpeedrun;
import com.fx.srp.model.run.ISpeedrun;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

@SuppressWarnings("unused")
public class WorldEventListener implements Listener {

    private final GameManager gameManager;

    public WorldEventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Determine which run this player participates in
        Optional<ISpeedrun> run = gameManager.getActiveRun(killer);
        if (run.isEmpty()) return; // Not in a speedrun

        ISpeedrun speedrun = run.get();

        // Only process if the run is actually running
        if (speedrun.getState() != AbstractSpeedrun.State.RUNNING) return;

        // Trigger completion logic on the run manager
        gameManager.finishRun(speedrun, killer);
    }
}

