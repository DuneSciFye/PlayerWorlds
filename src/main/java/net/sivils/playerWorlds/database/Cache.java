package net.sivils.playerWorlds.database;

import net.sivils.playerWorlds.PlayerWorlds;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

  private final Map<String, WorldData> worldsCache; // A cache of the Worlds and their WorldData
  private final Map<UUID, ArrayList<String>> playersWorlds; // A cache of which player owns what worlds
  private final Map<Pair<String, UUID>, PlayerAccess> playerAccessesCache; // A cache of the player accesses data in DB
  private final Map<UUID, PlayerInfo> playerInfoCache; // A cache of the player info data in DB

  /**
   * Initializes the Cache
   */
  public Cache() {
    worldsCache = new ConcurrentHashMap<>();
    playersWorlds = new ConcurrentHashMap<>();
    playerAccessesCache = new ConcurrentHashMap<>();
    playerInfoCache = new ConcurrentHashMap<>();
  }

  // Save cache to the database
  private void saveCache() {

  }

  /**
   * Adds a world to the Worlds Cache
   * @param worldUUID The String UUID of the World to add to Cache
   */
  public void setupWorldCacheData(@NotNull String worldUUID) {
    if (isInWorldCache(worldUUID)) return;

    final PlayerWorlds playerWorlds = PlayerWorlds.getInstance();
    final Database db = playerWorlds.getDatabase();

    Bukkit.getScheduler().runTaskAsynchronously(playerWorlds, () -> {
      try {
        worldsCache.put(worldUUID, db.getWorldData(worldUUID));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Checks if a world is in cache
   * @param worldUUID The String UUID of the world to check
   * @return true if the world is in the cache, otherwise false
   */
  public boolean isInWorldCache(@NotNull String worldUUID) {
    return worldsCache.containsKey(worldUUID);
  }

  /**
   * First saves the Cache Data to SQLite Database, then removes it from Cache
   * @param worldUUID The String UUID of the world to Save and Remove
   */
  public void saveAndRemoveWorldCacheData(@NotNull String worldUUID) {
    // Removing from Cache
    final WorldData worldData = worldsCache.remove(worldUUID);
    if (worldData == null) return;

    // Saving to SQLite
    final PlayerWorlds playerWorlds = PlayerWorlds.getInstance();
    final Database db = playerWorlds.getDatabase();

    Bukkit.getScheduler().runTaskAsynchronously(playerWorlds, () -> {
      try {
        db.saveWorldData(worldUUID, worldData);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Removes cache from database without saving
   * @param worldUUID The String UUID of the world to remove
   * @return Whether or not there was data to remove
   */
  public boolean removeCachedWorldData(@NotNull String worldUUID) {
    // Removing from Cache
    return worldsCache.remove(worldUUID) != null;
  }

  /**
   * Returns the Cached WorldData for a world. If the world doesn't exist in cache, then
   * it will try to obtain the WorldData for the world, store it in cache, and return that.
   * @param worldUUID The String UUID of the world
   * @return WorldData of the world
   */
  public WorldData getCachedWorldData(String worldUUID) {
    return worldsCache.computeIfAbsent(worldUUID, uuid -> {
      final Database db = PlayerWorlds.getInstance().getDatabase();
      try {
        return db.getWorldData(uuid);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public void setCachedWorldData(@NotNull String worldUUID, @NotNull WorldData worldData) {
    worldsCache.put(worldUUID, worldData);
  }

  /**
   * Sets the worlds that a player owns
   * @param uuid The UUID of the player
   * @param worlds An ArrayList of the String UUIDs of the worlds
   */
  public void setPlayersWorlds(@NotNull UUID uuid, @NotNull ArrayList<String> worlds) {
    playersWorlds.put(uuid, worlds);
  }

  /**
   * Obtain the Worlds a player owns
   * @param uuid The UUID of the Player
   * @return An Arraylist of the String UUIDs of the worlds
   */
  public ArrayList<String> getPlayersWorlds(@NotNull UUID uuid) {
    return playersWorlds.get(uuid);
  }

  /**
   * Adds a player to the PlayerInfo cache
   * @param playerUUID The UUID of the Player to add to Cache
   */
  public void setupPlayerInfoCache(@NotNull UUID playerUUID) {
    final PlayerWorlds playerWorlds = PlayerWorlds.getInstance();
    final Database db = playerWorlds.getDatabase();

    Bukkit.getScheduler().runTaskAsynchronously(playerWorlds, () -> {
      try {
        playerInfoCache.put(playerUUID, db.getPlayerInfoData(playerUUID));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Obtains the Cached PlayerInfo for a Player
   * @param playerUUID The UUID of the Player
   * @return PlayerInfo stored in Cache
   */
  public PlayerInfo getCachedPlayerInfo(UUID playerUUID) {
    return playerInfoCache.get(playerUUID);
  }

  /**
   * Sets a new cached PlayerInfo
   * @param playerUUID The UUID of the Player
   * @param playerInfo The PlayerInfo to set in cache
   */
  public void setCachedPlayerInfo(@NotNull UUID playerUUID, @NotNull PlayerInfo playerInfo) {
    playerInfoCache.put(playerUUID, playerInfo);
  }


  /**
   * Adds to cache all worlds that a player has specified accesses for
   * @param playerUUID The UUID of the Player to add to Cache
   */
  public void setupPlayerAccessCache(@NotNull UUID playerUUID) {
    final PlayerWorlds playerWorlds = PlayerWorlds.getInstance();
    final Database db = playerWorlds.getDatabase();

    Bukkit.getScheduler().runTaskAsynchronously(playerWorlds, () -> {
      try {
        HashMap<String, PlayerAccess> playerAccesses = db.getPlayerAccessData(playerUUID.toString());
        for (Map.Entry<String, PlayerAccess> entry : playerAccesses.entrySet()) {
          playerAccessesCache.put(Pair.of(entry.getKey(), playerUUID), entry.getValue());
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Obtains the Cached PlayerAccess for a Player
   * @param worldUUID The String UUID of the World to check
   * @param playerUUID The UUID of the Player
   * @return PlayerAccess stored in Cache for a World, Player pair
   */
  public PlayerAccess getCachedPlayerAccess(String worldUUID, UUID playerUUID) {
    return playerAccessesCache.get(Pair.of(worldUUID, playerUUID));
  }

  /**
   * Sets a new cached PlayerAccess
   * @param playerUUID The UUID of the Player
   * @param playerAccess The PlayerAccess to set in cache
   */
  public void setCachedPlayerAccess(@NotNull String worldUUID, @NotNull UUID playerUUID,
                                    @NotNull PlayerAccess playerAccess) {
    playerAccessesCache.put(Pair.of(worldUUID, playerUUID), playerAccess);
  }

  /**
   * Removes Player Access from the Cache, then asynchronously from the Database
   * @param worldUUID The String UUID of the world to remove Access in
   * @param playerUUID The UUID of the Player to remove Access for
   * @return Whether or not any data was removed
   */
  public boolean deletePlayerAccess(@NotNull String worldUUID, @NotNull UUID playerUUID) {
    // Deleting from SQLite Database
    PlayerWorlds playerWorlds = PlayerWorlds.getInstance();
    Bukkit.getScheduler().runTaskAsynchronously(playerWorlds, () -> {
      Database db = playerWorlds.getDatabase();
      try {
        db.removePlayerAccess(worldUUID, playerUUID.toString());
      } catch (SQLException e) {
        playerWorlds.getLogger().warning("There was a database error while removing Player Access " + worldUUID + ", " + playerUUID +
          " from the " +
          "Database!");
        throw new RuntimeException(e);
      }
    });

    return playerAccessesCache.remove(Pair.of(worldUUID, playerUUID)) != null;

  }

}
