package com.fx.srp;

import com.fx.srp.commands.Commands;
import com.fx.srp.commands.Subcommands;
import com.fx.srp.listeners.*;
import com.fx.srp.managers.LeaderboardManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;

import java.util.*;
import java.util.logging.Logger;

import com.fx.srp.commands.CommandInterceptor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SpeedRunPlus extends JavaPlugin {

    @Getter @NonNull private World mainWorld;

    @Getter @NonNull private World podiumWorld;

    public LeaderboardManager leaderboardManager;

    private final Logger logger = Bukkit.getLogger();

    // Keep track of players that are frozen
    @Getter private final Set<UUID> frozenPlayers = new HashSet<>();

    // Keep track of active players and time of last movements (to identify AFKs)
    @Getter private final Map<UUID, Long> activePlayers = new HashMap<>();

    // Keep track of players last movements (to identify AFKs)
    @Getter private final Map<UUID, Location> playerLocations = new HashMap<>();

    // Keep track of players' time
    @Getter private final Map<UUID, StopWatch> playerStopWatches = new HashMap<>();

    // Keep track of runs in progress, scheduled for ending in MAX_RUN_TIME
    @Getter protected final Map<UUID, BukkitTask> scheduledShutdownTasks = new HashMap<>();

    // Keep track of players inventory
    @Getter private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    // Keep track of players armor
    @Getter private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    // Keep track of players armor
    @Getter private final Map<UUID, Map<Advancement, Set<String>>> savedAdvancements = new HashMap<>();

    // Configurations
    @Getter private String MAIN_WORLD_NAME;
    @Getter private String SRP_OVERWORLD_PREFIX;
    @Getter private String SRP_NETHER_PREFIX;
    @Getter private String SRP_END_PREFIX;
    @Getter private String PODIUM_WORLD_NAME;
    @Getter private final Map<String, Location> PODIUM_POSITIONS = new HashMap<>();
    @Getter private int TIMER_COUNTDOWN;
    @Getter private long AFK_TIMEOUT;
    @Getter private long AFK_CHECK_INTERVAL;
    @Getter private double AFK_MIN_DISTANCE;
    @Getter private int MAX_PLAYERS;
    @Getter private long MAX_RUN_TIME;

    public void onEnable() {
        // Initialize and verify that Multiverse & Multiverse-NetherPortals is present
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager()
                .getPlugin("Multiverse-Core");
        MultiverseNetherPortals netherPortals = (MultiverseNetherPortals) Bukkit.getServer().getPluginManager()
                .getPlugin("Multiverse-NetherPortals");
        if (core == null || !checkPrerequisites(core, netherPortals)){
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load configurations
        saveDefaultConfig();
        loadConfiguration();

        // Set the main world
        World world = Bukkit.getWorld(MAIN_WORLD_NAME);
        if (world == null){
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        mainWorld = world;

        // World management
        MVWorldManager worldManager = core.getMVWorldManager();
        worldManager.unloadWorld("world_nether");
        worldManager.unloadWorld("world_the_end");

        // Command interceptor - act on SPR commands
        CommandInterceptor commandInterceptor = new CommandInterceptor(core, netherPortals);
        Objects.requireNonNull(getCommand(Commands.SRP.getCommand())).setExecutor(commandInterceptor);

        // Register an event listeners - act on certain events
        getServer().getPluginManager().registerEvents(new PlayerListener(core), this);
        getServer().getPluginManager().registerEvents(new WorldListener(core), this);
        getServer().getPluginManager().registerEvents(new CommandListener(), this);

        // Ensure that the podium (leaderboard) is reloaded from the leaderboard.yml file
        // Delay it slightly, ensuring that the world is loaded
        leaderboardManager = new LeaderboardManager(this);
        Bukkit.getScheduler().runTask(this, () -> {
            resetState();
            leaderboardManager.updatePodium();
        });

        logger.info("[SRP] The plugin has started successfully!");
    }

    public void onDisable() {
        resetState();
        logger.info("[SRP] The plugin has stopped successfully!");
    }

    private void resetState(){
        // Ensure that no players are frozen and reset AFK tracking
        frozenPlayers.clear();
        activePlayers.clear();
        playerLocations.clear();
        savedInventories.clear();
        savedArmor.clear();

        // Save changes to the leaderboard
        leaderboardManager.saveLeaderboard();

        // Clear podium
        for (Entity entity : podiumWorld.getEntitiesByClass(ArmorStand.class)) {
            Set<String> tags = entity.getScoreboardTags();
            if (tags.contains("spr_podium_head") ||
                    tags.contains("spr_podium_name") ||
                    tags.contains("spr_podium_time")
            ) entity.remove();
        }
    }

    private boolean checkPrerequisites(MultiverseCore core, MultiverseNetherPortals netherPortals) {
        // Multiverse-NetherPortals is a prerequisite and must be of protocol version 24 or later
        if (core == null) {
            logger.severe("[SRP] Multiverse-Core not installed!");
            return false;
        }
        if (core.getProtocolVersion() < 24) {
            logger.severe(String.format("[SRP] Multiverse-Core is out of date! Using protocol: %d," +
                    " must be: %d or later.", core.getProtocolVersion(), 24)
            );
            return false;
        }

        // Multiverse-NetherPortals is also a prerequisite
        if (netherPortals == null) {
            logger.severe("[SRP] Multiverse-NetherPortals not installed!");
            return false;
        }

        return true;
    }

    private void loadConfiguration() {
        reloadConfig();

        // WORLD settings
        MAIN_WORLD_NAME = getConfig().getString("main-world", "world");
        SRP_OVERWORLD_PREFIX = getConfig().getString("world-prefix.overworld", "srp-overworld-");
        SRP_NETHER_PREFIX = getConfig().getString("world-prefix.nether", "srp-nether-");
        SRP_END_PREFIX = getConfig().getString("world-prefix.end", "srp-end-");

        // Timer settings
        TIMER_COUNTDOWN = getConfig().getInt("timer.countdown-seconds", 10);

        // AFK settings
        long timeoutMinutes = getConfig().getLong("afk.timeout-minutes", 5);
        long checkIntervalSeconds = getConfig().getLong("afk.check-interval-seconds", 60);
        AFK_TIMEOUT = timeoutMinutes * 60 * 1000;
        AFK_CHECK_INTERVAL = checkIntervalSeconds * 20L;
        AFK_MIN_DISTANCE = getConfig().getDouble("afk.min-distance", 1.0);

        // PODIUM settings
        PODIUM_WORLD_NAME = getConfig().getString("podium.world", MAIN_WORLD_NAME);
        World world = PODIUM_WORLD_NAME != null ? Bukkit.getWorld(PODIUM_WORLD_NAME) : null;
        if (world == null) {
            getLogger().warning("[SRP] PODIUM_WORLD not found: " + PODIUM_WORLD_NAME);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        podiumWorld = world;
        PODIUM_POSITIONS.clear();
        if (getConfig().isConfigurationSection("podium.positions")) {
            for (String key : getConfig().getConfigurationSection("podium.positions").getKeys(false)) {
                String path = "podium.positions." + key;
                double x = getConfig().getDouble(path + ".x");
                double y = getConfig().getDouble(path + ".y");
                double z = getConfig().getDouble(path + ".z");
                float yaw = (float) getConfig().getDouble(path + ".yaw", 0);
                PODIUM_POSITIONS.put(key, new Location(podiumWorld, x, y, z, yaw, 0));
            }
        }

        // GAME RULES settings
        MAX_PLAYERS = getConfig().getInt("game-rules.max-players", 4);
        MAX_RUN_TIME = getConfig().getLong("game-rules.max-run-time-minutes", 30) * 60 * 1000;

        logger.info(String.format("[SRP] Configuration loaded (MAIN_WORLD_NAME=%s)", MAIN_WORLD_NAME));
    }
}

