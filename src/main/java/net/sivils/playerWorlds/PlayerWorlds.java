package net.sivils.playerWorlds;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import net.sivils.playerWorlds.commands.*;
import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.hooks.PlaceholderAPIHook;
import net.sivils.playerWorlds.listeners.WorldLoadListener;
import net.sivils.playerWorlds.plugins.NotTooExpensive;
import net.sivils.playerWorlds.plugins.Plugins;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.logging.Logger;

public final class PlayerWorlds extends JavaPlugin {

    private Database db;
    private static PlayerWorlds instance;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }


    @Override
    public void onEnable() {
        instance = this;
        Logger logger = this.getLogger();

        CommandAPI.onEnable();

        try {
            if (!getDataFolder().exists()) {
                if (!getDataFolder().mkdirs()) logger.severe("Failed to create data folder.");
            }

            db = new Database(getDataFolder().getAbsolutePath() + "/playerworlds.db");
        } catch (SQLException ex) {
            logger.severe("Failed to connect to database playerworlds.db");
            Bukkit.getPluginManager().disablePlugin(this);
            throw new RuntimeException(ex);
        }

        // Hooks
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            logger.severe("Multiverse-Core not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Inventories")) {
            logger.severe("Multiverse-Inventories not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logger.info("Detected PlaceholderAPI. Enabling hook for it.");
            new PlaceholderAPIHook(this);
        }

        registerPlugins();
        registerCommands();
        registerListeners();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    LocalTime time = LocalTime.now();
                    if (time.getMinute() == 0) {
                        WorldUtils.runWorldDeletion();
                    }

                    for (String worldUUID : WorldUtils.activeWorldPlugins.keySet()) {
                        WorldUtils.updateWorldUseTime(worldUUID);
                    }
                } catch (SQLException e) {
                    logger.severe("Failed to run automated WorldDeletion. A database error occurred.");
                    throw new RuntimeException(e);
                }
            }
        }.runTaskTimer(this, 0L, 1200L);

        new Config(this);

        logger.info("Plugin enabled");
    }

    @Override
    public void onDisable() {
        try {
            db.closeConnection();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        CommandAPI.onDisable();
    }



    public void registerCommands() {
        new PlayerWorldsCommand().register(this);
        new CreateWorld().register(this);
        new Password().register(this);
        new RunWorldDeletion().register();
        new SetDeletionTime().register();
        new AccessCommand().register();
        new PluginsCommand().register(this);
    }

    public void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new WorldLoadListener(), this);
    }

    private void registerPlugins() {
        Plugins.registerPlugin(new NotTooExpensive(), "NotTooExpensive");
    }

    public Database getDatabase() {
        return db;
    }

    public static PlayerWorlds getInstance() {
        return instance;
    }

}
