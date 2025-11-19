package net.sivils.playerWorlds.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncPlayerProfileArgument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Cache;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.database.PlayerAccess;
import net.sivils.playerWorlds.database.WorldData;
import net.sivils.playerWorlds.utils.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AccessCommand {

  public void register() {

    Database db = PlayerWorlds.getInstance().getDatabase();

    MultiLiteralArgument accessTypeArg = new MultiLiteralArgument("Access Type", "can_join", "bypass_password", "break_blocks", "place_blocks", "pickup_items");
    BooleanArgument valueArg = new BooleanArgument("Value");
    AsyncPlayerProfileArgument targetArg = new AsyncPlayerProfileArgument("Player");

    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("access"), new LiteralArgument("set"))
      .withArguments(accessTypeArg, valueArg)
      .withOptionalArguments(targetArg)
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);
        final UUID playerUUID = player.getUniqueId();
        final String accessType = args.getByArgument(accessTypeArg);
        final boolean value = args.getByArgument(valueArg);

        Cache cache = PlayerWorlds.getCache();

        ArrayList<String> ownedWorlds = cache.getPlayersWorlds(playerUUID);
        if (ownedWorlds == null || ownedWorlds.isEmpty()) {
          player.sendMessage(Component.text("You don't own any worlds!", NamedTextColor.RED));
          return;
        }

        final String worldUUID = ownedWorlds.getFirst();

        // Getting target player
        final CompletableFuture<List<PlayerProfile>> profiles = args.getByArgumentOrDefault(targetArg, null);

        profiles.thenAccept(profileList -> {
          if (profileList == null || profileList.isEmpty()) { // Setting value for default perms
            WorldData worldData = cache.getCachedWorldData(worldUUID);
            PlayerAccess playerAccess = worldData.defaultPlayerAccess();

            // Setting new values in Player Access
            playerAccess = playerAccess.updateAccess(accessType, value);
            worldData = worldData.withDefaultPlayerAccess(playerAccess);

            cache.setCachedWorldData(worldUUID, worldData);
            player.sendMessage(Component.text("Successfully updated access for default permissions!",
              NamedTextColor.GREEN));
          } else { // Setting value for a specified player
            final UUID targetUUID = profileList.getFirst().getId();
            final String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            PlayerAccess playerAccess = cache.getCachedPlayerAccess(worldUUID, targetUUID);

            // Setting new values in Player Access
            playerAccess = playerAccess.updateAccess(accessType, value);

            cache.setCachedPlayerAccess(worldUUID, targetUUID, playerAccess);
            player.sendMessage(Component.text("Successfully updated access for " + targetName + "!",
              NamedTextColor.GREEN));
          }

        }).exceptionally(throwable -> {
          // We have to partly handle exceptions ourselves, since we are using a CompletableFuture
          Throwable cause = throwable.getCause();
          Throwable rootCause = cause instanceof RuntimeException ? cause.getCause() : cause;

          sender.sendMessage(rootCause.getMessage());
          return null;
        });
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();


    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("access"), new LiteralArgument("remove"))
      .withArguments(targetArg)
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);
        final UUID playerUUID = player.getUniqueId();

        final CompletableFuture<List<PlayerProfile>> profiles = args.getByArgumentOrDefault(targetArg, null);

        profiles.thenAccept(profileList -> {
          Cache cache = PlayerWorlds.getCache();
          final ArrayList<String> ownedWorlds = cache.getPlayersWorlds(playerUUID);
          if (ownedWorlds == null || ownedWorlds.isEmpty()) {
            player.sendMessage(Component.text("You don't own any worlds!", NamedTextColor.RED));
            return;
          }
          final String worldUUID = ownedWorlds.getFirst();
          final UUID targetUUID = profileList.getFirst().getId();

          cache.deletePlayerAccess(worldUUID, targetUUID);
          player.sendMessage(Component.text("Successfully removed player access for " + args.getRaw("Target") + "!",
            NamedTextColor.GREEN));

        }).exceptionally(throwable -> {
          // We have to partly handle exceptions ourselves, since we are using a CompletableFuture
          Throwable cause = throwable.getCause();
          Throwable rootCause = cause instanceof RuntimeException ? cause.getCause() : cause;

          sender.sendMessage(rootCause.getMessage());
          return null;
        });

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();


    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("access"), new LiteralArgument("add"))
      .withArguments(targetArg)
      .executes((sender, args) -> {
        final Player player = CommandUtils.getPlayer(sender);
        final UUID playerUUID = player.getUniqueId();

        final CompletableFuture<List<PlayerProfile>> profiles = args.getByArgumentOrDefault(targetArg, null);

        profiles.thenAccept(profileList -> {
          Cache cache = PlayerWorlds.getCache();
          final ArrayList<String> ownedWorlds = cache.getPlayersWorlds(playerUUID);
          if (ownedWorlds.isEmpty()) {
            player.sendMessage(Component.text("You don't own any worlds!", NamedTextColor.RED));
            return;
          }

          final UUID targetUUID = profileList.getFirst().getId();
          final String worldUUID = ownedWorlds.getFirst();
          final String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();

          if (cache.getCachedPlayerAccess(worldUUID, targetUUID) != null) {
            player.sendMessage(Component.text(targetName + " is already defined!", NamedTextColor.RED));
            return;
          }

          cache.setCachedPlayerAccess(worldUUID, targetUUID, new PlayerAccess(
            true,
            targetName,
            null,
            null,
            null,
            null,
            null));
        });

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();
  }

}
