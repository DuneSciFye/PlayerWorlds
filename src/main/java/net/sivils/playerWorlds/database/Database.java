package net.sivils.playerWorlds.database;

import net.sivils.playerWorlds.PlayerWorlds;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Database {

    private final Connection connection;

    public Database(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
            CREATE TABLE IF NOT EXISTS worlds (
            world_uuid TEXT PRIMARY KEY,
            owner_uuid TEXT NOT NULL,
            display_name TEXT DEFAULT NULL,
            creation_time TIMESTAMP NOT NULL,
            last_use_time TIMESTAMP NOT NULL,
            deletion_time INT DEFAULT 604800,
            seed LONG NOT NULL,
            password TEXT DEFAULT NULL,
            password_enabled BOOLEAN DEFAULT 0,
            cheats_enabled BOOLEAN DEFAULT 0,
            plugins TEXT
            )
        """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS player_accesses (
            world_uuid TEXT NOT NULL,
            player_uuid TEXT NOT NULL,
            player_username TEXT NOT NULL,
            can_join BOOLEAN DEFAULT NULL,
            bypass_password BOOLEAN DEFAULT NULL,
            break_blocks BOOLEAN DEFAULT NULL,
            place_blocks BOOLEAN DEFAULT NULL,
            pickup_items BOOLEAN DEFAULT NULL,
            PRIMARY KEY (world_uuid, player_uuid)
            )
            """);

            statement.execute("""
              CREATE TABLE IF NOT EXISTS player_info (
              player_uuid TEXT NOT NULL PRIMARY KEY,
              last_join_worlds TEXT,
              last_join_times TEXT
              )
              """);

        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void addWorld(String playerUUID, String worldUUID, long seed) throws SQLException {
        if (ownsWorld(playerUUID)) return;

        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO worlds (world_uuid, " +
          "owner_uuid, creation_time, last_use_time, seed) VALUES (?, ?, ?, ?, ?) ")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            preparedStatement.setLong(5, seed);
            preparedStatement.executeUpdate();
        }

        // Create the default permissions for this world
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO player_accesses (world_uuid, " +
          "player_uuid, player_username, can_join) VALUES (?, ?, ?, ?) ")) {
            stmt.setString(1, worldUUID);
            stmt.setString(2, worldUUID);
            stmt.setString(3, "");
            stmt.setBoolean(4, true);
            stmt.executeUpdate();
        }
    }

    public boolean ownsWorld(String playerUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM worlds WHERE owner_uuid = ?")) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    /**
     * Gets the UUID of the world a player owns, or null if none
     * @param playerUUID The String UUID of the player
     * @return String UUID of the world the player owns
     * @throws SQLException for any database error
     */
    public String getWorld(String playerUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT world_uuid FROM worlds WHERE owner_uuid = ?")) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("world_uuid");
            } else {
                return null;
            }
        }
    }

    public boolean worldExists(String worldUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM worlds WHERE world_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    public void removeWorld(String worldUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM worlds WHERE world_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.executeUpdate();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM player_accesses WHERE world_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.executeUpdate();
        }
    }

    public List<String> getAllWorlds() throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT world_uuid FROM worlds")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> worlds = new ArrayList<>();
            while (resultSet.next()) {
                worlds.add(resultSet.getString("world_uuid"));
            }
            return worlds;
        }
    }
    public void setWorldField(String worldUUID, String column, Object value) throws SQLException {
        String sql = "UPDATE worlds SET " + column + " = ? WHERE world_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, value);
            stmt.setString(2, worldUUID);
            stmt.executeUpdate();
        }
    }

    public Object getWorldField(String worldUUID, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM worlds WHERE world_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, worldUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getObject(column) : null;
        }
    }

    public Boolean getBooleanField(String worldUUID, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM worlds WHERE world_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, worldUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getBoolean(column) : null;
        }
    }
    public Timestamp getTimestampField(String worldUUID, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM worlds WHERE world_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, worldUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getTimestamp(column) : null;
        }
    }


    public void setPlayerAccess(String worldUUID, String playerUUID, String playerName, String accessType,
                                boolean accessValue) throws SQLException {
        if (!playerAccessExists(worldUUID, playerUUID)) createDefaultPlayerAccess(worldUUID, playerUUID, playerName);

        String query = "UPDATE player_accesses SET " + accessType + " = ? WHERE world_uuid = ? AND player_uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setBoolean(1, accessValue);
            preparedStatement.setString(2, worldUUID);
            preparedStatement.setString(3, playerUUID);
            preparedStatement.executeUpdate();
        }
    }

    public void removePlayerAccess(String worldUUID, String playerUUID) throws SQLException {
        if (!playerAccessExists(worldUUID, playerUUID)) return;

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM player_accesses WHERE world_uuid = ? AND player_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            preparedStatement.executeUpdate();
        }
    }

    public boolean getPlayerAccess(String worldUUID, String playerUUID, String accessType) throws SQLException {
        String query = "SELECT " + accessType + " FROM player_accesses WHERE world_uuid = ? AND player_uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            // Get default access if specified access isn't available
            if (!resultSet.next()) {
                preparedStatement.setString(2, worldUUID);
                ResultSet resultSet2 = preparedStatement.executeQuery();
                return resultSet2.next() && resultSet2.getBoolean(accessType);
            }
            return resultSet.getBoolean(accessType);
        }
    }

    public String getPlayerAccessString(String worldUUID, String playerUUID, String accessType) throws SQLException {
        String query = "SELECT " + accessType + " FROM player_accesses WHERE world_uuid = ? AND player_uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.getString(accessType);
        }
    }

    public ArrayList<String> getPlayerAccessesString(String worldUUID, String column) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_accesses WHERE " +
          "world_uuid = ? AND player_uuid != ?")) {
            stmt.setString(1, worldUUID);
            stmt.setString(2, worldUUID);
            ResultSet resultSet = stmt.executeQuery();
            ArrayList<String> accesses = new ArrayList<>();
            while (resultSet.next()) {
                accesses.add(resultSet.getString(column));
            }
            return accesses;
        }
    }
    public ArrayList<Boolean> getPlayerAccessesBoolean(String worldUUID, String column) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_accesses WHERE " +
          "world_uuid = ? AND player_uuid != ?")) {
            stmt.setString(1, worldUUID);
            stmt.setString(2, worldUUID);
            ResultSet resultSet = stmt.executeQuery();
            ArrayList<Boolean> accesses = new ArrayList<>();
            while (resultSet.next()) {
                accesses.add(resultSet.getBoolean(column));
            }
            return accesses;
        }
    }

    public boolean playerAccessExists(String worldUUID, String playerUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT player_uuid FROM player_accesses WHERE world_uuid = ? AND player_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }


    public void createDefaultPlayerAccess(String worldUUID, String playerUUID, String playerName) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO player_accesses " +
          "(world_uuid, player_uuid, player_username) VALUES (?, ?, ?)")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            preparedStatement.setString(3, playerName);
            preparedStatement.executeUpdate();
        }
    }

    public void setPlayerInfoField(String playerUUID, String column, Object value) throws SQLException {
        if (!playerInfoExists(playerUUID)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO player_info " +
              "(player_uuid) VALUES (?)")) {
                preparedStatement.setString(1, playerUUID);
                preparedStatement.executeUpdate();
            }
        }
        String sql = "UPDATE player_info SET " + column + " = ? WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, value);
            stmt.setString(2, playerUUID);
            stmt.executeUpdate();
        }
    }

    public String getPlayerInfoField(String playerUUID, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM player_info WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString(column) : null;
        }
    }

    public boolean playerInfoExists(String playerUUID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT player_uuid FROM player_info " +
          "WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public void enablePlugin(String worldUUID, String pluginName) throws SQLException {
        try (PreparedStatement stmt =
               connection.prepareStatement("UPDATE worlds SET plugins = ? WHERE world_uuid = ?")) {
            stmt.setString(2, worldUUID);
            String pluginsString = getPlugins(worldUUID);
            if (pluginsString == null) pluginsString = "";
            ArrayList<String> plugins = new ArrayList<>(List.of(pluginsString.split(",")));
            if (!plugins.contains(pluginName)) {
                plugins.add(pluginName);
                stmt.setString(1, String.join(",", plugins));
                stmt.executeUpdate();
            }
        }
    }
    public void disablePlugin(String worldUUID, String pluginName) throws SQLException {
        try (PreparedStatement stmt =
               connection.prepareStatement("UPDATE worlds SET plugins = ? WHERE world_uuid = ?")) {
            stmt.setString(2, worldUUID);
            String pluginsString = getPlugins(worldUUID);
            if (pluginsString == null) pluginsString = "";
            ArrayList<String> plugins = new ArrayList<>(List.of(pluginsString.split(",")));
            if (plugins.contains(pluginName)) {
                plugins.remove(pluginName);
                stmt.setString(1, String.join(",", plugins));
                stmt.executeUpdate();
            }
        }
    }

    public String getPlugins(String worldUUID) throws SQLException {
        String sql = "SELECT plugins FROM worlds WHERE world_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, worldUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("plugins") : "";
        }
    }

    /**
     * Gets all of the stored SQLite data of a world
     * @param worldUUID The String UUID of the World to get data of
     * @return The WorldData of the World
     * @throws SQLException Throws a Database Exception
     */
    public WorldData getWorldData(String worldUUID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM worlds WHERE world_uuid = ?")) {
            stmt.setString(1, worldUUID);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;

            return new WorldData(
              false,
              worldUUID,
              rs.getString("owner_uuid"),
              rs.getString("display_name"),
              rs.getTimestamp("creation_time"),
              rs.getTimestamp("last_use_time"),
              rs.getInt("deletion_time"),
              rs.getLong("seed"),
              rs.getString("password"),
              rs.getBoolean("password_enabled"),
              rs.getBoolean("cheats_enabled"),
              rs.getString("plugins"),
              getPlayerAccessData(worldUUID).get(worldUUID) // Default Player Accesses
            );
        }
    }

    public void saveWorldData(String worldUUID, WorldData worldData) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE worlds SET " +
                 "owner_uuid = ?, " +
                 "display_name = ?, " +
                 "creation_time = ?, " +
                 "last_use_time = ?, " +
                 "deletion_time = ?, " +
                 "seed = ?, " +
                 "password = ?, " +
                 "password_enabled = ?, " +
                 "cheats_enabled = ?, " +
                 "plugins = ? " +
                 "WHERE world_uuid = ?")) {
            stmt.setString(1, worldData.ownerUUID());
            stmt.setString(2, worldData.displayName());
            stmt.setTimestamp(3, worldData.creationTime());
            stmt.setTimestamp(4, worldData.lastUseTime());
            stmt.setInt(5, worldData.deletionTime());
            stmt.setLong(6, worldData.seed());
            stmt.setString(7, worldData.password());
            stmt.setBoolean(8, worldData.passwordEnabled());
            stmt.setBoolean(9, worldData.cheatsEnabled());
            stmt.setString(10, worldData.plugins());
            stmt.setString(11, worldUUID);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                PlayerWorlds.getInstance().getLogger().warning("Failed to save world data for world " + worldUUID +
                  ". No rows were updated, meaning the world didn't exist in the Database.");
            }
        }
    }

    public HashMap<String, PlayerAccess> getPlayerAccessData(String playerUUID) throws SQLException {
        final HashMap<String, PlayerAccess> playerAccesses = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_accesses WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                playerAccesses.put(rs.getString("world_uuid"), new PlayerAccess(
                  false,
                  rs.getString("player_username"),
                  rs.getBoolean("can_join"),
                  rs.getBoolean("bypass_password"),
                  rs.getBoolean("break_blocks"),
                  rs.getBoolean("place_blocks"),
                  rs.getBoolean("pickup_items")
                ));
            }

            return playerAccesses;
        }
    }

    /**
     * Gets all of the stored SQLite data of a player
     * @param playerUUID The UUID of the Player to get data of
     * @return The PlayerInfo of the Player
     * @throws SQLException Throws a Database Exception
     */
    public PlayerInfo getPlayerInfoData(final UUID playerUUID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_info WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;

            return new PlayerInfo(
              false,
              rs.getString("last_join_worlds"),
              rs.getString("last_join_times")
            );
        }
    }

}
