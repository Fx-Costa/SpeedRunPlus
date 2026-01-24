package com.fx.srp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Represents a single throw of an Eye of Ender by a player.
 *
 * <p>This class encapsulates the essential data needed for assisted
 * stronghold triangulation in speedruns, including the player who threw
 * the eye, the location it was spawned from, the target location it flew
 * toward, and the timestamp of the throw.</p>
 *
 * <p>Instances of this class are immutable.</p>
 */
@Getter
@AllArgsConstructor
public class EyeThrow {

    private final Player player;

    private final Location spawnLocation;

    private final Location targetLocation;

    private final long timestamp;

}
