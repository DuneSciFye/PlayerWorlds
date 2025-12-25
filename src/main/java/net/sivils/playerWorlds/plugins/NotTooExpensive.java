package net.sivils.playerWorlds.plugins;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.view.AnvilView;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class NotTooExpensive extends Plugins implements Listener {

  @EventHandler
  public void onPrepareAnvil(PrepareAnvilEvent e) {
    Player p = (Player) e.getView().getPlayer();
    String worldName = p.getWorld().getName();
    String worldUUID = WorldUtils.getWorldUUID(worldName);

    Database db = PlayerWorlds.getInstance().getDatabase();

    try {
      String plugins = db.getPlugins(worldUUID);
      if (plugins != null && plugins.contains("NotTooExpensive")) {
        LuckPerms lp = PlayerWorlds.getLuckPerms();
        User user = lp.getPlayerAdapter(Player.class).getUser(p);
        PermissionNode node =
          PermissionNode.builder().permission("ca.affected").expiry(Duration.ofDays(1)).withContext("world", worldName).build();
        user.data().add(node);
        lp.getUserManager().saveUser(user);
      }
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @EventHandler
  public void onAnvilClose(InventoryCloseEvent e) {
    // Using AnvilInventory instead of AnvilView to support 1.20.6
    if (!(e.getView() instanceof AnvilInventory inventory)) return;
    Player p = (Player) inventory.getViewers().getFirst();
    String worldName = p.getWorld().getName();
    String worldUUID = WorldUtils.getWorldUUID(worldName);

    Database db = PlayerWorlds.getInstance().getDatabase();
    try {
      String plugins = db.getPlugins(worldUUID);
      if (plugins != null && plugins.contains("NotTooExpensive")) {
        LuckPerms lp = PlayerWorlds.getLuckPerms();
        User user = lp.getPlayerAdapter(Player.class).getUser(p);
        PermissionNode node =
          PermissionNode.builder().permission("ca.affected").expiry(Duration.ofDays(1)).withContext("world", worldName).build();
        user.data().remove(node);
        lp.getUserManager().saveUser(user);
      }
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }


}
