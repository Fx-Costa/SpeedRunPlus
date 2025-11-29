package com.fx.srp.managers.gamemodes;

import com.fx.srp.commands.SRPCommand;
import org.bukkit.entity.Player;

/**
 * Defines the contract for a manager of a specific game mode in the speedrun plugin.
 *
 * <p>Each game mode (e.g., solo, battle) must implement this interface to handle
 * player commands, start, reset, and stop the game mode-specific speedrun logic.</p>
 *
 * @param <T> the type of speedrun this manager handles
 */
public interface IGameModeManager<T> {

    /**
     * Handles a command issued by a player in this game mode.
     *
     * <p>Implementations should perform any necessary validation, execute actions,
     * and provide feedback to the player.</p>
     *
     * @param player  the player executing the command
     * @param command the command to handle
     */
    void handleCommand(Player player, SRPCommand command);

    /**
     * Starts a new speedrun for the given player in this game mode.
     *
     * <p>This typically involves creating worlds, initializing timers, freezing/unfreezing
     * players, and setting up the game state.</p>
     *
     * @param player the player starting the speedrun
     */
    void start(Player player);

    /**
     * Resets an existing speedrun for a player in this game mode.
     *
     * <p>This usually involves recreating worlds, resetting player state,
     * while preserving the original seed.</p>
     *
     * @param speedrun the speedrun instance to reset
     * @param player   the player requesting the reset
     */
    void reset(T speedrun, Player player);

    /**
     * Stops an active speedrun in this game mode.
     *
     * <p>This includes finishing timers, announcing results, updating leaderboards,
     * and performing cleanup.</p>
     *
     * @param speedrun the speedrun instance to stop
     * @param player   optionally, the player associated with stopping the run
     */
    void stop(T speedrun, Player player);
}
