package com.fx.srp.listeners;

import com.fx.srp.commands.Action;
import com.fx.srp.commands.GameMode;
import com.fx.srp.managers.GameManager;
import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/**
 * Listener responsible for restricting command execution while a player
 * is participating in an active SpeedRunPlus run.
 *
 * <p>
 * This listener intercepts all player-issued commands at a high priority
 * and prevents execution of commands that are not explicitly allowed
 * during an active run.
 * </p>
 *
 * <p>
 * Administrators (players with the {@code srp.admin} permission) are
 * exempt from all restrictions enforced by this listener.
 * </p>
 */
@AllArgsConstructor
public class CommandListener implements Listener {

    private final GameManager gameManager;


    /**
     * Intercepts all player-issued commands and cancels execution if
     * the player is currently in an active run and the command is not
     * explicitly allowed.
     *
     * @param event the command preprocess event fired by Bukkit
     */
    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Admins bypass command restrictions
        if (player.hasPermission("srp.admin")) return;

        // Player must be in an active run
        if (!gameManager.isInRun(player)) return;

        // Partially parse the command
        String[] parts = event.getMessage().substring(1).split(" ", 4);

        // If not a valid SRP command allowed during runs, block it
        if (parts.length < 3 || !"srp".equalsIgnoreCase(parts[0]) || !isValid(parts[1], parts[2])) {
            block(event, player);
        }
    }

    private boolean isValid(String gameModePart, String actionPart) {
        try {
            GameMode gameMode = GameMode.valueOf(gameModePart.toUpperCase(Locale.ROOT));
            Action action = Action.valueOf(actionPart.toUpperCase(Locale.ROOT));
            return gameMode.isAllowedDuringRun(action);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void block(PlayerCommandPreprocessEvent event, Player player) {
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You cannot use this command during a run!");
    }
}
