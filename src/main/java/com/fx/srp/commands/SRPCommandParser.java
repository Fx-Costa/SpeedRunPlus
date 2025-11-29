package com.fx.srp.commands;

import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Parses raw string input into a structured {@link SRPCommand}.
 *
 * <p>This parser is responsible for interpreting the command label and
 * arguments issued by a player and converting them into a validated
 * {@link SRPCommand} instance using the {@link SRPCommand.Builder}.</p>
 */
@NoArgsConstructor
public class SRPCommandParser {

    private static final int MINIMUM_ARGS = 2;

    /**
     * Attempts to parse a raw command label and argument array into an
     * {@link SRPCommand}. Any invalid combination or missing data results
     * in an empty {@link Optional}.
     *
     * @param commandString the base command used (e.g., "srp")
     * @param args          the arguments passed after the command
     * @return an {@link Optional} containing the parsed {@link SRPCommand},
     *         or {@code Optional.empty()} if parsing fails at any stage
     */
    public Optional<SRPCommand> parse(String commandString, String... args) {
        if (!commandString.equalsIgnoreCase(SRPCommand.getSrp())) return Optional.empty();
        if (args.length < MINIMUM_ARGS) return Optional.empty();

        com.fx.srp.commands.GameMode mode = com.fx.srp.commands.GameMode.parse(args[0]);
        if (mode == null) return Optional.empty();

        com.fx.srp.commands.Action action = com.fx.srp.commands.Action.parse(args[1], mode);
        if (action == null) return Optional.empty();

        Player targetPlayer = null;
        if (args.length > MINIMUM_ARGS && mode == GameMode.BATTLE) {
            targetPlayer = Bukkit.getPlayer(args[2]);
        }

        return Optional.of(
                SRPCommand.builder()
                        .withGameMode(mode)
                        .withAction(action)
                        .withTargetPlayer(targetPlayer)
                        .build()
        );
    }
}
