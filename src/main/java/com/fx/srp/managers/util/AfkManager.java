package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Supplier;

public class AfkManager {

    private final ConfigHandler config = ConfigHandler.getInstance();
    private final SpeedRunPlus plugin;

    // Player tracking
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Set<UUID> warnedPlayers = new HashSet<>();

    private BukkitTask task;
    private boolean running;

    public AfkManager(SpeedRunPlus plugin) {
        this.plugin = plugin;
    }

    // Update the player's activity (location + timestamp)
    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocations.put(uuid, player.getLocation());
        lastActivity.put(uuid, System.currentTimeMillis());
        warnedPlayers.remove(uuid);
    }

    // Remove a player from tracking
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocations.remove(uuid);
        lastActivity.remove(uuid);
        warnedPlayers.remove(uuid);
    }

    // Start the scheduled AFK checker
    public void startAfkChecker(Supplier<Collection<Player>> activePlayersSupplier, AfkTimeoutHandler handler) {
        long interval = config.getAfkCheckInterval();
        running = true;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Player player : activePlayersSupplier.get()) {
                    if (!player.isOnline()) continue;

                    UUID uuid = player.getUniqueId();
                    Location lastLoc = lastLocations.get(uuid);
                    long lastTime = lastActivity.getOrDefault(uuid, now);

                    if (lastLoc == null || !player.getLocation().getWorld().equals(lastLoc.getWorld())) {
                        updateActivity(player);
                        continue;
                    }

                    double distance = player.getLocation().distance(lastLoc);
                    if (distance >= config.getAfkMinDistance()) {
                        updateActivity(player);
                        continue;
                    }

                    long timeAfk = now - lastTime;

                    // Warn 1 minute before timeout
                    if (timeAfk >= config.getAfkTimeout() - 60_000 && !warnedPlayers.contains(uuid)) {
                        warnedPlayers.add(uuid);
                        player.sendMessage(ChatColor.YELLOW + "You’ve been inactive. Run ends in 1 minute if AFK!");
                    }

                    // AFK timeout
                    if (timeAfk >= config.getAfkTimeout()) {
                        handler.onAfkTimeout(player);
                        remove(player);
                    }
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void stopAfkChecker() {
        if (!running) return;
        running = false;

        if (task != null) task.cancel();
    }

    // Callback interface to handle AFK timeout
    public interface AfkTimeoutHandler {
        void onAfkTimeout(Player player);
    }
}

