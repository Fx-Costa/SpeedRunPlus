package com.fx.srp.model.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class PendingRequest {

    private final UUID playerUUID;
    private final long timestamp;

    @Getter @Setter private int timeoutTaskId = -1;

    public PendingRequest(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isByPlayer(UUID playerUUID) {
        return this.playerUUID.equals(playerUUID);
    }

}
