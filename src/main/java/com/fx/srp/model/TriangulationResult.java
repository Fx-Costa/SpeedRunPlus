package com.fx.srp.model;

import lombok.Getter;
import org.bukkit.util.Vector;

@Getter
public class TriangulationResult {
    private final Vector overworld;
    private final Vector nether;

    public TriangulationResult(Vector overworld) {
        this.overworld = overworld;
        this.nether = new Vector(overworld.getX() / 8.0, overworld.getY(), overworld.getZ() / 8.0);
    }
}
