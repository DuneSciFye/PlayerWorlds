package net.sivils.playerWorlds.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.*;
import net.sivils.playerWorlds.utils.CommandUtils;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerWorldsCommand {

  public void register(PlayerWorlds plugin) {

    /*
      Command for teleporting to player's world
    */
    MultiLiteralArgument teleportArgs = new MultiLiteralArgument("Function", "goto", "go", "tp", "teleport");
    AsyncPlayerProfileArgument playerArg = new AsyncPlayerProfileArgument("Player");
    GreedyStringArgument passwordArg = new GreedyStringArgument("Password");

    // Teleporting to own world
    new CommandAPICommand("playerworlds")
      .withArguments(teleportArgs)
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);
        final UUID playerUUID = player.getUniqueId();

        final Cache cache = PlayerWorlds.getCache();
        final String worldUUID = cache.getPlayersWorlds(playerUUID).getFirst();

        // Player doesn't own a world
        if (worldUUID == null) {
          player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
          return;
        }

        // Attempt to join world
        WorldUtils.goToWorld(worldUUID, player, cache);

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

    // Teleporting to another player's world
    new CommandAPICommand("playerworlds")
      .withArguments(teleportArgs, playerArg)
      .withOptionalArguments(passwordArg)
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);
        final UUID playerUUID = player.getUniqueId();
        final CompletableFuture<List<PlayerProfile>> profiles = args.getByArgumentOrDefault(playerArg, null);

        final Cache cache = PlayerWorlds.getCache();

        profiles.thenAccept(profileList -> {
          final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(profileList.getFirst().getId());
          final UUID targetUUID = offlineTarget.getUniqueId();

          final String targetName = offlineTarget.getName();

          // CF to get the target player's world
          CompletableFuture<WorldData> cf = new CompletableFuture<>();
          Database db = PlayerWorlds.getInstance().getDatabase();
          Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
            try {
              String worldUUID = db.getWorld(targetUUID.toString());
              WorldData worldData = cache.getCachedWorldData(worldUUID);
              cf.complete(worldData);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          });

          cf.whenComplete((worldData, err) -> {
            final String worldUUID = worldData.worldUUID();
            PlayerAccess playerAccess = cache.getCachedPlayerAccess(worldUUID, playerUUID);

            // Whitelist logic
            if (!worldData.canJoinWorld(playerAccess)) {
              player.sendMessage(Component.text("You cannot join " + offlineTarget.getName() + "'s world!", NamedTextColor.RED));
              return;
            }

            // Password Logic
            if (worldData.passwordEnabled()) {
              final String password = args.getByArgument(passwordArg);
              if (password == null || password.isEmpty()) {
                player.sendMessage(Component.text("This world requires a password!", NamedTextColor.RED));
                return;
              } else if (!worldData.password().equals(password)) {
                player.sendMessage(Component.text("Incorrect password!", NamedTextColor.RED));
                return;
              }
            }

            // Attempt to join world
            WorldUtils.goToWorld(worldUUID, player, cache);

          });



      }).exceptionally(throwable -> {
          // We have to partly handle exceptions ourselves, since we are using a CompletableFuture
          Throwable cause = throwable.getCause();
          Throwable rootCause = cause instanceof RuntimeException ? cause.getCause() : cause;

          sender.sendMessage(rootCause.getMessage());
          return null;
        });

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();


        /*
            Command for deleting one's world
         */
    LiteralArgument deleteArg = new LiteralArgument("delete");
    new CommandAPICommand("playerworlds")
      .withArguments(deleteArg)
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);

        final Cache cache = PlayerWorlds.getCache();
        final UUID playerUUID = player.getUniqueId();

        player.sendMessage(Component.text("Attempting world deletion now...", NamedTextColor.GREEN));

        ArrayList<String> ownedWorlds = cache.getPlayersWorlds(playerUUID);

        if (ownedWorlds == null || ownedWorlds.isEmpty()) {
          player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
          return;
        }

        final String worldUUID = ownedWorlds.getFirst();

        Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
          try {
            WorldUtils.deleteWorld(worldUUID);
          } catch (SQLException e) {
            player.sendMessage(Component.text("There was a Database error when deleting your world! Please report to an adinistrator!", NamedTextColor.RED));
            throw new RuntimeException(e);
          }
        });
        player.sendMessage(Component.text("Deleted your world!", NamedTextColor.GREEN));
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("getcreationtime"))
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);
        final UUID playerUUID = player.getUniqueId();

        Cache cache = PlayerWorlds.getCache();
        ArrayList<String> ownedWorlds = cache.getPlayersWorlds(playerUUID);

        if (ownedWorlds == null || ownedWorlds.isEmpty()) {
          player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
          return;
        }

        WorldData worldData = cache.getCachedWorldData(ownedWorlds.getFirst());
        player.sendMessage(worldData.creationTime().toString());
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("getallworlds"))
      .executesPlayer((player, args) -> {
        CompletableFuture<List<String>> cf = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(PlayerWorlds.getInstance(), () -> {
          try {
            List<String> worlds = plugin.getDatabase().getAllWorlds();
            cf.complete(worlds);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });

        cf.whenComplete((worlds, err) -> {
          String result = String.join(", ", worlds);

          player.sendMessage(Component.text(result));
        });
      })
      .register();
  }

}
