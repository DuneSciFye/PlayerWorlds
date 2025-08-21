package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.sivils.playerWorlds.utils.CommandUtils.getPlayer;

public class TransferCommand {

  public final static HashMap<UUID, UUID> OUTGOING_REQUESTS = new HashMap<>(); // Sender, Target
  public final static HashMap<UUID, UUID> INCOMING_REQUESTS = new HashMap<>(); // Target, Sender
  public final static HashMap<UUID, BukkitTask> TRANSFER_TASKS = new HashMap<>(); // Target, Task

  public void register() {

    LiteralArgument transferArg = new LiteralArgument("transfer");
    PlayerArgument targetArg = new PlayerArgument("Target");
    MultiLiteralArgument functionArg = new MultiLiteralArgument("Function", "accept", "deny", "cancel");

    new CommandAPICommand("playerworlds")
      .withArguments(transferArg, targetArg)
      .executes((commandSender, args) -> {
        Player sender = getPlayer(commandSender);
        Player target = args.getByArgument(targetArg);

        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        UUID playerUUID = sender.getUniqueId();
        String playerName = sender.getName();

        if (targetUUID.equals(playerUUID)) {
          sender.sendMessage(Component.text("You can't transfer to yourself!", NamedTextColor.RED));
          return;
        }

        if (INCOMING_REQUESTS.containsKey(targetUUID)) {
          sender.sendMessage(Component.text(targetName + " already has a transfer request!.", NamedTextColor.RED));
          return;
        }

        // Checking for error cases
        CompletableFuture<Component> cf = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
          Database db = PlayerWorlds.getInstance().getDatabase();

          try {
            if (!db.ownsWorld(playerUUID.toString()))
              cf.complete(Component.text("You don't own a world!", NamedTextColor.RED));
            else if (db.ownsWorld(targetUUID.toString()))
              cf.complete(Component.text(targetName + " already owns a world!", NamedTextColor.RED));
            cf.complete(null);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });

        cf.whenComplete((component, err) -> {
          if (component != null) {
            sender.sendMessage(component);
            return;
          }

          target.sendMessage(Component.text(playerName + " wants to transfer ownership of their world to you! [/pw]",
            NamedTextColor.GREEN));
          sender.sendMessage(Component.text("Sent a transfer request to " + targetName + ".", NamedTextColor.GREEN));

          // Remove old task if there is one
          if (OUTGOING_REQUESTS.containsValue(playerUUID)) {
            cleanup(playerUUID);
          }

          OUTGOING_REQUESTS.put(playerUUID, targetUUID);
          INCOMING_REQUESTS.put(targetUUID, playerUUID);

          BukkitTask task = Bukkit.getScheduler().runTaskLater(PlayerWorlds.getInstance(), () -> {
            cleanup(playerUUID);
            if (sender.isOnline())
              sender.sendMessage(Component.text("Your transfer request to " + targetName + " has expired", NamedTextColor.RED));
          }, 20L * 60L);

          TRANSFER_TASKS.put(targetUUID, task);

        });
      }, ExecutorType.PROXY, ExecutorType.PLAYER)
      .register();

    new CommandAPICommand("playerworlds")
      .withArguments(transferArg, functionArg)
      .executes((commandSender, args) -> {

        final Player player = CommandUtils.getPlayer(commandSender);
        UUID playerUUID = player.getUniqueId();
        final String playerStringUUID = playerUUID.toString();
        final String playerName = player.getName();

        final UUID senderUUID = INCOMING_REQUESTS.get(playerUUID);

        switch (args.getByArgument(functionArg)) {
          case "accept" -> {
            if (senderUUID == null) {
              player.sendMessage(Component.text("You don't have any incoming transfer requests!", NamedTextColor.RED));
              return;
            }

            final Player sender = Bukkit.getPlayer(senderUUID);
            final String senderName = sender.getName();

            CompletableFuture<String> cf = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
              Database db = PlayerWorlds.getInstance().getDatabase();

              try {
                if (!db.ownsWorld(senderUUID.toString())) {
                  cf.complete("SENDER_DOES_NOT_OWN_WORLD");
                } else if (db.ownsWorld(playerStringUUID)) {
                  cf.complete("SENDER_OWNS_WORLD");
                } else {
                  cf.complete("NO_ERRORS");
                }
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            });

            cf.whenComplete((string, err) -> {
              switch (string) {
                case "SENDER_DOES_NOT_OWN_WORLD" -> {
                  player.sendMessage(Component.text(senderName + " does not own a world anymore!", NamedTextColor.RED));
                  sender.sendMessage(Component.text(playerName + " accepted your request but you don't own a world anymore!", NamedTextColor.RED));
                  return;
                }
                case "SENDER_OWNS_WORLD" -> {
                  player.sendMessage(Component.text("You already own a world!", NamedTextColor.RED));
                  sender.sendMessage(Component.text(playerName + " accepted your request but they already own a world!", NamedTextColor.RED));
                  return;
                }
              }

              player.sendMessage(Component.text("You accepted " + senderName + "'s transfer request!", NamedTextColor.GREEN));
              sender.sendMessage(Component.text(playerName + " accepted your transfer request!", NamedTextColor.GREEN));
              Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
                try {
                  Database db = PlayerWorlds.getInstance().getDatabase();
                  String worldUUID = db.getWorld(senderUUID.toString());
                  db.setWorldField(worldUUID, "owner_uuid", playerStringUUID);
                } catch (SQLException e) {
                  sender.sendMessage(Component.text("There was a fatal database error while trying to save your " +
                    "transfer request! Please report to an administrator!", NamedTextColor.RED));
                  throw new RuntimeException(e);
                }
              });
            });
          }
          case "deny" -> {
            if (senderUUID == null) {
              player.sendMessage(Component.text("You don't have any incoming transfer requests!", NamedTextColor.RED));
              return;
            }

            Player sender = Bukkit.getPlayer(senderUUID);

            String senderName = sender.getName();
            sender.sendMessage(Component.text(playerName + " denied your transfer request!", NamedTextColor.RED));
            player.sendMessage(Component.text("You denied " + senderName + "'s transfer request!", NamedTextColor.GREEN));
          }
          case "cancel" -> {
            if (!OUTGOING_REQUESTS.containsKey(playerUUID)) {
              player.sendMessage(Component.text("You don't have any outgoing transfer requests!", NamedTextColor.RED));
              return;
            }

            player.sendMessage(Component.text("You cancelled your transfer request!", NamedTextColor.GREEN));

            cleanup(playerUUID);
            return;
          }
        }

        cleanup(senderUUID);
      }, ExecutorType.PROXY, ExecutorType.PLAYER)
      .register();
  }

  private void cleanup(UUID playerUUID) {
    UUID target = OUTGOING_REQUESTS.remove(playerUUID);
    INCOMING_REQUESTS.remove(target);
    BukkitTask oldTask = TRANSFER_TASKS.remove(target);
    if (oldTask != null) oldTask.cancel();
  }

}
