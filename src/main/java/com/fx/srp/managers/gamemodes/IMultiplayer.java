package com.fx.srp.managers.gamemodes;

import com.fx.srp.commands.GameMode;
import org.bukkit.entity.Player;

/**
 * Defines the contract for a manager of a specific game mode in the speedrun plugin.
 *
 * <p>Each multiplayer game mode (e.g., battle, coop) must implement this interface to handle
 * requests-, accepts, declines with the game mode-specific speedrun logic.</p>
 */
public interface IMultiplayer {

    /**
     * Sends a multiplayer speedrun request from one player to another.
     * <p>
     * Implementations should validate whether the sender and target are eligible
     * for a multiplayer session (for example, permissions, current game state,
     * or existing requests) and handle any necessary feedback messages.
     * </p>
     *
     * @param sender the player initiating the multiplayer request
     * @param target the player receiving the multiplayer request
     * @param gameMode the game mode requested
     */
    void request(Player sender, Player target, GameMode gameMode);

    /**
     * Accepts a pending multiplayer speedrun request for the given player.
     * <p>
     * Implementations should verify that a valid request exists and transition
     * the involved players into the appropriate multiplayer game state.
     * </p>
     *
     * @param target the player accepting the multiplayer request
     */
    void accept(Player target);

    /**
     * Declines a pending multiplayer speedrun request for the given player.
     * <p>
     * Implementations should cancel the request and notify all affected players
     * accordingly.
     * </p>
     *
     * @param target the player declining the multiplayer request
     */
    void decline(Player target);
}
