package com.fx.srp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@Getter
@AllArgsConstructor
public class EyeThrow {

    private final Player player;

    private final Location position;

    private final Vector direction;

    private final long timestamp;

}
