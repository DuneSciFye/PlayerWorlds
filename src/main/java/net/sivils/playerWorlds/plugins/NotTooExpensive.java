package net.sivils.playerWorlds.plugins;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.sql.SQLException;
import java.util.HashMap;

@SuppressWarnings("UnstableApiUsage")
public class NotTooExpensive extends Plugins implements Listener {

  private final HashMap<AnvilView, Integer> repairCosts = new HashMap<>();

  @EventHandler
  public void onAnvil(PrepareAnvilEvent e) {
    AnvilView view = e.getView();
    System.out.println("a");
    Player p = (Player) view.getPlayer();
    String worldName = p.getWorld().getName();

    Database db = PlayerWorlds.getInstance().getDatabase();

    try {
      String plugins = db.getPlugins(worldName);
      if (plugins != null && plugins.contains("NotTooExpensive")) {
        System.out.println("b");
        int cost = view.getRepairCost();
        if (cost > 39) {
          System.out.println("c");
          System.out.println(e.getResult());
          Bukkit.getScheduler().runTask(PlayerWorlds.getInstance(), () -> {
            view.setRepairCost(39);
            p.updateInventory();
            repairCosts.put(view, cost);
          });
        }
      }
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @EventHandler
  public void onAnvilClick(InventoryClickEvent e) {
    System.out.println("1");
    if (!(e.getView() instanceof AnvilView view)) return;
    System.out.println("2");
    if (!repairCosts.containsKey(view)) return;
    System.out.println("3");
    if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
    System.out.println("4");

    ItemStack result = view.getItem(2);

    if (result != null) {
      System.out.println("5");
      int cost = repairCosts.get(view);
      Player player = (Player) e.getWhoClicked();

      if (player.getLevel() >= cost) {
        System.out.println("6");
        player.setLevel(player.getLevel() - cost);
        player.getInventory().addItem(result);
        view.setItem(0, null);
        view.setItem(1, null);
        view.setItem(2, null);
      } else {
        System.out.println("7");
        player.sendMessage(Component.text("You don't have enough levels for this! Costs " + cost + "!",
          NamedTextColor.RED));
      }
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onAnvilClose(InventoryCloseEvent e) {
    if (!(e.getView() instanceof AnvilView view)) return;
    repairCosts.remove(view);
  }


}
