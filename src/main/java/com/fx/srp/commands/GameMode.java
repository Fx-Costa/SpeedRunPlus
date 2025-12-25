package com.fx.srp.commands;

import com.fx.srp.managers.gamemodes.IGameModeManager;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

public enum GameMode {
    // Single player speedrun
    SOLO(
            // All actions (subcommands) for the solo game mode
            EnumSet.of(Action.START, Action.RESET, Action.STOP),

            // All commands allowed during a solo speedrun
            EnumSet.of(Action.RESET, Action.STOP)
    ),

    // Multiplayer coop speedrun
    COOP(
            // All actions (subcommands) for the coop game mode
            EnumSet.of(Action.REQUEST, Action.ACCEPT, Action.DECLINE, Action.STOP),

            // All commands allowed during a coop speedrun
            EnumSet.of(Action.STOP)
    ),

    // Multiplayer (1v1) speedrun
    BATTLE(
            // All actions (subcommands) for the battle game mode
            EnumSet.of(Action.REQUEST, Action.RESET, Action.ACCEPT, Action.DECLINE, Action.STOP),

            // All commands allowed during a battle speedrun
            EnumSet.of(Action.RESET, Action.STOP)
    );

    // All actions for a given game mode
    @Getter private final Set<Action> actions;

    // Actions that is allowed for the given game mode during a run
    private final Set<Action> allowedDuringRun;

    // The IGameModeManager that manages the game mode
    @Getter private IGameModeManager manager;

    /**
     * Creates a game mode for commands.
     *
     * @param actions all supported actions for this game mode
     * @param allowedActions actions permissible during an active run
     */
    GameMode(Set<Action> actions, Set<Action> allowedActions) {
        this.actions = actions;
        this.allowedDuringRun = allowedActions;
    }

    /**
     * Binds a {@code IGameModeManager} to a GameMode - making it aware of the class that manages it
     *
     * @param manager the {@code IGameModeManager} that manages the game mode
     */
    public void bindManager(IGameModeManager manager) {
        this.manager = manager;
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
}
