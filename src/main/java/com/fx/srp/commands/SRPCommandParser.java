package com.fx.srp.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SRPCommandParser {

    public Optional<SRPCommand> parse(String commandString, String[] args) {
        if (!commandString.equalsIgnoreCase(SRPCommand.SRP)) return Optional.empty();
        if (args.length < 2) return Optional.empty();

        com.fx.srp.commands.GameMode mode = com.fx.srp.commands.GameMode.parse(args[0]);
        if (mode == null) return Optional.empty();

        com.fx.srp.commands.Action action = com.fx.srp.commands.Action.parse(args[1], mode);
        if (action == null) return Optional.empty();

        Player targetPlayer = null;
        if (args.length > 2 && mode == GameMode.BATTLE) {
            targetPlayer = Bukkit.getPlayer(args[2]);
        }

        return Optional.of(
                SRPCommand.builder()
                        .gameMode(mode)
                        .action(action)
                        .targetPlayer(targetPlayer)
                        .build()
        );
    }
}
