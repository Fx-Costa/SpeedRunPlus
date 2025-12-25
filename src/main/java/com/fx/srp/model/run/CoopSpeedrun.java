package com.fx.srp.model.run;

import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.ui.TimerUtil;
import com.fx.srp.commands.GameMode;
import lombok.Getter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Represents a coop speedrun with two players: a party leader and a partner.
 * <p>
 * Extends {@link Speedrun} and provides logic for managing a two-player competitive run.
 * </p>
 */
public class CoopSpeedrun extends Speedrun {

    @Getter
    private final Speedrunner leader;

    @Getter
    private final Speedrunner partner;

    /**
     * Constructs a new {@code CoopSpeedrun}.
     *
     * @param gameMode the {@code GameMode} that the run represents
     * @param leader      The {@code Speedrunner} who initiated the coop speedrun.
     * @param partner     The {@code Speedrunner} who was invited.
     * @param stopWatch   The {@code StopWatch} instance to track elapsed time.
     * @param seed        Optional seed for world generation. May be {@code null}.
     */
    public CoopSpeedrun(GameMode gameMode,
                        Speedrunner leader,
                        Speedrunner partner,
                        StopWatch stopWatch,
                        Long seed
    ) {
        super(gameMode, leader, stopWatch, seed);
        this.leader = leader;
        this.partner = partner;
    }

    /**
     * Initializes timers for both participants of the coop run.
     * <p>
     * Uses {@link TimerUtil#createTimer(List, StopWatch)} to create a shared timer HUD for both players.
     * </p>
     */
    @Override
    public void initializeTimers() {
        TimerUtil.createTimer(List.of(leader.getPlayer(), partner.getPlayer()), getStopWatch());
    }

    /**
     * Returns a list of all speedrunners participating in this coop run.
     *
     * @return a {@code List<Speedrunner>} containing {@code leader} and {@code partner}.
     */
    @Override
    public List<Speedrunner> getSpeedrunners() {
        return List.of(leader, partner);
    }

    /**
     * Called when a player leaves the server during this coop run.
     * <p>
     * Automatically ends the coop run and declares no winners.
     * </p>
     *
     * @param leaver The {@code Player} who left the server.
     */
    @Override
    public void onPlayerLeave(Player leaver) {
        // No winners
        gameMode.getManager().abort(this, null, "Your partner has left the game!");
    }
}
