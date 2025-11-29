package com.fx.srp.commands;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Logger logger = Bukkit.getLogger();

    private final GameManager gameManager;
    private final SRPCommandParser parser = new SRPCommandParser();

    public CommandHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String commandString,
                             @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        // Parse the command
        Optional<SRPCommand> parsed = parser.parse(commandString, args);
        if (parsed.isEmpty()) return true;

        SRPCommand srpCommand = parsed.get();

        // Re-execute it on the player's behalf if they're allowed to at the current state
        if (!canUseCommand(player, srpCommand)) return true;

        // Execute the command
        logger.info(player.getName() + " executed: " + srpCommand);
        executeCommand(player, srpCommand);
        return true;
    }

    private boolean canUseCommand(Player player, SRPCommand command) {
        // OPs bypass everything
        if (player.hasPermission("srp.admin")) return true;

        GameMode gameMode = command.getGameMode();
        Action action = command.getAction();

        // Cannot use commands while frozen
        Optional<Speedrunner> runnerOpt = gameManager.getSpeedrunner(player);
        if (runnerOpt.isPresent() && runnerOpt.get().isFrozen()) {
            player.sendMessage(ChatColor.RED + "You cannot use commands during the countdown!");
            return false;
        }

        // Restrictions during an active run
        if (gameManager.isInRun(player)) {
            if (!gameMode.isAllowedDuringRun(action)) {
                player.sendMessage(ChatColor.RED + "You cannot use this command during a run!");
                return false;
            }
        }

        // Enforce permissions
        // Overall usage
        if (!player.hasPermission("srp.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }

        // Gamemode-specific permission
        if (!player.hasPermission("srp." + gameMode.getName().toLowerCase())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use the " + gameMode.getName() + " gamemode!");
            return false;
        }

        // Action-specific permission
        if (!player.hasPermission("srp." + gameMode.getName().toLowerCase() + "." + action.getName().toLowerCase())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to " + action.getName() + "!");
            return false;
        }

        return true;
    }

    private void executeCommand(Player player, SRPCommand command) {
        GameMode gameMode = command.getGameMode();
        switch (gameMode) {
            case SOLO: gameManager.getSoloManager().handleCommand(player, command); break;
            case BATTLE: gameManager.getBattleManager().handleCommand(player, command); break;
            default: player.sendMessage(ChatColor.RED + "Unknown game mode."); break;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;

        // Helper: filter list by input
        BiFunction<Collection<String>, String, List<String>> filterByInput = (list, input) ->
                list.stream()
                        .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                        .collect(Collectors.toList());

        switch (args.length) {

            // At: /srp <TAB>
            case 1: {
                // Filtering by game modes the player has permission
                List<String> allowedGamemodes = Arrays.stream(GameMode.values())
                        .map(GameMode::getName)
                        .filter(mode -> player.hasPermission("srp." + mode.toLowerCase()))
                        .collect(Collectors.toList());

                return filterByInput.apply(allowedGamemodes, args[0]);
            }

            // At: /srp <gamemode> <TAB>
            case 2: {
                // Filtering by actions based on the selected game mode the player has permission
                GameMode mode = GameMode.parse(args[0]);
                if (mode == null || !player.hasPermission("srp." + mode.getName().toLowerCase()))
                    return Collections.emptyList();

                List<String> allowedActions = mode.getActions().stream()
                        .map(Action::getName)
                        .filter(name -> player.hasPermission("srp." + mode.getName().toLowerCase() + "." + name.toLowerCase()))
                        .collect(Collectors.toList());

                return filterByInput.apply(allowedActions, args[1]);
            }

            // At: /srp <gamemode> <action> <TAB>
            case 3: {
                // Filtering by online players if the game mode is multiplayer and the player has permission
                GameMode mode = GameMode.parse(args[0]);
                if (mode == null || !mode.isMultiplayer()|| !player.hasPermission("srp." + mode.getName().toLowerCase()))
                    return Collections.emptyList();

                List<String> onlinePlayerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());

                return filterByInput.apply(onlinePlayerNames, args[2]);
            }

            default: return Collections.emptyList();
        }
    }
}
