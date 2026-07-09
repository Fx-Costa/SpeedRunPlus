package com.fx.srp.model.party;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a forming, pre-run party for a multiplayer game mode (e.g. coop).
 * <p>
 * A party tracks a single leader and the set of players who have joined them, before
 * any {@link com.fx.srp.model.run.Speedrun} is created. Membership is accumulated over
 * time as invited players accept, and is expected to be finalized by the leader
 * (e.g. via a "start" action) into an actual run.
 * </p>
 * <p>
 * The leader is always considered a member and is added automatically on construction.
 * Member order is preserved in insertion order, which may be relevant for UI/listing
 * purposes.
 * </p>
 */
@Getter
public class MultiplayerParty {

    private final UUID leaderUUID;
    private final Set<UUID> memberUUIDs = new LinkedHashSet<>(); // insertion order preserved

    public MultiplayerParty(UUID leaderUUID) {
        this.leaderUUID = leaderUUID;
        this.memberUUIDs.add(leaderUUID);
    }

    /**
     * Adds a player to this party.
     *
     * @param uuid the UUID of the player to add
     * @return {@code true} if the player was not already a member and has been added,
     *         {@code false} if they were already a member
     */
    public boolean addMember(UUID uuid) {
        return memberUUIDs.add(uuid);
    }

    /**
     * Removes a player from this party.
     * <p>
     * Note that this does not prevent removal of the leader themselves; callers
     * responsible for party lifecycle (e.g. disbanding on leader disconnect) should
     * handle that case explicitly rather than relying on this method.
     *
     * @param uuid the UUID of the player to remove
     * @return {@code true} if the player was a member and has been removed,
     *         {@code false} if they were not a member
     */
    public boolean removeMember(UUID uuid) {
        return memberUUIDs.remove(uuid);
    }

    /**
     * Checks whether the given player is currently a member of this party.
     *
     * @param uuid the UUID of the player to check
     * @return {@code true} if the player is a member (including the leader), {@code false} otherwise
     */
    public boolean contains(UUID uuid) {
        return memberUUIDs.contains(uuid);
    }

    /**
     * Returns the current number of members in this party, including the leader.
     *
     * @return the party size
     */
    public int size() {
        return memberUUIDs.size();
    }
}
