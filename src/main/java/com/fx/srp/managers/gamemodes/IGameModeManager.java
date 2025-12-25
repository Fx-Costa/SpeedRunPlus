package com.fx.srp.managers.gamemodes;

import com.fx.srp.model.run.Speedrun;
import lombok.NonNull;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Defines the contract for a manager of a specific game mode in the speedrun plugin.
 *
 * <p>Each game mode (e.g., solo, battle) must implement this interface to handle
 * start, reset, and stop the game mode-specific speedrun logic.</p>
 */
public interface IGameModeManager {

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
     * @param player the player requesting the reset
     */
    void reset(Player player);

    /**
     * Stops an active speedrun in this game mode.
     *
     * <p>This includes finishing timers, announcing results, updating leaderboards,
     * and performing cleanup.</p>
     *
     * @param player The player who won the run
     */
    void stop(@NonNull Player player);

    /**
     * Abort an active speedrun in this game mode.
     *
     * <p>This includes finishing timers, announcing results, and performing cleanup.</p>
     *
     * @param run the run that is aborted
     * @param sender an optional {@code CommandSender} responsible for aborting the run
     * @param reason an optional reason for why the run was aborted
     */
    void abort(@NonNull Speedrun run, CommandSender sender, String reason);

    /**
     * Exposes this manager as a MultiplayerGameModeManager if supported.
     */
    default Optional<MultiplayerGameModeManager<?>> asMultiplayerManager() {
        if (this instanceof MultiplayerGameModeManager) {
            return Optional.of((MultiplayerGameModeManager<?>) this);
        }
        return Optional.empty();
    }
}
