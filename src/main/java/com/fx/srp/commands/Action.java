package com.fx.srp.commands;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.context.CommandContext;
import com.fx.srp.managers.GameManager;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public enum Action {

    START((gameManager, ctx) -> {
        Player sender = (Player) ctx.getSender();
        GameMode gameMode = ctx.get(CommandRegistry.ContextKeys.GAME_MODE.getKey());
        gameMode.getManager().start(sender);
    }),

    RESET((gameManager, ctx) -> {
        Player sender = (Player) ctx.getSender();
        GameMode gameMode = ctx.get(CommandRegistry.ContextKeys.GAME_MODE.getKey());
        gameMode.getManager().reset(sender);
    }),

    STOP((gameManager, ctx) -> {
        Player sender = (Player) ctx.getSender();
        gameManager.abortRun(sender);
    }),

    REQUEST((gameManager, ctx) -> {
        Player sender = (Player) ctx.getSender();
        GameMode gameMode = ctx.get(CommandRegistry.ContextKeys.GAME_MODE.getKey());
        Player target = ctx.get(CommandRegistry.ContextKeys.PLAYER_TARGET.getKey());
        gameMode.getManager().asMultiplayerManager().ifPresent(gameModeManager ->
                gameModeManager.request(sender, target, gameMode)
        );
    }, PlayerArgument.of(CommandRegistry.ContextKeys.PLAYER_TARGET.getKey())),

    ACCEPT((gameManager, ctx) -> {
        GameMode gameMode = ctx.get(CommandRegistry.ContextKeys.GAME_MODE.getKey());
        gameMode.getManager().asMultiplayerManager().ifPresent(gameModeManager ->
                gameModeManager.accept((Player) ctx.getSender())
        );
    }),

    DECLINE((gameManager, ctx) -> {
        GameMode gameMode = ctx.get(CommandRegistry.ContextKeys.GAME_MODE.getKey());
        Player target = ctx.get(CommandRegistry.ContextKeys.PLAYER_TARGET.getKey());
        gameMode.getManager().asMultiplayerManager().ifPresent(gameModeManager ->
                gameModeManager.decline(target)
        );
    });

    private final BiConsumer<GameManager, CommandContext<CommandSender>> executor;
    private final List<CommandArgument<CommandSender, ?>> arguments;

    /**
     * Creates an action for the SRP Commands.
     *
     * @param executor the executor for the command
     */
    @SafeVarargs
    Action(BiConsumer<GameManager, CommandContext<CommandSender>> executor, CommandArgument<CommandSender, ?>... args) {
        this.executor = executor;
        this.arguments = new ArrayList<>();
        if (args != null) this.arguments.addAll(Arrays.asList(args));
    }
}
