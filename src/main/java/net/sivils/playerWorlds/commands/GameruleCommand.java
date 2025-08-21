package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.sivils.playerWorlds.gamerules.Gamerule;
import net.sivils.playerWorlds.utils.CommandUtils;
import org.bukkit.entity.Player;

public class GameruleCommand {

  LiteralArgument gameruleArg = new LiteralArgument("gamerule");
  MultiLiteralArgument gameruleNameArg = new MultiLiteralArgument("Gamerule Name",
    Gamerule.getGameruleNames().toArray(new String[0]));
  MultiLiteralArgument functionArg = new MultiLiteralArgument("Function", "enable", "disable");

  public void register() {
    new CommandAPICommand("playerworlds")
      .withArguments(gameruleArg, gameruleNameArg, functionArg)
      .executes((commandSender, args) -> {
        Player player = CommandUtils.getPlayer(commandSender);


      }, ExecutorType.PROXY, ExecutorType.PLAYER)
      .register();
  }

}
