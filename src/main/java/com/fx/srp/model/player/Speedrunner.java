package com.fx.srp.model.player;

import com.fx.srp.managers.util.WorldManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@Getter
public class Speedrunner {

    private final Player player;

    // Saved pre-speedrun state
    @Setter private ItemStack[] savedInventory;
    @Setter private ItemStack[] savedArmor;
    @Setter private Map<Advancement, Set<String>> savedAdvancements;
    @Setter private int savedLevel;
    @Setter private float savedExp;
    @Setter private GameMode savedGameMode;
    private boolean freeze = false;

    // Worlds
    @Getter @Setter private WorldManager.WorldSet worldSet;

    // Stopwatch
    private final StopWatch stopWatch;

    public Speedrunner(Player player, StopWatch stopWatch) {
        this.player = player;
        this.stopWatch = stopWatch;
    }

    /* ==========================================================
     *                      Player states
     * ========================================================== */
    public void freeze(){
        if (!isFrozen()){
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
            player.setAllowFlight(false);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.JUMP,
                    Integer.MAX_VALUE,
                    128,
                    false,
                    false,
                    false
            ));
            this.freeze = true;
        }
    }

    public void unfreeze(){
        if (isFrozen()){
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.removePotionEffect(PotionEffectType.JUMP);
            this.freeze = false;
        }
    }

    public boolean isFrozen(){
        return freeze;
    }

    public void captureState(){
        // Save the player state
        setSavedGameMode(player.getGameMode());
        setSavedInventory(clonePlayerInventory(player));
        setSavedArmor(clonePlayerArmor(player));
        setSavedLevel(getPlayerLevel(player));
        setSavedExp(getExp(player));
        setSavedAdvancements(clonePlayerAdvancements(player));
    }

    public void resetState(){
        // Reset the player state
        resetPlayerStats(player);
        clearPlayerInventory(player);
        clearPlayerAdvancements(player);

        // Give the player a new empty scoreboard
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void restoreState(){
        // Reset the player state
        resetState();

        // Get the player and their inventory
        PlayerInventory inventory = player.getInventory();

        // Restore using the saved state
        inventory.setArmorContents(getSavedArmor());
        inventory.setContents(getSavedInventory());
        player.setLevel(getSavedLevel());
        player.setExp(getSavedExp());
        player.setGameMode(getSavedGameMode());
        for (Map.Entry<Advancement, Set<String>> entry : getSavedAdvancements().entrySet()) {
            Advancement advancement = entry.getKey();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criterion : entry.getValue()) {
                progress.awardCriteria(criterion);
            }
        }
    }

    /* ==========================================================
     *                       HELPERS
     * ========================================================== */
    private ItemStack[] clonePlayerArmor(Player player) {
        return player.getInventory().getArmorContents().clone();
    }

    private ItemStack[] clonePlayerInventory(Player player){
        return player.getInventory().getContents().clone();
    }

    private int getPlayerLevel(Player player){
        return player.getLevel();
    }

    private float getExp(Player player){
        return player.getExp();
    }

    private Map<Advancement, Set<String>> clonePlayerAdvancements(Player player){
        Map<Advancement, Set<String>> advancements = new HashMap<>();
        Bukkit.getServer().advancementIterator().forEachRemaining(advancement -> {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (!progress.getAwardedCriteria().isEmpty()) {
                advancements.put(advancement, new HashSet<>(progress.getAwardedCriteria()));
            }
        });
        return advancements;
    }

    private void resetPlayerStats(Player player){
        // Set survival
        player.setGameMode(GameMode.SURVIVAL);

        // Reset stats
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setExp(0.0f);
        player.setLevel(0);
        player.setSaturation(20.0f);

        // Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void clearPlayerInventory(Player player){
        PlayerInventory inventory = player.getInventory();

        // Clear armor & inventory
        inventory.clear();
        inventory.setArmorContents(null);
    }

    private void clearPlayerAdvancements(Player player){
        Bukkit.getServer().advancementIterator().forEachRemaining(advancement -> {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criteria : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criteria);
            }
        });
    }
}
