package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;

public class WhitelistCommand {

    public void register(PlayerWorlds plugin) {

        LiteralArgument enableArg = new LiteralArgument("enable");
        LiteralArgument disableArg = new LiteralArgument("disable");
        LiteralArgument addArg = new LiteralArgument("add");
        LiteralArgument removeArg = new LiteralArgument("remove");
        LiteralArgument listArg = new LiteralArgument("list");
        OfflinePlayerArgument playerArg = new OfflinePlayerArgument("Player");

        new CommandTree("whitelist")
            .then(enableArg
                .executesPlayer((player, args) -> {
                    Database db = plugin.getDatabase();
                    String playerUUID = player.getUniqueId().toString();
                    try {
                        if (!db.ownsWorld(playerUUID)) {
                            player.sendMessage(Component.text("You do not own a world!", NamedTextColor.RED));
                        } else {
                            db.setWhitelist(db.getWorld(playerUUID), true);
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
                    String playerUUID = player.getUniqueId().toString();
                    try {
                        if (!db.ownsWorld(playerUUID)) {
                            player.sendMessage(Component.text("You do not own a world!", NamedTextColor.RED));
                        } else {
                            db.setWhitelist(db.getWorld(playerUUID), false);
                        }
                    } catch (SQLException e) {
                        player.sendMessage(Component.text("There was an error fetching the database for command /whitelist disable. Please report to an administrator.", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                })
            )
            .then(addArg
                .then(playerArg
                    .executesPlayer((player, args) -> {
                        Database db = plugin.getDatabase();
                        OfflinePlayer target = args.getByArgument(playerArg);
                        String playerUUID = player.getUniqueId().toString();
                        if (target == null) {
                            player.sendMessage(Component.text("Target is null!", NamedTextColor.RED));
                            return;
                        }

                        try {
                            if (!db.ownsWorld(playerUUID)) {
                                player.sendMessage(Component.text("You do not own a world!", NamedTextColor.RED));
                                return;
                            }
                            db.setPlayerAccess(db.getWorld(playerUUID), target.getUniqueId().toString(), "can_join", true);

                        } catch (SQLException e) {
                            player.sendMessage(Component.text("There was an error fetching the database for command /whitelist add. Please report to an administrator.", NamedTextColor.RED));
                            throw new RuntimeException(e);
                        }

                    })
                )
            )
            .then(removeArg
                .then(playerArg
                    .executesPlayer((player, args) -> {
                        Database db = plugin.getDatabase();
                        OfflinePlayer target = args.getByArgument(playerArg);
                        String playerUUID = player.getUniqueId().toString();
                        if (target == null) {
                            player.sendMessage(Component.text("Target is null!", NamedTextColor.RED));
                            return;
                        }


                        try {
                            if (!db.ownsWorld(playerUUID)) {
                                player.sendMessage(Component.text("You do not own a world!", NamedTextColor.RED));
                                return;
                            }
                            db.setPlayerAccess(db.getWorld(playerUUID), target.getUniqueId().toString(), "can_join", false);

                        } catch (SQLException e) {
                            player.sendMessage(Component.text("There was an error fetching the database for command /whitelist remove. Please report to an administrator.", NamedTextColor.RED));
                            throw new RuntimeException(e);
                        }

                    })
                )
            )
            .register();

    }

}
