package com.fx.srp.model;

import lombok.Getter;
import org.bukkit.util.Vector;

/**
 * Represents the result of triangulating the location of a stronghold.
 *
 * <p>This class stores the calculated stronghold position in both the
 * Overworld and the Nether. The Nether coordinates are automatically
 * derived by dividing the Overworld X and Z coordinates by 8, following
 * Minecraft's world scale between dimensions.</p>
 *
 * <p>Instances of this class are immutable.</p>
 */
@Getter
public class TriangulationResult {
    private final Vector overworld;
    private final Vector nether;

    /**
     * Constructs a TriangulationResult from an Overworld location.
     *
     * @param overworld the calculated stronghold location in the Overworld as a {@code Vector}
     */
    public TriangulationResult(Vector overworld) {
        double scale = 8.0;
        this.overworld = overworld;
        this.nether = new Vector(overworld.getX() / scale, overworld.getY(), overworld.getZ() / scale);
    }
}
