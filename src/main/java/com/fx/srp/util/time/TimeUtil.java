package com.fx.srp.util.time;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.time.StopWatch;

/**
 * Utility class for extracting time components (hours, minutes, seconds, milliseconds)
 * from a {@link StopWatch} or a raw millisecond value.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtil {

    /**
     * Gets the milliseconds component (2-digit) from a {@link StopWatch}.
     *
     * @param stopWatch the {@code StopWatch}; may be {@code null}
     * @return the milliseconds component in {@code [0, 99]} or {@code 0} if {@code stopWatch} is {@code null}
     */
    public static long getMilliseconds(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long milliseconds = stopWatch.getTime() % 1000L;
        return milliseconds / 10L;  // 2 Digits
    }

    /**
     * Gets the hours component from a {@link StopWatch}.
     *
     * @param stopWatch the {@code StopWatch}; may be {@code null}
     * @return the hours component or {@code 0} if {@code stopWatch} is {@code null}
     */
    public static long getHours(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long seconds = stopWatch.getTime() / 1000L;
        return seconds / 3600;
    }

    /**
     * Gets the milliseconds component (2-digit) from a raw millisecond value.
     *
     * @param milliseconds the total milliseconds
     * @return the milliseconds component in {@code [0, 99]}
     */
    public static long getMilliseconds(long milliseconds) {
        return milliseconds % 1000L / 10L; // 2 digits
    }

    /**
     * Gets the seconds component from a raw millisecond value.
     *
     * @param milliseconds the total milliseconds
     * @return the seconds component in {@code [0, 59]}
     */
    public static long getSeconds(long milliseconds) {
        long seconds = milliseconds / 1000L;
        return seconds % 60;
    }

    /**
     * Gets the minutes component from a raw millisecond value.
     *
     * @param milliseconds the total milliseconds
     * @return the minutes component in {@code [0, 59]}
     */
    public static long getMinutes(long milliseconds) {
        long seconds = milliseconds / 1000L;
        return seconds % 3600 / 60;
    }

    /**
     * Gets the hours component from a raw millisecond value.
     *
     * @param milliseconds the total milliseconds
     * @return the hours component
     */
    public static long getHours(long milliseconds) {
        long seconds = milliseconds / 1000L;
        return seconds / 3600;
    }
}
