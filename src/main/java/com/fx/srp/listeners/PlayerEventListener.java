package com.fx.srp.listeners;

import com.fx.srp.managers.GameManager;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;


@AllArgsConstructor
@SuppressWarnings("unused")
public class PlayerEventListener implements Listener {

    private final GameManager gameManager;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        gameManager.handlePlayerMove(event.getPlayer(), event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        gameManager.handlePlayerInteract(event.getPlayer(), event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        gameManager.handlePlayerRespawn(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event.getPlayer());
    }
}

