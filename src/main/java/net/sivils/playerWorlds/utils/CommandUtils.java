package net.sivils.playerWorlds.utils;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class CommandUtils {

  public static Player getPlayer(CommandSender sender) {
    return sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;
  }

  public static Argument<String> playerNameArg(String nodeName) {
    return new CustomArgument<>(new StringArgument(nodeName), CustomArgument.CustomArgumentInfo::input
    ).replaceSuggestions(ArgumentSuggestions.strings(
      // List of Online player names
      Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new)));
  }

}
