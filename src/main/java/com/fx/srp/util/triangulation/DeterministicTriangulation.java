package com.fx.srp.util.triangulation;

import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import lombok.NoArgsConstructor;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * A deterministic implementation of {@link TriangulationStrategy} that calculates
 * the stronghold location from exactly two Eye of Ender throws.
 *
 * <p>This strategy works by intersecting the 2D rays defined by the spawn and target
 * locations of each EyeThrow in the XZ plane. If the rays are parallel or nearly
 * parallel, triangulation fails.</p>
 *
 * <p>Requires two EyeThrows are supported. Using fewer or more will result in a {@code null} result.</p>
 */
@NoArgsConstructor
public class DeterministicTriangulation implements TriangulationStrategy {

    /**
     * Small threshold used to determine if two rays are effectively parallel.
     */
    private static final double EPSILON = 1e-6;

    /**
     * Triangulates a stronghold location from exactly two EyeThrow objects.
     *
     * @param eyeThrows a list of exactly two EyeThrows
     * @return a {@link TriangulationResult} containing the calculated stronghold
     *         location in the Overworld (and Nether), or {@code null} if triangulation
     *         is not possible (e.g., rays are parallel or list size != 2)
     */
    @Override
    public TriangulationResult triangulate(List<EyeThrow> eyeThrows) {
        int requiredEyeCount = 2;
        if (eyeThrows.size() != requiredEyeCount) return null;

        Vector intersection = intersectRays2D(eyeThrows.get(0), eyeThrows.get(1));
        if (intersection == null) return null;

        return new TriangulationResult(intersection);
    }

    private Vector intersectRays2D(EyeThrow throwA, EyeThrow throwB) {
        // Each ray is defined by: P + tD, where;
        // P = (x, 0, z) - the coordinates of the throw
        // t = Unknown scalar - the distance multiplier we want to find
        // D = Normalized direction
        Vector pos1 = new Vector(throwA.getSpawnLocation().getX(), 0, throwA.getSpawnLocation().getZ());
        Vector dir1 = throwA.getTargetLocation().toVector().subtract(throwA.getSpawnLocation().toVector())
                .setY(0).normalize();

        Vector pos2 = new Vector(throwB.getSpawnLocation().getX(), 0, throwB.getSpawnLocation().getZ());
        Vector dir2 = throwB.getTargetLocation().toVector().subtract(throwB.getSpawnLocation().toVector())
                .setY(0).normalize();

        // Ensure the rays are not (near) parallel (i.e. do not intersect) making triangulation impossible,
        // done by calculating the 2D cross product, when the cross product = 0, the rays are parallel
        double cross = dir1.getX() * dir2.getZ() - dir1.getZ() * dir2.getX();
        if (Math.abs(cross) < EPSILON) return null;

        // Determine the vector between the first and second throw, to find their relativity (distance, etc.)
        Vector relation = pos2.clone().subtract(pos1);

        // Solve for t
        double multiplier = (relation.getX() * dir2.getZ() - relation.getZ() * dir2.getX()) / cross;

        // Triangulate
        return pos1.clone().add(dir1.clone().multiply(multiplier));
    }

    /**
     * Returns the name of this triangulation strategy.
     *
     * @return the name "DETERMINISTIC"
     */
    @Override
    public String getName() {
        return "DETERMINISTIC";
    }
}
