package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.CommandUtils;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CheatsCommand {

  public void register() {

    LiteralArgument cheatsArg = new LiteralArgument("cheats");
    LiteralArgument enableArg = new LiteralArgument("enable");

    new CommandAPICommand("playerworlds")
      .withArguments(cheatsArg, enableArg)
      .executes((commandSender, args) -> {
        Player player = CommandUtils.getPlayer(commandSender);
        String uuid = player.getUniqueId().toString();
        Database db = PlayerWorlds.getInstance().getDatabase();

        try {
          String worldUUID = db.getWorld(uuid);

          if (!db.ownsWorld(uuid)) {
            player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
            return;
          }

          db.setWorldField(worldUUID, "cheats_enabled", true);
          player.sendMessage(Component.text("Enabled cheats.", NamedTextColor.GREEN));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();
  }

}
