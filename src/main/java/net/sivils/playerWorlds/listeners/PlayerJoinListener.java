package net.sivils.playerWorlds.listeners;

import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Cache;
import net.sivils.playerWorlds.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerJoinListener implements Listener {

  /**
   * Adds the player's world to cache on join
   * @param e PlayerJoinEvent
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    final Player p = e.getPlayer();
    final UUID playerUUID = p.getUniqueId();

    // CF For getting world name of player
    final CompletableFuture<String> cf = new CompletableFuture<>();
    Utils.getWorldUUID(cf, playerUUID.toString());

    // Add to Cache Data when CF is complete
    cf.whenComplete((worldUUID, err) -> {
      if (worldUUID != null) {
        final Cache cache = PlayerWorlds.getCache();
        cache.setupWorldCacheData(worldUUID);
        cache.setPlayersWorlds(playerUUID, new ArrayList<>(List.of((worldUUID))));
        cache.setupPlayerInfoCache(playerUUID);
        cache.setupPlayerAccessCache(playerUUID);
      }
    });
  }

}
