package com.fx.srp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class EyeThrow {

    private final Player player;

    private final Location spawnLocation;

    private final Location targetLocation;

    private final long timestamp;

}
