package net.sivils.playerWorlds.plugins;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.InheritanceNode;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

public class GSit extends Plugins implements Listener {

  @EventHandler
  public void onChangeWorld(PlayerChangedWorldEvent e) {
    Player p = e.getPlayer();
    String worldName = p.getWorld().getName();
    String worldUUID = WorldUtils.getWorldUUID(worldName);

    if (worldUUID == null) return;

    Database db = PlayerWorlds.getInstance().getDatabase();

    try {
      String plugins = db.getPlugins(worldUUID);
      LuckPerms lp = PlayerWorlds.getLuckPerms();
      User user = lp.getPlayerAdapter(Player.class).getUser(p);
      InheritanceNode node = InheritanceNode.builder("group.gsit")
        .expiry(Duration.ofDays(1))
        .withContext("world", worldUUID)
        .withContext("world", worldUUID + "_nether")
        .withContext("world", worldUUID + "_the_end")
        .build();

      if (plugins != null && !plugins.contains("GSit")) user.data().add(node);
      else user.data().remove(node);

      lp.getUserManager().saveUser(user);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }

  }

  @Override
  public void commandEnable(String worldUUID) {
    LuckPerms lp = PlayerWorlds.getLuckPerms();
    InheritanceNode node = InheritanceNode.builder("group.gsit")
      .expiry(Duration.ofDays(1))
      .withContext("world", worldUUID)
      .withContext("world", worldUUID + "_nether")
      .withContext("world", worldUUID + "_the_end")
      .build();
    UserManager userManager = lp.getUserManager();

    List<Player> players = WorldUtils.getPlayersInPlayerWorld(worldUUID);
    if (players == null) return;

    for (Player p : players) {
      User user = lp.getPlayerAdapter(Player.class).getUser(p);
      user.data().remove(node);
      userManager.saveUser(user);
    }
  }

  @Override
  public void commandDisable(String worldUUID) {
    LuckPerms lp = PlayerWorlds.getLuckPerms();
    InheritanceNode node = InheritanceNode.builder("group.gsit")
      .expiry(Duration.ofDays(1))
      .withContext("world", worldUUID)
      .withContext("world", worldUUID + "_nether")
      .withContext("world", worldUUID + "_the_end")
      .build();
    UserManager userManager = lp.getUserManager();

    List<Player> players = WorldUtils.getPlayersInPlayerWorld(worldUUID);
    if (players == null) return;

    for (Player p : players) {
      User user = lp.getPlayerAdapter(Player.class).getUser(p);
      user.data().add(node);
      userManager.saveUser(user);
    }
  }

}
