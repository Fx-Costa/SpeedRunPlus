package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.commands.GameMode;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.requests.PendingRequest;
import com.fx.srp.model.run.Speedrun;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for managing multiplayer game mode logic in SRP.
 * <p>
 * This class extends {@link GameModeManager} and implements {@link IMultiplayer},
 * providing shared functionality for multiplayer-enabled game modes.
 * </p>
 *
 * <p>
 * Concrete subclasses are responsible for implementing game mode–specific
 * behavior on top of this shared infrastructure.
 * </p>
 *
 * @param <T> the specific type of {@link Speedrun} managed by this game mode
 */
public abstract class MultiplayerGameModeManager<T extends Speedrun>
        extends GameModeManager<T> implements IMultiplayer {

    // Track requests
    protected final Map<UUID, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Creates a new multiplayer game mode manager.
     * <p>
     * This constructor initializes the common dependencies required for
     * multiplayer game modes, including plugin access, game state management,
     * and world handling.
     * </p>
     *
     * @param plugin       the main plugin instance
     * @param gameManager  the central game manager responsible for active speedruns
     * @param worldManager the world manager responsible for speedrun worlds
     */
    public MultiplayerGameModeManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                       REQUEST
     * ========================================================== */
    /**
     * Sends a speedrun request from one player to another.
     *
     * <p>Sends messages to both players and schedules a timeout for the request.</p>
     *
     * @param sender the player initiating the request
     * @param target the player being requested
     */
    @Override
    public void request(Player sender, Player target, GameMode gameMode){
        // A player cannot challenge nobody
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "You must specify a player to target with!");
            return;
        }

        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        // A player cannot challenge themselves
        if (senderUUID.equals(targetUUID)) {
            sender.sendMessage(ChatColor.RED + "You cannot challenge yourself!");
            return;
        }

        // target is already in speedrun
        if (gameManager.isInRun(target)) {
            sender.sendMessage(
                    ChatColor.GRAY + target.getName() + " " +
                    ChatColor.YELLOW + " is already in a speedrun!"
            );
            return;
        }

        // target already has a pending request
        if (pendingRequests.containsKey(targetUUID)) {
            sender.sendMessage(
                    ChatColor.GRAY + target.getName() + " " +
                    ChatColor.YELLOW + " already has a pending request!"
            );
            return;
        }

        // sender has already requested within threshold
        if (pendingRequests.values().stream().anyMatch(req -> req.isByPlayer(senderUUID))) {
            sender.sendMessage(ChatColor.YELLOW + " You've already sent a request! Please wait.");
            return;
        }

        // Make the request
        PendingRequest request = new PendingRequest(senderUUID);
        pendingRequests.put(targetUUID, request);
        String gameModeName = gameMode.name().toLowerCase(Locale.ROOT);
        sender.sendMessage(
                ChatColor.YELLOW + "You’ve sent a request to " +
                ChatColor.GRAY + target.getName() +
                ChatColor.YELLOW + "!"
        );
        target.sendMessage(
                ChatColor.YELLOW + "You’ve been requested to a " + gameModeName + " run by " +
                ChatColor.GRAY + sender.getName() + ChatColor.YELLOW + "!"
        );
        target.sendMessage(
                ChatColor.YELLOW + "Use " +
                ChatColor.GRAY + "/srp " + gameModeName + " accept" +
                ChatColor.YELLOW + ", or " +
                ChatColor.GRAY + "/srp " + gameModeName + " decline" +
                ChatColor.YELLOW + "!"
        );

        // Schedule request timeout
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingRequests.remove(targetUUID);
            sender.sendMessage(
                    ChatColor.YELLOW + "Your request to " +
                    ChatColor.GRAY + target.getName() + " " +
                    ChatColor.YELLOW + "has expired!"
            );
            target.sendMessage(ChatColor.YELLOW + "The speedrun request has expired.");
        }, configHandler.getMaxRequestTime() / 50L).getTaskId();

        request.setTimeoutTaskId(taskId);
    }

    /* ==========================================================
     *                       ACCEPT
     * ========================================================== */
    /**
     * Accept a pending request.
     *
     * <p>Removes the pending request and cancels its timeout task.
     * Starts the run</p>
     *
     * @param target the player accepting the request
     */
    @Override
    public void accept(Player target){
        start(target);
    }

    /* ==========================================================
     *                       DECLINE
     * ========================================================== */
    /**
     * Declines a pending request.
     *
     * <p>Removes the pending request and cancels its timeout task.
     * Sends messages to both the partner and the leader.</p>
     *
     * @param target the player declining the request
     */
    @Override
    public void decline(Player target){
        UUID partnerUUID = target.getUniqueId();

        // If the partner has not received a request for a speedrun
        if (!pendingRequests.containsKey(partnerUUID)) {
            target.sendMessage(ChatColor.YELLOW + "You have no pending request!");
            return;
        }

        PendingRequest request = pendingRequests.remove(partnerUUID);
        int taskId = request.getTimeoutTaskId();
        if (taskId > 0) Bukkit.getScheduler().cancelTask(request.getTimeoutTaskId());

        // Decline the request
        Player leader = Bukkit.getPlayer(request.getPlayerUUID());
        target.sendMessage(ChatColor.YELLOW + "You’ve decline the request!");
        if (leader != null) {
            leader.sendMessage(
                    ChatColor.GRAY + target.getName() + " " +
                    ChatColor.YELLOW + "has declined your request!"
            );
        }
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    /**
     * Retrieves and validates the sender of a pending multiplayer request for the given target player.
     * <p>
     * This method performs several checks:
     * </p>
     * <ul>
     *   <li>Verifies that a pending request exists for the target player.</li>
     *   <li>Ensures the target player is not already participating in a speedrun.</li>
     *   <li>Checks that the sender is online and not currently in a speedrun.</li>
     *   <li>Cancels the request timeout task to prevent it from expiring after processing.</li>
     * </ul>
     * <p>
     * If any validation fails, a message is sent to the target player explaining the issue,
     * and {@code null} is returned. Otherwise, the sender {@link Player} is returned.
     * </p>
     *
     * @param target the player receiving the multiplayer request
     * @return the sender {@link Player} who initiated the request, or {@code null} if the request
     *         is invalid, expired, or the sender is unavailable
     */
    protected Player getRequestSender(Player target){
        // Get the request
        PendingRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null || request.getTimeoutTaskId() <= 0) {
            target.sendMessage(ChatColor.YELLOW + "You have no pending request!");
            return null;
        }

        // If already in a speedrun
        if (gameManager.isInRun(target)) {
            target.sendMessage(ChatColor.RED + "You are already in a speedrun!");
            return null;
        }

        // Get the sender from the request
        Player sender = Bukkit.getPlayer(request.getPlayerUUID());
        if (sender == null || !sender.isOnline() || gameManager.isInRun(sender)) {
            target.sendMessage(ChatColor.YELLOW + "The other player is no longer available!");
            return null;
        }

        // Cancel request timeout task
        Bukkit.getScheduler().cancelTask(request.getTimeoutTaskId());

        return sender;
    }
}