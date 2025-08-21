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
    String worldUUID = WorldUtils.getWorldUUID(worldName);
    if (worldUUID != null)
      WorldUtils.unloadWorld(worldUUID);
  }

  @EventHandler
  public void playerChangeWorld(PlayerChangedWorldEvent e) {
    World worldFrom = e.getFrom();
    String worldFromName = worldFrom.getName();
    String worldFromUUID = WorldUtils.getWorldUUID(worldFromName);
    if (worldFromUUID != null)
      WorldUtils.unloadWorld(worldFromUUID);

    World worldTo = e.getPlayer().getWorld();
    String worldToName = worldTo.getName();
    String worldToUUID = WorldUtils.getWorldUUID(worldToName);
    if (worldToUUID != null)
      WorldUtils.loadWorld(worldToUUID);
  }

}
