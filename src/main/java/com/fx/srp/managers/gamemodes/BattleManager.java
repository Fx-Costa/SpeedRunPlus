package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.commands.Action;
import com.fx.srp.commands.SRPCommand;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.requests.PendingRequest;
import com.fx.srp.model.run.AbstractSpeedrun;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.BattleSpeedrun;
import com.fx.srp.model.run.ISpeedrun;
import com.fx.srp.util.time.TimeFormatter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class BattleManager extends AbstractGameModeManager<BattleSpeedrun> {

    // Track battle requests
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();

    public BattleManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                       COMMANDS
     * ========================================================== */
    @Override
    public void handleCommand(Player player, SRPCommand command) {
        Action action = command.getAction();
        Player target = command.getTargetPlayer();
        switch (action) {
            case REQUEST: request(player, target); break;
            case ACCEPT: start(player); break;
            case DECLINE: decline(player); break;
            case RESET: getActiveRun(player).ifPresent(run -> reset(run, player)); break;
            case SURRENDER: surrender(player); break;
            default: player.sendMessage(ChatColor.RED + "Invalid command!"); break;
        }
    }

    /* ==========================================================
     *                       REQUEST BATTLE
     * ========================================================== */
    public void request(Player challenger, Player challengee) {
        // A player cannot challenge nobody
        if (challengee == null) {
            challenger.sendMessage(ChatColor.RED + "You must specify a player to challenge!");
            return;
        }

        UUID challengerUUID = challenger.getUniqueId();
        UUID challengeeUUID = challengee.getUniqueId();

        // A player cannot challenge themselves
        if (challengerUUID.equals(challengeeUUID)) {
            challenger.sendMessage(ChatColor.RED + "You cannot challenge yourself!");
            return;
        }

        // Challengee is already in battle
        if (gameManager.isInRun(challengee)) {
            challenger.sendMessage(
                    ChatColor.GRAY + challengee.getName() + " " +
                    ChatColor.YELLOW + " is already in a speedrun battle!"
            );
            return;
        }

        // Challengee already has a pending request
        if (pendingRequests.containsKey(challengeeUUID)) {
            challenger.sendMessage(
                    ChatColor.GRAY + challengee.getName() + " " +
                    ChatColor.YELLOW + " already has a pending request!"
            );
            return;
        }

        // Challenger has already requested a battle within threshold
        if (pendingRequests.values().stream().anyMatch(req -> req.isByPlayer(challengerUUID))) {
            challenger.sendMessage(ChatColor.YELLOW + " You've already sent a request! Please wait.");
            return;
        }

        // Make the battle request
        PendingRequest request = new PendingRequest(challengerUUID);
        pendingRequests.put(challengeeUUID, request);
        challenger.sendMessage(
                ChatColor.YELLOW + "You’ve sent a speedrun battle request to " +
                ChatColor.GRAY + challengee.getName() + " " +
                ChatColor.YELLOW + "!"
        );
        challengee.sendMessage(
                ChatColor.YELLOW + "You’ve been challenged to a speedrun battle by " +
                ChatColor.GRAY + challenger.getName() + " " +
                ChatColor.YELLOW + "! Use " +
                ChatColor.GRAY + "/srp battle accept" +
                ChatColor.YELLOW + " , or " +
                ChatColor.GRAY + "/srp battle decline " +
                ChatColor.YELLOW + "!"
        );

        // Schedule request timeout
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingRequests.remove(challengeeUUID);
            challenger.sendMessage(
                    ChatColor.YELLOW + "Your battle request to " +
                    ChatColor.GRAY + challengee.getName() + " " +
                    ChatColor.YELLOW + "has expired!"
            );
            challengee.sendMessage(ChatColor.YELLOW + "The battle request has expired.");
        }, configHandler.getMaxRequestTime() / 50L).getTaskId();

        request.setTimeoutTaskId(taskId);
    }

    /* ==========================================================
     *                       ACCEPT BATTLE
     * ========================================================== */
    @Override
    public void start(Player challengee) {
        PendingRequest request = pendingRequests.remove(challengee.getUniqueId());
        if (request == null || request.getTimeoutTaskId() <= 0) return;
        Player challenger = Bukkit.getPlayer(request.getPlayerUUID());
        if (challenger == null || !challenger.isOnline()) return;

        // Cancel request timeout
        Bukkit.getScheduler().cancelTask(request.getTimeoutTaskId());

        // Setup stopwatch
        StopWatch sw = new StopWatch();
        Speedrunner challengerSpeedrunner = new Speedrunner(challenger, sw);
        Speedrunner challengeeSpeedrunner = new Speedrunner(challengee, sw);

        // Capture the players' state - their inventory, levels, etc.
        challengerSpeedrunner.captureState();
        challengeeSpeedrunner.captureState();

        BattleSpeedrun battleSpeedrun = new BattleSpeedrun(gameManager, challengerSpeedrunner, challengeeSpeedrunner, sw, null);
        gameManager.registerRun(battleSpeedrun);

        initializeRun(battleSpeedrun);

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
    @Override
    public void reset(BattleSpeedrun battleSpeedrun, Player player) {
        // If not already in a run
        if (!gameManager.isInRun(player)) {
            player.sendMessage(ChatColor.RED + "You are not in a speedrun!");
            return;
        }

        // Get the speedrunner trying to
        Optional<Speedrunner> speedrunner = gameManager.getSpeedrunner(player);
        if (speedrunner.isEmpty()) return;

        // Get the seed of the existing world
        Long seed = battleSpeedrun.getSeed();

        player.sendMessage(ChatColor.YELLOW + "Resetting the world...");

        recreateWorldsForReset(speedrunner.get(), seed, () -> {});
    }

    /* ==========================================================
     *                       DECLINE BATTLE
     * ========================================================== */
    public void decline(Player challengee) {
        UUID challengeeUUID = challengee.getUniqueId();

        // If the challengee has not received a request for battle
        if (!pendingRequests.containsKey(challengeeUUID)) {
            challengee.sendMessage(ChatColor.YELLOW + "You have no pending request!");
            return;
        }

        PendingRequest request = pendingRequests.remove(challengeeUUID);
        int id = request.getTimeoutTaskId();
        if (id > 0) Bukkit.getScheduler().cancelTask(request.getTimeoutTaskId());

        // Decline the battle
        Player challenger = Bukkit.getPlayer(request.getPlayerUUID());
        challengee.sendMessage(ChatColor.YELLOW + "You’ve decline the battle request!");
        if (challenger != null) {
            challenger.sendMessage(
                    ChatColor.GRAY + challengee.getName() + " " +
                    ChatColor.YELLOW + "has declined your battle request!"
            );
        }
    }

    /* ==========================================================
     *                       SURRENDER
     * ========================================================== */
    public void surrender(Player player){
        // If not in an active battleSpeedrun
        if (!gameManager.isInRun(player)) {
            player.sendMessage(ChatColor.YELLOW + "You are not in a battleSpeedrun!");
            return;
        }

        // If the battleSpeedrun is not in a running state
        Optional<ISpeedrun> run = gameManager.getActiveRun(player);
        if (run.isEmpty() || !(run.get() instanceof BattleSpeedrun)) {
            player.sendMessage(ChatColor.YELLOW + "You are not in a battleSpeedrun!");
            return;
        }
        BattleSpeedrun battleSpeedrun = (BattleSpeedrun) run.get();

        // Determine winner and loser
        Player challenger = battleSpeedrun.getChallenger().getPlayer();
        Player challengee = battleSpeedrun.getChallengee().getPlayer();
        Player winner = (player.equals(challenger)) ? challengee : challenger;

        // End the battleSpeedrun
        stop(battleSpeedrun, winner);
    }

    /* ==========================================================
     *                       STOP BATTLE
     * ========================================================== */
    @Override
    public void stop(BattleSpeedrun battleSpeedrun, Player winner) {
        battleSpeedrun.setState(AbstractSpeedrun.State.FINISHED);

        // Get the final time
        String formattedTime = new TimeFormatter(battleSpeedrun.getStopWatch())
                .includeHours()
                .superscriptMs()
                .format();

        if (winner == null) {
            battleSpeedrun.getChallenger().getPlayer().sendTitle(ChatColor.RED + "Time's up!", "", 10, 140, 20);
            battleSpeedrun.getChallengee().getPlayer().sendTitle(ChatColor.RED + "Time's up!", "", 10, 140, 20);
        }
        else {
            winner.sendTitle(
                    ChatColor.GREEN + "You won! ",
                    ChatColor.GREEN + "With a time of: " +
                            ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                    10,
                    140,
                    20
            );
            getOpponent(battleSpeedrun, winner).sendTitle(
                    ChatColor.GREEN + "You lost! ",
                    ChatColor.GRAY + winner.getName() +
                            ChatColor.GREEN + " won with a time of: " +
                            ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                    10,
                    140,
                    20
            );
        }

        finishRun(battleSpeedrun);
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    private Player getOpponent(BattleSpeedrun battleSpeedrun, Player player) {
        if (battleSpeedrun.getChallenger().getPlayer().equals(player)) return battleSpeedrun.getChallengee().getPlayer();
        return battleSpeedrun.getChallenger().getPlayer();
    }

    protected Optional<BattleSpeedrun> getActiveRun(Player p) {
        return gameManager.getActiveRun(p).filter(r -> r instanceof BattleSpeedrun)
                .map(r -> (BattleSpeedrun) r);
    }
}
