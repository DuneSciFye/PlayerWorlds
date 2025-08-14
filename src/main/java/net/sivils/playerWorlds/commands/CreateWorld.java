package net.sivils.playerWorlds.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.ExecutorType;
import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.CreateFailureReason;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;
import org.mvplugins.multiverse.inventories.share.Sharables;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class CreateWorld {

  public void register(PlayerWorlds plugin) {

    LiteralArgument createArg = new LiteralArgument("create");
    MultiLiteralArgument worldTypeArg = new MultiLiteralArgument("World Type",
      Arrays.stream(WorldType.values()).map(WorldType::getName).toArray(String[]::new));
    MultiLiteralArgument difficultyArg = new MultiLiteralArgument("Difficulty",
      Arrays.stream(Difficulty.values()).map(Difficulty::toString).toArray(String[]::new));
    BooleanArgument generateStructuresArg = new BooleanArgument("Generate Structures");
    BooleanArgument pvpArg = new BooleanArgument("PvP");
    BooleanArgument advancementsArg = new BooleanArgument("Allow Advancements");
    TextArgument seedArg = new TextArgument("Seed");
    TextArgument generatorArg = new TextArgument("Generator");
    BooleanArgument cheatsArg = new BooleanArgument("Cheats");

    new CommandAPICommand("playerworlds")
      .withArguments(createArg)
      .withOptionalArguments(seedArg, worldTypeArg, generateStructuresArg, difficultyArg, pvpArg, advancementsArg,
        generatorArg, cheatsArg)
      .executes((sender, args) -> {
        Database db = plugin.getDatabase();
        Player player = sender instanceof ProxiedCommandSender proxy ? (Player) proxy.getCallee() : (Player) sender;

        player.sendMessage(Component.text("Attempting world creation now...", NamedTextColor.GREEN));
        try {
          if (db.ownsWorld(player.getUniqueId().toString())) {
            player.sendMessage(Component.text("You already own a world!", NamedTextColor.RED));
            return;
          }

          String worldUUID = UUID.randomUUID().toString();
          String[] worldNames = {worldUUID, worldUUID + "_nether", worldUUID + "_the_end"};
          World.Environment[] environments = {World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END};

          // Multiverse-Core setup
          WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();

          WorldType worldType = WorldType.getByName(args.getByArgumentOrDefault(worldTypeArg, "NORMAL"));
          boolean generateStructures = args.getByArgumentOrDefault(generateStructuresArg, true);
          Long seed = null;
          String rawSeed = args.getByArgument(seedArg);
          if (rawSeed != null && !rawSeed.isBlank()) {
            try {
              seed = Long.parseLong(rawSeed);
            } catch (NumberFormatException nfe) {
              seed = (long) rawSeed.hashCode();
            }
          }
          String generator = args.getByArgument(generatorArg);

          for (int i = 0; i < worldNames.length; i++) {
            CreateWorldOptions worldOptions = CreateWorldOptions.worldName(worldNames[i])
              .environment(environments[i])
              .doFolderCheck(true)
              .useSpawnAdjust(true)
              .generateStructures(generateStructures);
            if (worldType != null) worldOptions.worldType(worldType);
            if (seed != null) worldOptions.seed(seed);
            if (generator != null && !generator.isBlank()) worldOptions.generator(generator);

            Attempt<LoadedMultiverseWorld, CreateFailureReason> attempt = worldManager.createWorld(worldOptions);
            if (attempt.isFailure()) {
              player.sendMessage(Component.text("Failed to create world " + attempt.get().getName() + "! " +
                "Failure Reason: " + attempt.getFailureReason().toString() + ". Please report " +
                "this to an administrator.", NamedTextColor.RED));
              return;
            } else {
              attempt.get().setAutoLoad(false);
            }

          }

          // Multiverse-Inventory setup
          WorldGroupManager groupManager = MultiverseInventoriesApi.get().getWorldGroupManager();
          WorldGroup group = groupManager.newEmptyGroup(worldUUID);
          for (String worldName : worldNames) group.addWorld(worldName);
          group.getShares().addAll(Sharables.allOf());
          groupManager.updateGroup(group);

          player.sendMessage(Component.text("Created a new world!", NamedTextColor.GREEN));
          for (String worldName : worldNames) {
            MultiverseWorld world = worldManager.getWorld(worldName).get();
            if (world == null) return;

            String difficulty = args.getByArgument(difficultyArg);
            if (difficulty != null) world.setDifficulty(Difficulty.valueOf(difficulty));

            Boolean pvp = args.getByArgument(pvpArg);
            if (pvp != null) world.setPvp(pvp);

            Boolean allowAdvancements = args.getByArgument(advancementsArg);
            if (allowAdvancements != null) world.setAllowAdvancementGrant(allowAdvancements);
          }

          MultiverseWorld world = worldManager.getWorld(worldUUID).get();

          // Set respawn-world of main world to fix a weird bug where players spawn in Lobby world
          world.setRespawnWorld(world);

          player.teleport(world.getSpawnLocation());
          db.addWorld(player.getUniqueId().toString(), worldUUID, world.getSeed());

          Boolean cheats = args.getByArgument(cheatsArg);
          if (cheats != null && cheats) db.setWorldField(worldUUID, "cheats_enabled", 1);

          WorldUtils.activeWorldPlugins.put(worldUUID, new ArrayList<>());

          DiscordSRV.getPlugin().getMainTextChannel().sendMessage("Creating a new world for " + player.getName() + ".").queue();

        } catch (SQLException e) {
          player.sendMessage(Component.text("An error occurred while adding a world to the database. Please report " +
            "to an administrator.", NamedTextColor.RED));
          throw new RuntimeException(e);
        }
      }, ExecutorType.PLAYER, ExecutorType.PROXY)
      .register();

  }
}
