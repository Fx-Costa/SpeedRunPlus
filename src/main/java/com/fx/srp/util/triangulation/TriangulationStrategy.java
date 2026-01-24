package com.fx.srp.util.triangulation;

import com.fx.srp.model.EyeThrow;
import com.fx.srp.model.TriangulationResult;

import java.util.List;

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
