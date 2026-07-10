package com.fx.srp.model.seed;

import org.bukkit.ChatColor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps each {@link SeedCategory.SeedType} to a display color, used when rendering
 * the seed type in-game (e.g. during the run countdown).
 */
public final class SeedTypeDisplay {

    private static final Map<SeedCategory.SeedType, ChatColor> COLORS = new EnumMap<>(SeedCategory.SeedType.class);

    static {
        COLORS.put(SeedCategory.SeedType.MAPLESS, ChatColor.GOLD);
        COLORS.put(SeedCategory.SeedType.VILLAGE, ChatColor.DARK_GREEN);
        COLORS.put(SeedCategory.SeedType.TEMPLE, ChatColor.YELLOW);
        COLORS.put(SeedCategory.SeedType.SHIPWRECK, ChatColor.AQUA);
        COLORS.put(SeedCategory.SeedType.RUINED_PORTAL, ChatColor.DARK_PURPLE);
        COLORS.put(SeedCategory.SeedType.RANDOM, ChatColor.DARK_RED);
    }

    private SeedTypeDisplay() {}

    /**
     * Returns the display color for the given seed type, or white if unmapped.
     *
     * @param type the seed type
     * @return the color to render this seed type in
     */
    public static ChatColor colorFor(SeedCategory.SeedType type) {
        return COLORS.getOrDefault(type, ChatColor.WHITE);
    }

    /**
     * Returns the seed type formatted for display, colored and with underscores
     * replaced by spaces, e.g. {@code "RUINED PORTAL"} in dark purple.
     *
     * @param type the seed type
     * @return the colored display string
     */
    public static String format(SeedCategory.SeedType type) {
        return colorFor(type) + type.name().replace('_', ' ');
    }
}
