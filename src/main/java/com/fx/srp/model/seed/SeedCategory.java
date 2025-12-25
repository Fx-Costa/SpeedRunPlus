package com.fx.srp.model.seed;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents a category of seeds used for controlled or filtered world generation.
 *
 * <p>A {@code SeedCategory} groups together:</p>
 * <ul>
 *     <li>A {@link SeedType}, describing the world feature present near spawn</li>
 *     <li>A weight value, used for weighted random selection</li>
 *     <li>A list of long integer seed values belonging to that category</li>
 * </ul>
 */
@AllArgsConstructor
public class SeedCategory {

    @Getter private final SeedType seedType;
    @Getter private final int weight;
    @Getter private final List<Long> seeds;

    /**
     * Represents a type of seed, with a given structure near spawn
     */
    @Getter
    public enum SeedType {
        MAPLESS("zsg"),
        VILLAGE("zsgvillage"),
        TEMPLE("zsgtemple"),
        SHIPWRECK("zsgshipwreck"),
        RUINED_PORTAL("rpseedbank"),
        RANDOM(null);

        private final String fsgName;

        SeedType(String fsgName) {
            this.fsgName = fsgName;
        }
    }
}
