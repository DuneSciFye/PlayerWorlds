package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.ExecutorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Cache;
import net.sivils.playerWorlds.database.WorldData;
import net.sivils.playerWorlds.utils.CommandUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class CheatsCommand {

  public void register() {

    LiteralArgument cheatsArg = new LiteralArgument("cheats");
    LiteralArgument enableArg = new LiteralArgument("enable");

    new CommandAPICommand("playerworlds")
      .withArguments(cheatsArg, enableArg)
      .executes((commandSender, args) -> {
        final Player player = CommandUtils.getPlayer(commandSender);
        final UUID playerUUID = player.getUniqueId();

        Cache cache = PlayerWorlds.getCache();
        ArrayList<String> ownedWorlds = cache.getPlayersWorlds(playerUUID);

        if (ownedWorlds == null || ownedWorlds.isEmpty()) {
          player.sendMessage(Component.text("You don't own any worlds!", NamedTextColor.RED));
          return;
        }

        String worldUUID = ownedWorlds.getFirst();

        WorldData worldData = cache.getCachedWorldData(worldUUID);
        worldData = worldData.withCheatsEnabled(true);
        cache.setCachedWorldData(worldUUID, worldData);
        player.sendMessage(Component.text("Enabled cheats.", NamedTextColor.GREEN));
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();
  }

}
