package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.util.time.TimeFormatter;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final Logger logger = Bukkit.getLogger();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final SpeedRunPlus plugin;

    private final File dataFile;
    private final List<RunEntry> leaderboard = new ArrayList<>();
    private final List<PodiumEntry> currentPodium = new ArrayList<>();

    public LeaderboardManager(SpeedRunPlus plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        loadLeaderboard();
        updatePodium();
    }

    public static class RunEntry {
        public String playerName;
        public UUID playerUUID;
        public long time; // milliseconds

        public RunEntry(String playerName, UUID playerUUID, long time) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.time = time;
        }
    }

    private static class PodiumEntry {
        ArmorStand headStand;
        ArmorStand timeStand;
    }

    public void loadLeaderboard() {
        // Cannot load nor create leaderboard directories / file
        if (!createLeaderboardFileIfNotPresent(dataFile)) return;

        try {
            List<String> lines = Files.readAllLines(dataFile.toPath());
            leaderboard.clear();
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    leaderboard.add(new RunEntry(parts[0], UUID.fromString(parts[1]), Long.parseLong(parts[2])));
                }
            }
            sortLeaderboard();
        }
        catch (IOException e) {
            this.logger.warning("[SRP] Error while trying to load leaderboard: " + e.getMessage());
        }
    }

    public void saveLeaderboard() {
        List<String> lines = leaderboard.stream()
                .map(e -> e.playerName + "," + e.playerUUID + "," + e.time)
                .collect(Collectors.toList());
        try {
            Files.write(dataFile.toPath(), lines);
        } catch (IOException e) {
            this.logger.warning("[SRP] Error while saving leaderboard: " + e.getMessage());
        }
    }

    public void finishRun(Player player, long time) {
        leaderboard.add(new RunEntry(player.getName(), player.getUniqueId(), time));
        sortLeaderboard();
        saveLeaderboard();
        updatePodium();
    }

    public void updatePodium() {
        if (leaderboard.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {

            // Remove old Armor Stands
            for (PodiumEntry entry : currentPodium) {
                if (entry.headStand != null && !entry.headStand.isDead()) entry.headStand.remove();
                if (entry.timeStand != null && !entry.timeStand.isDead()) entry.timeStand.remove();
            }
            currentPodium.clear();

            // The positions of the podium in the world
            World world = configHandler.getPodiumWorld();
            List<Location> podiumLocations = new ArrayList<>(configHandler.getPodiumPositions().values());

            for (int i = 0; i < Math.min(leaderboard.size(), podiumLocations.size()); i++) {
                RunEntry entry = leaderboard.get(i);
                long milliseconds = entry.time;

                // Locations for placing names, times relative to the heads
                Location baseLoc = podiumLocations.get(i);
                Location headLoc = baseLoc.clone().add(0, 0.5, 0);
                Location nameLoc = headLoc.clone().add(0, 2, 0);    // slightly above the head
                Location timeLoc = headLoc.clone().add(0, 0.85, 0); // slightly below the head

                // Create player head item
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.playerUUID);
                if (offline.hasPlayedBefore() || offline.isOnline()) {
                    skullMeta.setOwningPlayer(offline);
                } else {
                    skullMeta.setOwningPlayer(null);
                }
                head.setItemMeta(skullMeta);

                // An armor stand for the Player's head
                ArmorStand headStand = world.spawn(headLoc, ArmorStand.class);
                headStand.setVisible(false);
                headStand.setGravity(false);
                headStand.setMarker(true);
                headStand.setInvulnerable(true);
                headStand.setRotation(180f, 0f);
                EntityEquipment equipment = headStand.getEquipment();
                if (equipment != null) equipment.setHelmet(head);
                headStand.addScoreboardTag("spr_podium_head");

                // An armor stand for the Player's name
                ArmorStand nameStand = world.spawn(nameLoc, ArmorStand.class);
                nameStand.setVisible(false);
                nameStand.setInvulnerable(true);
                nameStand.setCustomName(entry.playerName);
                nameStand.setCustomNameVisible(true);
                nameStand.setGravity(false);
                nameStand.setMarker(true);
                nameStand.addScoreboardTag("spr_podium_name");
                nameStand.setRotation(180f, 0f);

                // An armor stand for the Player's time
                ArmorStand timeStand = world.spawn(timeLoc, ArmorStand.class);
                timeStand.setVisible(false);
                timeStand.setInvulnerable(true);
                timeStand.setCustomName(new TimeFormatter(milliseconds).includeHours().useSuffixes().format());
                timeStand.setCustomNameVisible(true);
                timeStand.setGravity(false);
                timeStand.setMarker(true);
                timeStand.addScoreboardTag("spr_podium_time");

                // Add to the podium
                PodiumEntry podiumEntry = new PodiumEntry();
                podiumEntry.headStand = headStand;
                podiumEntry.timeStand = timeStand;
                currentPodium.add(podiumEntry);
            }
        });
    }

    private void sortLeaderboard() {
        leaderboard.sort(Comparator.comparingLong(e -> e.time));
        if (leaderboard.size() > 10) {
            leaderboard.subList(10, leaderboard.size()).clear();
        }
    }

    private boolean createLeaderboardFileIfNotPresent(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warning("[SRP] Failed to create directories for leaderboard: " + parent.getAbsolutePath());
                return false;
            }

            if (!file.exists() && !file.createNewFile()) {
                logger.warning("[SRP] Failed to create file for leaderboard: " + file.getAbsolutePath());
                return false;
            }

            return true;
        } catch (IOException e) {
            logger.warning("[SRP] IOException while creating file: " + e.getMessage());
            return false;
        }
    }
}
