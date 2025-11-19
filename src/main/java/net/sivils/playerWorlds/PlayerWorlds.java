package net.sivils.playerWorlds;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import dev.jorel.commandapi.network.CommandAPIProtocol;
import net.luckperms.api.LuckPerms;
import net.sivils.playerWorlds.commands.*;
import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.database.Cache;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.gamerules.Gamerule;
import net.sivils.playerWorlds.gamerules.PVP;
import net.sivils.playerWorlds.hooks.PlaceholderAPIHook;
import net.sivils.playerWorlds.listeners.WorldLoadListener;
import net.sivils.playerWorlds.plugins.*;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.logging.Logger;

public final class PlayerWorlds extends JavaPlugin {

    private Database db;
    private static PlayerWorlds instance;
    private static LuckPerms luckPerms;
    private static Cache cache;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIPaperConfig(this));
    }


    @Override
    public void onEnable() {
        instance = this;
        Logger logger = this.getLogger();

        // CommandAPI Stuff
        CommandAPI.onEnable();
        Bukkit.getScheduler().runTaskLater(PlayerWorlds.getInstance(), () -> {
            for (String channel : CommandAPIProtocol.getAllChannelIdentifiers()) {
                Bukkit.getMessenger().unregisterIncomingPluginChannel(this, channel);
                Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, channel);
            }
        }, 20L);


        // Create Database
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
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logger.info("Detected PlaceholderAPI. Enabling hook for it.");
            new PlaceholderAPIHook(this);
        }
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            logger.info("Detected LuckPerms. Enabling hook for it.");
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
            }

        }

        // Register stuff
        registerGamerules();
        registerPlugins();
        registerCommands();
        registerListeners();

        // Setup Cache
        cache = new Cache();

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
        new TransferCommand().register();
        new GameruleCommand().register();
        new CheatsCommand().register();
    }

    public void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new WorldLoadListener(), this);
    }

    private void registerPlugins() {
        Plugins.registerPlugin(new NotTooExpensive(), "NotTooExpensive");
        Plugins.registerPlugin(new DimensionStacking(), "DimensionStacking");
        Plugins.registerPlugin(new GSit(), "GSit");
        Plugins.registerPlugin(new TPA(), "TPA");
    }

    private void registerGamerules() {
        Gamerule.registerGamerule(new PVP(), "PVP");
    }

    public Database getDatabase() {
        return db;
    }

    /**
     * Obtains the main instance of PlayerWorlds
     * @return Instance of PlayerWorlds
     */
    public static PlayerWorlds getInstance() {
        return instance;
    }

    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }

    /**
     * Obtains the main Cache of PlayerWorlds
     * @return Cache of Database Data
     */
    public static Cache getCache() {
        return cache;
    }

}
