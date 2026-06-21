package dev._2lstudios.squidgame.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import dev._2lstudios.jelly.config.Configuration;
import org.bukkit.Bukkit;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.economy.UnlockType;

public class PlayerDataManager {

    private final SquidGame plugin;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final Object connectionLock = new Object();
    private PlayerDatabase database;

    public PlayerDataManager(final SquidGame plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (!this.isEnabled()) {
            return;
        }

        this.database = new PlayerDatabase(this.plugin);

        try {
            this.database.initialize();
        } catch (final Exception exception) {
            this.plugin.getLogger().severe("Could not initialize player database: " + exception.getMessage());
            exception.printStackTrace();
            this.database = null;
        }
    }

    public void shutdown() {
        for (final PlayerProfile profile : this.cache.values()) {
            this.saveProfileSync(profile);
        }

        this.cache.clear();

        if (this.database != null) {
            this.database.close();
            this.database = null;
        }
    }

    public boolean isEnabled() {
        return this.plugin.getMainConfig().getBoolean("economy.enabled", true);
    }

    public boolean isDatabaseReady() {
        return this.database != null;
    }

    public DatabaseType getDatabaseType() {
        return this.database == null ? DatabaseType.SQLITE : this.database.getType();
    }

    public PlayerProfile getProfile(final UUID uuid) {
        return this.cache.get(uuid);
    }

