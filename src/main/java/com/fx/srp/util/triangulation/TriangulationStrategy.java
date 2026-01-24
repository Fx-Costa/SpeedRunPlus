package com.fx.srp.util.triangulation;

import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;

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
}
