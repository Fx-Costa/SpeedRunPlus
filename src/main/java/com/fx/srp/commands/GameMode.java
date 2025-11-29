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

    GameMode(String name, Set<Action> actions, Set<Action> allowedActions, boolean isMultiplayer) {
        this.name = name;
        this.actions = actions;
        this.allowedDuringRun = allowedActions;
        this.isMultiplayer = isMultiplayer;
    }

    public boolean isValidAction(Action action) {
        return actions.contains(action);
    }

    public boolean isAllowedDuringRun(Action action) {
        return allowedDuringRun.contains(action);
    }

    public static GameMode parse(String input) {
        for (GameMode gm : values()) {
            if (gm.name.equalsIgnoreCase(input)) return gm;
        }
        return null;
    }
}

