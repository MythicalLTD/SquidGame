package dev._2lstudios.squidgame.player;

public enum DatabaseType {
    SQLITE,
    MYSQL;

    public static DatabaseType fromConfig(final String value) {
        if (value == null) {
            return SQLITE;
        }

        switch (value.trim().toLowerCase()) {
        case "mysql":
        case "mariadb":
            return MYSQL;
        case "sqlite":
        default:
            return SQLITE;
        }
    }
}
