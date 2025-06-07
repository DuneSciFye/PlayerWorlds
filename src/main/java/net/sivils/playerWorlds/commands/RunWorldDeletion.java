package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.sivils.playerWorlds.utils.WorldUtils;

import java.sql.SQLException;

public class RunWorldDeletion {

  public void register() {
    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("runworlddeletion"))
      .executes((sender, args) -> {
        sender.sendMessage("Running world deletion...");
        try {
          WorldUtils.runWorldDeletion();
          sender.sendMessage("World deletion complete.");
        } catch (SQLException e) {
          sender.sendMessage("A database error occurred while running world deletion.");
          throw new RuntimeException(e);
        }
      })
      .withPermission("playerworlds.command.runworlddeletion")
      .register();
  }

}
