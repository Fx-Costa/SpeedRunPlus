package com.fx.srp.util;

import com.fx.srp.model.EyeThrow;
import org.bukkit.util.Vector;

import java.util.List;

public class TriangulationCalculator {

    private static final double EPSILON = 1e-6;

    public static Vector triangulate(List<EyeThrow> eyeThrows) {
        if  (eyeThrows.size() != 2) return null;

        // Return the intersection of the two rays formed by each eye throw
        return intersectRays2D(eyeThrows.getFirst(),  eyeThrows.getLast());
    }

    private static Vector intersectRays2D(EyeThrow a, EyeThrow b) {
        // Each ray is defined by: P + tD, where;
        // P = (x, 0, z) - the coordinates of the throw
        // t = Unknown scalar - the distance multiplier we want to find
        // D = Normalized direction
        Vector p1 = new Vector(a.getPosition().getX(), 0, a.getPosition().getZ());
        Vector d1 = a.getDirection();
        Vector p2 = new Vector(b.getPosition().getX(), 0, b.getPosition().getZ());
        Vector d2 = b.getDirection();

        // Ensure the rays are not (near) parallel (i.e. do not intersect) making triangulation impossible,
        // done by calculating the 2D cross product, when the cross product = 0, the rays are parallel
        double crossProduct = d1.getX() * d2.getZ() - d1.getZ() * d2.getX();
        if (Math.abs(crossProduct) < EPSILON) return null;

        // Determine the vector between the first and second throw, to find their relativity (distance, etc.)
        Vector relationVector = p2.clone().subtract(p1);

        // Solve for t
        double t = (relationVector.getX() * d2.getZ() - relationVector.getZ() * d2.getX()) / crossProduct;

        // Triangulate
        return p1.clone().add(d1.clone().multiply(t));
    }
}
