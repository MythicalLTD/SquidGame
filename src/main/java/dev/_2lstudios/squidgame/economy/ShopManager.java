package dev._2lstudios.squidgame.economy;

import java.util.List;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.cosmetics.CosmeticType;
import dev._2lstudios.squidgame.music.LobbyMusicCosmetic;
import dev._2lstudios.squidgame.player.PlayerDataManager;
import dev._2lstudios.squidgame.player.PlayerProfile;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class ShopManager {

    private final SquidGame plugin;
    private final PlayerDataManager playerDataManager;

    public ShopManager(final SquidGame plugin, final PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public boolean isEnabled() {
        return this.playerDataManager.isEnabled();
    }

    public boolean canBypass(final SquidPlayer player) {
        if (!this.isEnabled() || player == null) {
            return true;
        }

        if (!this.plugin.getMainConfig().getBoolean("economy.admin-bypass", true)) {
            return false;
        }

        return player.getBukkitPlayer().hasPermission("squidgame.admin");
    }

    public int getCosmeticPrice(final String cosmeticId) {
        if (this.isFreeCosmetic(cosmeticId)) {
            return 0;
        }

        final Configuration config = this.plugin.getMainConfig();
        final int configured = config.getInt("economy.shop.cosmetic-prices." + cosmeticId, -1);

        if (configured >= 0) {
            return configured;
        }

        return config.getInt("economy.shop.cosmetic-default-price", 200);
    }

    public int getMusicUnlockPrice(final String musicId) {
        if (this.isFreeMusic(musicId)) {
            return 0;
        }

        final Configuration config = this.plugin.getMainConfig();
        final int configured = config.getInt("economy.shop.music-prices." + musicId, -1);

        if (configured >= 0) {
            return configured;
        }

        return config.getInt("economy.shop.music-unlock-default-price", 400);
    }

    public int getMusicPlayCost(final String musicId) {
        final Configuration config = this.plugin.getMainConfig();
        final int configured = config.getInt("economy.shop.music-play-costs." + musicId, -1);

        if (configured >= 0) {
            return configured;
        }

        return config.getInt("economy.shop.music-play-cost", 25);
    }

    public boolean ownsCosmetic(final SquidPlayer player, final CosmeticType cosmetic) {
        if (!this.isEnabled() || this.canBypass(player) || cosmetic == CosmeticType.NONE) {
            return true;
        }

        if (this.getCosmeticPrice(cosmetic.getId()) <= 0) {
            return true;
        }

        final PlayerProfile profile = this.playerDataManager.getProfile(player.getBukkitPlayer().getUniqueId());
        return profile != null && profile.hasUnlock(UnlockType.COSMETIC, cosmetic.getId());
    }

    public boolean ownsMusic(final SquidPlayer player, final LobbyMusicCosmetic cosmetic) {
        if (!this.isEnabled() || this.canBypass(player) || cosmetic == null) {
            return true;
        }

        if (this.getMusicUnlockPrice(cosmetic.getId()) <= 0) {
            return true;
        }

        final PlayerProfile profile = this.playerDataManager.getProfile(player.getBukkitPlayer().getUniqueId());
        return profile != null && profile.hasUnlock(UnlockType.MUSIC, cosmetic.getId());
    }

    public boolean tryPurchaseCosmetic(final SquidPlayer player, final CosmeticType cosmetic) {
        if (this.ownsCosmetic(player, cosmetic)) {
            return true;
        }

        final PlayerProfile profile = this.playerDataManager.getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.profile-loading");
            return false;
        }

        final int price = this.getCosmeticPrice(cosmetic.getId());

        if (price <= 0) {
            profile.unlock(UnlockType.COSMETIC, cosmetic.getId());
            this.playerDataManager.saveProfile(profile);
            return true;
        }

        if (profile.getCoins() < price) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.not-enough-coins", "{coins}",
                    String.valueOf(profile.getCoins()), "{price}", String.valueOf(price));
            return false;
        }

        profile.removeCoins(price);
        profile.unlock(UnlockType.COSMETIC, cosmetic.getId());
        this.playerDataManager.saveProfile(profile);
        MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.purchased-cosmetic", "{item}",
                cosmetic.getDisplayName(), "{price}", String.valueOf(price), "{coins}",
                String.valueOf(profile.getCoins()));
        return true;
    }

    public boolean tryPurchaseMusic(final SquidPlayer player, final LobbyMusicCosmetic cosmetic) {
        if (this.ownsMusic(player, cosmetic)) {
            return true;
        }

        final PlayerProfile profile = this.playerDataManager.getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.profile-loading");
            return false;
        }

        final int price = this.getMusicUnlockPrice(cosmetic.getId());

        if (price <= 0) {
            profile.unlock(UnlockType.MUSIC, cosmetic.getId());
            this.playerDataManager.saveProfile(profile);
            return true;
        }

        if (profile.getCoins() < price) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.not-enough-coins", "{coins}",
                    String.valueOf(profile.getCoins()), "{price}", String.valueOf(price));
            return false;
        }

        profile.removeCoins(price);
        profile.unlock(UnlockType.MUSIC, cosmetic.getId());
        this.playerDataManager.saveProfile(profile);
        MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.purchased-music", "{item}",
                cosmetic.getDisplayName(), "{price}", String.valueOf(price), "{coins}",
                String.valueOf(profile.getCoins()));
        return true;
    }

    public boolean tryChargeMusicPlay(final SquidPlayer player, final LobbyMusicCosmetic cosmetic) {
        if (!this.isEnabled() || this.canBypass(player) || cosmetic == null) {
            return true;
        }

        final int playCost = this.getMusicPlayCost(cosmetic.getId());

        if (playCost <= 0) {
            return true;
        }

        final PlayerProfile profile = this.playerDataManager.getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.profile-loading");
            return false;
        }

        if (profile.getCoins() < playCost) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.not-enough-play-coins", "{coins}",
                    String.valueOf(profile.getCoins()), "{price}", String.valueOf(playCost));
            return false;
        }

        profile.removeCoins(playCost);
        this.playerDataManager.saveProfile(profile);
        MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.charged-play", "{price}",
                String.valueOf(playCost), "{coins}", String.valueOf(profile.getCoins()));
        return true;
    }

    public int getPlayerCoins(final SquidPlayer player) {
        if (player == null) {
            return 0;
        }

        final PlayerProfile profile = this.playerDataManager.getProfile(player.getBukkitPlayer().getUniqueId());
        return profile == null ? 0 : profile.getCoins();
    }

    private boolean isFreeCosmetic(final String cosmeticId) {
        if (cosmeticId == null || cosmeticId.isEmpty() || "none".equalsIgnoreCase(cosmeticId)) {
            return true;
        }

        final List<String> freeList = this.plugin.getMainConfig().getStringList("economy.shop.free-cosmetics");

        for (final String freeId : freeList) {
            if (freeId != null && freeId.equalsIgnoreCase(cosmeticId)) {
                return true;
            }
        }

        return false;
    }

    private boolean isFreeMusic(final String musicId) {
        if (musicId == null || musicId.isEmpty() || "none".equalsIgnoreCase(musicId)) {
            return true;
        }

        final List<String> freeList = this.plugin.getMainConfig().getStringList("economy.shop.free-music");

        for (final String freeId : freeList) {
            if (freeId != null && freeId.equalsIgnoreCase(musicId)) {
                return true;
            }
        }

        return false;
    }
}
