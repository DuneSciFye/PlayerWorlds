package net.sivils.playerWorlds.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.database.Cache;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.database.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.LoadFailureReason;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static net.sivils.playerWorlds.utils.Utils.prependAndLimit;

public class WorldUtils {

  private static final Database db = PlayerWorlds.getInstance().getDatabase();
  public static HashMap<String, ArrayList<String>> activeWorldPlugins = new HashMap<>(); // World UUID, List of Active Plugins

  public static void loadWorld(String worldUUID) {
    try {
      if (!db.worldExists(worldUUID)) return;

      List<String> worldNames = List.of(worldUUID + "_nether", worldUUID + "_the_end");
      WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();

      for (String worldName : worldNames) {
        MultiverseWorld mvWorld = worldManager.getWorld(worldName).get();
        if (mvWorld != null && mvWorld.isLoaded()) return; // Only load worlds if all the worlds were originally unloaded
      }

      for (String worldName : worldNames) {
        worldManager.loadWorld(worldName);
      }

      db.setWorldField(worldUUID, "last_use_time", Timestamp.from(Instant.now()));
      activeWorldPlugins.put(worldUUID, new ArrayList<>());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void unloadWorld(@NotNull String worldUUID) {
    try {
      if (!db.worldExists(worldUUID)) return;

      List<String> worldNames = List.of(worldUUID, worldUUID + "_nether", worldUUID + "_the_end");
      List<LoadedMultiverseWorld> loadedWorlds = new ArrayList<>();
      WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();

      for (String worldName : worldNames) {
        MultiverseWorld mvWorld = worldManager.getWorld(worldName).get();
        if (mvWorld != null && mvWorld.isLoaded()) {
          if (!((LoadedMultiverseWorld) mvWorld).getPlayers().get().isEmpty()) {
            return; // Only unload worlds if all worlds are empty
          }
          loadedWorlds.add((LoadedMultiverseWorld) mvWorld);
        }
      }

      for (LoadedMultiverseWorld loadedWorld : loadedWorlds) {
        worldManager.unloadWorld(UnloadWorldOptions.world(loadedWorld));
      }

      updateWorldUseTime(worldUUID);

      activeWorldPlugins.remove(worldUUID);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void updateWorldUseTime(String worldUUID) throws SQLException {
    Timestamp timestamp = db.getTimestampField(worldUUID, "last_use_time");
    Duration duration = Duration.between(timestamp.toInstant(), Instant.now());

    db.setWorldField(worldUUID, "last_use_time", Timestamp.from(Instant.now()));
    long deletionTime = (int) db.getWorldField(worldUUID, "deletion_time");
    deletionTime += duration.toSeconds();
    db.setWorldField(worldUUID, "deletion_time", deletionTime);
  }

  public static void deleteWorld(String worldUUID) throws SQLException {
    WorldGroupManager groupManager = MultiverseInventoriesApi.get().getWorldGroupManager();
    WorldGroup group = groupManager.getGroup(worldUUID);

    Location spawnLoc = Config.spawnWorld.getSpawnLocation();

    // MultiverseCore
    WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
    for (String worldName : group.getWorlds()) {
      MultiverseWorld mvWorld = worldManager.getWorld(worldName).get();
      if (mvWorld != null && mvWorld.isLoaded()) {
        for (Player p : ((LoadedMultiverseWorld) mvWorld).getPlayers().get()) p.teleport(spawnLoc);
      }
      worldManager.deleteWorld(DeleteWorldOptions.world(mvWorld));
    }
    groupManager.removeGroup(group);
    db.removeWorld(worldUUID);

    // Remove plugins
    WorldUtils.activeWorldPlugins.remove(worldUUID);

    // Remove from Cache
    Cache cache = PlayerWorlds.getCache();
    if (cache.removeCachedWorldData(worldUUID)) {
      PlayerWorlds.getInstance().getLogger().warning("Tried to remove world " + worldUUID + " from cache when " +
        "deleting world, but it wasn't in Cache.");
    }

    // Deleting files
    deleteFolder(new File(Bukkit.getPluginsFolder(), "Multiverse-Inventories/groups/" + worldUUID));
    deleteFolder(new File(Bukkit.getPluginsFolder(), "Multiverse-Inventories/worlds/" + worldUUID));
    deleteFolder(new File(Bukkit.getPluginsFolder(), "WorldGuard/worlds/" + worldUUID));
    deleteFolder(new File(Bukkit.getPluginsFolder(), "WorldGuard/worlds/" + worldUUID + "_nether"));
    deleteFolder(new File(Bukkit.getPluginsFolder(), "WorldGuard/worlds/" + worldUUID + "_the_end"));
  }

  public static void runWorldDeletion() throws SQLException {
    List<String> worldNames = db.getAllWorlds();
    for (String worldName : worldNames) {
      Timestamp lastUseTime = db.getTimestampField(worldName, "last_use_time");
      Duration deletionTime = Duration.ofSeconds((int) db.getWorldField(worldName, "deletion_time"));

      if (Duration.between(lastUseTime.toInstant(), Instant.now()).compareTo(deletionTime) >= 0) {
        deleteWorld(worldName);
        PlayerWorlds.getInstance().getLogger().info("Deleted world " + worldName + ". Last use time: " + lastUseTime.toInstant() + ". Deletion time: " + getDeletionTimeMessage(deletionTime));
      }
    }
  }


  private static boolean deleteFolder(File folder) {
    File[] files = folder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          if (!deleteFolder(file)) return false;
        } else {
          if (!file.delete()) return false;
        }
      }
    }
    return folder.delete();
  }

  public static String getDeletionTimeMessage(Duration duration) {
    String message = "";
    long days = duration.toDaysPart();
    if (days == 1) {
      message += "1 day, ";
    } else if (days > 1) {
      message += days + " days, ";
    }

    long hours = duration.toHoursPart();
    if (hours == 1) {
      message += "1 hour, ";
    } else {
      message += hours + " hours, ";
    }

    long minutes = duration.toMinutesPart();
    if (minutes == 1) {
      message += "1 minute, ";
    } else {
      message += minutes + " minutes, ";
    }

    long seconds = duration.toSecondsPart();
    if (seconds == 1) {
      message += "1 second";
    } else {
      message += seconds + " seconds";
    }

    return message;
  }

  public static String getWorldUUID(String worldName) {
    if (worldName.length() >= 36) {
      String worldUUID = worldName.substring(0, 36);
      try {
        if (PlayerWorlds.getInstance().getDatabase().worldExists(worldUUID)) return worldUUID;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  public static List<Player> getPlayersInPlayerWorld(String worldName) {
    String worldUUID = getWorldUUID(worldName);
    if (worldUUID == null) return null;
    World world = Bukkit.getWorld(worldName);
    World nether = Bukkit.getWorld(worldName + "_nether");
    World end = Bukkit.getWorld(worldName + "_the_end");

    if (world == null || nether == null || end == null) return null;

    List<Player> players = world.getPlayers();
    players.addAll(nether.getPlayers());
    players.addAll(end.getPlayers());

    return players;
  }

  public static void goToWorld(String worldUUID, Player player, Cache cache) {
    final UUID playerUUID = player.getUniqueId();
    final WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
    final MultiverseWorld world = worldManager.getWorld(worldUUID).get();

    // Loading World
    if (!world.isLoaded()) {
      Attempt<LoadedMultiverseWorld, LoadFailureReason> loadedWorld = worldManager.loadWorld(worldUUID);

      if (loadedWorld.isFailure()) {
        player.sendMessage(Component.text("Failed to load world " + loadedWorld.get().getName() + ". Please report to an administrator.", NamedTextColor.RED));
        player.sendMessage(Component.text(loadedWorld.getFailureMessage().toString(), NamedTextColor.RED));
        return;
      }
    }

    WorldUtils.loadWorld(worldUUID);

    player.teleport(world.getSpawnLocation());

    // Set Last Join Worlds and Last Join Times
    final PlayerInfo playerInfo = cache.getCachedPlayerInfo(playerUUID);
    if (playerInfo == null) return;

    final String worldUUIDs = prependAndLimit(playerInfo.lastJoinWorlds(), worldUUID, 5);
    final String times = prependAndLimit(playerInfo.lastJoinTimes(), Timestamp.from(Instant.now()).toString(), 5);

    cache.setCachedPlayerInfo(playerUUID,
      playerInfo.withLastJoinWorlds(worldUUIDs).withLastJoinTimes(times)
    );

  }

}

