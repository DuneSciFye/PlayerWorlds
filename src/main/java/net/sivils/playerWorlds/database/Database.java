package net.sivils.playerWorlds.database;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
            cheats_enabled BOOLEAN DEFAULT 0)
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

}
