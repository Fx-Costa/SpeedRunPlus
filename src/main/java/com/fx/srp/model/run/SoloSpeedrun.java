package com.fx.srp.model.run;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.ui.TimerUtil;
import lombok.Getter;
import org.apache.commons.lang.time.StopWatch;

import java.util.List;

/**
 * Represents a solo speedrun for a single player.
 * <p>
 * Extends {@link AbstractSpeedrun} and manages a single {@link Speedrunner}.
 * </p>
 */
@Getter
public class SoloSpeedrun extends AbstractSpeedrun {

    private final Speedrunner speedrunner;

    /**
     * Constructs a new {@code SoloSpeedrun}.
     *
     * @param gameManager the {@code GameManager} handling this run
     * @param speedrunner the {@code Speedrunner} participating in the run
     * @param stopWatch the {@code StopWatch} used to track elapsed time
     * @param seed the seed used for world generation, may be {@code null}
     */
    public SoloSpeedrun(GameManager gameManager, Speedrunner speedrunner, StopWatch stopWatch, Long seed) {
        super(gameManager, speedrunner, stopWatch, seed);
        this.speedrunner = speedrunner;
    }

    /**
     * Initializes the timers for this solo speedrun.
     * <p>
     * This creates a timer HUD for the single participant.
     * </p>
     */
    @Override
    public void initializeTimers() {
        TimerUtil.createTimer(List.of(speedrunner.getPlayer()), getStopWatch());
    }
}
