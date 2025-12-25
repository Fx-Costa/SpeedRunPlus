package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.Speedrun;
import com.fx.srp.model.run.CoopSpeedrun;
import com.fx.srp.util.time.TimeFormatter;
import com.fx.srp.commands.GameMode;
import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Manager responsible for handling all aspects of the Coop game mode (cooperative speedruns).
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Tracking pending coop requests and enforcing timeouts</li>
 *     <li>Starting and stopping {@link CoopSpeedrun} instances</li>
 * </ul>
 */
public class CoopManager extends MultiplayerGameModeManager<CoopSpeedrun> {

    /**
     * Constructs a new CoopManager.
     *
     * @param plugin the main {@link SpeedRunPlus} plugin instance
     * @param gameManager the {@link GameManager} for run registration and player management
     * @param worldManager the {@link WorldManager} for creating and deleting worlds
     */
    public CoopManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                       ACCEPT COOP
     * ========================================================== */
    /**
     * Accepts a pending coop request and starts a {@link CoopSpeedrun}.
     *
     * <p>Sets up a shared {@link StopWatch}, captures player states, and more.</p>
     *
     * @param partner the player accepting the request
     */
    @Override
    public void start(Player partner) {
        Player leader = getRequestSender(partner);
        if (leader == null) return;

        // Setup stopwatch
        StopWatch stopWatch = new StopWatch();
        Speedrunner leaderSpeedrunner = new Speedrunner(leader, stopWatch);
        Speedrunner partnerSpeedrunner = new Speedrunner(partner, stopWatch);

        // Capture the players' state - their inventory, levels, etc.
        leaderSpeedrunner.captureState();
        partnerSpeedrunner.captureState();

        CoopSpeedrun coopSpeedrun = new CoopSpeedrun(
                GameMode.COOP,
                leaderSpeedrunner,
                partnerSpeedrunner,
                stopWatch,
                null
        );
        gameManager.registerRun(coopSpeedrun);

        initializeRun(coopSpeedrun);

        leader.sendMessage(ChatColor.YELLOW + "Creating the world...");
        partner.sendMessage(ChatColor.YELLOW + "Creating the world...");
        worldManager.createWorldsForPlayers(List.of(leader), null, sets -> {
            // Get the set of worlds (overworld, nether, end) for each of the two players
            WorldManager.WorldSet leaderWorldSet = sets.get(leader.getUniqueId());

            // Assign them the world sets and set the shared seed
            leaderSpeedrunner.setWorldSet(leaderWorldSet);
            partnerSpeedrunner.setWorldSet(leaderWorldSet);
            coopSpeedrun.setSeed(leaderWorldSet.getOverworld().getSeed());

            // Freeze the players
            leaderSpeedrunner.freeze();
            partnerSpeedrunner.freeze();

            // Teleport players
            leader.teleport(leaderSpeedrunner.getWorldSet().getSpawn());
            partner.teleport(partnerSpeedrunner.getWorldSet().getSpawn());

            // Reset players' state (health, hunger, inventory, etc.)
            leaderSpeedrunner.resetState();
            partnerSpeedrunner.resetState();

            startCountdown(coopSpeedrun, List.of(leaderSpeedrunner, partnerSpeedrunner));
        });
    }

    /* ==========================================================
     *                       RESET COOP
     * ========================================================== */
    /**
     * Resets the worlds and state of a player in a {@link CoopSpeedrun}.
     *
     * <p>Does nothing.</p>
     *
     * @param player the player requesting the reset
     */
    @Override
    public void reset(Player player) {
        // Does nothing
    }

    /* ==========================================================
     *                       STOP COOP
     * ========================================================== */
    /**
     * Stops a {@link CoopSpeedrun}, showing results to both players.
     *
     * <p>Displays titles with winner information and formatted run time.
     * Calls {@link GameModeManager#finishRun} for cleanup.</p>
     *
     * @param winner any non-null Player declares all as winners
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
        CoopSpeedrun coopSpeedrun = (CoopSpeedrun) optional.get();

        // Update the state
        coopSpeedrun.setState(Speedrun.State.FINISHED);

        // Get the players
        Player leader = coopSpeedrun.getLeader().getPlayer();
        Player partner = coopSpeedrun.getPartner().getPlayer();

        // Get the final time
        String formattedTime = new TimeFormatter(coopSpeedrun.getStopWatch())
                .withHours()
                .withSuperscriptMs()
                .format();

        // Announce winners
        leader.sendTitle(
                ChatColor.GREEN + "You won! ",
                ChatColor.GREEN + "With a time of: " +
                        ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                10,
                140,
                20
        );
        partner.sendTitle(
                ChatColor.GREEN + "You won! ",
                ChatColor.GREEN + "With a time of: " +
                        ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                10,
                140,
                20
        );

        finishRun(coopSpeedrun, 200);
    }
}
