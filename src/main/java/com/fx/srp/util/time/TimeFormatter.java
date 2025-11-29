package com.fx.srp.util.time;

import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;

public class TimeFormatter {

    private final StopWatch stopWatch;
    private final long milliseconds;
    private boolean superscriptMs = false;
    private boolean useSuffixes = false;
    private boolean includeHours = false;

    public TimeFormatter(@NonNull StopWatch stopWatch) {
        this.stopWatch = stopWatch;
        this.milliseconds = stopWatch.getTime();
    }

    public TimeFormatter(long milliseconds) {
        this.stopWatch = null;
        this.milliseconds = milliseconds;
    }

    public TimeFormatter superscriptMs() {
        this.superscriptMs = true;
        return this;
    }

    public TimeFormatter useSuffixes() {
        this.useSuffixes = true;
        return this;
    }

    public TimeFormatter includeHours() {
        long hours = (stopWatch != null)
                ? TimeUtil.getHours(stopWatch)
                : TimeUtil.getHours(milliseconds);

        if (hours > 0) {
            this.includeHours = true;
        }
        return this;
    }

    public String format() {
        long ms = stopWatch != null ? TimeUtil.getMilliseconds(stopWatch) : TimeUtil.getMilliseconds(milliseconds);
        long sec = stopWatch != null ? TimeUtil.getSeconds(stopWatch) : TimeUtil.getSeconds(milliseconds);
        long min = stopWatch != null ? TimeUtil.getMinutes(stopWatch) : TimeUtil.getMinutes(milliseconds);
        long hours = stopWatch != null ? TimeUtil.getHours(stopWatch) : TimeUtil.getHours(milliseconds);

        // Build the formatted string
        StringBuilder builder = new StringBuilder();

        // Add hours if specified
        if (includeHours && hours > 0 && useSuffixes) builder.append(String.format("%02d", hours)).append("h ");
        else if (includeHours && hours > 0) builder.append(String.format("%02d", hours)).append(":");

        // Always add minutes, seconds & milliseconds
        // Minutes
        if (useSuffixes) builder.append(String.format("%02d", min)).append("min ");
        else builder.append(String.format("%02d", min)).append(":");

        // Seconds
        if (useSuffixes) builder.append(String.format("%02d", sec)).append("sec ");
        else builder.append(String.format("%02d", sec));

        // Milliseconds (superscript if specified)
        if (useSuffixes) builder.append(ms).append("ms");
        else if (superscriptMs) builder.append(superscriptMs(ms));
        else builder.append(".").append(ms);

        return builder.toString();
    }

    @SuppressWarnings("all")
    private String superscriptMs(long ms) {
        return String.format("%02d", ms)
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
}
