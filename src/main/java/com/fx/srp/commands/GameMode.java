package com.fx.srp.commands;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum GameMode {
    // Single player speedrun
    SOLO("solo",
            // All actions (subcommands) for the solo game mode
            EnumSet.of(Action.START, Action.RESET, Action.STOP),

            // All commands allowed during a solo speedrun
            EnumSet.of(Action.RESET, Action.STOP),

            false
    ),

    // Multiplayer (1v1) speedrun
    BATTLE("battle",
            // All actions (subcommands) for the battle game mode
            EnumSet.of(Action.REQUEST, Action.RESET, Action.ACCEPT, Action.DECLINE, Action.SURRENDER),

            // All commands allowed during a battle speedrun
            EnumSet.of(Action.RESET, Action.SURRENDER),

            true
    );

    private final String name;

    // All actions for a given game mode
    private final Set<Action> actions;

    // Actions that is allowed for the given game mode during a run
    private final Set<Action> allowedDuringRun;

    // Whether the game mode is multiplayer
    private final boolean isMultiplayer;

    /**
     * Creates a game mode for SRPCommands.
     *
     * @param name           the name of the game mode
     * @param actions        all supported actions for this game mode
     * @param allowedActions actions permissible during an active run
     * @param isMultiplayer  whether the mode supports multiple players
     */
    GameMode(String name, Set<Action> actions, Set<Action> allowedActions, boolean isMultiplayer) {
        this.name = name;
        this.actions = actions;
        this.allowedDuringRun = allowedActions;
        this.isMultiplayer = isMultiplayer;
    }

    /**
     * Checks whether the given action is valid for this game mode.
     *
     * @param action the action to test
     * @return {@code true} if the action is supported by this mode, otherwise {@code false}
     */
    public boolean isValidAction(Action action) {
        return actions.contains(action);
    }

    /**
     * Determines whether the given action is permitted while a run
     * in this game mode is currently active.
     *
     * @param action the action to test
     * @return {@code true} if this action is allowed during a run
     */
    public boolean isAllowedDuringRun(Action action) {
        return allowedDuringRun.contains(action);
    }

    /**
     * Attempts to match a string to a {@link GameMode} by its command name.
     *
     * @param input the user-provided game mode name
     * @return the matching {@code GameMode}, or {@code null} if none match
     */
    public static GameMode parse(String input) {
        for (GameMode gameMode : values()) {
            if (gameMode.name.equalsIgnoreCase(input)) return gameMode;
        }
        return null;
    }
}

