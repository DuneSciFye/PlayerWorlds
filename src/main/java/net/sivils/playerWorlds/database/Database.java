package net.sivils.playerWorlds.database;

import org.bukkit.entity.Player;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
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
            display_name TEXT,
            creation_time TIMESTAMP NOT NULL,
            last_use_time TIMESTAMP,
            world_settings TEXT,
            whitelist BOOLEAN)
        """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS player_accesses (
            world_uuid TEXT NOT NULL,
            player_uuid TEXT NOT NULL,
            can_join BOOLEAN,
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

    /**
     * Creates a world in the database
     * @param player
     * @param displayName
     * @return World UUID
     * @throws SQLException if
     */
    public String addWorld(Player player, String displayName) throws SQLException {
        if (ownsWorld(player.getUniqueId().toString())) return null;

        String worldUUID = UUID.randomUUID().toString();
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO worlds (world_uuid, owner_uuid, display_name, creation_time, last_use_time, world_settings, whitelist) VALUES (?, ?, ?, ?, null, null, null) ")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setString(3, displayName);
            preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            preparedStatement.executeUpdate();
        }

        return worldUUID;
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

    public void removeWorld(String playerUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM worlds WHERE world_uuid = ?")) {
            preparedStatement.setString(1, getWorld(playerUUID));
            preparedStatement.executeUpdate();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM player_accesses WHERE world_uuid = ?")) {
            preparedStatement.setString(1, getWorld(playerUUID));
            preparedStatement.executeUpdate();
        }
    }

    public Timestamp getCreationTime(String worldUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT creation_time FROM worlds WHERE world_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getTimestamp("creation_time");
            } else {
                return null;
            }
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

    public void setWhitelist(String worldUUID, boolean enabled) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE worlds SET whitelist = ? WHERE world_uuid = ?")) {
            preparedStatement.setBoolean(1, enabled);
            preparedStatement.setString(2, worldUUID);
            preparedStatement.executeUpdate();
        }
    }

    public boolean getWhitelist(String worldUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT whitelist FROM worlds WHERE world_uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() && resultSet.getBoolean("whitelist");
        }
    }

    public void setPlayerAccess(String worldUUID, String playerUUID, String accessType, boolean accessValue) throws SQLException {
        if (!playerAccessExists(worldUUID, playerUUID)) createDefaultPlayerAccess(worldUUID, playerUUID);

        String query = "UPDATE player_accesses SET " + accessType + " = ? WHERE world_uuid = ? AND player_uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setBoolean(1, accessValue);
            preparedStatement.setString(2, worldUUID);
            preparedStatement.setString(3, playerUUID);
            preparedStatement.executeUpdate();
        }
    }

    public boolean getPlayerAccess(String worldUUID, String playerUUID, String accessType) throws SQLException {
        String query = "SELECT " + accessType + " FROM player_accesses WHERE world_uuid = ? AND player_uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() && resultSet.getBoolean(accessType);
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

    private void createDefaultPlayerAccess(String worldUUID, String playerUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO player_accesses (world_uuid, player_uuid) VALUES (?, ?)")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, playerUUID);
            preparedStatement.executeUpdate();
        }
    }

}
