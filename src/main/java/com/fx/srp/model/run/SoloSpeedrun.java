package com.fx.srp.model.run;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.ui.TimerUtil;
import lombok.Getter;
import org.apache.commons.lang.time.StopWatch;

import java.util.List;

@Getter
public class SoloSpeedrun extends AbstractSpeedrun {

    private final Speedrunner speedrunner;

    public SoloSpeedrun(GameManager gameManager, Speedrunner speedrunner, StopWatch stopWatch, Long seed) {
        super(gameManager, speedrunner, stopWatch, seed);
        this.speedrunner = speedrunner;
    }

    @Override
    public void initializeTimers() {
        TimerUtil.createTimer(List.of(speedrunner.getPlayer()), getStopWatch());
    }
}
