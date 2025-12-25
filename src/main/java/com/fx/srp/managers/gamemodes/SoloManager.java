package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.run.Speedrun;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.SoloSpeedrun;
import com.fx.srp.util.time.TimeFormatter;
import com.fx.srp.commands.GameMode;
import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Manager responsible for handling all aspects of the Solo game mode (single-player speedruns).
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Starting, resetting, and stopping {@link SoloSpeedrun} instances</li>
 *     <li>Managing player state, teleportation, and world creation</li>
 *     <li>Displaying countdowns, titles, and final time to the player</li>
 * </ul>
 */
public class SoloManager extends GameModeManager<SoloSpeedrun> {

    /**
     * Constructs a new SoloManager.
     *
     * @param plugin the main {@link SpeedRunPlus} plugin instance
     * @param gameManager the {@link GameManager} for run registration and player management
     * @param worldManager the {@link WorldManager} for creating and deleting worlds
     */
    public SoloManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                       START SOLO RUN
     * ========================================================== */
    /**
     * Starts a new {@link SoloSpeedrun} for a player.
     *
     * <p>Creates the required worlds, captures the player state, and more.</p>
     *
     * @param player the player starting the solo speedrun
     */
    @Override
    public void start(Player player) {
        // If already in a speedrun
        if (gameManager.isInRun(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a speedrun!");
            return;
        }

        StopWatch stopWatch = new StopWatch();
        Speedrunner runner = new Speedrunner(player, stopWatch);
        runner.captureState();

        SoloSpeedrun soloSpeedrun = new SoloSpeedrun(GameMode.SOLO, runner, stopWatch, null);
        gameManager.registerRun(soloSpeedrun);

        initializeRun(soloSpeedrun);

        player.sendMessage(ChatColor.YELLOW + "Creating the world...");
        worldManager.createWorldsForPlayers(List.of(player), null, sets -> {
            // Get the set of worlds (overworld, nether, end)
            WorldManager.WorldSet worldSet = sets.get(player.getUniqueId());

            // Assign the speedrunner the world set and set the seed
            runner.setWorldSet(worldSet);
            soloSpeedrun.setSeed(worldSet.getOverworld().getSeed());

            // Freeze the player
            runner.freeze();

            // Teleport player
            player.teleport(worldSet.getSpawn());

            // Reset player state (health, hunger, inventory, etc.)
            runner.resetState();

            startCountdown(soloSpeedrun, List.of(runner));
        });
    }

    /* ==========================================================
     *                       RESET SOLO RUN
     * ========================================================== */
    /**
     * Resets the worlds and state of a player in a {@link SoloSpeedrun}.
     *
     * <p>Teleports the player, recreates worlds with the previous seed, and more.</p>
     *
     * @param player the player requesting the reset
     */
    public void reset(Player player) {
        // If not already in a speedrun
        Optional<Speedrun> optional = gameManager.getActiveRun(player);
        if (optional.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a speedrun!");
            return;
        }

        // Get the run
        SoloSpeedrun soloSpeedrun = (SoloSpeedrun) optional.get();

        // Update the state
        soloSpeedrun.setState(Speedrun.State.CREATING_WORLDS);

        // Get the seed of the existing world
        Long seed = soloSpeedrun.getSeed();

        player.sendMessage(ChatColor.YELLOW + "Resetting the world...");
        Speedrunner speedrunner = soloSpeedrun.getSpeedrunners().get(0);

        recreateWorldsForReset(speedrunner, seed, () -> soloSpeedrun.setState(Speedrun.State.RUNNING));
    }

    /* ==========================================================
     *                       STOP SOLO RUN
     * ========================================================== */
    /**
     * Stops a {@link SoloSpeedrun} announcing the player's time as a win.
     *
     * <p>Displays a title with the final run time and calls {@link GameModeManager#finishRun}
     * for cleanup.</p>
     *
     * @param winner the player who completed the run
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
        SoloSpeedrun soloSpeedrun = (SoloSpeedrun) optional.get();

        // Update the state
        soloSpeedrun.setState(Speedrun.State.FINISHED);

        // Get the final time
        String formattedTime = new TimeFormatter(soloSpeedrun.getStopWatch())
                .withHours()
                .withSuperscriptMs()
                .format();

        // Announce winner and time
        winner.sendTitle(
                ChatColor.GREEN + "You won! ",
                ChatColor.GREEN + "With a time of: " +
                        ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                10,
                140,
                20
        );

        finishRun(soloSpeedrun, 200);
    }
}
