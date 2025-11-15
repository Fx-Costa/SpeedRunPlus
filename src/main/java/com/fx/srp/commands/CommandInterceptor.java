package com.fx.srp.commands;

import com.fx.srp.utils.PlayerUtil;
import com.fx.srp.utils.WorldUtil;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;
import com.fx.srp.SpeedRunPlus;
import com.fx.srp.utils.TimerUtil;
import lombok.Getter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandInterceptor implements CommandExecutor {

    private final MVWorldManager worldManager;
    private final MultiverseNetherPortals netherPortals;

    SpeedRunPlus plugin = SpeedRunPlus.getPlugin(SpeedRunPlus.class);

    // Keep track of countdown tasks, for any premature exits
    @Getter protected final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    public CommandInterceptor(MultiverseCore core, MultiverseNetherPortals netherPortals) {
        this.worldManager = core.getMVWorldManager();
        this.netherPortals = netherPortals;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[SRP] This command must be executed by a player!");
            return false;
        }

        // Get information about the player, worlds and the command
        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();
        String overworld = plugin.getSRP_OVERWORLD_PREFIX() + uuid;
        String nether = plugin.getSRP_NETHER_PREFIX() + uuid;
        String end = plugin.getSRP_END_PREFIX() + uuid;

        // Parse the command
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /srp <start|stop|reset|battle|coop> [player]");
            return false;
        }

        Subcommands subcommand = Subcommands.valueOf(args[0].toUpperCase());
        Player otherPlayer;

        // Other player to include in co-op or battle
        if (args.length == 2) {
            otherPlayer = args[1] != null ? Bukkit.getPlayer(args[1]) : null;
        }

        // Execute command
        switch (subcommand) {
            // Start a run
            case START:
                return startRun(player, overworld, nether, end);

            // Stop a run
            case STOP:
                return endRun(player, overworld, nether, end, null);

            // Reset a run
            case RESET:
                return resetRun(player, overworld, nether, end);

            default: return false;
        }
    }

    private boolean startRun(Player player, String overworld, String nether, String end){
        // Check
        if (plugin.getActivePlayers().size() >= plugin.getMAX_PLAYERS()) {
            player.sendMessage(ChatColor.RED + "Lobbies are full! Please wait.");
            return false;
        }

        // Check whether the player's world already exists
        if (worldManager.isMVWorld(overworld)) {
            player.sendMessage(ChatColor.RED + "You are already in a speedrun! Use "
                    + ChatColor.GRAY + "/reset "
                    + ChatColor.RED + "to reset, or "
                    + ChatColor.GRAY + "/endrun "
                    + ChatColor.RED + "to quit."
            );
            return false;
        }

        // Freeze the player
        PlayerUtil.freezePlayer(plugin, player);
        player.sendMessage(ChatColor.YELLOW + "Starting a run...");

        // Reset the player's state
        PlayerUtil.resetPlayerState(plugin, player);

        // Build world
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location spawn = WorldUtil.generateWorlds(overworld, nether, end, worldManager, netherPortals);
            player.teleport(spawn);

            // Delay a bit to ensure world fully loads before countdown
            Bukkit.getScheduler().runTaskLater(plugin, () -> startCountdown(player), 40L);
        });

        return true;
    }

    private boolean endRun(Player player, String overworld, String nether, String end, @Nullable Runnable onComplete){
        UUID uuid = player.getUniqueId();

        // Check whether the player's world already exists
        if (!worldManager.isMVWorld(overworld) || !worldManager.isMVWorld(nether) || !worldManager.isMVWorld(end)) {
            player.sendMessage(ChatColor.RED + "You are not in a speedrun! Use "
                    + ChatColor.GRAY + "/startrun "
                    + ChatColor.RED + "to start a speedrun."
            );
            return false;
        }

        // Cancel any countdowns in progress
        BukkitTask existing = countdownTasks.remove(uuid);
        if (existing != null) existing.cancel();

        Bukkit.getScheduler().cancelTasks(plugin);
        player.sendMessage(ChatColor.YELLOW + "Ending the run...");
        StopWatch stopWatch = plugin.getPlayerStopWatches().remove(uuid);
        if (stopWatch != null) stopWatch.stop();

        // Stop tracking the player
        plugin.getActivePlayers().remove(uuid);
        plugin.getPlayerLocations().remove(uuid);
        BukkitTask scheduledShutdownTask = plugin.getScheduledShutdownTasks().remove(uuid);
        if (scheduledShutdownTask != null) scheduledShutdownTask.cancel();

        // Schedule world deletion and player restoral
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            WorldUtil.deleteWorlds(plugin, worldManager, player);
            PlayerUtil.restorePlayerState(plugin, player);
            player.sendMessage(ChatColor.YELLOW + "Run ended");

            if (onComplete != null) onComplete.run();

        }, 100L);

        return true;
    }

    private boolean resetRun(Player player, String overworld, String nether, String end) {
        return endRun(player, overworld, nether, end, () -> {
            if (player.isOnline()) {
                startRun(player, overworld, nether, end);
            }
        });
    }

    private void startCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        String overworld = plugin.getSRP_OVERWORLD_PREFIX() + uuid;
        String nether = plugin.getSRP_NETHER_PREFIX() + uuid;
        String end = plugin.getSRP_END_PREFIX() + uuid;

        BukkitTask task = new BukkitRunnable() {
            int seconds = plugin.getTIMER_COUNTDOWN();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (seconds > 0) {
                    player.sendTitle(ChatColor.YELLOW + "Starting in " + seconds + "...", "", 0, 20, 0);
                    seconds--;
                }
                else {
                    player.sendTitle(ChatColor.GREEN + "GO!", "", 0, 40, 20);
                    PlayerUtil.unfreezePlayer(plugin, player);

                    // Create and start a timer
                    StopWatch stopWatch = new StopWatch();
                    plugin.getPlayerStopWatches().put(uuid, stopWatch);
                    TimerUtil.createTimer(player, stopWatch);
                    stopWatch.start();
                    Bukkit.getScheduler().runTaskTimer(
                            plugin,
                            () -> TimerUtil.updateTimer(player, stopWatch),
                            0L,
                            5L
                    );

                    // Set the player as active
                    plugin.getActivePlayers().put(player.getUniqueId(), System.currentTimeMillis());

                    // Schedule the run to end when it exceeds the maximum speedrun time
                    BukkitTask scheduledShutdownTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline() && plugin.getActivePlayers().containsKey(uuid)) {
                            player.sendMessage(ChatColor.RED + "Your speedrun has ended (time limit reached)!");
                            endRun(player, overworld, nether, end, null);
                        }
                    }, plugin.getMAX_RUN_TIME() / 50L);
                    plugin.getScheduledShutdownTasks().put(uuid, scheduledShutdownTask);

                    countdownTasks.remove(uuid);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdownTasks.put(uuid, task);
    }
}

