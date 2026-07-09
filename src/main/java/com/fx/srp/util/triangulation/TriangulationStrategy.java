package com.fx.srp.util.triangulation;

import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a strategy for triangulating the location of a stronghold
 * based on Eye of Ender throws.
 *
 * <p>Implementations of this interface define the algorithm used to
 * calculate the stronghold position from a set of {@link EyeThrow} objects.
 * Different strategies may provide varying levels of assistance, determinism,
 * or variance.</p>
 *
 * <p>All triangulation calculations are performed in the XZ plane (horizontal),
 * as Eye of Ender Y coordinates are generally ignored for triangulation purposes.</p>
 */
public interface TriangulationStrategy {

    /**
     * Small threshold used to determine if two rays are effectively parallel.
     */
    double PARALLEL_EPSILON = 1e-6;

    /**
     * Minecraft Java Edition stronghold rings.
     * See <a href="https://minecraft.wiki/w/Stronghold">Wiki - Stronghold</a>.
     */
    ProbabilisticTriangulation.StrongholdRing[] STRONGHOLD_RINGS = new ProbabilisticTriangulation.StrongholdRing[]{
            new ProbabilisticTriangulation.StrongholdRing(3, 1280, 2816),
            new ProbabilisticTriangulation.StrongholdRing(6, 4352, 5888),
            new ProbabilisticTriangulation.StrongholdRing(10, 7424, 8960),
            new ProbabilisticTriangulation.StrongholdRing(15, 10496, 12032),
            new ProbabilisticTriangulation.StrongholdRing(21, 13568, 15104),
            new ProbabilisticTriangulation.StrongholdRing(28, 16640, 18176),
            new ProbabilisticTriangulation.StrongholdRing(36, 19712, 21248),
            new ProbabilisticTriangulation.StrongholdRing(9, 22784, 24320)
    };

    /**
     * A model of Stronghold Rings in Minecraft Java Edition, with info such as the number of strongholds in a given
     * ring, and the ring's radii. Useful for computing probabilities in triangulations.
     */
    class StrongholdRing {
        public final int count;
        public final double minRadius;
        public final double maxRadius;

        StrongholdRing(int count, double minRadius, double maxRadius) {
            this.count = count;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
        }
    }

    /**
     * Immutable-in-practice holder for the result of a ray intersection calculation:
     * the computed intersection point on the XZ-plane and its associated confidence
     * score (0-100).
     */
    @Setter
    @Getter
    @AllArgsConstructor
    class IntersectionData {
        public final Vector point;
        public final double confidence;
    }

    /**
     * 2D Euclidean distance from origin (0, 0, 0) to a position (x, 0, z)
     */
    default double getEuclideanDistanceFromOrigin(Vector position) {
        return Math.sqrt(position.getX() * position.getX() + position.getZ() * position.getZ());
    }

    /**
     * Compute the likelihood of a position being in a Stronghold ring
     */
    default double computeStrongholdRingLikelihood(Vector position) {
        // Distance from the world origin
        double distance = getEuclideanDistanceFromOrigin(position);

        // Find the closest ring by distance to its midpoint
        StrongholdRing closestRing = Arrays.stream(STRONGHOLD_RINGS)
                .min(Comparator.comparingDouble(ring -> {
                    double ringMid = (ring.minRadius + ring.maxRadius) / 2.0;
                    return Math.abs(distance - ringMid);
                }))
                .orElse(null);

        // Gaussian falloff: the closer to the ring center, the higher the likelihood
        double ringMid = (closestRing.minRadius + closestRing.maxRadius) / 2.0;
        double sigma = (closestRing.maxRadius - closestRing.minRadius) / 2.0; // half-width of ring

        return Math.exp(-Math.pow(distance - ringMid, 2) / (2 * sigma * sigma));
    }

    /**
     * Triangulate a stronghold location based on eye throws.
     *
     * @param eyeThrows The eye throws recorded
     * @return null if triangulation fails
     */
    TriangulationResult triangulate(List<EyeThrow> eyeThrows);

    /**
     * Returns a human-readable name for this strategy.
     */
    String getName();

    /**
     * Whether all necessary info is present to start triangulation
     */
    boolean isReady(List<EyeThrow> eyeThrows);

    /**
     * The player message (feedback) provided when the first eye of ender is spawned
     */
    String getFirstEyeMessage();
}
