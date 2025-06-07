package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class AccessCommand {

  public void register() {

    Database db = PlayerWorlds.getInstance().getDatabase();

    MultiLiteralArgument accessTypeArg = new MultiLiteralArgument("Access Type", "can_join", "bypass_password", "break_blocks", "place_blocks", "pickup_items");
    BooleanArgument valueArg = new BooleanArgument("Value");
    OfflinePlayerArgument targetArg = new OfflinePlayerArgument("Target");

    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("access"), new LiteralArgument("set"))
      .withArguments(accessTypeArg, valueArg)
      .withOptionalArguments(targetArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        try {
          String playerUUID = player.getUniqueId().toString();
          String worldUUID = db.getWorld(playerUUID);
          String accessType = args.getByArgument(accessTypeArg);
          boolean value = args.getByArgument(valueArg);
          OfflinePlayer target = args.getByArgument(targetArg);

          if (target == null) { // Setting value for default perms
            db.setPlayerAccess(worldUUID, worldUUID, "", accessType, value);
            player.sendMessage(Component.text("Successfully updated access for default permissions!",
              NamedTextColor.GREEN));
          } else { // Setting value for a specified player
            String targetName = args.getRaw("Target");
            db.setPlayerAccess(worldUUID, target.getUniqueId().toString(), targetName, accessType, value);
            player.sendMessage(Component.text("Successfully updated access for " + targetName + "!",
              NamedTextColor.GREEN));
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();


    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("access"), new LiteralArgument("remove"))
      .withArguments(targetArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        try {
          String playerUUID = player.getUniqueId().toString();
          String worldUUID = db.getWorld(playerUUID);
          String targetUUID = args.getByArgument(targetArg).getUniqueId().toString();

          db.removePlayerAccess(worldUUID, targetUUID);
          player.sendMessage(Component.text("Successfully removed player access for " + args.getRaw("Target") + "!",
            NamedTextColor.GREEN));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();


    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("access"), new LiteralArgument("add"))
      .withArguments(targetArg)
      .executes((sender, args) -> {
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        try {
          String playerUUID = player.getUniqueId().toString();
          String worldUUID = db.getWorld(playerUUID);
          String targetUUID = args.getByArgument(targetArg).getUniqueId().toString();
          String targetName = args.getRaw("Target");

          if (db.playerAccessExists(worldUUID, targetUUID)) {
            player.sendMessage(Component.text(targetName + " is already defined!", NamedTextColor.RED));
            return;
          }

          db.createDefaultPlayerAccess(worldUUID, targetUUID, targetName);
          player.sendMessage(Component.text("Successfully added player access for " + targetName + "!",
            NamedTextColor.GREEN));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();
  }

}
