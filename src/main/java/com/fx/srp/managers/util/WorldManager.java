package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.seed.SeedCategory;
import com.fx.srp.model.seed.SelectedSeed;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles creation, management, and deletion of speedrun worlds for players.
 *
 * <p>This class relies on Multiverse-Core and Multiverse-Nether-Portals to manage
 * multiple isolated world sets per player. Each player can have an Overworld, Nether,
 * and End world linked together. It also ensures leftover worlds from previous sessions
 * are cleaned up on plugin initialization.</p>
 *
 * <p>Main responsibilities:</p>
 * <ul>
 *     <li>Create world sets for one or more players with optional seeds.</li>
 *     <li>Delete world sets for speedrunners and clean up resources.</li>
 *     <li>Link and unlink worlds for proper portal traversal.</li>
 *     <li>Ensure unique world names to prevent collisions.</li>
 * </ul>
 */
public class WorldManager {

    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final SpeedRunPlus plugin;

    private final MVWorldManager mvWorldManager;
    private final MultiverseNetherPortals portalManager;
    private final SeedManager seedManager;

    /**
     * Represents a player's set of worlds: Overworld, Nether, End.
     * Provides the spawn location for teleportation purposes.
     */
    @Getter
    public static class WorldSet {
        private final MultiverseWorld overworld;
        private final MultiverseWorld nether;
        private final MultiverseWorld end;
        private final Location spawn;
        private final SeedCategory.SeedType seedType;

        /**
         * Constructs a set of worlds with a default spawn point.
         *
         * @param overworld The {@code MultiverseWorld} overworld.
         * @param nether The {@code MultiverseWorld} nether.
         * @param end The {@code MultiverseWorld} end.
         * @param seedType The filtered {@link SeedCategory.SeedType} used for this world set's
         *                 seed, or {@code null} if an explicit seed was supplied or filtered
         *                 seeds are disabled.
         */
        public WorldSet(MultiverseWorld overworld, MultiverseWorld nether, MultiverseWorld end,
                        SeedCategory.SeedType seedType) {
            this.overworld = overworld;
            this.nether = nether;
            this.end = end;
            this.spawn = overworld.getSpawnLocation();
            this.seedType = seedType;
        }
    }

    /**
     * Constructs the world manager and removes leftover SRP worlds from previous sessions.
     *
     * @param plugin The main plugin instance.
     */
    public WorldManager(SpeedRunPlus plugin, SeedManager seedManager) {
        this.mvWorldManager = plugin.getMvWorldManager();
        this.portalManager = plugin.getPortalManager();
        this.plugin = plugin;

        // Seed manager
        this.seedManager = seedManager;

        // Cleanup leftover worlds
        cleanupLeftoverSrpWorlds();
    }

