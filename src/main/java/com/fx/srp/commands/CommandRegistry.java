package com.fx.srp.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import com.fx.srp.managers.GameManager;
import com.fx.srp.model.seed.SeedCategory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Central registry responsible for registering all {@code /srp} commands.
 * <p>
 * This class dynamically builds and registers commands based on available
 * {@link GameMode}s and their associated {@link Action}s. Each action defines
 * its own arguments and execution logic, allowing new commands to be added
 * by extending the corresponding enums without modifying this registry.
 * </p>
 *
 * <p>
 * The registry is static and non-instantiable.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandRegistry {

    private static final String BASE_COMMAND = "srp";

    private static final String PERMISSION_PREFIX = BASE_COMMAND + ".";

    // Context keys
    public enum ContextKeys {
        GAME_MODE("gamemode"),
        PLAYER_TARGET("target");

        @Getter private final String key;

        ContextKeys(String key) {
            this.key = key;
        }
    }

    /**
     * Registers all plugin commands with the provided {@link BukkitCommandManager}.
     * <p>
     * For each {@link GameMode}, all associated {@link Action}s are registered
     * under the root {@code /srp} command using the following structure:
     * </p>
     *
     * <pre>
     * /srp &lt;gamemode&gt; &lt;action&gt; [arguments...]
     * </pre>
     *
     * <p>
     * and applying permissions.
     * </p>
     *
     * @param commandManager the command manager used to register commands
     * @param gameManager   the game manager instance passed to command executors
     */
    public static void registerCommands(CommandManager<CommandSender> commandManager, GameManager gameManager) {
        String usagePermission = PERMISSION_PREFIX + "use";

        // Register game mode commands
        Arrays.stream(GameMode.values()).forEach(gameMode -> {
            final String gameModeName = gameMode.name().toLowerCase();

            gameMode.getActions().forEach(action -> {
                final String actionName = action.name().toLowerCase();

                // Start builder: /srp <gamemode> <action>
                Command.Builder<CommandSender> builder = commandManager.commandBuilder(BASE_COMMAND)
                        .literal(gameModeName)
                        .literal(actionName)
                        .permission(usagePermission)
                        .permission(PERMISSION_PREFIX + gameModeName);

                // Append any arguments that this Action declares (PlayerArgument, etc.)
                // Use copy() so each registration gets its own instance
                builder = action.getArguments().stream()
                        .map(CommandArgument::copy)
                        .reduce(
                                builder,
                                Command.Builder::argument,
                                (b1, b2) -> b1
                        );

                // Set context entries before calling executor
                builder = builder.handler(ctx -> {
                    ctx.set(ContextKeys.GAME_MODE.getKey(), gameMode);

                    // Player-only enforcement
                    if (!(ctx.getSender() instanceof Player)) {
                        ctx.getSender().sendMessage("This command must be run by a player.");
                        return;
                    }

                    // Execute action
                    action.getExecutor().accept(gameManager, ctx);
                });

                commandManager.command(builder);
            });
        });

        // Register help command
        commandManager.command(
                commandManager.commandBuilder(BASE_COMMAND)
                        .literal("help")
                        .permission(usagePermission)
                        .handler(ctx -> gameManager.sendHelpMessage(ctx.getSender()))
        );

        // Register admin commands
        registerAdminCommands(commandManager, gameManager);
    }

    private static void registerAdminCommands(CommandManager<CommandSender> commandManager, GameManager gameManager) {
        String adminPermission = PERMISSION_PREFIX + "admin";
        String adminLiteral = "admin";

        // Admin help command
        commandManager.command(
                commandManager.commandBuilder(BASE_COMMAND)
                        .literal(adminLiteral)
                        .literal("help")
                        .permission(adminPermission)
                        .handler(ctx ->
                                gameManager.sendAdminHelpMessage(ctx.getSender())
                        )
        );

        // Admin stop command
        commandManager.command(
                commandManager.commandBuilder(BASE_COMMAND)
                        .literal(adminLiteral)
                        .literal("stop")
                        .permission(adminPermission)
                        .argument(PlayerArgument.optional("target"))
                        .handler(ctx ->
                                AdminAction.STOP.getExecutor().accept(gameManager, ctx)
                        )
        );

        // Admin podium command
        CommandArgument<CommandSender, String> podiumArg =
                StringArgument.<CommandSender>builder("action")
                        .withSuggestionsProvider((ctx, input) ->
                                List.of("load", "unload")
                        )
                        .build();

        commandManager.command(
                commandManager.commandBuilder(BASE_COMMAND)
                        .literal(adminLiteral)
                        .literal("podium")
                        .argument(podiumArg)
                        .permission(adminPermission)
                        .handler(ctx ->
                                AdminAction.PODIUM.getExecutor().accept(gameManager, ctx)
                        )
        );

        // Admin seed command
        CommandArgument<CommandSender, SeedCategory.SeedType> seedTypeArg = EnumArgument.of(
                SeedCategory.SeedType.class,
                "type"
        );

        CommandArgument<CommandSender, Integer> amountArg = IntegerArgument.<CommandSender>builder("amount")
                .withMin(1)
                .withMax(10)
                .build();

        commandManager.command(commandManager.commandBuilder(BASE_COMMAND)
                .literal(adminLiteral)
                .literal("seed")
                .permission(adminPermission)
                .argument(seedTypeArg)
                .argument(amountArg)
                .handler(ctx ->
                        AdminAction.SEED.getExecutor().accept(gameManager, ctx)
                )
        );
    }
}
