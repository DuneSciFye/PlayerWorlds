package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.reasons.LoadFailureReason;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PlayerWorldsCommand {

  public void register(PlayerWorlds plugin) {


        /*
            Command for teleporting to player's world
         */
    MultiLiteralArgument teleportArgs = new MultiLiteralArgument("Function", "goto", "go", "tp", "teleport");
    OfflinePlayerArgument playerArg = new OfflinePlayerArgument("Player");
    GreedyStringArgument passwordArg = new GreedyStringArgument("Password");

    new CommandAPICommand("playerworlds")
      .withArguments(teleportArgs)
      .withOptionalArguments(playerArg, passwordArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        try {
          OfflinePlayer offlineTarget = args.getByArgumentOrDefault(playerArg, null);
          String playerUUID = player.getUniqueId().toString();
          Database db = plugin.getDatabase();
          String worldUUID;
          String targetName = player.getName();

          // Go to player's own world
          if (offlineTarget == null) {
            if (!db.ownsWorld(playerUUID)) {
              player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
              return;
            }

            worldUUID = db.getWorld(playerUUID);
          }
          // Teleporting to someone else's world
          else {
            String targetUUID = offlineTarget.getUniqueId().toString();
            targetName = offlineTarget.getName();

            if (!db.ownsWorld(targetUUID)) {
              player.sendMessage(Component.text(offlineTarget.getName() + " doesn't own a world!", NamedTextColor.RED));
              return;
            }

            worldUUID = db.getWorld(targetUUID);

            // Whitelist logic
            if (!db.getPlayerAccess(worldUUID, playerUUID, "can_join")) {
              player.sendMessage(Component.text("You cannot join " + offlineTarget.getName() + "'s world!", NamedTextColor.RED));
              return;
            }

            // Password Logic
            if (db.getBooleanField(worldUUID, "password_enabled")) {
              String password = args.getByArgument(passwordArg);
              if (password == null) {
                player.sendMessage(Component.text("This world requires a password!", NamedTextColor.RED));
                return;
              } else if (!db.getWorldField(worldUUID, "password").equals(password)) {
                player.sendMessage(Component.text("Incorrect password!", NamedTextColor.RED));
                return;
              }
            }
          }


          WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
          MultiverseWorld world = worldManager.getWorld(worldUUID).get();
          if (!world.isLoaded()) {
            Attempt<LoadedMultiverseWorld, LoadFailureReason> loadedWorld = worldManager.loadWorld(worldUUID);

            if (loadedWorld.isFailure()) {
              player.sendMessage(Component.text("Failed to load world " + loadedWorld.get().getName() + ". Please report to an administrator.", NamedTextColor.RED));
              player.sendMessage(Component.text(loadedWorld.getFailureMessage().toString(), NamedTextColor.RED));
              return;
            }
          }

          WorldUtils.loadWorld(worldUUID);

          player.teleport(world.getSpawnLocation());

          // Set Last Join Worlds and Last Join Times
          String stringWorlds = db.getPlayerInfoField(playerUUID, "last_join_worlds");
          ArrayList<String> worlds = stringWorlds == null ? new ArrayList<>() : new ArrayList<>(List.of(stringWorlds.split(",")));
          worlds.addFirst(targetName);
          if (worlds.size() > 5) worlds.removeLast();
          db.setPlayerInfoField(playerUUID, "last_join_worlds", String.join(",", worlds));

          String stringTimes = db.getPlayerInfoField(playerUUID, "last_join_times");
          ArrayList<String> times = stringTimes == null ? new ArrayList<>() : new ArrayList<>(List.of(stringTimes.split(",")));
          times.addFirst(Timestamp.from(Instant.now()).toString());
          if (times.size() > 5) worlds.removeLast();
          db.setPlayerInfoField(playerUUID, "last_join_times", String.join(",", times));

        } catch (SQLException e) {
          player.sendMessage(Component.text("An error occurred while running this command. Please report to an administrator.", NamedTextColor.RED));
          throw new RuntimeException(e);
        }
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();


        /*
            Command for deleting one's world
         */
    LiteralArgument deleteArg = new LiteralArgument("delete");

    new CommandAPICommand("playerworlds")
      .withArguments(deleteArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        player.sendMessage(Component.text("Attempting world deletion now...", NamedTextColor.GREEN));
        String playerUUID = player.getUniqueId().toString();

        try {
          if (!plugin.getDatabase().ownsWorld(playerUUID)) {
            player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
            return;
          }

          final String worldUUID = plugin.getDatabase().getWorld(playerUUID);

          WorldUtils.deleteWorld(worldUUID);
          WorldUtils.activeWorldPlugins.remove(worldUUID);

          player.sendMessage(Component.text("Deleted your world!", NamedTextColor.GREEN));
        } catch (SQLException e) {
          player.sendMessage(Component.text("There was a database error when deleting your world! Please report to an administrator.", NamedTextColor.RED));
          throw new RuntimeException(e);
        }
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("getcreationtime"))
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        try {
          Database db = plugin.getDatabase();
          String worldUUID = db.getWorld(player.getUniqueId().toString());

          Timestamp timestamp = db.getTimestampField(worldUUID, "creation_time");

          player.sendMessage(Component.text(timestamp.toString()));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("getallworlds"))
      .executesPlayer((player, args) -> {
        try {
          List<String> worlds = plugin.getDatabase().getAllWorlds();
          String result = String.join(", ", worlds);

          player.sendMessage(Component.text(result));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      })
      .register();
  }

}
