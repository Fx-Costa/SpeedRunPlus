package com.fx.srp.model.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a pending speedrun request for multiplayer game modes sent from one player to another.
 * <p>
 * This class stores the UUID of the player who initiated the request, the timestamp
 * when the request was created, and the Bukkit task ID used to schedule a timeout.
 * </p>
 */
@Getter
public class PendingRequest {

    private final UUID playerUUID;
    private final long timestamp;

    @Getter @Setter private int timeoutTaskId = -1;

    /**
     * Constructs a new PendingRequest for the given player UUID.
     *
     * @param playerUUID The UUID of the player who sent the request.
     */
    public PendingRequest(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Checks if this request was sent by a specific player.
     *
     * @param playerUUID The UUID of the player to check.
     * @return {@code true} if this request was sent by the given player, {@code false} otherwise.
     */
    public boolean isByPlayer(UUID playerUUID) {
        return this.playerUUID.equals(playerUUID);
    }
}
