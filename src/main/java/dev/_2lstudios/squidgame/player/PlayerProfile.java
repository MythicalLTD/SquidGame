package dev._2lstudios.squidgame.player;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import dev._2lstudios.squidgame.economy.UnlockType;

public class PlayerProfile {

    private final UUID uuid;
    private String username;
    private int coins;
    private int wins;
    private int deaths;
    private int gamesPlayed;
    private final Set<String> unlockedCosmetics = new HashSet<>();
    private final Set<String> unlockedMusic = new HashSet<>();
    private volatile boolean dirty;

    public PlayerProfile(final UUID uuid, final String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        if (username != null && !username.equals(this.username)) {
            this.username = username;
            this.dirty = true;
        }
    }

    public int getCoins() {
        return this.coins;
    }

    public void setCoins(final int coins) {
        this.coins = Math.max(0, coins);
        this.dirty = true;
    }

    public void addCoins(final int amount) {
        if (amount <= 0) {
            return;
        }

        this.coins += amount;
        this.dirty = true;
    }

    public boolean removeCoins(final int amount) {
        if (amount <= 0) {
            return true;
        }

        if (this.coins < amount) {
            return false;
        }

        this.coins -= amount;
        this.dirty = true;
        return true;
    }

    public int getWins() {
        return this.wins;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public int getGamesPlayed() {
        return this.gamesPlayed;
    }

    public void recordWin() {
        this.wins++;
        this.dirty = true;
    }

    public void recordDeath() {
        this.deaths++;
        this.dirty = true;
    }

    public void recordGamePlayed() {
        this.gamesPlayed++;
        this.dirty = true;
    }

    public boolean hasUnlock(final UnlockType type, final String id) {
        if (id == null || id.isEmpty()) {
            return true;
        }

        switch (type) {
        case COSMETIC:
            return this.unlockedCosmetics.contains(id.toLowerCase());
        case MUSIC:
            return this.unlockedMusic.contains(id.toLowerCase());
        default:
            return false;
        }
    }

    public void unlock(final UnlockType type, final String id) {
        if (id == null || id.isEmpty()) {
            return;
        }

        final String normalized = id.toLowerCase();
        final Set<String> target = this.getUnlockSet(type);

        if (target.add(normalized)) {
            this.dirty = true;
        }
    }

    public Set<String> getUnlockedCosmetics() {
        return new HashSet<>(this.unlockedCosmetics);
    }

    public Set<String> getUnlockedMusic() {
        return new HashSet<>(this.unlockedMusic);
    }

    public void setUnlockedCosmetics(final Set<String> unlocks) {
        this.unlockedCosmetics.clear();

        if (unlocks != null) {
            for (final String unlock : unlocks) {
                if (unlock != null && !unlock.isEmpty()) {
                    this.unlockedCosmetics.add(unlock.toLowerCase());
                }
            }
        }
    }

    public void setUnlockedMusic(final Set<String> unlocks) {
        this.unlockedMusic.clear();

        if (unlocks != null) {
            for (final String unlock : unlocks) {
                if (unlock != null && !unlock.isEmpty()) {
                    this.unlockedMusic.add(unlock.toLowerCase());
                }
            }
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void loadStats(final int coins, final int wins, final int deaths, final int gamesPlayed) {
        this.coins = Math.max(0, coins);
        this.wins = Math.max(0, wins);
        this.deaths = Math.max(0, deaths);
        this.gamesPlayed = Math.max(0, gamesPlayed);
        this.dirty = false;
    }

    private Set<String> getUnlockSet(final UnlockType type) {
        switch (type) {
        case COSMETIC:
            return this.unlockedCosmetics;
        case MUSIC:
            return this.unlockedMusic;
        default:
            throw new IllegalArgumentException("Unknown unlock type: " + type);
        }
    }

    public static Set<UnlockType> allUnlockTypes() {
        return EnumSet.allOf(UnlockType.class);
    }
}
