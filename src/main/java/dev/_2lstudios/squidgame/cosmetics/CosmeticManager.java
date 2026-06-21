package dev._2lstudios.squidgame.cosmetics;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.LobbyItems;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class CosmeticManager {

    private final SquidGame plugin;
    private final Map<UUID, ItemStack> savedHelmets = new HashMap<>();
    private Configuration storage;
    private BukkitTask particleTask;

    public CosmeticManager(final SquidGame plugin) {
        this.plugin = plugin;
        this.loadStorage();
        this.startParticleTask();
    }

    public void shutdown() {
        if (this.particleTask != null) {
            this.particleTask.cancel();
        }

        this.saveStorage();
    }

    public boolean canShowCosmetics(final SquidPlayer player) {
        if (player == null || player.isSpectator()) {
            return false;
        }

        final Arena arena = player.getArena();

        if (arena == null || LobbyItems.hasLobbyAccess(player)) {
            return true;
        }

        if (!this.plugin.getMainConfig().getBoolean("lobby.cosmetics-in-game", true)) {
            return false;
        }

        final ArenaState state = arena.getState();

        return state == ArenaState.IN_GAME || state == ArenaState.INTERMISSION || state == ArenaState.EXPLAIN_GAME
                || state == ArenaState.FINISHING_GAME;
    }

    public CosmeticType getCosmetic(final SquidPlayer player) {
        return CosmeticType.fromId(player.getCosmeticId());
    }

    public void setCosmetic(final SquidPlayer player, final CosmeticType cosmetic) {
        if (!SquidGame.getInstance().getShopManager().ownsCosmetic(player, cosmetic)) {
            MessageUtils.send(this.plugin, player.getBukkitPlayer(), "economy.cosmetic-locked", "{cosmetic}",
                    cosmetic.getDisplayName());
            return;
        }

        final CosmeticType previous = this.getCosmetic(player);
        player.setCosmeticId(cosmetic.getId());
        this.storage.set("players." + player.getBukkitPlayer().getUniqueId(), cosmetic.getId());
        this.storage.safeSave();
        this.clearCosmeticEffects(player, previous);
        this.applyCosmeticEffects(player, cosmetic);
    }

    public String getMusicCosmeticId(final SquidPlayer player) {
        return this.storage.getString("music." + player.getBukkitPlayer().getUniqueId(), "none");
    }

    public void setMusicCosmetic(final SquidPlayer player, final String musicCosmeticId) {
        final String id = musicCosmeticId == null || musicCosmeticId.isEmpty() ? "none" : musicCosmeticId;
        player.setMusicCosmeticId(id);
        this.storage.set("music." + player.getBukkitPlayer().getUniqueId(), id);
        this.storage.safeSave();
    }

    public boolean getMusicMuted(final SquidPlayer player) {
        return this.storage.getBoolean("music-muted." + player.getBukkitPlayer().getUniqueId(), false);
    }

    public void setMusicMuted(final SquidPlayer player, final boolean muted) {
        player.setMusicMuted(muted);
        this.storage.set("music-muted." + player.getBukkitPlayer().getUniqueId(), muted);
        this.storage.safeSave();
    }

    public void loadPlayerMusicCosmetic(final SquidPlayer player) {
        player.setMusicCosmeticId(this.getMusicCosmeticId(player));
        player.setMusicVolume(this.getMusicVolume(player));
        player.setMusicMuted(this.getMusicMuted(player));
    }

    public void savePlayerPreferences(final SquidPlayer player) {
        if (player == null) {
            return;
        }

        final String uuid = player.getBukkitPlayer().getUniqueId().toString();
        this.storage.set("players." + uuid, player.getCosmeticId());
        this.storage.set("music." + uuid, player.getMusicCosmeticId());
        this.storage.set("music-volume." + uuid, player.getMusicVolume());
        this.storage.set("music-muted." + uuid, player.isMusicMuted());
        this.storage.safeSave();
    }

    public int getMusicVolume(final SquidPlayer player) {
        return this.storage.getInt("music-volume." + player.getBukkitPlayer().getUniqueId(), 100);
    }

    public void setMusicVolume(final SquidPlayer player, final int volume) {
        final int clamped = Math.max(10, Math.min(100, volume));
        player.setMusicVolume(clamped);
        this.storage.set("music-volume." + player.getBukkitPlayer().getUniqueId(), clamped);
        this.storage.safeSave();
    }

    public void refreshCosmetic(final SquidPlayer player) {
        final CosmeticType cosmetic = this.getCosmetic(player);
        this.clearCosmeticEffects(player, cosmetic);

        if (this.canShowCosmetics(player)) {
            this.applyCosmeticEffects(player, cosmetic);
        }
    }

    public void refreshCosmeticsForGameStart(final SquidPlayer player) {
        final CosmeticType cosmetic = this.getCosmetic(player);
        this.clearCosmeticEffects(player, cosmetic);

        if (this.canShowCosmetics(player)) {
            this.applyCosmeticEffects(player, cosmetic);
        }
    }

    public void hideCosmeticEffects(final SquidPlayer player) {
        this.clearCosmeticEffects(player, this.getCosmetic(player));
    }

    public void clearCosmeticEffects(final SquidPlayer player) {
        this.clearCosmeticEffects(player, this.getCosmetic(player));
        player.setCosmeticId(CosmeticType.NONE.getId());
    }

    private void clearCosmeticEffects(final SquidPlayer player, final CosmeticType cosmetic) {
        final Player bukkitPlayer = player.getBukkitPlayer();
        final UUID uuid = bukkitPlayer.getUniqueId();

        if (cosmetic.isHat() || this.savedHelmets.containsKey(uuid)) {
            final ItemStack saved = this.savedHelmets.remove(uuid);

            if (saved != null) {
                bukkitPlayer.getInventory().setHelmet(saved);
            } else {
                bukkitPlayer.getInventory().setHelmet(null);
            }

            bukkitPlayer.updateInventory();
        }
    }

    private void applyCosmeticEffects(final SquidPlayer player, final CosmeticType cosmetic) {
        if (cosmetic == CosmeticType.NONE || !this.canShowCosmetics(player)) {
            return;
        }

        final Player bukkitPlayer = player.getBukkitPlayer();

        if (cosmetic.isHat()) {
            if (!LobbyItems.hasLobbyAccess(player)) {
                return;
            }

            final UUID uuid = bukkitPlayer.getUniqueId();

            if (!this.savedHelmets.containsKey(uuid)) {
                final ItemStack current = bukkitPlayer.getInventory().getHelmet();
                this.savedHelmets.put(uuid, current == null ? null : current.clone());
            }

            bukkitPlayer.getInventory().setHelmet(cosmetic.createHatItem());
            bukkitPlayer.updateInventory();
        }
    }

    private void startParticleTask() {
        final long interval = Math.max(4L, this.plugin.getMainConfig().getLong("lobby.cosmetics.particle-interval", 8L));

        this.particleTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (final Player online : Bukkit.getOnlinePlayers()) {
                final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(online);

                if (player == null || !this.canShowCosmetics(player)) {
                    continue;
                }

                final CosmeticType cosmetic = this.getCosmetic(player);

                if (!cosmetic.isHat() && cosmetic != CosmeticType.NONE) {
                    cosmetic.apply(online);
                }
            }
        }, interval, interval);
    }

    private void loadStorage() {
        final File file = new File(this.plugin.getDataFolder(), "cosmetics.yml");
        this.storage = new Configuration(file);

        try {
            if (file.exists()) {
                this.storage.load();
            }
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Could not load cosmetics.yml: " + exception.getMessage());
        }
    }

    private void saveStorage() {
        this.storage.safeSave();
    }

    public void loadPlayerCosmetic(final SquidPlayer player) {
        final String saved = this.storage.getString("players." + player.getBukkitPlayer().getUniqueId(),
                CosmeticType.NONE.getId());
        player.setCosmeticId(saved);
        this.applyCosmeticEffects(player, CosmeticType.fromId(saved));
    }
}
