package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class Password {

  public void register(PlayerWorlds plugin) {

    Database db = plugin.getDatabase();

    LiteralArgument passwordArg = new LiteralArgument("password");
    LiteralArgument enableArg = new LiteralArgument("enable");
    LiteralArgument resetArg = new LiteralArgument("reset");
    LiteralArgument disableArg = new LiteralArgument("disable");
    LiteralArgument setArg = new LiteralArgument("set");
    GreedyStringArgument passwordValueArg = new GreedyStringArgument("Password");

    new CommandTree("playerworlds")
      .then(passwordArg
        .then(enableArg
          .executes((sender, args) -> {
            Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

            try {
              db.setWorldField(db.getWorld(player.getUniqueId().toString()), "password_enabled", true);
              player.sendMessage(Component.text("Password enabled.", NamedTextColor.GREEN));
            } catch (SQLException e) {
              player.sendMessage(Component.text("A database error occurred while enabling passwords! Please report " +
                "to an administrator!", NamedTextColor.RED));
              throw new RuntimeException(e);
            }
          }, ExecutorType.PLAYER, ExecutorType.PROXY)
        )
        .then(disableArg
          .executes((sender, args) -> {
            Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

            try {
              db.setWorldField(db.getWorld(player.getUniqueId().toString()), "password_enabled", false);
              player.sendMessage(Component.text("Password disabled.", NamedTextColor.GREEN));
            } catch (SQLException e) {
              player.sendMessage(Component.text("A database error occurred while disabling passwords! Please report " +
                "to an administrator!", NamedTextColor.RED));
              throw new RuntimeException(e);
            }
          }, ExecutorType.PLAYER, ExecutorType.PROXY)
        )
        .then(resetArg
          .executes((sender, args) -> {
            Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

            try {
              db.setWorldField(db.getWorld(player.getUniqueId().toString()), "password", null);
              player.sendMessage(Component.text("Password reset.", NamedTextColor.GREEN));
            } catch (SQLException e) {
              player.sendMessage(Component.text("A database error occurred while resetting password! Please report " +
                "to an administrator!", NamedTextColor.RED));
              throw new RuntimeException(e);
            }
          }, ExecutorType.PLAYER, ExecutorType.PROXY)
        )
        .then(setArg
          .then(passwordValueArg
            .executes((sender, args) -> {
              Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

              try {
                String password = args.getByArgument(passwordValueArg);
                db.setWorldField(db.getWorld(player.getUniqueId().toString()), "password", password);
                player.sendMessage(Component.text("Password set.", NamedTextColor.GREEN));
              } catch (SQLException e) {
                player.sendMessage(Component.text("A database error occurred while setting password! Please report " +
                  "to an administrator!", NamedTextColor.RED));
                throw new RuntimeException(e);
              }
            }, ExecutorType.PLAYER, ExecutorType.PROXY)
          )
        )
      )
      .register();

  }

}
