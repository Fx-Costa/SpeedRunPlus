package com.fx.srp.managers;

import com.fx.srp.model.run.Speedrun;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Maintains a registry of all currently active speedruns and the players
 * participating in them.
 *
 * <p>This singleton class allows tracking, adding, removing, and querying
 * active runs. Each player is associated with a single {@link Speedrun} at a time.</p>
 *
 * <p>Provides a singleton instance accessible via {@link #getINSTANCE()}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActiveRunRegistry {

    @Getter private static final ActiveRunRegistry INSTANCE = new ActiveRunRegistry();

    private final Map<UUID, Speedrun> activeRuns = new ConcurrentHashMap<>();

    /**
     * Checks whether the given player is currently participating in any active run.
     *
     * @param playerId the UUID of the player
     * @return {@code true} if the player is in a run, otherwise {@code false}
     */
    public boolean isPlayerInAnyRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    /**
     * Registers a player in an active run.
     *
     * @param playerId the UUID of the player
     * @param run the {@link Speedrun} the player is participating in
     */
    public void addRun(UUID playerId, Speedrun run) {
        activeRuns.put(playerId, run);
    }

    /**
     * Removes a player from the active run registry.
     *
     * @param playerId the UUID of the player to remove
     */
    public void removeRun(UUID playerId) {
        activeRuns.remove(playerId);
    }

    /**
     * Retrieves the active run associated with a player.
     *
     * @param playerId the UUID of the player
     * @return the {@link Speedrun} the player is in, or {@code null} if none
     */
    public Speedrun getActiveRun(UUID playerId) {
        return activeRuns.get(playerId);
    }

    /**
     * Returns a collection of all currently active runs.
     *
     * @return a {@link Collection} of {@link Speedrun} objects
     */
    public Collection<Speedrun> getAllRuns() {
        return activeRuns.values();
    }

    /**
     * Returns a list of all player UUIDs currently participating in any run.
     *
     * @return a {@link List} of player UUIDs
     */
    public List<UUID> getAllPlayersInRuns() {
        return new ArrayList<>(activeRuns.keySet());
    }

    /**
     * Returns a list of all player UUIDs currently participating in any run.
     *
     * @return a {@link List} of player UUIDs
     */
    public List<UUID> getAllPlayersInRun(Speedrun run) {
        return activeRuns.entrySet().stream()
                .filter(entry -> entry.getValue() == run)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
