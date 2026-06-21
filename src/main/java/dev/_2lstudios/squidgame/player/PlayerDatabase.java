package dev._2lstudios.squidgame.player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;

public class PlayerDatabase {

    private final SquidGame plugin;
    private final DatabaseType type;
    private final String tablePrefix;
    private Connection connection;

    public PlayerDatabase(final SquidGame plugin) {
        this.plugin = plugin;
        final Configuration config = plugin.getMainConfig();
        this.type = DatabaseType.fromConfig(config.getString("economy.database.type", "sqlite"));
        this.tablePrefix = this.sanitizeTablePrefix(config.getString("economy.database.table-prefix", "sg_"));
    }

    public void initialize() throws SQLException, ClassNotFoundException {
        this.openConnection();
        this.createTables();
        this.plugin.getLogger().info("Player database ready (" + this.type.name().toLowerCase() + ").");
    }

    public void close() {
        if (this.connection == null) {
            return;
        }

        try {
            this.connection.close();
        } catch (final SQLException exception) {
            this.plugin.getLogger().warning("Failed to close player database: " + exception.getMessage());
        }

        this.connection = null;
    }

    public DatabaseType getType() {
        return this.type;
    }

    public Connection getConnection() throws SQLException {
        this.ensureConnection();
        return this.connection;
    }

    public String playersTable() {
        return this.tablePrefix + "players";
    }

    public String unlocksTable() {
        return this.tablePrefix + "unlocks";
    }

    public String insertPlayerIgnoreSql() {
        if (this.type == DatabaseType.MYSQL) {
            return "INSERT IGNORE INTO " + this.playersTable()
                    + " (uuid, username, coins, wins, deaths, games_played, last_seen) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        return "INSERT OR IGNORE INTO " + this.playersTable()
                + " (uuid, username, coins, wins, deaths, games_played, last_seen) VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    public String upsertPlayerSql() {
        if (this.type == DatabaseType.MYSQL) {
            return "INSERT INTO " + this.playersTable()
                    + " (uuid, username, coins, wins, deaths, games_played, last_seen) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "username = VALUES(username), "
                    + "coins = VALUES(coins), "
                    + "wins = VALUES(wins), "
                    + "deaths = VALUES(deaths), "
                    + "games_played = VALUES(games_played), "
                    + "last_seen = VALUES(last_seen)";
        }

        return "INSERT INTO " + this.playersTable()
                + " (uuid, username, coins, wins, deaths, games_played, last_seen) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid) DO UPDATE SET "
                + "username = excluded.username, "
                + "coins = excluded.coins, "
                + "wins = excluded.wins, "
                + "deaths = excluded.deaths, "
                + "games_played = excluded.games_played, "
                + "last_seen = excluded.last_seen";
    }

    private void openConnection() throws SQLException, ClassNotFoundException {
        final Configuration config = this.plugin.getMainConfig();

        if (this.type == DatabaseType.MYSQL) {
            Class.forName("com.mysql.cj.jdbc.Driver");

            final String host = config.getString("economy.database.mysql.host", "localhost");
            final int port = config.getInt("economy.database.mysql.port", 3306);
            final String database = config.getString("economy.database.mysql.database", "squidgame");
            final String username = config.getString("economy.database.mysql.username", "root");
            final String password = config.getString("economy.database.mysql.password", "");
            final boolean useSsl = config.getBoolean("economy.database.mysql.use-ssl", false);

            final StringBuilder url = new StringBuilder("jdbc:mysql://");
            url.append(host).append(':').append(port).append('/').append(database);
            url.append("?useSSL=").append(useSsl);
            url.append("&autoReconnect=true");
            url.append("&characterEncoding=utf8");
            url.append("&useUnicode=true");

            this.connection = DriverManager.getConnection(url.toString(), username, password);
            return;
        }

        Class.forName("org.sqlite.JDBC");
        this.plugin.getDataFolder().mkdirs();

        final String fileName = config.getString("economy.database.sqlite.file", "data.db");
        final File databaseFile = new File(this.plugin.getDataFolder(), fileName);
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    private void ensureConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            try {
                this.openConnection();
            } catch (final ClassNotFoundException exception) {
                throw new SQLException("Database driver is missing: " + exception.getMessage(), exception);
            }
        }
    }

    private void createTables() throws SQLException {
        final String playersSql;
        final String unlocksSql;

        if (this.type == DatabaseType.MYSQL) {
            playersSql = "CREATE TABLE IF NOT EXISTS " + this.playersTable() + " ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "username VARCHAR(16) NOT NULL, "
                    + "coins INT NOT NULL DEFAULT 0, "
                    + "wins INT NOT NULL DEFAULT 0, "
                    + "deaths INT NOT NULL DEFAULT 0, "
                    + "games_played INT NOT NULL DEFAULT 0, "
                    + "last_seen BIGINT NOT NULL DEFAULT 0"
                    + ")";
            unlocksSql = "CREATE TABLE IF NOT EXISTS " + this.unlocksTable() + " ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "unlock_type VARCHAR(16) NOT NULL, "
                    + "unlock_id VARCHAR(64) NOT NULL, "
                    + "PRIMARY KEY (uuid, unlock_type, unlock_id)"
                    + ")";
        } else {
            playersSql = "CREATE TABLE IF NOT EXISTS " + this.playersTable() + " ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "username TEXT NOT NULL, "
                    + "coins INTEGER NOT NULL DEFAULT 0, "
                    + "wins INTEGER NOT NULL DEFAULT 0, "
                    + "deaths INTEGER NOT NULL DEFAULT 0, "
                    + "games_played INTEGER NOT NULL DEFAULT 0, "
                    + "last_seen INTEGER NOT NULL DEFAULT 0"
                    + ")";
            unlocksSql = "CREATE TABLE IF NOT EXISTS " + this.unlocksTable() + " ("
                    + "uuid TEXT NOT NULL, "
                    + "unlock_type TEXT NOT NULL, "
                    + "unlock_id TEXT NOT NULL, "
                    + "PRIMARY KEY (uuid, unlock_type, unlock_id)"
                    + ")";
        }

        try (Statement statement = this.getConnection().createStatement()) {
            statement.executeUpdate(playersSql);
            statement.executeUpdate(unlocksSql);
        }
    }

    private String sanitizeTablePrefix(final String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "sg_";
        }

        final StringBuilder safe = new StringBuilder();

        for (final char character : prefix.toCharArray()) {
            if (Character.isLetterOrDigit(character) || character == '_') {
                safe.append(character);
            }
        }

        if (safe.length() == 0) {
            return "sg_";
        }

        if (safe.charAt(safe.length() - 1) != '_') {
            safe.append('_');
        }

        return safe.toString();
    }
}
