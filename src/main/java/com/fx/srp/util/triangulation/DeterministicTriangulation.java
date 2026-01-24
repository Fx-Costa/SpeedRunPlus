package com.fx.srp.util.triangulation;

import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import org.bukkit.util.Vector;

import java.util.List;

public class DeterministicTriangulation implements TriangulationStrategy {

    private static final double EPSILON = 1e-6;

    @Override
    public TriangulationResult triangulate(List<EyeThrow> eyeThrows) {
        if (eyeThrows.size() != 2) return null;

        Vector intersection = intersectRays2D(eyeThrows.get(0), eyeThrows.get(1));
        if (intersection == null) return null;

        return new TriangulationResult(intersection);
    }

    private Vector intersectRays2D(EyeThrow a, EyeThrow b) {
        // Each ray is defined by: P + tD, where;
        // P = (x, 0, z) - the coordinates of the throw
        // t = Unknown scalar - the distance multiplier we want to find
        // D = Normalized direction
        Vector p1 = new Vector(a.getSpawnLocation().getX(), 0, a.getSpawnLocation().getZ());
        Vector d1 = a.getTargetLocation().toVector().subtract(a.getSpawnLocation().toVector()).setY(0).normalize();
        Vector p2 = new Vector(b.getSpawnLocation().getX(), 0, b.getSpawnLocation().getZ());
        Vector d2 = b.getTargetLocation().toVector().subtract(b.getSpawnLocation().toVector()).setY(0).normalize();

        // Ensure the rays are not (near) parallel (i.e. do not intersect) making triangulation impossible,
        // done by calculating the 2D cross product, when the cross product = 0, the rays are parallel
        double cross = d1.getX() * d2.getZ() - d1.getZ() * d2.getX();
        if (Math.abs(cross) < EPSILON) return null;

        // Determine the vector between the first and second throw, to find their relativity (distance, etc.)
        Vector relation = p2.clone().subtract(p1);

        // Solve for t
        double t = (relation.getX() * d2.getZ() - relation.getZ() * d2.getX()) / cross;

        // Triangulate
        return p1.clone().add(d1.clone().multiply(t));
    }

    @Override
    public String getName() {
        return "DETERMINISTIC";
    }
}
