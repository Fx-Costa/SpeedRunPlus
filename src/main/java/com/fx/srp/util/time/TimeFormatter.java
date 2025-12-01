package com.fx.srp.util.time;

import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;

import java.util.function.Function;

/**
 * Formats time values from a {@link StopWatch} or a raw millisecond value into
 * a human-readable string.
 * <p>
 * Supports options such as including hours, using suffixes like {@code "min"} or
 * {@code "sec"}, and rendering milliseconds as superscript.
 * </p>
 */
public class TimeFormatter {

    private final StopWatch stopWatch;
    private final long milliseconds;
    private boolean superscriptMs;
    private boolean useSuffixes;
    private boolean includeHours;

    private static final String TWO_DECIMALS_FORMAT = "%02d";

    /**
     * Constructs a {@code TimeFormatter} for a given {@link StopWatch}.
     *
     * @param stopWatch the {@code StopWatch} to format; must not be {@code null}
     */
    public TimeFormatter(@NonNull StopWatch stopWatch) {
        this.stopWatch = stopWatch;
        this.milliseconds = -1;
    }

    /**
     * Constructs a {@code TimeFormatter} from a raw millisecond value.
     *
     * @param milliseconds the time in milliseconds
     */
    public TimeFormatter(long milliseconds) {
        this.stopWatch = null;
        this.milliseconds = milliseconds;
    }

    /**
     * Formats the milliseconds as superscript if applicable.
     *
     * @return this {@code TimeFormatter} instance for chaining
     */
    public TimeFormatter withSuperscriptMs() {
        this.superscriptMs = true;
        return this;
    }

    /**
     * Uses time unit suffixes such as {@code "min"}, {@code "sec"}, {@code "ms"}.
     *
     * @return this {@code TimeFormatter} instance for chaining
     */
    public TimeFormatter withSuffixes() {
        this.useSuffixes = true;
        return this;
    }

    /**
     * Includes hours in the formatted output if the value is greater than zero.
     *
     * @return this {@code TimeFormatter} instance for chaining
     */
    public TimeFormatter withHours() {
        long hours = (stopWatch != null)
                ? TimeUtil.getHours(stopWatch)
                : TimeUtil.getHours(milliseconds);

        if (hours > 0) {
            this.includeHours = true;
        }
        return this;
    }

    /**
     * Builds the formatted time string based on the configuration of this formatter.
     *
     * @return the formatted time as a {@code String}
     */
    public String format() {
        long milliseconds = getValue(TimeUtil::getMilliseconds);
        long seconds = getValue(TimeUtil::getSeconds);
        long minutes = getValue(TimeUtil::getMinutes);
        long hours = getValue(TimeUtil::getHours);

        final String millisecondsFormatted = useSuffixes
                ? (milliseconds + "ms")
                : superscriptMs
                ? formatSuperscriptedMs(milliseconds)
                : "." + milliseconds;

        return (includeHours && hours > 0 ? formatComponent(hours, useSuffixes ? "h " : ":") : "") +
                // Minutes
                formatComponent(minutes, useSuffixes ? "min " : ":") +
                // Seconds
                formatComponent(seconds, useSuffixes ? "sec " : "") +
                // Milliseconds
                millisecondsFormatted;
    }

    @SuppressWarnings("all")
    private String formatSuperscriptedMs(long milliseconds) {
        return String.format(TWO_DECIMALS_FORMAT, milliseconds)
                .replace("0", "\u2070")
                .replace("1", "\u00B9")
                .replace("2", "\u00B2")
                .replace("3", "\u00B3")
                .replace("4", "\u2074")
                .replace("5", "\u2075")
                .replace("6", "\u2076")
                .replace("7", "\u2077")
                .replace("8", "\u2078")
                .replace("9", "\u2079");
    }

    private long getValue(Function<Long, Long> extractor) {
        long base = (stopWatch != null)
                ? stopWatch.getTime()
                : milliseconds;
        return extractor.apply(base);
    }

    private String formatComponent(long value, String suffixOrSeparator) {
        return String.format(TWO_DECIMALS_FORMAT, value) + suffixOrSeparator;
    }
}
