package com.fx.srp.util.time;

import org.apache.commons.lang.time.StopWatch;

public class TimeUtil {

    public static long getMilliseconds(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long ms = stopWatch.getTime() % 1000L;
        return (ms / 10L);  // 2 Digits
    }

    public static long getSeconds(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long sec = stopWatch.getTime() / 1000L;
        return sec % 60;
    }

    public static long getMinutes(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long sec = stopWatch.getTime() / 1000L;
        return sec % 3600 / 60;
    }

    public static long getHours(StopWatch stopWatch) {
        if (stopWatch == null) return 0;
        long sec = stopWatch.getTime() / 1000L;
        return sec / 3600;
    }

    public static long getMilliseconds(long milliseconds) {
        long ms = milliseconds % 1000L;
        return ms / 10L; // 2 digits
    }

    public static long getSeconds(long milliseconds) {
        long sec = milliseconds / 1000L;
        return sec % 60;
    }

    public static long getMinutes(long milliseconds) {
        long sec = milliseconds / 1000L;
        return (sec % 3600) / 60;
    }

    public static long getHours(long milliseconds) {
        long sec = milliseconds / 1000L;
        return sec / 3600;
    }
}
