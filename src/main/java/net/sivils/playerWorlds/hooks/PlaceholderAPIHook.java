package net.sivils.playerWorlds.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sivils.playerWorlds.PlayerWorlds;
import net.sivils.playerWorlds.database.Database;
import net.sivils.playerWorlds.utils.WorldUtils;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final PlayerWorlds plugin;

    public PlaceholderAPIHook(PlayerWorlds plugin) {
        this.register(); // For PAPI

        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playerworlds";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DuneSciFye";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_", 2);
        if (parts.length> 1) parts[1] = PlaceholderAPI.setBracketPlaceholders(player, parts[1]);

        try {
            Database db = plugin.getDatabase();
            String playerUUID = player.getUniqueId().toString();
            String worldUUID = db.getWorld(playerUUID);

            switch (parts[0].toLowerCase()) {
                case "ownsworld" -> {
                    return String.valueOf(db.ownsWorld(playerUUID));
                }
                case "worlduuid" -> {
                    return worldUUID;
                }
                case "whitelistenabled" -> {
                    return String.valueOf(db.getBooleanField(worldUUID, "whitelist"));
                }
                case "passwordenabled" -> {
                    return String.valueOf(db.getBooleanField(worldUUID, "password_enabled"));
                }
                case "password" -> {
                    Object pswd = db.getWorldField(worldUUID, "password");
                    return pswd == null ? "" : pswd.toString();
                }
                case "seed" -> {
                    return String.valueOf(db.getWorldField(worldUUID, "seed"));
                }
                case "playeraccess" -> {
                    String[] args = parts[1].split(",");
                    ;
                    ArrayList<String> accesses = db.getPlayerAccessesString(worldUUID, args[0]);
                    accesses.addFirst(db.getPlayerAccessString(worldUUID, worldUUID, args[0])); // Default permissions come first
                    int i = Integer.parseInt(args[1]);
                    return i < accesses.size() ? accesses.get(i) : "";
                }
                case "playeraccessbool" -> {
                    String[] args = parts[1].split(",");

                    ArrayList<Boolean> accesses = db.getPlayerAccessesBoolean(worldUUID, args[0]);
                    accesses.addFirst(db.getPlayerAccess(worldUUID, worldUUID, args[0])); // Default permissions come first
                    int i = Integer.parseInt(args[1]);
                    return i < accesses.size() ? String.valueOf(accesses.get(i)) : "";
                }
                case "lastusetime" -> {
                    Timestamp lastUseTime = db.getTimestampField(worldUUID, "last_use_time");
                    return lastUseTime.toString();
                }
                case "deletiontime" -> {
                    int deletionTime = (int) db.getWorldField(worldUUID, "deletion_time");
                    Duration duration = Duration.ofSeconds(deletionTime);
                    return WorldUtils.getDeletionTimeMessage(duration);
                }
                case "timeuntildeletion" -> {
                    int deletionSeconds = (int) db.getWorldField(worldUUID, "deletion_time");
                    Duration deletionDuration = Duration.ofSeconds(deletionSeconds);

                    Timestamp lastUseTime = db.getTimestampField(worldUUID, "last_use_time");
                    Instant deletionDeadline = lastUseTime.toInstant().plus(deletionDuration);

                    Duration timeLeft = Duration.between(Instant.now(), deletionDeadline);

                    return WorldUtils.getDeletionTimeMessage(timeLeft);
                }
                case "activeworlds" -> {
                    return String.join(",", WorldUtils.activeWorldPlugins.keySet());
                }
                case "cheatsenabled" -> {
                    return String.valueOf(db.getBooleanField(worldUUID, "cheats_enabled"));
                }
                case "lastjoinworlds" -> {
                    String[] args = parts[1].split(",");
                    int index = Integer.parseInt(args[0]);

                    String stringWorlds = db.getPlayerInfoField(playerUUID, "last_join_worlds");
                    String[] worlds = stringWorlds.split(",");
                    return worlds.length > index ? worlds[index] : "";
                }
                case "lastjointimes" -> {
                    String[] args = parts[1].split(",");
                    int index = Integer.parseInt(args[0]);

                    String stringTimes = db.getPlayerInfoField(playerUUID, "last_join_times");
                    String[] times = stringTimes.split(",");
                    return times.length > index ? times[index] : "";
                }
                case "plugins" -> {
                    return db.getPlugins(worldUUID);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return super.onRequest(player, params);
    }
}
