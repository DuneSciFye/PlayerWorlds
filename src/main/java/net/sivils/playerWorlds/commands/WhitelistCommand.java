package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;

import java.sql.SQLException;

public class WhitelistCommand {

    public void register(PlayerWorlds plugin) {

        LiteralArgument enableArg = new LiteralArgument("enable");
        LiteralArgument disableArg = new LiteralArgument("disable");

        new CommandTree("whitelist")
            .then(enableArg
                .executesPlayer((player, args) -> {
                    Database db = plugin.getDatabase();
                    try {
                        if (!db.ownsWorld(player)) {
                            player.sendMessage(Component.text("You do not own a world!", NamedTextColor.RED));
                        } else {
                            db.setWhitelist(db.getWorld(player), true);
                        }
                    } catch (SQLException e) {
                        player.sendMessage(Component.text("There was an error fetching the database for command /whitelist enable. Please report to an administrator.", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                })
            )
            .then(disableArg
                .executesPlayer((player, args) -> {
                    Database db = plugin.getDatabase();
                    try {
                        if (!db.ownsWorld(player)) {
                            player.sendMessage(Component.text("You do not own a world!", NamedTextColor.RED));
                        } else {
                            db.setWhitelist(db.getWorld(player), false);
                        }
                    } catch (SQLException e) {
                        player.sendMessage(Component.text("There was an error fetching the database for command /whitelist disable. Please report to an administrator.", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                })
            )
            .register();

    }

}
