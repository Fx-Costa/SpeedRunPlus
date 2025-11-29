package com.fx.srp.model.run;

import com.fx.srp.managers.GameManager;
import com.fx.srp.managers.util.WorldManager;
import com.fx.srp.model.player.Speedrunner;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

abstract public class AbstractSpeedrun implements ISpeedrun {

    protected final GameManager gameManager;

    @Getter private final StopWatch stopWatch;

    @Getter @Setter private State state = State.WAITING;

    @Getter @Setter private Long seed;

    private final Speedrunner owner;

    @Getter @Setter protected BukkitTask timerUpdateTask;

    @Getter @Setter protected BukkitTask timeoutTask;

    public AbstractSpeedrun(GameManager gameManager, Speedrunner owner, StopWatch stopWatch, Long seed) {
        this.gameManager = gameManager;
        this.owner = owner;
        this.stopWatch = stopWatch;
        this.seed = seed;
    }

    public List<Speedrunner> getSpeedrunners() {
        return List.of(owner);
    }

    public void onPlayerLeave(Player player) {
        gameManager.finishRun(this, null);
    }

    public void onPlayerRespawn(Speedrunner speedrunner, PlayerRespawnEvent event) {
        WorldManager.WorldSet worlds = speedrunner.getWorldSet();

        // Get the respawn location's world
        World respawnWorld = event.getRespawnLocation().getWorld();
        String respawnWorldName = respawnWorld.getName();

        // Overwrite the respawn location, if it is not in a speedrun world
        if (!respawnWorldName.equals(worlds.getOverworld().getName())
                && !respawnWorldName.equals(worlds.getNether().getName())
                && !respawnWorldName.equals(worlds.getEnd().getName())) {
            // Always respawn in the speedrun overworld
            event.setRespawnLocation(worlds.getOverworld().getSpawnLocation());
        }
    }
}
