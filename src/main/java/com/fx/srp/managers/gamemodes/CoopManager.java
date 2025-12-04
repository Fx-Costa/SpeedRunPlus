package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.commands.Action;
import com.fx.srp.commands.SRPCommand;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.requests.PendingRequest;
import com.fx.srp.model.run.AbstractSpeedrun;
import com.fx.srp.model.run.CoopSpeedrun;
import com.fx.srp.util.time.TimeFormatter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager responsible for handling all aspects of the Coop game mode (cooperative speedruns).
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Handling SRP coop commands: {@link Action#REQUEST}, {@link Action#ACCEPT},
 *     {@link Action#DECLINE}, {@link Action#STOP}</li>
 *     <li>Tracking pending coop requests and enforcing timeouts</li>
 *     <li>Starting and stopping {@link CoopSpeedrun} instances</li>
 * </ul>
 */
public class CoopManager extends AbstractGameModeManager<CoopSpeedrun> {

    // Track coop requests
    private final Map<UUID, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

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
     *                       COMMANDS
     * ========================================================== */
    /**
     * Handles a {@link SRPCommand} for a coop player.
     *
     * <p>Delegates to the appropriate method based on the {@link Action}:
     * REQUEST, ACCEPT, DECLINE, STOP.</p>
     *
     * @param player the player executing the command
     * @param command the parsed {@link SRPCommand}
     */
    @Override
    public void handleCommand(Player player, SRPCommand command) {
        Action action = command.getAction();
        Player target = command.getTargetPlayer();
        switch (action) {
            case REQUEST: request(player, target); break;
            case ACCEPT: start(player); break;
            case DECLINE: decline(player); break;
            case STOP: getActiveRun(player).ifPresent(run -> stop(run, null)); break;
            default: player.sendMessage(ChatColor.RED + "Invalid command!"); break;
        }
    }

    /* ==========================================================
     *                       REQUEST COOP
     * ========================================================== */
    /**
     * Sends a coop request from one player to another.
     *
     * <p>Sends messages to both players and schedules a timeout for the request.</p>
     *
     * @param leader the player initiating the request
     * @param partner the player being challenged
     */
    public void request(Player leader, Player partner) {
        // A player cannot challenge nobody
        if (partner == null) {
            leader.sendMessage(ChatColor.RED + "You must specify a player to partner with!");
            return;
        }

        UUID leaderUUID = leader.getUniqueId();
        UUID partnerUUID = partner.getUniqueId();

        // A player cannot challenge themselves
        if (leaderUUID.equals(partnerUUID)) {
            leader.sendMessage(ChatColor.RED + "You cannot challenge yourself!");
            return;
        }

        // Partner is already in coop speedrun
        if (gameManager.isInRun(partner)) {
            leader.sendMessage(
                    ChatColor.GRAY + partner.getName() + " " +
                    ChatColor.YELLOW + " is already in a speedrun!"
            );
            return;
        }

        // Partner already has a pending request
        if (pendingRequests.containsKey(partnerUUID)) {
            leader.sendMessage(
                    ChatColor.GRAY + partner.getName() + " " +
                    ChatColor.YELLOW + " already has a pending request!"
            );
            return;
        }

        // Leader has already requested a coop speedrun within threshold
        if (pendingRequests.values().stream().anyMatch(req -> req.isByPlayer(leaderUUID))) {
            leader.sendMessage(ChatColor.YELLOW + " You've already sent a request! Please wait.");
            return;
        }

        // Make the coop request
        PendingRequest request = new PendingRequest(leaderUUID);
        pendingRequests.put(partnerUUID, request);
        leader.sendMessage(
                ChatColor.YELLOW + "You’ve sent a speedrun coop request to " +
                ChatColor.GRAY + partner.getName() + " " +
                ChatColor.YELLOW + "!"
        );
        partner.sendMessage(
                ChatColor.YELLOW + "You’ve been requested to a coop speedrun by " +
                ChatColor.GRAY + leader.getName() + " " +
                ChatColor.YELLOW + "! Use " +
                ChatColor.GRAY + "/srp coop accept" +
                ChatColor.YELLOW + " , or " +
                ChatColor.GRAY + "/srp coop decline " +
                ChatColor.YELLOW + "!"
        );

        // Schedule request timeout
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingRequests.remove(partnerUUID);
            leader.sendMessage(
                    ChatColor.YELLOW + "Your coop request to " +
                    ChatColor.GRAY + partner.getName() + " " +
                    ChatColor.YELLOW + "has expired!"
            );
            partner.sendMessage(ChatColor.YELLOW + "The coop request has expired.");
        }, configHandler.getMaxRequestTime() / 50L).getTaskId();

        request.setTimeoutTaskId(taskId);
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
        PendingRequest request = pendingRequests.remove(partner.getUniqueId());
        if (request == null || request.getTimeoutTaskId() <= 0) return;
        Player leader = Bukkit.getPlayer(request.getPlayerUUID());
        if (leader == null || !leader.isOnline() || gameManager.isInRun(leader)) return;

        // Cancel request timeout
        Bukkit.getScheduler().cancelTask(request.getTimeoutTaskId());

        // Setup stopwatch
        StopWatch stopWatch = new StopWatch();
        Speedrunner leaderSpeedrunner = new Speedrunner(leader, stopWatch);
        Speedrunner partnerSpeedrunner = new Speedrunner(partner, stopWatch);

        // Capture the players' state - their inventory, levels, etc.
        leaderSpeedrunner.captureState();
        partnerSpeedrunner.captureState();

        CoopSpeedrun coopSpeedrun = new CoopSpeedrun(
                gameManager,
                leaderSpeedrunner,
                partnerSpeedrunner,
                stopWatch,
                null
        );
        gameManager.registerRun(coopSpeedrun);

        initializeRun(coopSpeedrun);

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
     * @param coopSpeedrun the coop run to reset
     * @param player the player requesting the reset
     */
    @Override
    public void reset(CoopSpeedrun coopSpeedrun, Player player) {}

    /* ==========================================================
     *                       DECLINE COOP
     * ========================================================== */
    /**
     * Declines a pending coop request.
     *
     * <p>Removes the pending request and cancels its timeout task.
     * Sends messages to both the partner and the leader.</p>
     *
     * @param partner the player declining the request
     */
    public void decline(Player partner) {
        UUID partnerUUID = partner.getUniqueId();

        // If the partner has not received a request for coop
        if (!pendingRequests.containsKey(partnerUUID)) {
            partner.sendMessage(ChatColor.YELLOW + "You have no pending request!");
            return;
        }

        PendingRequest request = pendingRequests.remove(partnerUUID);
        int taskId = request.getTimeoutTaskId();
        if (taskId > 0) Bukkit.getScheduler().cancelTask(request.getTimeoutTaskId());

        // Decline the coop
        Player leader = Bukkit.getPlayer(request.getPlayerUUID());
        partner.sendMessage(ChatColor.YELLOW + "You’ve decline the coop request!");
        if (leader != null) {
            leader.sendMessage(
                    ChatColor.GRAY + partner.getName() + " " +
                    ChatColor.YELLOW + "has declined your coop request!"
            );
        }
    }

    /* ==========================================================
     *                       STOP COOP
     * ========================================================== */
    /**
     * Stops a {@link CoopSpeedrun}, showing results to both players.
     *
     * <p>Displays titles with winner/loser information and formatted run time.
     * Calls {@link AbstractGameModeManager#finishRun} for cleanup.</p>
     *
     * @param coopSpeedrun the coop run to stop
     * @param winner any non-null Player declares all as winners
     */
    @Override
    public void stop(CoopSpeedrun coopSpeedrun, Player winner) {
        coopSpeedrun.setState(AbstractSpeedrun.State.FINISHED);
        Player leader = coopSpeedrun.getLeader().getPlayer();
        Player partner = coopSpeedrun.getPartner().getPlayer();

        // Get the final time
        String formattedTime = new TimeFormatter(coopSpeedrun.getStopWatch())
                .withHours()
                .withSuperscriptMs()
                .format();

        if (winner == null) {
            leader.sendTitle(
                    ChatColor.RED + "Time's up!", "", 10, 140, 20
            );
            partner.sendTitle(
                    ChatColor.RED + "Time's up!", "", 10, 140, 20
            );
        }
        else {
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
        }

        finishRun(coopSpeedrun);
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    private Optional<CoopSpeedrun> getActiveRun(Player player) {
        return gameManager.getActiveRun(player).filter(r -> r instanceof CoopSpeedrun)
                .map(r -> (CoopSpeedrun) r);
    }
}
