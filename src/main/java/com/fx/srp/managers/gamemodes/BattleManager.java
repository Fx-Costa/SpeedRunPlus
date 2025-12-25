package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.run.Speedrun;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.BattleSpeedrun;
import com.fx.srp.util.time.TimeFormatter;
import com.fx.srp.commands.GameMode;
import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manager responsible for handling all aspects of the Battle game mode (1v1 speedrun battles).
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Tracking pending battle requests and enforcing timeouts</li>
 *     <li>Starting and stopping {@link BattleSpeedrun} instances</li>
 *     <li>Resetting player worlds and state</li>
 *     <li>Determining winners and results</li>
 * </ul>
 */
public class BattleManager extends MultiplayerGameModeManager<BattleSpeedrun> {

    /**
     * Constructs a new BattleManager.
     *
     * @param plugin the main {@link SpeedRunPlus} plugin instance
     * @param gameManager the {@link GameManager} for run registration and player management
     * @param worldManager the {@link WorldManager} for creating and deleting worlds
     */
    public BattleManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                       ACCEPT BATTLE
     * ========================================================== */
    /**
     * Accepts a pending battle request and starts a {@link BattleSpeedrun}.
     *
     * <p>Sets up a shared {@link StopWatch}, captures player states, and more.</p>
     *
     * @param challengee the player accepting the request
     */
    @Override
    public void start(Player challengee) {
        Player challenger = getRequestSender(challengee);
        if (challenger == null) return;

        // Setup stopwatch
        StopWatch stopWatch = new StopWatch();
        Speedrunner challengerSpeedrunner = new Speedrunner(challenger, stopWatch);
        Speedrunner challengeeSpeedrunner = new Speedrunner(challengee, stopWatch);

        // Capture the players' state - their inventory, levels, etc.
        challengerSpeedrunner.captureState();
        challengeeSpeedrunner.captureState();

        BattleSpeedrun battleSpeedrun = new BattleSpeedrun(
                GameMode.BATTLE,
                challengerSpeedrunner,
                challengeeSpeedrunner,
                stopWatch,
                null
        );
        gameManager.registerRun(battleSpeedrun);

        initializeRun(battleSpeedrun);

        challenger.sendMessage(ChatColor.YELLOW + "Creating the world...");
        challengee.sendMessage(ChatColor.YELLOW + "Creating the world...");
        worldManager.createWorldsForPlayers(List.of(challenger, challengee), null, sets -> {
            // Get the set of worlds (overworld, nether, end) for each of the two players
            WorldManager.WorldSet challengerWorldSet = sets.get(challenger.getUniqueId());
            WorldManager.WorldSet challengeeWorldSet = sets.get(challengee.getUniqueId());

            // Assign them the world sets and set the shared seed
            challengerSpeedrunner.setWorldSet(challengerWorldSet);
            challengeeSpeedrunner.setWorldSet(challengeeWorldSet);
            battleSpeedrun.setSeed(challengerWorldSet.getOverworld().getSeed());

            // Freeze the players
            challengerSpeedrunner.freeze();
            challengeeSpeedrunner.freeze();

            // Teleport players
            challenger.teleport(challengerSpeedrunner.getWorldSet().getSpawn());
            challengee.teleport(challengeeSpeedrunner.getWorldSet().getSpawn());

            // Reset players' state (health, hunger, inventory, etc.)
            challengerSpeedrunner.resetState();
            challengeeSpeedrunner.resetState();

            startCountdown(battleSpeedrun, List.of(challengerSpeedrunner, challengeeSpeedrunner));
        });
    }

    /* ==========================================================
     *                       RESET BATTLE
     * ========================================================== */
    /**
     * Resets the worlds and state of a player in a {@link BattleSpeedrun}.
     *
     * <p>Teleports the player, recreates worlds, and restores state.</p>
     *
     * @param player the player requesting the reset
     */
    @Override
    public void reset(Player player) {
        // If not already in a speedrun
        Optional<Speedrun> optional = gameManager.getActiveRun(player);
        if (optional.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a speedrun!");
            return;
        }

        // Get the run
        BattleSpeedrun battleSpeedrun = (BattleSpeedrun) optional.get();

        // Get the speedrunner trying to reset
        Optional<Speedrunner> speedrunner = gameManager.getSpeedrunner(player);
        if (speedrunner.isEmpty()) return;

        // Get the seed of the existing world
        Long seed = battleSpeedrun.getSeed();

        player.sendMessage(ChatColor.YELLOW + "Resetting the world...");

        recreateWorldsForReset(speedrunner.get(), seed, () -> {});
    }

    /* ==========================================================
     *                       STOP BATTLE
     * ========================================================== */
    /**
     * Stops a {@link BattleSpeedrun}, showing results to both players.
     *
     * <p>Displays titles with winner/loser information and formatted run time.
     * Calls {@link GameModeManager#finishRun} for cleanup.</p>
     *
     * @param winner the player who won
     */
    @Override
    public void stop(@NonNull Player winner) {
        // If not already in a speedrun
        Optional<Speedrun> optional = gameManager.getActiveRun(winner);
        if (optional.isEmpty()) {
            winner.sendMessage(ChatColor.RED + "You are not in a speedrun!");
            return;
        }

        // Get the run
        BattleSpeedrun battleSpeedrun = (BattleSpeedrun) optional.get();

        // Update the state
        battleSpeedrun.setState(Speedrun.State.FINISHED);

        // Get the final time
        String formattedTime = new TimeFormatter(battleSpeedrun.getStopWatch())
                .withHours()
                .withSuperscriptMs()
                .format();

        // Announce winner and loser (with times)
        winner.sendTitle(
                ChatColor.GREEN + "You won! ",
                ChatColor.GREEN + "With a time of: " +
                        ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                10,
                140,
                20
        );
        getOpponent(battleSpeedrun, winner).sendTitle(
                ChatColor.RED + "You lost! ",
                ChatColor.GRAY + winner.getName() +
                        ChatColor.RED + " won with a time of: " +
                        ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                10,
                140,
                20
        );

        finishRun(battleSpeedrun, 200);
    }

    /* ==========================================================
     *                       ABORT BATTLE
     * ========================================================== */
    @Override
    public void abort(@NonNull Speedrun run, CommandSender sender, String reason) {
        BattleSpeedrun battleSpeedrun = (BattleSpeedrun) run;

        // If the sender is a participant, the opponent wins
        List<Player> players = gameManager.getAllPlayersInRun(run);
        if (sender instanceof Player && players.contains((Player) sender)) {
            // Determine winner and loser
            Player challenger = battleSpeedrun.getChallenger().getPlayer();
            Player challengee = battleSpeedrun.getChallengee().getPlayer();
            Player winner = sender.equals(challenger) ? challengee : challenger;

            // End the battleSpeedrun
            stop(winner);
            return;
        }

        // Otherwise fall back to the default abort
        super.abort(run, sender, reason);
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    private Player getOpponent(BattleSpeedrun battleSpeedrun, Player player) {
        Player challenger = battleSpeedrun.getChallenger().getPlayer();
        if (challenger.equals(player)) return battleSpeedrun.getChallengee().getPlayer();
        return challenger;
    }
}
