package net.sivils.playerWorlds.listeners;

import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.utils.Utils;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.CompletableFuture;

public class PlayerLeaveListener implements Listener {

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent e) {
    final Player p = e.getPlayer();
    final String playerUUID = p.getUniqueId().toString();

    p.teleport(Config.spawnLocation);

    // Unloading the world the player was in, if it was a PlayerWorld
    final World world = p.getWorld();
    final String worldName = world.getName();
    final String currentWorldUUID = WorldUtils.getWorldUUID(worldName);
    if (currentWorldUUID != null)
      WorldUtils.unloadWorld(currentWorldUUID);

    // CF For getting world name of player
    final CompletableFuture<String> cf = new CompletableFuture<>();
    Utils.getWorldUUID(cf, playerUUID);

    // Remove World from Cache Data when CF is complete
    cf.whenComplete((worldUUID, err) -> {
      if (worldUUID != null) {
        PlayerWorlds.getCache().saveAndRemoveWorldCacheData(worldUUID);
      }
    });
  }

}
