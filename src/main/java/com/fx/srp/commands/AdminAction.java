package com.fx.srp.commands;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.context.CommandContext;
import com.fx.srp.managers.GameManager;
import com.fx.srp.model.seed.SeedCategory;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public enum AdminAction {

    HELP((gameManager, ctx) ->
            gameManager.sendAdminHelpMessage(ctx.getSender())
    ),

    STOP((gameManager, ctx) -> {
        if (ctx.contains("target")) {
            Player target = ctx.get("target");
            gameManager.abortRun(target);
        } else {
            gameManager.abortAllRuns();
        }
    }, PlayerArgument.optional("target")),

    PODIUM((gameManager, ctx) -> {
        String load = "load";
        String unload = "unload";
        String action = ctx.get("action");

        if (load.equalsIgnoreCase(action)) {
            gameManager.loadPodium();
        }
        else if (unload.equalsIgnoreCase(action)) {
            gameManager.unloadPodium();
        }
    }),

    SEED((gameManager, ctx) -> {
        SeedCategory.SeedType type = ctx.get("type");
        int amount = ctx.get("amount");
        CommandSender sender = ctx.get("sender");
        gameManager.addSeed(type, amount, sender);
    }, EnumArgument.of(SeedCategory.SeedType.class, "type"), IntegerArgument.of("amount"));

    private final BiConsumer<GameManager, CommandContext<CommandSender>> executor;
    private final List<CommandArgument<CommandSender, ?>> arguments;

    /**
     * Creates an Admin action for the SRP commands.
     *
     * @param executor the executor for the command
     */
    @SafeVarargs
    AdminAction(
            BiConsumer<GameManager, CommandContext<CommandSender>> executor,
            CommandArgument<CommandSender, ?>... args
    ) {
        this.executor = executor;
        this.arguments = new ArrayList<>();
        if (args != null) this.arguments.addAll(Arrays.asList(args));
    }
}
