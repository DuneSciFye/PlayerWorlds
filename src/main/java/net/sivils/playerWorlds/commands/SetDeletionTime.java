package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class SetDeletionTime {

  public void register() {
    new CommandAPICommand("playerworlds")
      .withArguments(new LiteralArgument("setdeletiontime"), new PlayerArgument("Player"), new IntegerArgument("Seconds"))
      .executes((sender, args) -> {
        int seconds = args.getUnchecked("Seconds");
        Player p = args.getUnchecked("Player");

        Database db = PlayerWorlds.getInstance().getDatabase();
        try {
          db.setWorldField(db.getWorld(p.getUniqueId().toString()), "deletion_time", seconds);
          sender.sendMessage("Set deletion time to " + seconds + " seconds");
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      })
      .withPermission("playerworlds.setdeletiontime")
      .register();
  }

}
