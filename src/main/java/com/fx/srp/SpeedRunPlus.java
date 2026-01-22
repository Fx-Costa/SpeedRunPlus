package com.fx.srp;

import cloud.commandframework.CommandManager;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import com.fx.srp.listeners.CommandListener;
import com.fx.srp.listeners.PlayerEventListener;
import com.fx.srp.listeners.WorldEventListener;
import com.fx.srp.commands.CommandRegistry;
import com.fx.srp.managers.GameManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * SpeedRunPlus-plugin entry point
 */
@NoArgsConstructor
public class SpeedRunPlus extends JavaPlugin {

    private final Logger logger = Bukkit.getLogger();

    // Managers
    @Getter private GameManager gameManager;

    // External plugin APIs
    @Getter private MVWorldManager mvWorldManager;
    @Getter private MultiverseNetherPortals portalManager;

    /**
     * Loading the plugin when enabled by Bukkit
     */
    @Override
    public void onEnable() {
        // Add the default config file
        saveDefaultConfig();

        // Load 3rd party plugin dependencies
        loadDependencies();

        // Initialize the game manager
        gameManager = new GameManager(this);

        // Initialize Cloud Command framework
        initializeCloudCommands(gameManager);

        // Register event listeners
        registerListeners();

        // Plugin server console banner
        String version = getDescription().getVersion();
        String reset = "\u001B[0m";
        String yellow = "\u001B[33m";
        logger.info("");
        logger.info(yellow + "   ____                _____              __ " + reset);
        logger.info(yellow + "  / __/__  ___ ___ ___/ / _ \\__ _____  __/ /_" + reset);
        logger.info(yellow + " _\\ \\/ _ \\/ -_) -_) _  / , _/ // / _ \\/_  __/" + reset);
        logger.info(yellow + "/___/ .__/\\__/\\__/\\_,_/_/|_|\\_,_/_//_/ /_/   " + reset);
        logger.info(yellow + "   /_/                                       " + reset);
        logger.info(yellow + "\tby Fx-Costa" + "\t\t" + "v" + version + reset);
        logger.info("");
    }

    /**
     * Unloading the plugin when disabled by Bukkit
     */
    @Override
    public void onDisable() {
        gameManager.abortAllRuns();
        logger.info("[SRP] The plugin has stopped successfully!");
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void initializeCloudCommands(GameManager gameManager) {
        try {
            // Create the Cloud command manager
            CommandManager<CommandSender> commandManager;

            // Initialize the CommandManager based on platform
            if (isPaper()) {
                PaperCommandManager<CommandSender> paperCommandManager = new PaperCommandManager<>(
                        this,
                        CommandExecutionCoordinator.simpleCoordinator(),
                        Function.identity(),
                        Function.identity()
                );

                // Enable Brigadier (tab completion)
                paperCommandManager.registerBrigadier();

                // Enable async completions
                paperCommandManager.registerAsynchronousCompletions();

                commandManager = paperCommandManager;
            }
            else {
                commandManager = new BukkitCommandManager<>(
                        this,
                        CommandExecutionCoordinator.simpleCoordinator(),
                        Function.identity(),
                        Function.identity()
                );
            }

            // Register commands
            CommandRegistry.registerCommands(commandManager, gameManager);

        } catch (Exception e) {
            logger.severe("Failed to initialize commands");
        }
    }

    private void loadDependencies() {
        final Plugin mvCore = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        final Plugin mvNether = Bukkit.getPluginManager().getPlugin("Multiverse-NetherPortals");

        if (!(mvCore instanceof MultiverseCore)) {
            logger.severe("[SRP] Multiverse-Core is missing!");
            setEnabled(false);
            return;
        }

        if (!(mvNether instanceof MultiverseNetherPortals)) {
            logger.severe("[SRP] Multiverse-NetherPortals is missing!");
            setEnabled(false);
            return;
        }

        mvWorldManager = ((MultiverseCore) mvCore).getMVWorldManager();
        portalManager = (MultiverseNetherPortals) mvNether;
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(gameManager), this);
        Bukkit.getPluginManager().registerEvents(new WorldEventListener(gameManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(gameManager), this);
    }

    private boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
