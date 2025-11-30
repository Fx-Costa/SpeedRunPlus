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

    private static final int MINIMUM_ARGS = 1;
    private static final int MINIMUM_GAME_ARGS = 2;

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
        if (!commandString.equalsIgnoreCase(SRPCommand.getSrp()) || args.length < MINIMUM_ARGS) return Optional.empty();

        // Likely a game command with a player argument
        if (args.length > MINIMUM_GAME_ARGS) return parseGameCommand(args[0], args[1], args[2]);

        // Likely a game command with no player argument
        if (args.length == MINIMUM_GAME_ARGS) return parseGameCommand(args[0], args[1], null);

        // Likely a utility command
        return parseUtilityCommand(args[0]);
    }

    private Optional<SRPCommand> parseGameCommand(String gameModeArg, String actionArg, String playerArg) {
        GameMode mode = GameMode.parse(gameModeArg);
        if (mode == null) return Optional.empty();

        Action action = Action.parse(actionArg, mode);
        if (action == null) return Optional.empty();

        Player targetPlayer = mode.isMultiplayer() ? Bukkit.getPlayer(playerArg) : null;

        return Optional.of(
                SRPCommand.builder()
                        .withGameMode(mode)
                        .withAction(action)
                        .withTargetPlayer(targetPlayer)
                        .build()
        );
    }

    private Optional<SRPCommand> parseUtilityCommand(String command) {
        Action action = Action.parse(command);
        return Optional.of(
                SRPCommand.builder()
                        .withAction(action)
                        .build()
        );
    }
}
