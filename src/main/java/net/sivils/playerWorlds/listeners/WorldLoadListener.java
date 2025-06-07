package net.sivils.playerWorlds.listeners;

import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WorldLoadListener implements Listener {

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();
    p.teleport(Config.spawnLocation);

    World world = p.getWorld();
    String worldName = world.getName();
    if (worldName.length() >= 36) {
      String worldUUID = world.getName().substring(0, 37);
      WorldUtils.unloadWorld(worldUUID);
    }
  }

  @EventHandler
  public void playerChangeWorld(PlayerChangedWorldEvent e) {
    World worldFrom = e.getFrom();
    String worldFromName = worldFrom.getName();
    if (worldFromName.length() >= 36) {
      String worldFromUUID = worldFromName.substring(0, 36);
      WorldUtils.unloadWorld(worldFromUUID);
    }

    World worldTo = e.getPlayer().getWorld();
    String worldToName = worldTo.getName();
    if (worldToName.length() >= 36) {
      String worldToUUID = worldToName.substring(0, 36);
      WorldUtils.loadWorld(worldToUUID);
    }
  }

}
