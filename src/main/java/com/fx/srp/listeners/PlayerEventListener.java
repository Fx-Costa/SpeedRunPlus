package com.fx.srp.listeners;

import com.fx.srp.managers.GameManager;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player-related events and delegates handling to {@link GameManager}.
 *
 * <p>This listener captures events relevant to SRP gameplay, including movement,
 * interaction, respawn, and quit events.</p>
 */
@AllArgsConstructor
@SuppressWarnings("unused")
public class PlayerEventListener implements Listener {

    private final GameManager gameManager;

    /**
     * Handles {@link PlayerMoveEvent}.
     *
     * @param event the movement event triggered by a player
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        gameManager.handlePlayerMove(event.getPlayer(), event);
    }

    /**
     * Handles {@link PlayerInteractEvent}.
     *
     * @param event the interaction event triggered by a player
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        gameManager.handlePlayerInteract(event.getPlayer(), event);
    }

    /**
     * Handles {@link PlayerRespawnEvent}.
     *
     * @param event the respawn event triggered for a player
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        gameManager.handlePlayerRespawn(event);
    }

    /**
     * Handles {@link PlayerQuitEvent}.
     *
     * @param event the quit event triggered when a player leaves the server
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event.getPlayer());
    }
}

