package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.util.time.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
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

/**
 * Manages the speedrun leaderboard, including persistent storage and visual podium display.
 *
 * <p>This manager tracks completed runs, stores them in a file, sorts them by completion
 * time, and updates an in-game podium using Armor Stands to display player heads and times.</p>
 *
 * <p>The leaderboard stores a maximum of 10 entries and automatically updates the
 * podium positions in the configured world.</p>
 */
public class LeaderboardManager {

    private final Logger logger = Bukkit.getLogger();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final SpeedRunPlus plugin;

    private final File dataFile;
    private final List<RunEntry> leaderboard = new ArrayList<>();
    private final List<PodiumEntry> currentPodium = new ArrayList<>();

    /**
     * Represents a completed run entry for the leaderboard.
     */
    public static class RunEntry {
        public String playerName;
        public UUID playerUUID;
        public long time; // milliseconds

        /**
         * Constructs a new {@code RunEntry}.
         *
         * @param playerName the name of the player
         * @param playerUUID the UUID of the player
         * @param time       the completion time in milliseconds
         */
        public RunEntry(String playerName, UUID playerUUID, long time) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.time = time;
        }
    }

    /**
     * Represents a podium entry on the leaderboard.
     * <p>
     * Each entry contains the armor stands for displaying the player's head and their run time in the world.
     */
    private static final class PodiumEntry {
        private ArmorStand headStand;
        private ArmorStand nameStand;
        private ArmorStand timeStand;
    }

    /**
     * Constructs a LeaderboardManager and loads the existing leaderboard.
     *
     * @param plugin the main plugin instance
     */
    public LeaderboardManager(SpeedRunPlus plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        loadLeaderboard();
        updatePodium();
    }

    /**
     * Records a finished run for a player.
     *
     * @param player the player completing the run
     * @param time   the completion time in milliseconds
     */
    public void finishRun(Player player, long time) {
        leaderboard.add(new RunEntry(player.getName(), player.getUniqueId(), time));
        sortLeaderboard();
        saveLeaderboard();
        updatePodium();
    }

    /**
     * Unload the podium by destroying the armor stands
     */
    public void unloadPodium() {
        clearPodium();
    }

    /**
     * Load the podium re-creating armor stands from the leaderboard file
     */
    public void loadPodium() {
        loadLeaderboard();
        updatePodium();
    }

    /* ==========================================================
     *                      Helpers
     * ========================================================== */
    private void loadLeaderboard() {
        // Cannot load nor create leaderboard directories / file
        if (!createLeaderboardFileIfNotPresent(dataFile)) return;

        try {
            List<String> lines = Files.readAllLines(dataFile.toPath());
            leaderboard.clear();
            for (String line : lines) {
                String[] parts = line.split(",");
                int expectedSize = 3;
                if (parts.length == expectedSize) {
                    leaderboard.add(new RunEntry(parts[0], UUID.fromString(parts[1]), Long.parseLong(parts[2])));
                }
            }
            sortLeaderboard();
        }
        catch (IOException e) {
            this.logger.warning("[SRP] Error while trying to load leaderboard: " + e.getMessage());
        }
    }

    private void saveLeaderboard() {
        List<String> lines = leaderboard.stream()
                .map(e -> e.playerName + "," + e.playerUUID + "," + e.time)
                .collect(Collectors.toList());
        try {
            Files.write(dataFile.toPath(), lines);
        } catch (IOException e) {
            this.logger.warning("[SRP] Error while saving leaderboard: " + e.getMessage());
        }
    }

    private void updatePodium() {
        if (leaderboard.isEmpty()) {
            return;
        }

        clearPodium();
        List<Location> locations = new ArrayList<>(configHandler.getPodiumPositions().values());
        World world = configHandler.getPodiumWorld();

        Bukkit.getScheduler().runTask(plugin, () -> {
            int count = Math.min(leaderboard.size(), locations.size());
            for (int i = 0; i < count; i++) {
                createPodiumEntry(leaderboard.get(i), locations.get(i), world);
            }
        });
    }

    private void sortLeaderboard() {
        leaderboard.sort(Comparator.comparingLong(e -> e.time));
        if (leaderboard.size() > configHandler.getLeaderboardMaxEntries()) {
            leaderboard.subList(configHandler.getLeaderboardMaxEntries(), leaderboard.size()).clear();
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

    private void clearPodium() {
        for (PodiumEntry entry : currentPodium) {
            removeIfExists(entry.headStand);
            removeIfExists(entry.nameStand);
            removeIfExists(entry.timeStand);
        }
        currentPodium.clear();
    }

    private void removeIfExists(Entity entity) {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    private void createPodiumEntry(RunEntry run, Location baseLoc, World world) {
        Location headLoc = baseLoc.clone().add(0, 0.5, 0);
        Location nameLoc = headLoc.clone().add(0, 2, 0);
        Location timeLoc = headLoc.clone().add(0, 0.85, 0);

        ItemStack headItem = createHeadItem(run.playerUUID);

        ArmorStand headStand = createHeadStand(world, headLoc, headItem);
        ArmorStand nameStand = createNameStand(world, nameLoc, run.playerName);
        ArmorStand timeStand = createTimeStand(world, timeLoc, run.time);


        PodiumEntry entry = new PodiumEntry();
        entry.headStand = headStand;
        entry.nameStand = nameStand;
        entry.timeStand = timeStand;
        currentPodium.add(entry);
    }

    private ItemStack createHeadItem(UUID uuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.hasPlayedBefore() || player.isOnline()) {
            meta.setOwningPlayer(player);
        } else {
            meta.setOwningPlayer(null);
        }

        head.setItemMeta(meta);
        return head;
    }

    private ArmorStand createHeadStand(World world, Location loc, ItemStack head) {
        ArmorStand stand = spawnBaseStand(world, loc);
        stand.setRotation(180f, 0f);

        EntityEquipment equipment = stand.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(head);
        }

        stand.addScoreboardTag("spr_podium_head");
        return stand;
    }

    private ArmorStand createNameStand(World world, Location loc, String name) {
        ArmorStand stand = spawnBaseStand(world, loc);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.addScoreboardTag("spr_podium_name");
        stand.setRotation(180f, 0f);
        return stand;
    }

    private ArmorStand createTimeStand(World world, Location loc, long milliseconds) {
        ArmorStand stand = spawnBaseStand(world, loc);
        stand.setCustomName(
                new TimeFormatter(milliseconds).withHours().withSuffixes().format()
        );
        stand.setCustomNameVisible(true);
        stand.addScoreboardTag("spr_podium_time");
        return stand;
    }

    private ArmorStand spawnBaseStand(World world, Location loc) {
        ArmorStand armorStand = world.spawn(loc, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setInvulnerable(true);
        return armorStand;
    }
}
