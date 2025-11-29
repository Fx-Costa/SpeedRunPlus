package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages AFK (Away From Keyboard) detection and handling for players during speedruns.
 *
 * <p>This manager tracks player movement and activity, warns players before timeout,
 * and triggers a callback when a player exceeds the AFK threshold.</p>
 */
public class AfkManager {

    private final ConfigHandler config = ConfigHandler.getInstance();
    private final SpeedRunPlus plugin;

    // Player tracking
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> warnedPlayers = new HashSet<>();

    private BukkitTask task;
    private boolean running;

    /**
     * Constructs a new {@link AfkManager}.
     *
     * @param plugin the main {@link SpeedRunPlus} plugin instance
     */
    public AfkManager(SpeedRunPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the activity of a player by recording their current location and timestamp.
     *
     * <p>Also clears any prior AFK warnings for the player.</p>
     *
     * @param player the player whose activity is being updated
     */
    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocations.put(uuid, player.getLocation());
        lastActivity.put(uuid, System.currentTimeMillis());
        warnedPlayers.remove(uuid);
    }

    /**
     * Removes a player from AFK tracking.
     *
     * <p>This stops tracking their location, activity time, and warnings.</p>
     *
     * @param player the player to remove
     */
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocations.remove(uuid);
        lastActivity.remove(uuid);
        warnedPlayers.remove(uuid);
    }

    /**
     * Starts the scheduled AFK checker task.
     *
     * <p>The checker periodically evaluates the player's last location and activity timestamp.
     * Players who remain inactive beyond the configured threshold are warned and then
     * processed via {@link AfkTimeoutHandler}.</p>
     *
     * @param activePlayersSupplier a supplier that provides a collection of active players to monitor
     * @param handler the callback handler invoked when a player reaches the AFK timeout
     */
    public void startAfkChecker(Supplier<Collection<Player>> activePlayersSupplier, AfkTimeoutHandler handler) {
        long interval = config.getAfkCheckInterval();
        running = true;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Player player : activePlayersSupplier.get()) {
                    processPlayer(player, now, handler);
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    /**
     * Stops the scheduled AFK checker task.
     *
     * <p>No further AFK checks or warnings will be issued until restarted.</p>
     */
    public void stopAfkChecker() {
        if (!running) return;
        running = false;

        if (task != null) task.cancel();
    }

    /**
     * Callback interface for handling AFK timeouts.
     *
     * <p>Implement this interface to define custom behavior when a player has been
     * inactive for longer than the configured AFK timeout.</p>
     */
    @FunctionalInterface
    public interface AfkTimeoutHandler {

        /**
         * Called when a player exceeds the AFK timeout.
         *
         * @param player the player who is considered AFK
         */
        void onAfkTimeout(Player player);
    }

    /* ==========================================================
     *                      Helpers
     * ========================================================== */
    private void processPlayer(Player player, long now, AfkTimeoutHandler handler) {
        if (!player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Location lastLoc = lastLocations.get(uuid);
        long lastTime = lastActivity.getOrDefault(uuid, now);
        Location currentLoc = player.getLocation();

        if (shouldUpdateLocation(lastLoc, currentLoc)) {
            updateActivity(player);
            return;
        }

        if (isPlayerMoving(lastLoc, currentLoc)) {
            updateActivity(player);
            return;
        }

        long timeAfk = now - lastTime;

        handlePossibleWarning(player, uuid, timeAfk);
        handlePossibleTimeout(player, timeAfk, handler);
    }

    private boolean shouldUpdateLocation(Location lastLoc, Location currentLoc) {
        return lastLoc == null || currentLoc.getWorld() != lastLoc.getWorld();
    }

    private boolean isPlayerMoving(Location lastLoc, Location currentLoc) {
        return currentLoc.distance(lastLoc) >= config.getAfkMinDistance();
    }

    private void handlePossibleWarning(Player player, UUID uuid, long timeAfk) {
        long warnTime = config.getAfkTimeout() - 60_000;

        if (timeAfk >= warnTime && !warnedPlayers.contains(uuid)) {
            warnedPlayers.add(uuid);
            player.sendMessage(ChatColor.YELLOW + "You’ve been inactive. Run ends in 1 minute if AFK!");
        }
    }

    private void handlePossibleTimeout(Player player, long timeAfk, AfkTimeoutHandler handler) {
        if (timeAfk >= config.getAfkTimeout()) {
            handler.onAfkTimeout(player);
            remove(player);
        }
    }
}

