package com.fx.srp.model.run;

import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.ui.TimerUtil;
import com.fx.srp.commands.GameMode;
import lombok.Getter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Represents a battle speedrun between two players: a challenger and a challengee.
 * <p>
 * Extends {@link Speedrun} and provides logic for managing a two-player competitive run.
 * </p>
 */
public class BattleSpeedrun extends Speedrun {

    @Getter
    private final Speedrunner challenger;

    @Getter
    private final Speedrunner challengee;

    /**
     * Constructs a new {@code BattleSpeedrun}.
     *
     * @param gameMode the {@code GameMode} that the run represents
     * @param challenger  The {@code Speedrunner} who initiated the challenge.
     * @param challengee  The {@code Speedrunner} who was challenged.
     * @param stopWatch   The {@code StopWatch} instance to track elapsed time.
     * @param seed        Optional seed for world generation. May be {@code null}.
     */
    public BattleSpeedrun(GameMode gameMode,
                          Speedrunner challenger,
                          Speedrunner challengee,
                          StopWatch stopWatch,
                          Long seed
    ) {
        super(gameMode, challenger, stopWatch, seed);
        this.challenger = challenger;
        this.challengee = challengee;
    }

    /**
     * Initializes timers for both participants of the battle.
     * <p>
     * Uses {@link TimerUtil#createTimer(List, StopWatch)} to create a shared timer HUD for both players.
     * </p>
     */
    @Override
    public void initializeTimers() {
        TimerUtil.createTimer(List.of(challenger.getPlayer(), challengee.getPlayer()), getStopWatch());
    }

    /**
     * Returns a list of all speedrunners participating in this battle.
     *
     * @return a {@code List<Speedrunner>} containing {@code challenger} and {@code challengee}.
     */
    @Override
    public List<Speedrunner> getSpeedrunners() {
        return List.of(challenger, challengee);
    }

    /**
     * Called when a player leaves the server during this battle.
     * <p>
     * Automatically ends the battle and declares the remaining player as the winner.
     * </p>
     *
     * @param leaver The {@code Player} who left the server.
     */
    @Override
    public void onPlayerLeave(Player leaver) {
        gameMode.getManager().abort(this, leaver, "Your opponent has left the game!");
    }
}
