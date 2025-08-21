package net.sivils.playerWorlds.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class CommandUtils {

  public static Player getPlayer(CommandSender sender) {
    return sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;
  }
}
