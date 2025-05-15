package net.sivils.playerWorlds.config;

import net.sivils.playerWorlds.PlayerWorlds;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class Config {

    public static World spawnWorld;
    public static Location spawnLocation;

    public Config(PlayerWorlds plugin) {
        Logger logger = plugin.getLogger();

        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        spawnWorld = plugin.getServer().getWorld(config.getString("SpawnWorld", "world"));
        if (spawnWorld == null) {
            logger.warning("Spawn world not found.");
        }
        spawnLocation = spawnWorld.getSpawnLocation();
    }

}
