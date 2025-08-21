package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.plugins.Plugins;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class PluginsCommand {

  public void register(PlayerWorlds plugin) {

    LiteralArgument pluginsArg = new LiteralArgument("plugins");
    MultiLiteralArgument pluginNameArg = new MultiLiteralArgument("Plugin Name", Plugins.getPluginNames().toArray(new String[0]));
    MultiLiteralArgument functionArg = new MultiLiteralArgument("Function", "enable", "disable");

    new CommandAPICommand("playerworlds")
      .withArguments(pluginsArg, pluginNameArg, functionArg)
      .executes((sender, args) -> {
        Database db = plugin.getDatabase();
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        try {
          String playerUUID = player.getUniqueId().toString();
          String worldUUID = db.getWorld(playerUUID);

          String pluginName = args.getByArgument(pluginNameArg);
          String function =  args.getByArgument(functionArg);

          switch (function) {
            case "enable" -> {
              db.enablePlugin(worldUUID, pluginName);
              Plugins.getPlugin(pluginName).commandEnable(worldUUID);
              player.sendMessage(Component.text("Successfully enabled plugin " + pluginName, NamedTextColor.GREEN));
            }
            case "disable" -> {
              db.disablePlugin(worldUUID, pluginName);
              Plugins.getPlugin(pluginName).commandDisable(worldUUID);
              player.sendMessage(Component.text("Successfully disabled plugin " + pluginName, NamedTextColor.GREEN));
            }
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();
  }

}
