package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.commands.Action;
import com.fx.srp.commands.SRPCommand;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.run.AbstractSpeedrun;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.SoloSpeedrun;
import com.fx.srp.util.time.TimeFormatter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class SoloManager extends AbstractGameModeManager<SoloSpeedrun> {

    public SoloManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                       COMMANDS
     * ========================================================== */
    @Override
    public void handleCommand(Player player, SRPCommand command) {
        Action action = command.getAction();
        switch (action) {
            case START: start(player); break;
            case STOP: getActiveRun(player).ifPresent(run -> stop(run, null)); break;
            case RESET: getActiveRun(player).ifPresent(run -> reset(run, player)); break;
            default: player.sendMessage(ChatColor.RED + "Invalid command!"); break;
        }
    }

    /* ==========================================================
     *                       START SOLO RUN
     * ========================================================== */
    @Override
    public void start(Player player) {
        // If already in a speedrun
        if (gameManager.isInRun(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a speedrun!");
            return;
        }

        StopWatch sw = new StopWatch();
        Speedrunner runner = new Speedrunner(player, sw);
        runner.captureState();

        SoloSpeedrun soloSpeedrun = new SoloSpeedrun(gameManager, runner, sw, null);
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
    public void reset(SoloSpeedrun soloSpeedrun, Player player) {
        // If not already in a run
        if (!gameManager.isInRun(player)) {
            player.sendMessage(ChatColor.RED + "You are not in a speedrun!");
            return;
        }

        soloSpeedrun.setState(AbstractSpeedrun.State.CREATING_WORLDS);

        // Get the seed of the existing world
        Long seed = soloSpeedrun.getSeed();

        player.sendMessage(ChatColor.YELLOW + "Resetting the world...");
        Speedrunner speedrunner = soloSpeedrun.getSpeedrunners().get(0);

        recreateWorldsForReset(speedrunner, seed, () -> soloSpeedrun.setState(AbstractSpeedrun.State.RUNNING));
    }

    /* ==========================================================
     *                       STOP SOLO RUN
     * ========================================================== */
    @Override
    public void stop(SoloSpeedrun soloSpeedrun, Player player) {
        soloSpeedrun.setState(AbstractSpeedrun.State.FINISHED);

        // Announce winner and time
        if (player != null) {
            // Get the final time
            String formattedTime = new TimeFormatter(soloSpeedrun.getStopWatch())
                    .includeHours()
                    .superscriptMs()
                    .format();

            player.sendTitle(
                    ChatColor.GREEN + "You won! ",
                    ChatColor.GREEN + "With a time of: " +
                            ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                    10,
                    140,
                    20
            );
        }

        finishRun(soloSpeedrun);
    }

    /* ==========================================================
     *                      HELPERS
     * ========================================================== */
    protected Optional<SoloSpeedrun> getActiveRun(Player p) {
        return gameManager.getActiveRun(p).filter(r -> r instanceof SoloSpeedrun)
                .map(r -> (SoloSpeedrun) r);
    }
}
