package com.fx.srp.commands;

import lombok.Getter;

@Getter
public enum Action {
    HELP("help"),

    // Solo actions
    START("start"),
    RESET("reset"),
    STOP("stop"),

    // Battle actions
    REQUEST("request"),
    ACCEPT("accept"),
    DECLINE("decline"),
    SURRENDER("surrender");

    private final String name;

    /**
     * Creates an action for SRPCommands.
     *
     * @param name           the name of the game mode
     */
    Action(String name) {
        this.name = name;
    }

    /**
     * Attempts to match a string to a {@link Action} by its command name.
     *
     * @param input the user-provided action name
     * @return the matching {@code Action}, or {@code null} if none match
     */
    public static Action parse(String input, GameMode mode) {
        return mode.getActions().stream()
                .filter(a -> a.getName().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }

    /**
     * Attempts to match a string to a {@link Action} by its command name.
     *
     * @param input the user-provided action name
     * @return the matching {@code Action}, or {@code null} if none match
     */
    public static Action parse(String input) {
        for (Action action : values()) {
            if (action.name.equalsIgnoreCase(input)) return action;
        }
        return null;
    }
}