    public void loadPlayer(final UUID uuid, final String username, final Consumer<PlayerProfile> callback) {
        if (!this.isEnabled() || !this.isDatabaseReady()) {
            if (callback != null) {
                callback.accept(null);
            }

            return;
        }

        final PlayerProfile cached = this.cache.get(uuid);

        if (cached != null) {
            cached.setUsername(username);

            if (callback != null) {
                callback.accept(cached);
            }

            return;
        }

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final PlayerProfile profile = this.loadProfileFromDatabase(uuid, username);
            this.cache.put(uuid, profile);

            if (callback != null) {
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> callback.accept(profile));
            }
        });
    }

    public void unloadPlayer(final UUID uuid) {
        if (!this.isEnabled() || !this.isDatabaseReady()) {
            return;
        }

        final PlayerProfile profile = this.cache.remove(uuid);

        if (profile == null) {
            return;
        }

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveProfileSync(profile));
    }

    public void saveProfile(final PlayerProfile profile) {
        if (!this.isEnabled() || !this.isDatabaseReady() || profile == null || !profile.isDirty()) {
            return;
        }

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.saveProfileSync(profile));
    }

    public void saveProfileNow(final PlayerProfile profile) {
        if (!this.isEnabled() || !this.isDatabaseReady() || profile == null) {
            return;
        }

        this.saveProfileSync(profile);
    }

    public void recordWin(final SquidPlayer player, final int coinReward) {
        final PlayerProfile profile = this.getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            return;
        }

        if (this.plugin.getMainConfig().getBoolean("economy.stats.record-wins", true)) {
            profile.recordWin();
        }

        if (coinReward > 0) {
            profile.addCoins(coinReward);
        }

        this.saveProfile(profile);
    }

    public void recordDeath(final SquidPlayer player) {
        if (!this.plugin.getMainConfig().getBoolean("economy.stats.record-deaths", true)) {
            return;
        }

        final PlayerProfile profile = this.getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            return;
        }

        profile.recordDeath();
        this.saveProfile(profile);
    }

    public void recordGamePlayed(final SquidPlayer player, final int coinReward) {
        final PlayerProfile profile = this.getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            return;
        }

        if (this.plugin.getMainConfig().getBoolean("economy.stats.record-games-played", true)) {
            profile.recordGamePlayed();
        }

        if (coinReward > 0) {
            profile.addCoins(coinReward);
        }

        this.saveProfile(profile);
    }

    private PlayerProfile loadProfileFromDatabase(final UUID uuid, final String username) {
        final Configuration config = this.plugin.getMainConfig();
        PlayerProfile profile = null;

        synchronized (this.connectionLock) {
            try {
                profile = this.selectProfile(uuid);
            } catch (final SQLException exception) {
                this.plugin.getLogger().severe("Failed to load player profile for " + uuid + ": "
                        + exception.getMessage());
            }
        }

        if (profile == null) {
            profile = new PlayerProfile(uuid, username);
            profile.setCoins(config.getInt("economy.starting-coins", 100));
            profile.markDirty();
            this.insertProfile(profile);
        } else {
            profile.setUsername(username);

            try {
                this.loadUnlocks(profile);
            } catch (final SQLException exception) {
                this.plugin.getLogger().severe("Failed to load unlocks for " + uuid + ": " + exception.getMessage());
            }
        }

        return profile;
    }

    private PlayerProfile selectProfile(final UUID uuid) throws SQLException {
        final String sql = "SELECT username, coins, wins, deaths, games_played FROM " + this.database.playersTable()
                + " WHERE uuid = ?";

        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                final PlayerProfile profile = new PlayerProfile(uuid, result.getString("username"));
                profile.loadStats(result.getInt("coins"), result.getInt("wins"), result.getInt("deaths"),
                        result.getInt("games_played"));
                return profile;
            }
        }
    }

    private void loadUnlocks(final PlayerProfile profile) throws SQLException {
        final String sql = "SELECT unlock_type, unlock_id FROM " + this.database.unlocksTable() + " WHERE uuid = ?";
        final Set<String> cosmetics = new HashSet<>();
        final Set<String> music = new HashSet<>();

        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.getUuid().toString());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final String type = result.getString("unlock_type");
                    final String id = result.getString("unlock_id");

                    if (UnlockType.COSMETIC.getKey().equalsIgnoreCase(type)) {
                        cosmetics.add(id);
                    } else if (UnlockType.MUSIC.getKey().equalsIgnoreCase(type)) {
                        music.add(id);
                    }
                }
            }
        }

        profile.setUnlockedCosmetics(cosmetics);
        profile.setUnlockedMusic(music);
        profile.markClean();
    }

    private void insertProfile(final PlayerProfile profile) {
        synchronized (this.connectionLock) {
            try (Connection connection = this.database.getConnection();
                    PreparedStatement statement = connection.prepareStatement(this.database.insertPlayerIgnoreSql())) {
                statement.setString(1, profile.getUuid().toString());
                statement.setString(2, profile.getUsername());
                statement.setInt(3, profile.getCoins());
                statement.setInt(4, profile.getWins());
                statement.setInt(5, profile.getDeaths());
                statement.setInt(6, profile.getGamesPlayed());
                statement.setLong(7, System.currentTimeMillis());
                statement.executeUpdate();

                this.saveUnlocks(profile, connection);
                profile.markClean();
                this.scheduleScoreboardRefresh(profile.getUuid());
            } catch (final SQLException exception) {
                this.plugin.getLogger().severe("Failed to create player profile for " + profile.getUuid() + ": "
                        + exception.getMessage());
            }
        }
    }

    private void saveProfileSync(final PlayerProfile profile) {
        synchronized (this.connectionLock) {
            try (Connection connection = this.database.getConnection();
                    PreparedStatement statement = connection.prepareStatement(this.database.upsertPlayerSql())) {
                statement.setString(1, profile.getUuid().toString());
                statement.setString(2, profile.getUsername());
                statement.setInt(3, profile.getCoins());
                statement.setInt(4, profile.getWins());
                statement.setInt(5, profile.getDeaths());
                statement.setInt(6, profile.getGamesPlayed());
                statement.setLong(7, System.currentTimeMillis());
                statement.executeUpdate();

                this.saveUnlocks(profile, connection);
                profile.markClean();
                this.scheduleScoreboardRefresh(profile.getUuid());
            } catch (final SQLException exception) {
                this.plugin.getLogger().severe("Failed to save player profile for " + profile.getUuid() + ": "
                        + exception.getMessage());
            }
        }
    }

    private void saveUnlocks(final PlayerProfile profile, final Connection connection) throws SQLException {
        final String deleteSql = "DELETE FROM " + this.database.unlocksTable() + " WHERE uuid = ?";

        try (PreparedStatement delete = connection.prepareStatement(deleteSql)) {
            delete.setString(1, profile.getUuid().toString());
            delete.executeUpdate();
        }

        final String insertSql = "INSERT INTO " + this.database.unlocksTable()
                + " (uuid, unlock_type, unlock_id) VALUES (?, ?, ?)";

        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (final String cosmeticId : profile.getUnlockedCosmetics()) {
                insert.setString(1, profile.getUuid().toString());
                insert.setString(2, UnlockType.COSMETIC.getKey());
                insert.setString(3, cosmeticId);
                insert.addBatch();
            }

            for (final String musicId : profile.getUnlockedMusic()) {
                insert.setString(1, profile.getUuid().toString());
                insert.setString(2, UnlockType.MUSIC.getKey());
                insert.setString(3, musicId);
                insert.addBatch();
            }

            insert.executeBatch();
        }
    }

    private void scheduleScoreboardRefresh(final UUID uuid) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            final org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                return;
            }

            final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(player);

            if (squidPlayer != null) {
                squidPlayer.refreshScoreboard();
            }
        });
    }
}
