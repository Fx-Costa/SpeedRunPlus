package com.fx.srp;

import com.fx.srp.commands.SRPCommand;
import com.fx.srp.listeners.PlayerEventListener;
import com.fx.srp.listeners.WorldEventListener;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.commands.CommandHandler;
import com.fx.srp.managers.GameManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * SpeedRunPlus-plugin entry point
 */
@NoArgsConstructor
public class SpeedRunPlus extends JavaPlugin {

    private final Logger logger = Bukkit.getLogger();

    // Managers
    @Getter private GameManager gameManager;
    @Getter private ConfigHandler configHandler;

    // External plugin APIs
    @Getter private MVWorldManager mvWorldManager;
    @Getter private MultiverseNetherPortals portalManager;

    /**
     * Loading the plugin when enabled by Bukkit
     */
    @Override
    public void onEnable() {
        // Load 3rd party plugin dependencies
        loadDependencies();

        // Initialize managers
        configHandler = ConfigHandler.getInstance();
        gameManager = new GameManager(this);

        // Register event listeners
        registerListeners();

        // Set command executor
        Objects.requireNonNull(getCommand(SRPCommand.getSrp())).setExecutor(new CommandHandler(gameManager));

        logger.info("[SRP] The plugin has started successfully!");
    }

    /**
     * Unloading the plugin when disabled by Bukkit
     */
    @Override
    public void onDisable() {
        gameManager.stopAllRuns();
        logger.info("[SRP] The plugin has stopped successfully!");
    }

    private void loadDependencies() {
        final Plugin mvCore = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        final Plugin mvNether = Bukkit.getPluginManager().getPlugin("Multiverse-NetherPortals");

        if (!(mvCore instanceof MultiverseCore)) {
            getLogger().severe("[SRP] Multiverse-Core is missing!");
            setEnabled(false);
            return;
        }

        if (!(mvNether instanceof MultiverseNetherPortals)) {
            getLogger().severe("[SRP] Multiverse-NetherPortals is missing!");
            setEnabled(false);
            return;
        }

        mvWorldManager = ((MultiverseCore) mvCore).getMVWorldManager();
        portalManager = (MultiverseNetherPortals) mvNether;
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(gameManager), this);
        Bukkit.getPluginManager().registerEvents(new WorldEventListener(gameManager), this);
    }
}

