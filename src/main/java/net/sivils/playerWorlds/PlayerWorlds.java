package net.sivils.playerWorlds;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import net.sivils.playerWorlds.commands.PlayerWorldsCommand;
import net.sivils.playerWorlds.commands.WhitelistCommand;
import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.hooks.PlaceholderAPIHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class PlayerWorlds extends JavaPlugin {

    private Database database;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }


    @Override
    public void onEnable() {
        Logger logger = this.getLogger();

        CommandAPI.onEnable();
        registerCommands();

        try {
            if (!getDataFolder().exists()) {
                if (!getDataFolder().mkdirs()) logger.severe("Failed to create data folder.");
            }

            database = new Database(getDataFolder().getAbsolutePath() + "/playerworlds.db");
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
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("PlacholderAPI")) {
            logger.info("Detected PlaceholderAPI. Enabling hook for it.");
            PlaceholderAPIHook.setEnabled();
        }

        new Config(this);

        logger.info("Plugin enabled");
    }

    @Override
    public void onDisable() {
        try {
            database.closeConnection();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        CommandAPI.onDisable();
    }



    public void registerCommands() {
        new PlayerWorldsCommand().register(this);
        new WhitelistCommand().register(this);
    }

    public Database getDatabase() {
        return database;
    }
}
