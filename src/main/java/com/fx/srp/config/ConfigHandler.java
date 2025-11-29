package com.fx.srp.config;

import com.fx.srp.SpeedRunPlus;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigHandler {

    private final Logger logger = Bukkit.getLogger();
    private final SpeedRunPlus plugin;
    private FileConfiguration config;

    @Getter private static ConfigHandler instance;

    // World settings
    @Getter private String mainOverworldName;
    @Getter private World mainOverworld;
    @Getter private String mainNetherName;
    @Getter private World mainNether;
    @Getter private String mainEndName;
    @Getter private World mainEnd;
    @Getter private String overworldPrefix;
    @Getter private String netherPrefix;
    @Getter private String endPrefix;

    // Timer settings
    @Getter private int timerCountdown;

    // AFK settings
    @Getter private long afkTimeout;
    @Getter private long afkCheckInterval;
    @Getter private double afkMinDistance;

    // Podium settings
    @Getter private String podiumWorldName;
    @Getter private World podiumWorld;
    @Getter private final Map<String, Location> podiumPositions = new HashMap<>();

    // Game rules
    @Getter private int maxPlayers;
    @Getter private long maxRunTime;
    @Getter private long maxRequestTime;

    public ConfigHandler(SpeedRunPlus plugin) {
        this.plugin = plugin;
        instance = this;
        loadConfiguration();
    }

    private void loadConfiguration() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        loadWorldSettings();
        loadTimerSettings();
        loadAFKSettings();
        loadPodiumSettings();
        loadGameRules();
        logger.info("[SRP] Configuration file loaded!");
    }

    private void loadWorldSettings() {
        mainOverworldName = config.getString("main-overworld", "world");
        mainOverworld = mainOverworldName == null ? Bukkit.getWorld("world") : Bukkit.getWorld(mainOverworldName);
        mainNetherName = config.getString("main-nether", "world_nether");
        mainNether = mainNetherName == null ? Bukkit.getWorld("world_nether") : Bukkit.getWorld(mainNetherName);
        mainEndName = config.getString("main-end", "world_the_end");
        mainEnd = mainEndName == null ? Bukkit.getWorld("world_the_end") : Bukkit.getWorld(mainEndName);

        overworldPrefix = config.getString("world-prefix.overworld", "srp-overworld-");
        netherPrefix = config.getString("world-prefix.nether", "srp-nether-");
        endPrefix = config.getString("world-prefix.end", "srp-end-");
        podiumWorldName = config.getString("podium.world", mainOverworldName);
    }

    private void loadTimerSettings() {
        timerCountdown = config.getInt("timer.countdown-seconds", 10);
    }

    private void loadAFKSettings() {
        long timeoutMinutes = config.getLong("afk.timeout-minutes", 5);
        long checkIntervalSeconds = config.getLong("afk.check-interval-seconds", 60);
        afkTimeout = timeoutMinutes * 60 * 1000;
        afkCheckInterval = checkIntervalSeconds * 20L;
        afkMinDistance = config.getDouble("afk.min-distance", 1.0);
    }

    private void loadPodiumSettings() {
        podiumWorld = podiumWorldName != null ? Bukkit.getWorld(podiumWorldName) : mainOverworld;
        podiumPositions.clear();
        ConfigurationSection podiumSection = config.getConfigurationSection("podium.positions");
        if (podiumSection != null && !podiumSection.getKeys(false).isEmpty()) {
            podiumSection.getKeys(false).forEach(key -> {
                String path = "podium.positions." + key;
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw", 0);
                podiumPositions.put(key, new Location(podiumWorld, x, y, z, yaw, 0));
            });
        }
    }

    private void loadGameRules() {
        maxPlayers = config.getInt("game-rules.max-players", 4);
        maxRunTime = config.getLong("game-rules.max-run-time-minutes", 30) * 60 * 1000;
        maxRequestTime = config.getLong("game-rules.max-request-seconds", 30) * 1000;
    }
}

