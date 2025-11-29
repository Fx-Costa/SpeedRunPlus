package com.fx.srp.model.run;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.ui.TimerUtil;
import lombok.Getter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;

import java.util.List;

public class BattleSpeedrun extends AbstractSpeedrun {

    @Getter
    private final Speedrunner challenger;

    @Getter
    private final Speedrunner challengee;

    public BattleSpeedrun(GameManager gameManager,
                          Speedrunner challenger,
                          Speedrunner challengee,
                          StopWatch stopWatch,
                          Long seed
    ) {
        super(gameManager, challenger, stopWatch, seed);
        this.challenger = challenger;
        this.challengee = challengee;
    }

    @Override
    public void initializeTimers() {
        TimerUtil.createTimer(List.of(challenger.getPlayer(), challengee.getPlayer()), getStopWatch());
    }

    @Override
    public List<Speedrunner> getSpeedrunners() {
        return List.of(challenger, challengee);
    }

    @Override
    public void onPlayerLeave(Player leaver) {
        // The opponent wins
        Speedrunner winner = getChallenger().getPlayer().equals(leaver) ? challengee : challenger;
        gameManager.finishRun(this, winner.getPlayer());
    }
}
