package com.fx.srp.model.run;

import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.util.ui.TimerUtil;
import com.fx.srp.commands.GameMode;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a coop speedrun shared between two or more players on a single world set
 * and stopwatch.
 * <p>
 * Extends {@link Speedrun} and provides logic for managing a cooperative run between any
 * number of participants.
 * </p>
 */
public class CoopSpeedrun extends Speedrun {

    private final List<Speedrunner> speedrunners;

    /**
     * Constructs a new {@code CoopSpeedrun}.
     *
     * @param gameMode     the {@code GameMode} that the run represents
     * @param speedrunners the participants in this coop speedrun; by contract, the first
     *                     entry is the party leader (see {@link #getLeader()})
     * @param stopWatch    the {@code StopWatch} instance to track elapsed time
     * @param seed         optional seed for world generation. May be {@code null}
     */
    public CoopSpeedrun(GameMode gameMode,
                        List<Speedrunner> speedrunners,
                        StopWatch stopWatch,
                        Long seed
    ) {
        super(gameMode, speedrunners.get(0), stopWatch, seed);
        this.speedrunners = List.copyOf(speedrunners);
    }

    /** The party leader — always {@code speedrunners.get(0)} by construction. */
    public Speedrunner getLeader() {
        return speedrunners.get(0);
    }

    @Override
    public void initializeTimers() {
        TimerUtil.createTimer(
                speedrunners.stream().map(Speedrunner::getPlayer).collect(Collectors.toList()),
                getStopWatch()
        );
    }

    @Override
    public List<Speedrunner> getSpeedrunners() {
        return speedrunners;
    }

    @Override
    public void onPlayerLeave(Player leaver) {
        // Whole run ends if any participant disconnects
        gameMode.getManager().abort(this, null, "A teammate has left the game!");
    }
}