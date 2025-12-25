package com.fx.srp.model.run;

import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.commands.GameMode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Represents a generic speedrun session for a player or group of players.
 * <p>
 * This abstract class provides core functionality for starting, stopping, and
 * managing a speedrun, including stopwatch handling, state management, seed tracking,
 * and player respawn handling.
 * </p>
 */
public abstract class Speedrun implements ISpeedrun {

    @Getter protected final GameMode gameMode;

    @Getter private final StopWatch stopWatch;

    @Getter @Setter private State state = State.WAITING;

    @Getter @Setter private Long seed;

    private final Speedrunner owner;

    @Getter @Setter protected BukkitTask timerUpdateTask;

    @Getter @Setter protected BukkitTask timeoutTask;

    /**
     * Constructs a new speedrun instance.
     *
     * @param gameMode the {@code GameMode} that the run represents
     * @param owner       The {@code Speedrunner} who owns or participates in this run.
     * @param stopWatch   The {@code StopWatch} instance to track elapsed time.
     * @param seed        Optional seed for world generation. May be {@code null}.
     */
    public Speedrun(GameMode gameMode, Speedrunner owner, StopWatch stopWatch, Long seed) {
        this.gameMode = gameMode;
        this.owner = owner;
        this.stopWatch = stopWatch;
        this.seed = seed;
    }

    /**
     * Returns the list of players participating in this speedrun.
     *
     * @return an immutable {@code List} containing the Speedrunner(s).
     */
    public List<Speedrunner> getSpeedrunners() {
        return List.of(owner);
    }

    /**
     * Called when a player leaves the server during this speedrun.
     * <p>
     * By default, this finishes the run for all participants without
     * awarding a winner.
     * </p>
     *
     * @param player The {@code Player} who left the server.
     */
    public void onPlayerLeave(Player player) {
        gameMode.getManager().abort(this, null,null);
    }

    /**
     * Handles player respawn during the speedrun.
     * <p>
     * If the player respawns outside the speedrun worlds, their respawn
     * location is overridden to the overworld spawn of the speedrun.
     * </p>
     *
     * @param speedrunner The {@code Speedrunner} who respawned.
     * @param event       The {@code PlayerRespawnEvent} to modify.
     */
    public void onPlayerRespawn(Speedrunner speedrunner, PlayerRespawnEvent event) {
        WorldManager.WorldSet worlds = speedrunner.getWorldSet();

        // Get the respawn location's world
        World respawnWorld = event.getRespawnLocation().getWorld();
        String respawnWorldName = respawnWorld.getName();

        // Speedrun world names
        String speedRunOverworldName = worlds.getOverworld().getName();
        String speedRunNetherName = worlds.getNether().getName();
        String speedRunEndName = worlds.getEnd().getName();

        // Let the event pass if it is in a speedrun world
        if (respawnWorldName.equals(speedRunOverworldName) ||
                respawnWorldName.equals(speedRunNetherName) ||
                respawnWorldName.equals(speedRunEndName)
        ) return;

        // Otherwise, overwrite the respawn location
        Player player = speedrunner.getPlayer();
        Location bedSpawnLocation = player.getBedSpawnLocation();   // includes respawn-anchors
        boolean isBedSpawnLocationInSpeedrun = bedSpawnLocation != null && (
                bedSpawnLocation.getWorld().getName().equals(speedRunOverworldName) ||
                bedSpawnLocation.getWorld().getName().equals(speedRunNetherName) ||
                bedSpawnLocation.getWorld().getName().equals(speedRunEndName)
        );

        // Overwrite the spawn location at the bed/respawn-anchor if set
        if (isBedSpawnLocationInSpeedrun) {
            event.setRespawnLocation(bedSpawnLocation);
            return;
        }

        // Otherwise, respawn in the speedrun overworld spawn
        event.setRespawnLocation(worlds.getSpawn());
    }
}
