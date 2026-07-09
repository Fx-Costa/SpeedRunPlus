package com.fx.srp.managers.gamemodes;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.party.MultiplayerParty;
import com.fx.srp.model.player.Speedrunner;
import com.fx.srp.model.run.CoopSpeedrun;
import com.fx.srp.model.run.Speedrun;
import com.fx.srp.util.time.TimeFormatter;
import com.fx.srp.commands.GameMode;
import lombok.NonNull;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manager responsible for handling all aspects of the Coop game mode (cooperative speedruns).
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Tracking pending coop requests and enforcing timeouts (inherited)</li>
 *     <li>Accumulating accepted invitees into a forming {@link MultiplayerParty}</li>
 *     <li>Starting and stopping {@link CoopSpeedrun} instances once the leader finalizes the party</li>
 * </ul>
 */
public class CoopManager extends MultiplayerGameModeManager<CoopSpeedrun> {

    /** Parties currently forming, keyed by leader UUID. */
    private final Map<UUID, MultiplayerParty> formingParties = new ConcurrentHashMap<>();

    /** Reverse lookup: member UUID -> leader UUID, for anyone currently in a forming party. */
    private final Map<UUID, UUID> leaderByMember = new ConcurrentHashMap<>();

    public CoopManager(SpeedRunPlus plugin, GameManager gameManager, WorldManager worldManager) {
        super(plugin, gameManager, worldManager);
    }

    /* ==========================================================
     *                    ACCEPT (JOIN PARTY)
     * ========================================================== */
    /**
     * Accepts a pending coop invite, adding the accepting player to the sender's (leader's)
     * forming party. Does not start the run — the leader must separately run
     * {@code /srp coop start} once the party is ready.
     *
     * @param target the player accepting the invite
     */
    @Override
    public void accept(Player target) {
        Player leader = getRequestSender(target);
        if (leader == null) return;

        UUID leaderUUID = leader.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        MultiplayerParty party = formingParties.computeIfAbsent(leaderUUID, MultiplayerParty::new);

        // Check the number of available slots for a party
        int maxPartySize = configHandler.getMaxPartySize();
        int availableSlots = configHandler.getMaxPlayers() - gameManager.getAllPlayersInRuns().size();
        if (availableSlots <= 0) {
            target.sendMessage(ChatColor.RED + "The server is full of speedrunners right now! Try again later!");
            return;
        }

        // Verify that the party has open slots
        int maxSize = maxPartySize > 0 ? Math.min(maxPartySize, availableSlots) : availableSlots;
        if (party.size() >= maxSize) {
            target.sendMessage(ChatColor.RED + "That party is already full!");
            return;
        }

        party.addMember(targetUUID);
        leaderByMember.put(targetUUID, leaderUUID);

        leader.sendMessage(ChatColor.GREEN + target.getName() + " joined your party! ("
                + party.size() + "/" + maxSize + ")");
        target.sendMessage(ChatColor.GREEN + "You joined " + leader.getName() + "'s coop party!");
        leader.sendMessage(ChatColor.YELLOW +
                "Invite with /srp coop request <player>, or run /srp coop start when ready.");
    }

    /* ==========================================================
     *                  START (LEADER BEGINS RUN)
     * ========================================================== */
    /**
     * Called by a party leader to finalize their forming party and begin the {@link CoopSpeedrun}.
     *
     * <p>Sets up a shared {@link StopWatch}, captures each participant's state, creates a single
     * shared world set, and starts the countdown for all party members.</p>
     *
     * @param leader the player starting the run; must be the leader of a forming party of size >= 2
     */
    @Override
    public void start(Player leader) {
        UUID leaderUUID = leader.getUniqueId();

        if (!formingParties.containsKey(leaderUUID)) {
            if (leaderByMember.containsKey(leaderUUID)) {
                leader.sendMessage(ChatColor.RED + "Only the party leader can start the run!");
            } else {
                leader.sendMessage(ChatColor.RED + "You don't have a party! Invite someone with /srp coop request <player>.");
            }
            return;
        }

        MultiplayerParty party = formingParties.get(leaderUUID);
        if (party.size() < 2) {
            leader.sendMessage(ChatColor.RED + "You need at least one other player in your party first!");
            return;
        }

        formingParties.remove(leaderUUID);
        party.getMemberUUIDs().forEach(leaderByMember::remove);

        StopWatch stopWatch = new StopWatch();
        List<Speedrunner> speedrunners = party.getMemberUUIDs().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull) // member disconnected between accept and start
                .map(p -> new Speedrunner(p, stopWatch))
                .peek(Speedrunner::captureState)
                .collect(Collectors.toList());

