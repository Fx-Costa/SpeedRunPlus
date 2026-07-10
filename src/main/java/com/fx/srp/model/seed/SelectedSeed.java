package com.fx.srp.model.seed;

import lombok.Getter;

/**
 * The result of a seed selection: the {@link SeedCategory.SeedType} that was chosen
 * and the seed value to use, if any.
 * <p>
 * When {@code type} is {@link SeedCategory.SeedType#RANDOM}, {@code seed} is {@code null},
 * signaling that world generation should use a fully random seed rather than one from a
 * filtered pool.
 */
@Getter
public class SelectedSeed {
    private final SeedCategory.SeedType type;
    private final Long seed;

    public SelectedSeed(SeedCategory.SeedType type, Long seed) {
        this.type = type;
        this.seed = seed;
    }
}
