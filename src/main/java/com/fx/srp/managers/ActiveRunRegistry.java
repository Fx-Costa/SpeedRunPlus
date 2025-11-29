package com.fx.srp.managers;

import com.fx.srp.model.run.ISpeedrun;
import lombok.Getter;

import java.util.*;

public class ActiveRunRegistry {

    @Getter private static final ActiveRunRegistry instance = new ActiveRunRegistry();

    private final Map<UUID, ISpeedrun> activeRuns = new HashMap<>();

    public boolean isPlayerInAnyRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    public void addRun(UUID playerId, ISpeedrun run) {
        activeRuns.put(playerId, run);
    }

    public void removeRun(UUID playerId) {
        activeRuns.remove(playerId);
    }

    public ISpeedrun getActiveRun(UUID playerId) {
        return activeRuns.get(playerId);
    }

    public Collection<ISpeedrun> getAllRuns() {
        return activeRuns.values();
    }

    public List<UUID> getAllPlayersInRuns() {
        return new ArrayList<>(activeRuns.keySet());
    }
}
