package net.sivils.playerWorlds.utils;

import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.database.PlayerAccess;
import net.sivils.playerWorlds.database.WorldData;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

  /**
   * Will complete the CompletableFuture with a String UUID of the player's owned world
   * @param cf A CompletableFuture to complete
   * @param playerUUID String UUID of the player to get the world of
   */
  public static void getWorldUUID(final CompletableFuture<String> cf, final String playerUUID) {
    final Database db = PlayerWorlds.getInstance().getDatabase();

    Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
      try {
        final String worldUUID = db.getWorld(playerUUID);
        cf.complete(worldUUID);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  /**
   * Adds an item to the front of list and removes any items past the limit
   * @param csv The String version of the list
   * @param value The String value to prepend
   * @param limit The max number of items in the list
   * @return A new string with the value prepended
   */
  public static String prependAndLimit(String csv, String value, int limit) {
    return Stream.concat(Stream.of(value),
        (csv == null ? Stream.empty() : Arrays.stream(csv.split(","))))
      .limit(limit)
      .collect(Collectors.joining(","));
  }
}