    /**
     * Get owning player UUID.
     *
     * @param world the world.
     *
     * <p>Extracts the owning player UUID from the world's name.</p>
     */
    public Optional<UUID> getWorldOwnerUUID(World world) {
        // Must be an active speedrun world
        if (!isSpeedrunWorld(world)) return Optional.empty();

        // Remove the configured prefix
        int prefixLength;
        switch (world.getEnvironment()) {
            case NORMAL: prefixLength = configHandler.getOverworldPrefix().length(); break;
            case NETHER: prefixLength = configHandler.getNetherPrefix().length(); break;
            case THE_END: prefixLength = configHandler.getEndPrefix().length(); break;
            default: return Optional.empty();
        }

        // Ensure the world name is not just the prefix
        String worldName = world.getName();
        if (worldName.length() <= prefixLength) return Optional.empty();

        // Remove the prefix and any suffix if present
        String uuidString = world.getName().substring(prefixLength).split("_")[0];

        // Build a UUID
        try {
            return Optional.of(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /* ==========================================================
     *                 WORLD CREATION (N PLAYERS)
     * ========================================================== */
    /**
     * Creates world sets for multiple players.
     *
     * @param players   The players who need worlds.
     * @param inputSeed Optional world seed.
     * @param callback  Callback executed when all worlds are ready. Receives a map
     *                  linking each player's UUID to their WorldSet.
     * <p><br>If the given seed is null and the config 'use-filtered-seeds' is set, a weighted pseudo-random filtered
     * seed (for speedrun purposes) will be selected.
     * If 'use-filtered-seeds' is not set, seed generation is left to be handled by Minecraft.</p>
     */
    public void createWorldsForPlayers(
            Collection<Player> players,
            Long inputSeed,
            Consumer<Map<UUID, WorldSet>> callback
    ) {
        Map<UUID, WorldSet> sets = new ConcurrentHashMap<>();
        AtomicInteger done = new AtomicInteger(0);
        int total = players.size();

        // Determine the seed
        Long seed;
        SeedCategory.SeedType seedType = null;
        if (inputSeed != null) {
            seed = inputSeed;
        } else {
            SelectedSeed selectedSeed = seedManager.selectSeed();
            seed = selectedSeed != null ? selectedSeed.getSeed() : null;
            seedType = selectedSeed != null ? selectedSeed.getType() : null;
        }
        String seedString = seed != null ? String.valueOf(seed) : null;
        SeedCategory.SeedType finalSeedType = seedType;

        for (Player player : players) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                WorldSet set = createWorldSet(player, seedString, finalSeedType);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    sets.put(player.getUniqueId(), set);

                    if (done.incrementAndGet() == total) {
                        callback.accept(sets);
                    }
                });
            });
        }
    }

    private WorldSet createWorldSet(Player player, String seed, SeedCategory.SeedType seedType) {
        // Determine world names
        UUID uuid = player.getUniqueId();
        String overworldName = getWorldName(configHandler.getOverworldPrefix() + uuid);
        String netherName = getWorldName(configHandler.getNetherPrefix() + uuid);
        String endName = getWorldName(configHandler.getEndPrefix() + uuid);

        // Overworld
        mvWorldManager.addWorld(
                overworldName,
                World.Environment.NORMAL,
                seed,
                WorldType.NORMAL,
                true,
                null
        );
        MultiverseWorld overworld = mvWorldManager.getMVWorld(overworldName);

        // Nether
        mvWorldManager.addWorld(
                netherName,
                World.Environment.NETHER,
                seed,
                WorldType.NORMAL,
                true,
                null
        );
        MultiverseWorld nether = mvWorldManager.getMVWorld(netherName);
        nether.setRespawnToWorld(overworldName);

        // End
        mvWorldManager.addWorld(
                endName,
                World.Environment.THE_END,
                seed,
                WorldType.NORMAL,
                true,
                null
        );
        MultiverseWorld end = mvWorldManager.getMVWorld(endName);
        end.setRespawnToWorld(overworldName);

        // Link the three world
        linkWorlds(overworldName, netherName, endName);

        return new WorldSet(overworld, nether, end, seedType);
    }

    /* ==========================================================
     *                  WORLD DELETION (N PLAYERS)
     * ========================================================== */
    /**
     * Deletes the world sets for multiple speedrunners.
     *
     * @param speedrunners The speedrunners whose worlds are to be deleted.
     * @param callback     Callback invoked after all worlds are deleted.
     */
    public void deleteWorldsForPlayers(Collection<Speedrunner> speedrunners, Runnable callback) {
        AtomicInteger done = new AtomicInteger(0);
        int total = speedrunners.size();

        speedrunners.forEach(speedrunner -> Bukkit.getScheduler().runTask(plugin, () -> {
            deleteWorldSet(speedrunner);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (done.incrementAndGet() == total) {
                    callback.run();
                }
            });
        }));
    }

    private void deleteWorldSet(Speedrunner speedrunner) {
        // Get the player's worlds
        WorldSet worldSet = speedrunner.getWorldSet();

        // Get world names
        String overworldName = worldSet.overworld.getName();
        String netherName =worldSet.nether.getName();
        String endName = worldSet.end.getName();

        // Remove world links
        unlinkWorlds(overworldName, netherName, endName);

        mvWorldManager.deleteWorld(overworldName);
        mvWorldManager.deleteWorld(netherName);
        mvWorldManager.deleteWorld(endName);
    }

    private void cleanupLeftoverSrpWorlds() {
        String owPrefix = configHandler.getOverworldPrefix();
        String netherPrefix = configHandler.getNetherPrefix();
        String endPrefix = configHandler.getEndPrefix();

        mvWorldManager.getMVWorlds().forEach(world -> {
            String name = world.getName();

            if (name.startsWith(owPrefix) ||
                    name.startsWith(netherPrefix) ||
                    name.startsWith(endPrefix)) {

                Bukkit.getLogger().info("[SRP] Removing leftover world: " + name);
                mvWorldManager.deleteWorld(name);
            }
        });
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    private boolean isSpeedrunWorld(World world) {
        // Only active worlds
        if (Bukkit.getWorld(world.getUID()) == null) return false;

        // Get the world as a Multiverse world (a speedrun world is always a Multiverse world)
        MultiverseWorld mvWorld = mvWorldManager.getMVWorld(world);
        if (mvWorld == null) return false;

        // Get the world environment (overworld, nether, end)
        World.Environment environment = mvWorld.getEnvironment();

        // The world name must have the configured prefixes
        switch (environment) {
            // Whether the world has the configured overworld prefix
            case NORMAL: return mvWorld.getName().startsWith(configHandler.getOverworldPrefix());

            // Whether the world has the configured nether prefix
            case NETHER: return mvWorld.getName().startsWith(configHandler.getNetherPrefix());

            // Whether the world has the configured end prefix
            case THE_END: return mvWorld.getName().startsWith(configHandler.getEndPrefix());

            default: return false;
        }
    }

    private void linkWorlds(String overworldName, String netherName, String endName) {
        portalManager.addWorldLink(overworldName, netherName, PortalType.NETHER);
        portalManager.addWorldLink(netherName, overworldName, PortalType.NETHER);
        portalManager.addWorldLink(overworldName, endName, PortalType.ENDER);
        portalManager.addWorldLink(endName, overworldName, PortalType.ENDER);
    }

    private void unlinkWorlds(String overworldName, String netherName, String endName) {
        portalManager.removeWorldLink(overworldName, netherName, PortalType.NETHER);
        portalManager.removeWorldLink(netherName, overworldName, PortalType.NETHER);
        portalManager.removeWorldLink(overworldName, endName, PortalType.ENDER);
        portalManager.removeWorldLink(endName, overworldName, PortalType.ENDER);
    }

    private String getWorldName(String baseName) {
        String name = baseName;
        int accumulator = 1;

        // Check Bukkit world registry – Multiverse loads worlds here
        while (Bukkit.getWorld(name) != null) {
            name = baseName + "_" + accumulator;
            accumulator++;
        }
        return name;
    }
}
