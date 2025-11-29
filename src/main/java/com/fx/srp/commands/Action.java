package com.fx.srp.commands;

import lombok.Getter;

@Getter
public enum Action {
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

    Action(String name) {
        this.name = name;
    }

    public static Action parse(String input, GameMode mode) {
        return mode.getActions().stream()
                .filter(a -> a.getName().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }
}

