package net.sivils.playerWorlds.plugins;

import dev.geco.gsit.api.GSitAPI;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;

public class DimensionStacking extends Plugins implements Listener {

  private static final Map<Player, Player> players = new HashMap<>(); //Bottom player, top player

  public void voidHandler(PlayerWorlds plugin){
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (!players.containsValue(p)) {
          if (p.getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (p.getY() > 400) {
              if (players.containsKey(p)) { //If player has player on head
                Player topPlayer = players.get(p);

                Location location = new Location(Bukkit.getWorld("world_the_end"), p.getX(), -10, p.getZ());
                //Obtain a list of players in order from bottom to top
                List<Player> playerStack = new ArrayList<>(Arrays.asList(p, topPlayer));
                while (players.containsKey(topPlayer)) {
                  topPlayer = players.get(topPlayer);
                  playerStack.add(topPlayer);
                }
                //Teleport players in stack from top to bottom
                for (int i = playerStack.size() - 1; i >= 0; i--) {
                  Player player = playerStack.get(i);
                  location.setYaw(player.getYaw());
                  location.setPitch(player.getPitch());
                  player.teleport(location);
                }
                //Stack players bottom to top
                for (int i = 1; i < playerStack.size(); i++) {
                  GSitAPI.sitOnPlayer(playerStack.get(i), playerStack.get(i - 1));
                }

              } else {
                Vector vec = p.getVelocity();
                Location location = new Location(Bukkit.getWorld("world_the_end"), p.getX(), -10, p.getZ());
                location.setYaw(p.getYaw());
                location.setPitch(p.getPitch());
                p.setFallDistance(0);
                p.teleport(location);
                Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> p.setVelocity(vec.multiply(3)));
              }
            } else if (p.getY() < -80) {
              if (players.containsKey(p)) { //If player has player on head
                Player topPlayer = players.get(p);

                Location location = new Location(Bukkit.getWorld("world_nether"), p.getX() / 8, 320, p.getZ() / 8);
                //Obtain a list of players in order from bottom to top
                List<Player> playerStack = new ArrayList<>(Arrays.asList(p, topPlayer));
                while (players.containsKey(topPlayer)) {
                  topPlayer = players.get(topPlayer);
                  playerStack.add(topPlayer);
                }
                //Teleport players in stack from top to bottom
                for (int i = playerStack.size() - 1; i >= 0; i--) {
                  Player player = playerStack.get(i);
                  location.setYaw(player.getYaw());
                  location.setPitch(player.getPitch());
                  player.teleport(location);
                }
                //Stack players bottom to top
                for (int i = 1; i < playerStack.size(); i++) {
                  GSitAPI.sitOnPlayer(playerStack.get(i), playerStack.get(i - 1));
                }

              } else {
                Vector vec = p.getVelocity();
                Location location = new Location(Bukkit.getWorld("world_nether"), p.getX() / 8, 320, p.getZ() / 8);
                location.setYaw(p.getYaw());
                location.setPitch(p.getPitch());
                p.setFallDistance(0);
                p.teleport(location);
                Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> p.setVelocity(vec.multiply(3)));
              }
            }
          } else if (p.getWorld().getEnvironment() == World.Environment.NETHER) {
            if (p.getY() > 320) {
              if (players.containsKey(p)) { //If player has player on head
                Player topPlayer = players.get(p);

                Location location = new Location(Bukkit.getWorld("world"), p.getX() * 8, -60, p.getZ() * 8);
                //Obtain a list of players in order from bottom to top
                List<Player> playerStack = new ArrayList<>(Arrays.asList(p, topPlayer));
                while (players.containsKey(topPlayer)) {
                  topPlayer = players.get(topPlayer);
                  playerStack.add(topPlayer);
                }
                //Teleport players in stack from top to bottom
                for (int i = playerStack.size() - 1; i >= 0; i--) {
                  Player player = playerStack.get(i);
                  location.setYaw(player.getYaw());
                  location.setPitch(player.getPitch());
                  player.teleport(location);
                }
                //Stack players bottom to top
                for (int i = 1; i < playerStack.size(); i++) {
                  GSitAPI.sitOnPlayer(playerStack.get(i), playerStack.get(i - 1));
                }

              } else {
                Vector vec = p.getVelocity();
                Location location = new Location(Bukkit.getWorld("world"), p.getX() * 8, -60, p.getZ() * 8);
                location.setYaw(p.getYaw());
                location.setPitch(p.getPitch());
                p.setFallDistance(0);
                p.teleport(location);
                Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> p.setVelocity(vec.multiply(3)));
              }
            } else if (p.getY() < -20) {
              if (players.containsKey(p)) { //If player has player on head
                Player topPlayer = players.get(p);

                Location location = new Location(Bukkit.getWorld("world_the_end"), p.getX() / 8, 320, p.getZ() / 8);
                //Obtain a list of players in order from bottom to top
                List<Player> playerStack = new ArrayList<>(Arrays.asList(p, topPlayer));
                while (players.containsKey(topPlayer)) {
                  topPlayer = players.get(topPlayer);
                  playerStack.add(topPlayer);
                }
                //Teleport players in stack from top to bottom
                for (int i = playerStack.size() - 1; i >= 0; i--) {
                  Player player = playerStack.get(i);
                  location.setYaw(player.getYaw());
                  location.setPitch(player.getPitch());
                  player.teleport(location);
                }
                //Stack players bottom to top
                for (int i = 1; i < playerStack.size(); i++) {
                  GSitAPI.sitOnPlayer(playerStack.get(i), playerStack.get(i - 1));
                }

              } else {
                Vector vec = p.getVelocity();
                Location location = new Location(Bukkit.getWorld("world_the_end"), p.getX() / 8, 320, p.getZ() / 8);
                location.setYaw(p.getYaw());
                location.setPitch(p.getPitch());
                p.setFallDistance(0);
                p.teleport(location);
                Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> p.setVelocity(vec.multiply(3)));
              }
            }
          } else if (p.getWorld().getEnvironment() == World.Environment.THE_END) {
            if (p.getY() > 320) {
              if (players.containsKey(p)) { //If player has player on head
                Player topPlayer = players.get(p);

                Location location = new Location(Bukkit.getWorld("world_nether"), p.getX() / 8, -10, p.getZ() / 8);
                //Obtain a list of players in order from bottom to top
                List<Player> playerStack = new ArrayList<>(Arrays.asList(p, topPlayer));
                while (players.containsKey(topPlayer)) {
                  topPlayer = players.get(topPlayer);
                  playerStack.add(topPlayer);
                }
                //Teleport players in stack from top to bottom
                for (int i = playerStack.size() - 1; i >= 0; i--) {
                  Player player = playerStack.get(i);
                  location.setYaw(player.getYaw());
                  location.setPitch(player.getPitch());
                  player.teleport(location);
                }
                //Stack players bottom to top
                for (int i = 1; i < playerStack.size(); i++) {
                  GSitAPI.sitOnPlayer(playerStack.get(i), playerStack.get(i - 1));
                }

              } else {
                Vector vec = p.getVelocity();
                Location location = new Location(Bukkit.getWorld("world_nether"), p.getX() / 8, -10, p.getZ() / 8);
                location.setYaw(p.getYaw());
                location.setPitch(p.getPitch());
                p.setFallDistance(0);
                p.teleport(location);
                Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> p.setVelocity(vec.multiply(3)));
              }
            } else if (p.getY() < -30) {
              if (players.containsKey(p)) { //If player has player on head
                Player topPlayer = players.get(p);

                Location location = new Location(Bukkit.getWorld("world"), p.getX(), 399, p.getZ());
                //Obtain a list of players in order from bottom to top
                List<Player> playerStack = new ArrayList<>(Arrays.asList(p, topPlayer));
                while (players.containsKey(topPlayer)) {
                  topPlayer = players.get(topPlayer);
                  playerStack.add(topPlayer);
                }
                //Teleport players in stack from top to bottom
                for (int i = playerStack.size() - 1; i >= 0; i--) {
                  Player player = playerStack.get(i);
                  location.setYaw(player.getYaw());
                  location.setPitch(player.getPitch());
                  player.teleport(location);
                }
                //Stack players bottom to top
                for (int i = 1; i < playerStack.size(); i++) {
                  GSitAPI.sitOnPlayer(playerStack.get(i), playerStack.get(i - 1));
                }

              } else {
                Vector vec = p.getVelocity();
                Location location = new Location(Bukkit.getWorld("world"), p.getX(), 399, p.getZ(), p.getYaw(), p.getPitch());
                p.setFallDistance(0);
                p.teleport(location);
                Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> p.setVelocity(vec.multiply(3)));
              }
            }
          }
        }
      }
    }, 0L, 1L);
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player)) return;
    String worldName = e.getEntity().getWorld().getName();
    String worldUUID = WorldUtils.getWorldUUID(worldName);

    if (worldUUID == null) return; // Not in a PlayerWorld

    // Check if plugin is enabled
    Database db = PlayerWorlds.getInstance().getDatabase();
    try {
      String plugins = db.getPlugins(worldUUID);
      if (plugins == null || !plugins.contains("DimensionStacking")) return;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }

    if (e.getCause() != EntityDamageEvent.DamageCause.VOID) return;

    Entity entity = e.getEntity();
    World world = entity.getWorld();
    Location location = new Location(Bukkit.getWorld(worldUUID), entity.getX(), 399, entity.getZ());
    e.setCancelled(true);

    if (world.getEnvironment() == World.Environment.NORMAL) {
      location = new Location(Bukkit.getWorld(worldUUID + "_nether"), entity.getX() / 8, 320, entity.getZ() / 8);
    } else if (world.getEnvironment() == World.Environment.NETHER) {
      location = new Location(Bukkit.getWorld(worldUUID + "_the_end"), entity.getX() * 8, 320, entity.getZ() * 8);
    }

    location.setYaw(entity.getYaw());
    location.setPitch(entity.getPitch());

    entity.teleport(location);
  }


}