        if (speedrunners.size() < 2) {
            leader.sendMessage(ChatColor.RED + "Not enough party members are still online to start!");
            return;
        }

        CoopSpeedrun coopSpeedrun = new CoopSpeedrun(GameMode.COOP, speedrunners, stopWatch, null);
        gameManager.registerRun(coopSpeedrun);
        initializeRun(coopSpeedrun);

        List<Player> players = speedrunners.stream().map(Speedrunner::getPlayer).collect(Collectors.toList());
        players.forEach(p -> p.sendMessage(ChatColor.YELLOW + "Creating the world..."));

        worldManager.createWorldsForPlayers(List.of(leader), null, sets -> {
            WorldManager.WorldSet worldSet = sets.get(leaderUUID);
            coopSpeedrun.setSeed(worldSet.getOverworld().getSeed());

            speedrunners.forEach(sr -> {
                sr.setWorldSet(worldSet);
                sr.freeze();
            });

            players.forEach(p -> p.teleport(worldSet.getSpawn()));
            speedrunners.forEach(Speedrunner::resetState);

            startCountdown(coopSpeedrun, speedrunners);
        });
    }

    /* ==========================================================
     *                       RESET COOP
     * ========================================================== */
    @Override
    public void reset(Player player) {
        // Does nothing
    }

    /* ==========================================================
     *                       STOP COOP
     * ========================================================== */
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

        String formattedTime = new TimeFormatter(coopSpeedrun.getStopWatch())
                .withHours()
                .withSuperscriptMs()
                .format();

        coopSpeedrun.getSpeedrunners().forEach(sr -> sr.getPlayer().sendTitle(
                ChatColor.GREEN + "You won! ",
                ChatColor.GREEN + "With a time of: " + ChatColor.ITALIC + ChatColor.GRAY + formattedTime,
                10, 140, 20
        ));

        finishRun(coopSpeedrun, 200);
    }

    @Override
    public void handlePlayerQuit(Player player) {
        super.handlePlayerQuit(player); // handles any pending single invite

        UUID uuid = player.getUniqueId();

        // Player was leading a forming party -> disband it
        MultiplayerParty asLeader = formingParties.remove(uuid);
        if (asLeader != null) {
            asLeader.getMemberUUIDs().stream()
                    .filter(memberUUID -> !memberUUID.equals(uuid))
                    .forEach(memberUUID -> {
                        leaderByMember.remove(memberUUID);
                        Player member = Bukkit.getPlayer(memberUUID);
                        if (member != null) {
                            member.sendMessage(ChatColor.YELLOW + "Your party leader disconnected;" +
                                    " the party has been disbanded.");
                        }
                    });
            return;
        }

        // Player was a member of someone else's forming party -> just remove them
        UUID leaderUUID = leaderByMember.remove(uuid);
        if (leaderUUID != null) {
            MultiplayerParty party = formingParties.get(leaderUUID);
            if (party != null) {
                party.removeMember(uuid);
                Player leader = Bukkit.getPlayer(leaderUUID);
                if (leader != null) {
                    leader.sendMessage(ChatColor.YELLOW + player.getName() + " disconnected and" +
                            " left your party. (" + party.size() + ")");
                }
            }
        }
    }
}
