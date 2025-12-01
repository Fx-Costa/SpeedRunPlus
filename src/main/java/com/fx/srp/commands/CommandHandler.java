package com.fx.srp.commands;

import com.fx.srp.managers.GameManager;
import com.fx.srp.model.player.Speedrunner;
import lombok.AllArgsConstructor;
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
import java.util.stream.Stream;

/**
 * Handles execution and tab-completion of all SRP-related commands.
 * <p>
 * This class parses player input using {@link SRPCommandParser}, checks
 * permissions and gameplay constraints, and delegates execution to the
 * appropriate game-mode manager via {@link GameManager}.
 * </p>
 */
@AllArgsConstructor
public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Logger logger = Bukkit.getLogger();

    private final GameManager gameManager;
    private final SRPCommandParser parser = new SRPCommandParser();

    /**
     * Executes an SRP command issued by a player.
     *
     * @param sender        the entity that executed the command; must be a player
     * @param command       the Bukkit command object
     * @param commandString the raw command label used
     * @param args          the command arguments supplied by the player
     *
     * @return {@code true} always, as feedback is handled internally
     */
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

    /**
     * Provides dynamic in-game tab-completion for SRP commands.
     *
     * @param sender the entity requesting tab completion (player-only)
     * @param command the root Bukkit command
     * @param alias the alias used
     * @param args the arguments typed so far
     *
     * @return a list of valid completion options or an empty list if none apply
     */
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
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());

        String srp = SRPCommand.getSrp();
        switch (args.length) {

            // At: /srp <TAB>
            case 1: {
                // Allowed commands
                List<String> allowedCommands = List.of(Action.HELP.getName());

                // Filtering by game modes the player has permission
                List<String> allowedGamemodes = Arrays.stream(GameMode.values())
                        .map(GameMode::getName)
                        .filter(mode -> player.hasPermission(srp + "." + mode.toLowerCase()))
                        .collect(Collectors.toList());

                return filterByInput.apply(
                        Stream.concat(allowedGamemodes.stream(), allowedCommands.stream()).collect(Collectors.toList()),
                        args[0]
                );
            }

            // At: /srp <gamemode> <TAB>
            case 2: {
                // Filtering by actions based on the selected game mode the player has permission
                GameMode mode = GameMode.parse(args[0]);
                if (mode == null || !player.hasPermission(srp + "." + mode.getName().toLowerCase()))
                    return Collections.emptyList();

                String gameModeName = mode.getName().toLowerCase();
                List<String> allowedActions = mode.getActions().stream()
                        .map(Action::getName)
                        .filter(name ->
                                player.hasPermission(srp + "." + gameModeName + "." + name.toLowerCase())
                        )
                        .collect(Collectors.toList());

                return filterByInput.apply(allowedActions, args[1]);
            }

            // At: /srp <gamemode> <action> <TAB>
            case 3: {
                // Filtering by online players if the game mode is multiplayer and the player has permission
                GameMode mode = GameMode.parse(args[0]);
                if (mode == null || !mode.isMultiplayer())
                    return Collections.emptyList();

                List<String> onlinePlayerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());

                return filterByInput.apply(onlinePlayerNames, args[2]);
            }

            default: return Collections.emptyList();
        }
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    private boolean canUseCommand(Player player, SRPCommand command) {
        String srp = SRPCommand.getSrp();

        // Admins bypass everything
        if (isAdmin(player, srp)) return true;

        // Must have base permission
        if (!hasBasePermission(player, srp)) {
            return deny(player, "You do not have permission to use this command!");
        }

        // Frozen players cannot use commands
        if (isFrozen(player)) {
            return deny(player, "You cannot use commands during the countdown!");
        }

        GameMode gameMode = command.getGameMode();
        Action action = command.getAction();

        // Restrictions during a run (only applicable if gamemode is present)
        if (isInRunAndNotAllowed(player, gameMode, action)) {
            return deny(player, "You cannot use this command during a run!");
        }

        // If the command has no gamemode, nothing else to check
        if (gameMode == null) return true;

        String gameModeName = gameMode.getName().toLowerCase(Locale.ROOT);

        // Gamemode permission
        if (!hasGameModePermission(player, srp, gameModeName)) {
            return deny(player, "You do not have permission to use the " + gameModeName + " gamemode!");
        }

        // Action permission
        String actionName = action.getName().toLowerCase(Locale.ROOT);
        return hasActionPermission(player, srp, gameModeName, actionName) ||
                deny(player, "You do not have permission to use " + actionName + "!");
    }

    private boolean isAdmin(Player player, String srp) {
        return player.hasPermission(srp + ".admin");
    }

    private boolean hasBasePermission(Player player, String srp) {
        return player.hasPermission(srp + ".use");
    }

    private boolean isFrozen(Player player) {
        Optional<Speedrunner> optRunner = gameManager.getSpeedrunner(player);
        return optRunner.isPresent() && optRunner.get().isFrozen();
    }

    /**
     * Returns true when a player is in a run and the provided action is not allowed
     * during that run according to the game mode.
     */
    private boolean isInRunAndNotAllowed(Player player, GameMode gameMode, Action action) {
        return gameManager.isInRun(player)
                && gameMode != null
                && !gameMode.isAllowedDuringRun(action);
    }

    private boolean hasGameModePermission(Player player, String srp, String gameModeName) {
        return player.hasPermission(srp + "." + gameModeName);
    }

    private boolean hasActionPermission(Player player, String srp, String gameModeName, String actionName) {
        return player.hasPermission(srp + "." + gameModeName + "." + actionName);
    }

    private boolean deny(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
        return false;
    }

    private void executeCommand(Player player, SRPCommand command) {
        GameMode gameMode = command.getGameMode();
        Action action = command.getAction();

        if (action == Action.HELP) {
            sendHelpMessage(player);
            return;
        }

        switch (gameMode) {
            case SOLO: gameManager.getSoloManager().handleCommand(player, command); break;
            case BATTLE: gameManager.getBattleManager().handleCommand(player, command); break;
            default: player.sendMessage(ChatColor.RED + "Unknown game mode."); break;
        }
    }

    private void sendHelpMessage(Player player) {
        ChatColor green = ChatColor.GREEN;
        ChatColor yellow = ChatColor.YELLOW;
        ChatColor white = ChatColor.WHITE;

        player.sendMessage(green + "===== SpeedRunPlus Help =====");
        player.sendMessage(yellow + "/srp solo start" + white + " - Start a solo run");
        player.sendMessage(yellow + "/srp solo reset" + white + " - Reset your solo run");
        player.sendMessage(yellow + "/srp solo stop" + white + " - Stop your solo run");

        player.sendMessage(yellow + "/srp battle request <player>" + white + " - Challenge a player to a battle");
        player.sendMessage(yellow + "/srp battle accept" + white + " - Accept a request to battle");
        player.sendMessage(yellow + "/srp battle decline" + white + " - Decline a request to battle");
        player.sendMessage(yellow + "/srp battle surrender" + white + " - Surrender the battle run");

        player.sendMessage(yellow + "/srp help" + white + " - Show this help message");
        player.sendMessage(green + "===========================");
    }
}
