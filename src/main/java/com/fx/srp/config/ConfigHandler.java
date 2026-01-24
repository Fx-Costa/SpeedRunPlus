package com.fx.srp.config;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.model.seed.SeedCategory;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Handles loading and providing access to the configuration for the SpeedRunPlus plugin.
 *
 * <p>This class reads values from the plugin's {@code config.yml} and converts
 * them into runtime-friendly formats (e.g., {@link World} objects, {@link Location}s,
 * milliseconds for timers).</p>
 *
 * <p>Provides a singleton instance accessible via {@link #getInstance()} ()}.</p>
 */
public final class ConfigHandler {

    private final Logger logger = Bukkit.getLogger();
    private final SpeedRunPlus plugin;
    private FileConfiguration config;

    private static final ConfigHandler INSTANCE = new ConfigHandler(SpeedRunPlus.getPlugin(SpeedRunPlus.class));

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
    @Getter private int leaderboardMaxEntries;
    @Getter private String podiumWorldName;
    @Getter private World podiumWorld;
    @Getter private final Map<String, Location> podiumPositions = new ConcurrentHashMap<>();

    // Game rules
    @Getter private int maxPlayers;
    @Getter private long maxRunTime;
    @Getter private long maxRequestTime;
    @Getter private boolean assistedTriangulation;
    @Getter private String assistedTriangulationStrategy;
    @Getter @Setter private boolean filteredSeeds;
    private Map<SeedCategory.SeedType, Integer> seedWeights;
    @Getter private URL filteredSeedsApi;

    private ConfigHandler(SpeedRunPlus plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }

    public static ConfigHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Get a configured weight for a given type of seed.
     *
     * @param seedType     The type of seed to get the weight for.
     */
    public int getSeedWeight(SeedCategory.SeedType seedType) {
        if (!filteredSeeds) return 0;
        return seedWeights.getOrDefault(seedType, 0);
    }

    private void loadConfiguration() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        loadWorldSettings();
        loadTimerSettings();
        loadAFKSettings();
        loadPodiumSettings();
        loadGameRules();
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
        leaderboardMaxEntries = config.getInt("podium.max", 10);
        podiumWorld = podiumWorldName != null ? Bukkit.getWorld(podiumWorldName) : mainOverworld;
        podiumPositions.clear();
        ConfigurationSection podiumSection = config.getConfigurationSection("podium.positions");
        if (podiumSection != null && !podiumSection.getKeys(false).isEmpty()) {
            podiumSection.getKeys(false).forEach(key -> {
                String path = "podium.positions." + key;
                double xCoordinate = config.getDouble(path + ".x");
                double yCoordinate = config.getDouble(path + ".y");
                double zCoordinate = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw", 0);
                podiumPositions.put(key, new Location(
                        podiumWorld,
                        xCoordinate,
                        yCoordinate,
                        zCoordinate,
                        yaw,
                        0
                ));
            });
        }
    }

    private void loadGameRules() {
        maxPlayers = config.getInt("game-rules.max-players", 4);
        maxRunTime = config.getLong("game-rules.max-time-minutes", 30) * 60 * 1000;
        maxRequestTime = config.getLong("game-rules.max-request-seconds", 30) * 1000;
        filteredSeeds = config.getBoolean("game-rules.filtered-seeds.use-filtered-seeds", false);
        assistedTriangulation = config.getBoolean(
                "game-rules.assisted-triangulation.use-assisted-triangulation",
                false
        );
        assistedTriangulationStrategy = config.getString(
                "game-rules.assisted-triangulation.strategy",
                "DETERMINISTIC"
        );

        // Seed weights
        seedWeights = new ConcurrentHashMap<>();
        ConfigurationSection weightSection = config.getConfigurationSection("game-rules.filtered-seeds.weights");
        if (filteredSeeds && weightSection != null) {
            Arrays.stream(SeedCategory.SeedType.values()).forEach(seedType -> seedWeights.put(
                    seedType,
                    weightSection.getInt(seedType.name(), 0)
            ));
        }

        // FSGs Practice seeds API
        String fsgApi = config.getString("game-rules.filtered-seeds.filtered-seeds-api");
        if (fsgApi != null) {
            try {
                filteredSeedsApi = new URL(fsgApi);
            } catch (MalformedURLException e) {
                logger.warning("[SRP] Invalid Filtered Seeds API URL");
            }
        }
    }
}

