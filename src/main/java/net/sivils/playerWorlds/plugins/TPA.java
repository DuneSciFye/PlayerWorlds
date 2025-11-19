package net.sivils.playerWorlds.plugins;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TPA extends Plugins {

  @Override
  public void setup() {

    HashMap<Player, Player> tpaRequests = new HashMap<>(); // Target, Sender
    HashMap<Player, BukkitTask> tpaTasks = new HashMap<>(); // Target, Task

    MultiLiteralArgument functionArg = new MultiLiteralArgument("Function", "accept", "deny", "reject");

    List<Argument<?>> playerArg = new ArrayList<>();
    playerArg.add(new EntitySelectorArgument.OnePlayer("Target")
      .replaceSuggestions(ArgumentSuggestions.strings(info ->
        getPlayersInWorld(info.sender())
      ))
    );


    new CommandAPICommand("tpa")
      .withArguments(functionArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;
        String worldUUID = WorldUtils.getWorldUUID(player.getWorld().getName());
        Database db = PlayerWorlds.getInstance().getDatabase();

        try {
          String plugins = db.getPlugins(worldUUID);
          if (!plugins.contains("TPA")) {
            player.sendMessage(Component.text("TPA is not enabled in this world!", NamedTextColor.RED));
            return;
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        Player target = tpaRequests.get(player);

        if (target == null) {
          player.sendMessage(Component.text("You don't have a TPA Request!", NamedTextColor.RED));
          return;
        }

        String targetWorldUUID = WorldUtils.getWorldUUID(target.getWorld().getName());

        String playerName = player.getName();
        String targetName = target.getName();

        String function = args.getUnchecked("Function");
        assert function != null;
        switch (function) {
          case "accept" -> {
            if (worldUUID == null || !worldUUID.equals(targetWorldUUID)) {
              player.sendMessage(Component.text(targetName + " is not in the same PlayerWorld!", NamedTextColor.RED));
              return;
            }
            player.teleport(target);
          }
          case "deny", "reject" -> {
            player.sendMessage(Component.text("Denied " + targetName + "'s TPA Request!", NamedTextColor.RED));
            target.sendMessage(Component.text(playerName + " denied your TPA Request!", NamedTextColor.RED));
          }
        }

        tpaRequests.remove(player);
        tpaTasks.remove(player);
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

    new CommandAPICommand("tpa")
      .withArguments(playerArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;
        Database db = PlayerWorlds.getInstance().getDatabase();
        String worldName = player.getWorld().getName();
        String worldUUID = WorldUtils.getWorldUUID(worldName);

        try {
          String plugins = db.getPlugins(worldUUID);
          if (!plugins.contains("TPA")) {
            player.sendMessage(Component.text("TPA is not enabled in this world!", NamedTextColor.RED));
            return;
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        Player target = args.getUnchecked("Target");
        assert target != null;
        String targetName = target.getName();
        String targetWorld = WorldUtils.getWorldUUID(target.getWorld().getName());
        String playerName = player.getName();

        if (worldUUID == null || !worldUUID.equals(targetWorld)) {
          player.sendMessage(Component.text(targetName + " is not in the same PlayerWorld!", NamedTextColor.RED));
        }

        if (tpaRequests.containsKey(target)) {
          player.sendMessage(Component.text(targetName + " already has a TPA Request!", NamedTextColor.RED));
          return;
        }

        player.sendMessage(Component.text("Sent a TPA Request to " + targetName + "!", NamedTextColor.GREEN));
        target.sendMessage(Component.text(playerName + " sent a TPA Request! [/tpa accept] [/tpa deny]", NamedTextColor.GREEN));

        tpaRequests.put(target, player);

        BukkitTask oldTask = tpaTasks.remove(target);
        if (oldTask != null) oldTask.cancel();

        BukkitTask tpaTask = new BukkitRunnable() {
          @Override
          public void run() {
            tpaRequests.remove(target);
            tpaTasks.remove(target);
          }
        }.runTaskLater(PlayerWorlds.getInstance(), 20L * 60L);
        tpaTasks.put(target, tpaTask);
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();
  }

  private String[] getPlayersInWorld(CommandSender sender) {
    if (sender instanceof Player p) {
      List<Player> players = WorldUtils.getPlayersInPlayerWorld(p.getWorld().getName());
      if (players != null) {
        players.remove(p);
        return players.stream().map(Player::getName).toArray(String[]::new);
      }
    }
    return new String[0];
  }

}
