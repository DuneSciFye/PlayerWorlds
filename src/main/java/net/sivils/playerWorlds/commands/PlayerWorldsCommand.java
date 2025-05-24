package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.config.Config;
import net.sivils.playerWorlds.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.CreateFailureReason;
import org.mvplugins.multiverse.core.world.reasons.LoadFailureReason;
import org.mvplugins.multiverse.external.vavr.control.Option;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;
import org.mvplugins.multiverse.inventories.share.Sharables;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PlayerWorldsCommand {

    public void register(PlayerWorlds plugin) {



        /*
            Command for creating a player world
         */
        new CommandAPICommand("playerworlds")
            .withArguments(new LiteralArgument("create"))
            .executesPlayer((player, args) -> {
                player.sendMessage(Component.text("Attempting world creation now...", NamedTextColor.GREEN));
                try {
                    if (plugin.getDatabase().ownsWorld(player.getUniqueId().toString())) {
                        player.sendMessage(Component.text("You already own a world!", NamedTextColor.RED));
                        return;
                    }

                    String worldUUID = plugin.getDatabase().addWorld(player, null);

                    // Multiverse-Core setup
                    WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
                    List<Attempt<LoadedMultiverseWorld, CreateFailureReason>> worlds = new ArrayList<>();
                    worlds.add(worldManager.createWorld(CreateWorldOptions.worldName(worldUUID).environment(World.Environment.NORMAL)));
                    worlds.add(worldManager.createWorld(CreateWorldOptions.worldName(worldUUID + "_nether").environment(World.Environment.NETHER)));
                    worlds.add(worldManager.createWorld(CreateWorldOptions.worldName(worldUUID + "_the_end").environment(World.Environment.THE_END)));

                    for (Attempt<LoadedMultiverseWorld, CreateFailureReason> world : worlds) {
                        if (world.isFailure()) {
                            player.sendMessage(Component.text("Failed to create world " + world.get().getName() + "! Please report this to an administrator.", NamedTextColor.RED));
                            return;
                        } else {
                            world.get().setAutoLoad(false);
                        }
                    }

                    // Multiverse-Inventory setup
                    WorldGroupManager groupManager = MultiverseInventoriesApi.get().getWorldGroupManager();
                    WorldGroup group = groupManager.newEmptyGroup(worldUUID);
                    group.addWorld(worldUUID);
                    group.addWorld(worldUUID + "_nether");
                    group.addWorld(worldUUID + "_the_end");
                    group.getShares().addAll(Sharables.allOf());
                    groupManager.updateGroup(group);

                    player.sendMessage(Component.text("Created a new world!", NamedTextColor.GREEN));
                    Option<MultiverseWorld> world = worldManager.getWorld(worldUUID);
                    if (world != null) player.teleport(world.get().getSpawnLocation());

                } catch (SQLException e) {
                    player.sendMessage(Component.text("An error occurred while adding a world to the database. Please report to an administrator.", NamedTextColor.RED));
                    throw new RuntimeException(e);
                }
            })
            .register();


        /*
            Command for teleporting to player's world
         */
        MultiLiteralArgument teleportArgs = new MultiLiteralArgument("Function", "goto", "go", "tp", "teleport");
        OfflinePlayerArgument playerArg = new OfflinePlayerArgument("Player");

        new CommandAPICommand("playerworlds")
            .withArguments(teleportArgs)
            .withOptionalArguments(playerArg)
            .executesPlayer((player, args) -> {
                try {
                    OfflinePlayer offlineTarget = args.getByArgumentOrDefault(playerArg, null);
                    Database db = plugin.getDatabase();

                    // Go to player's own world
                    if (offlineTarget == null) {
                        String playerUUID = player.getUniqueId().toString();
                        if (!db.ownsWorld(playerUUID)) {
                            player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
                            return;
                        }

                        String worldUUID = db.getWorld(playerUUID);
                        WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
                        Option<MultiverseWorld> world = worldManager.getWorld(worldUUID);
                        if (world == null) {
                            player.sendMessage(Component.text("There was an error loading your world! Please report to an administrator. Your world UUID: " + worldUUID, NamedTextColor.RED));
                        } else {
                            player.teleport(world.get().getSpawnLocation());
                        }
                    }
                    // Teleporting to someone else's world
                    else {
                        String targetUUID = offlineTarget.getUniqueId().toString();

                        if (!db.ownsWorld(targetUUID)) {
                            player.sendMessage(Component.text(offlineTarget.getName() + " doesn't own a world!", NamedTextColor.RED));
                            return;
                        }

                        String worldUUID = db.getWorld(targetUUID);
                        WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
                        Option<MultiverseWorld> world = worldManager.getWorld(worldUUID);
                        if (world == null) {
                            player.sendMessage(Component.text("There was an error loading " + offlineTarget.getName() + "'s world! Please report to an administrator. Your world UUID: " + worldUUID, NamedTextColor.RED));
                            return;
                        }
                        // Whitelist logic
                        if (db.getWhitelist(worldUUID) && !db.getPlayerAccess(worldUUID, player.getUniqueId().toString(), "can_join")) {
                            player.sendMessage(Component.text(offlineTarget.getName() + "'s world has whitelist enabled!", NamedTextColor.RED));
                            return;
                        }

                        MultiverseWorld mvWorld = world.get();
                        Attempt<LoadedMultiverseWorld, LoadFailureReason> loadedWorld = worldManager.loadWorld(mvWorld);
                        if (loadedWorld.isFailure()) {
                            player.sendMessage(Component.text("Failed to load world " + loadedWorld.get().getName() + ". Please report to an administrator.", NamedTextColor.RED));
                            player.sendMessage(Component.text(loadedWorld.getFailureMessage().toString(), NamedTextColor.RED));
                            return;
                        }
                        player.teleport(mvWorld.getSpawnLocation());
                    }

                } catch (SQLException e) {
                    player.sendMessage(Component.text("An error occurred while running this command. Please report to an administrator.", NamedTextColor.RED));
                    throw new RuntimeException(e);
                }
            })
            .register();


        /*
            Command for deleting one's world
         */
        LiteralArgument deleteArg = new LiteralArgument("delete");

        new CommandAPICommand("playerworlds")
            .withArguments(deleteArg)
            .executesPlayer((player, args) -> {
                player.sendMessage(Component.text("Attempting world deletion now...", NamedTextColor.GREEN));
                String playerUUID = player.getUniqueId().toString();

                try {
                    if (!plugin.getDatabase().ownsWorld(playerUUID)) {
                        player.sendMessage(Component.text("You don't own a world!", NamedTextColor.RED));
                        return;
                    }

                    final String worldUUID = plugin.getDatabase().getWorld(playerUUID);
                    // MultiverseInventories
                    WorldGroupManager groupManager = MultiverseInventoriesApi.get().getWorldGroupManager();
                    WorldGroup group = groupManager.getGroup(worldUUID);
                    groupManager.removeGroup(group);

                    Location spawnLoc = Config.spawnWorld.getSpawnLocation();

                    // MultiverseCore
                    WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
                    for (String worldName : group.getWorlds()) {
                        LoadedMultiverseWorld loadedWorld = worldManager.getLoadedWorld(worldName).get();
                        for (Player p : loadedWorld.getPlayers().get()) p.teleport(spawnLoc);
                        worldManager.deleteWorld(DeleteWorldOptions.world(loadedWorld));
                    }
                    plugin.getDatabase().removeWorld(playerUUID);

                    deleteFolder(new File(Bukkit.getPluginsFolder(), "Multiverse-Inventories/groups/" + worldUUID));
                    deleteFolder(new File(Bukkit.getPluginsFolder(), "Multiverse-Inventories/worlds/" + worldUUID));
                    deleteFolder(new File(Bukkit.getPluginsFolder(), "WorldGuard/worlds/" + worldUUID));
                    deleteFolder(new File(Bukkit.getPluginsFolder(), "WorldGuard/worlds/" + worldUUID + "_nether"));
                    deleteFolder(new File(Bukkit.getPluginsFolder(), "WorldGuard/worlds/" + worldUUID + "_the_end"));

                    player.sendMessage(Component.text("Deleted your world!", NamedTextColor.GREEN));
                } catch (SQLException e) {
                    player.sendMessage(Component.text("There was a database error when deleting your world! Please report to an administrator.", NamedTextColor.RED));
                    throw new RuntimeException(e);
                }
            })
            .register();

        new CommandAPICommand("playerworlds")
            .withArguments(new LiteralArgument("getcreationtime"))
            .executesPlayer((player, args) -> {
                try {
                    String worldUUID = plugin.getDatabase().getWorld(player.getUniqueId().toString());

                    Timestamp timestamp = plugin.getDatabase().getCreationTime(worldUUID);

                    player.sendMessage(Component.text(timestamp.toString()));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            })
            .register();

        new CommandAPICommand("playerworlds")
            .withArguments(new LiteralArgument("getallworlds"))
            .executesPlayer((player, args) -> {
                try {
                    List<String> worlds = plugin.getDatabase().getAllWorlds();
                    String result = String.join(", ", worlds);

                    player.sendMessage(Component.text(result));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            })
            .register();
    }

    private boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!deleteFolder(file)) return false;
                } else {
                    if (!file.delete()) return false;
                }
            }
        }
        return folder.delete();
    }

}
