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
            uuid TEXT PRIMARY KEY,
            owner TEXT NOT NULL,
            displayname TEXT,
            creationtime TIMESTAMP NOT NULL,
            lastusetime TIMESTAMP,
            worldsettings TEXT,
            whitelist BOOLEAN)
        """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS players (
            uuid TEXT PRIMARY KEY,
            username TEXT NOT NULL,
            worlds TEXT NOT NULL)
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
        if (ownsWorld(player)) return null;

        String worldUUID = UUID.randomUUID().toString();
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO worlds (uuid, owner, displayname, creationtime, lastusetime, worldsettings, whitelist) VALUES (?, ?, ?, ?, null, null, null) ")) {
            preparedStatement.setString(1, worldUUID);
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setString(3, displayName);
            preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            preparedStatement.executeUpdate();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO players (uuid, username, worlds) VALUES (?, ?, ?) ")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setString(2, player.getName());
            preparedStatement.setString(3, worldUUID);
            preparedStatement.executeUpdate();
        }

        return worldUUID;
    }

    public boolean ownsWorld(Player player) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    public String getWorld(Player player) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT worlds FROM players WHERE uuid = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("worlds");
            } else {
                return null;
            }
        }
    }

    public void removeWorld(Player player) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM worlds WHERE uuid = ?")) {
            preparedStatement.setString(1, getWorld(player));
            preparedStatement.executeUpdate();
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM players WHERE uuid = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }

    public Timestamp getCreationTime(String worldUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT creationtime FROM worlds WHERE uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getTimestamp("creationtime");
            } else {
                return null;
            }
        }
    }

    public List<String> getAllWorlds() throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM worlds")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> worlds = new ArrayList<>();
            while (resultSet.next()) {
                worlds.add(resultSet.getString("uuid"));
            }
            return worlds;
        }
    }

    public void setWhitelist(String worldUUID, boolean enabled) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE worlds SET whitelist = ? WHERE uuid = ?")) {
            preparedStatement.setBoolean(1, enabled);
            preparedStatement.setString(2, worldUUID);
            preparedStatement.executeUpdate();
        }
    }

    public boolean getWhitelist(String worldUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT whitelist FROM worlds WHERE uuid = ?")) {
            preparedStatement.setString(1, worldUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() && resultSet.getBoolean("whitelist");
        }
    }

}
