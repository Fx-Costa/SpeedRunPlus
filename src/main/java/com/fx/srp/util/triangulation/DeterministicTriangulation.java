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
        Vector p1 = new Vector(a.getPosition().getX(), 0, a.getPosition().getZ());
        Vector d1 = a.getDirection();
        Vector p2 = new Vector(b.getPosition().getX(), 0, b.getPosition().getZ());
        Vector d2 = b.getDirection();

        double cross = d1.getX() * d2.getZ() - d1.getZ() * d2.getX();
        if (Math.abs(cross) < EPSILON) return null;

        Vector relation = p2.clone().subtract(p1);
        double t = (relation.getX() * d2.getZ() - relation.getZ() * d2.getX()) / cross;
        return p1.clone().add(d1.clone().multiply(t));
    }

    @Override
    public String getName() {
        return "DETERMINISTIC";
    }
}
