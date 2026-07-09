package com.fx.srp.util.triangulation;

import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import lombok.NoArgsConstructor;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A {@link TriangulationStrategy} that estimates a stronghold's location via weighted
 * least-squares intersection of the player's Eye of Ender throw rays, projected onto
 * the XZ-plane.
 * <p>
 * Each throw's ray is weighted inversely by its measured angular deviation from the
 * "optimal" ray (the true direction toward the stronghold, inferred from the eye's
 * spawn and landing/break location). The resulting intersection is scored with a
 * confidence value derived from two factors: the geometric spread of the throws'
 * uncertainty cones at the intersection point, and the likelihood that the intersection
 * falls within a valid stronghold ring.
 * <p>
 * This is the unassisted counterpart to {@link AssistedProbabilisticTriangulation} —
 * it performs the same calculation but does not send the player any intermediate
 * feedback.
 *
 * @see AssistedProbabilisticTriangulation
 */
@NoArgsConstructor
public class ProbabilisticTriangulation implements TriangulationStrategy {

    /**
     * Triangulates a stronghold location from the last two eyes of ender thrown with variance based on;
     * the player's viewing direction captured at the time of the eye dropping/breaking.
     *
     * @param eyeThrows a list of EyeThrows
     * @return a {@link TriangulationResult} containing the calculated stronghold
     *         location in the Overworld (and Nether) as well as a confidence-score, or {@code null} if triangulation
     *         is not possible (e.g., rays are parallel or list size < 2)
     */
    @Override
    public TriangulationResult triangulate(List<EyeThrow> eyeThrows) {
        if (!isReady(eyeThrows)) return null;

        // Filter out incomplete EyeThrows
        List<EyeThrow> validThrows = eyeThrows.stream()
                .filter(et -> et.getPlayerViewDirection() != null)
                .filter(et -> et.getPlayerPosition() != null)
                .collect(Collectors.toList());

        IntersectionData result = intersect(validThrows);
        if (result == null) return null;

        return new TriangulationResult(result.getPoint(), result.getConfidence());
    }

    private IntersectionData intersect(List<EyeThrow> eyeThrows) {
        int numberOfEyes = eyeThrows.size();
        Vector[] pos = new Vector[numberOfEyes];
        Vector[] dir = new Vector[numberOfEyes];
        double[] errors = new double[numberOfEyes];

        // Fill positions, directions, and per-throw angle errors
        IntStream.range(0, numberOfEyes).forEach(i -> {
            EyeThrow eye = eyeThrows.get(i);
            pos[i] = new Vector(eye.getPlayerPosition().getX(), 0, eye.getPlayerPosition().getZ());
            dir[i] = new Vector(eye.getPlayerViewDirection().getX(), 0, eye.getPlayerViewDirection().getZ());

            // Optimal ray (the ray of the eye of ender)
            Vector optimalDir = eye.getTargetLocation().toVector()
                    .subtract(eye.getSpawnLocation().toVector())
                    .setY(0).normalize();

            // Throw error (how far is the measured ray from the optimal ray)
            double error = dir[i].angle(optimalDir);
            errors[i] = error;
        });

        // Least-squares intersection in 2D (on the XZ-plane)
        double sumXX = 0, sumXZ = 0, sumZX = 0, sumZZ = 0, sumXP = 0, sumZP = 0;
        for (int i = 0; i < numberOfEyes; i++) {
            double dirX = dir[i].getX();
            double dirZ = dir[i].getZ();
            double posX = pos[i].getX();
            double posZ = pos[i].getZ();

            // Weight matrix
            double weightX = dirZ * dirZ;
            double weightZ = dirX * dirX;
            double weightXZ = -dirX * dirZ;

            sumXX += weightX;
            sumZZ += weightZ;
            sumXZ += weightXZ;
            sumZX += weightXZ;

            sumXP += weightX * posX + weightXZ * posZ;
            sumZP += weightXZ * posX + weightZ * posZ;
        }

        // Ensure the rays are not (near) parallel (i.e. do not intersect) making triangulation impossible,
        // done by calculating the cross product, when the cross product = 0, the rays are parallel
        double cross = sumXX * sumZZ - sumXZ * sumZX;
        if (Math.abs(cross) < PARALLEL_EPSILON) return null;

        // Intersection (the stronghold position)
        double intersectionX = (sumZZ * sumXP - sumXZ * sumZP) / cross;
        double intersectionZ = (-sumZX * sumXP + sumXX * sumZP) / cross;
        Vector intersection = new Vector(intersectionX, 0, intersectionZ);

        // Compute the uncertainty-cone radii, i.e. the width of a cone at the intersection.
        // An uncertainty-cone is a cone with its tip at the player's measurement position, a height equal to
        // the distance to the intersection, and radius equal to the measurement error at the intersection.
        // Because the error increases as the distance increases it forms a cone, which represent the uncertainty.
        // In multiple throws, the area of the uncertainty-cones overlap, giving us a measure of the uncertainty.
        double overlapRadius = IntStream.range(0, numberOfEyes)
                .mapToDouble(i -> {
                    double distance = pos[i].distance(intersection);
                    double angularError = errors[i]; // per-throw error
                    double coneRadius = distance * Math.tan(angularError);
                    return coneRadius * coneRadius;
                })
                .sum();

        // Map the overlapping area of uncertainty-cones to a confidence score (geometric confidence)
        double R0 = 64.0;
        double geometricConfidence = Math.exp(-(overlapRadius * overlapRadius) / (2 * R0 * R0));

        // Compute the likelihood of the intersection being in a stronghold ring
        double ringLikelihood = computeStrongholdRingLikelihood(intersection);

        // The final confidence score is the Geometric confidence * the Stronghold ring likelihood
        double finalConfidence = geometricConfidence * ringLikelihood * 100;

        return new IntersectionData(intersection, finalConfidence);
    }

    @Override
    public String getName() {
        return "PROBABILISTIC";
    }

    @Override
    public boolean isReady(List<EyeThrow> eyeThrows) {
        return eyeThrows.stream()
                .filter(eyeThrow -> eyeThrow.getPlayerViewDirection() != null)
                .filter(et -> et.getPlayerPosition() != null)
                .count() >= 2;
    }

    @Override
    public String getFirstEyeMessage() {
        return "#1 Eye of Ender! Look into the center of the eye when it drops/breaks.";
    }
}
