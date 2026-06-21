package dev._2lstudios.squidgame.player;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.player.PluginPlayer;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.hooks.PlaceholderAPIHook;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;

public class SquidPlayer extends PluginPlayer {

    private Arena arena = null;
    private PlayerWand wand = null;
    private boolean spectator = false;
    private String cosmeticId = "none";
    private String musicCosmeticId = "none";
    private boolean musicMuted = false;
    private int musicVolume = 100;

    private final SquidGame plugin;

    public SquidPlayer(final SquidGame plugin, final Player player) {
        super(player);
        this.plugin = plugin;
    }

    public PlayerWand getWand() {
        return this.wand;
    }

    public PlayerWand createWand(final PlayerWand wand) {
        this.wand = wand;
        return this.wand;
    }

    public Arena getArena() {
        return this.arena;
    }

    public void setArena(final Arena arena) {
        this.arena = arena;
    }

    public boolean isSpectator() {
        return this.spectator;
    }

    public void setSpectator(final boolean result) {
        this.spectator = result;
        if (result) {
            this.getBukkitPlayer().setGameMode(GameMode.SPECTATOR);
        } else {
            this.getBukkitPlayer().setGameMode(GameMode.SURVIVAL);
        }
    }

    public String getCosmeticId() {
        return this.cosmeticId;
    }

    public void setCosmeticId(final String cosmeticId) {
        this.cosmeticId = cosmeticId == null ? "none" : cosmeticId;
    }

    public String getMusicCosmeticId() {
        return this.musicCosmeticId;
    }

    public void setMusicCosmeticId(final String musicCosmeticId) {
        this.musicCosmeticId = musicCosmeticId == null ? "none" : musicCosmeticId;
    }

    public boolean isMusicMuted() {
        return this.musicMuted;
    }

    public void setMusicMuted(final boolean musicMuted) {
        this.musicMuted = musicMuted;
    }

    public int getMusicVolume() {
        return this.musicVolume;
    }

    public void setMusicVolume(final int musicVolume) {
        this.musicVolume = Math.max(10, Math.min(100, musicVolume));
    }

    public void teleportToLobby() {
        if (this.plugin.getMainConfig().getString("lobby.world", "").isEmpty()) {
            return;
        }

        this.teleport(this.plugin.getMainConfig().getLocation("lobby"));
    }

    public String getI18n(final String key) {
        return this.plugin.getMessagesConfig().getString(key);
    }

    private String formatMessage(final String message) {
        final String translatedMessage = this.getI18n(message);
        final String formatColor = ChatColor.translateAlternateColorCodes('&',
                translatedMessage == null
                        ? "§6§lWARNING: §eMissing translation key §7" + message + " §ein message.yml file"
                        : translatedMessage);
        final String replacedVariables = PlaceholderAPIHook.formatString(formatColor, this.getBukkitPlayer());
        return replacedVariables;
    }

    public void sendMessage(final String message) {
        this.getBukkitPlayer().sendMessage(this.formatMessage(message));
    }

    public void sendTitle(final String title, final String subtitle, final int duration) {
        super.sendTitle(this.formatMessage(title), this.formatMessage(subtitle), duration);
    }

    public void sendTitleRaw(final String title, final String subtitle, final int duration) {
        super.sendTitle(title, subtitle, duration);
    }

    public void sendScoreboard(final String scoreboardKey) {
        this.plugin.getScoreboardHook().request(this.getBukkitPlayer(),
                this.plugin.getScoreboardConfig().getStringList(scoreboardKey));
    }

    public void refreshScoreboard() {
        if (this.arena == null) {
            this.sendScoreboard("lobby");
            return;
        }

        this.sendScoreboard(this.arena.getState().toString().toLowerCase());
    }

    @SuppressWarnings("deprecation")
    public void sendActionBar(final String message) {
        CompatibilityUtils.sendActionBar(this.getBukkitPlayer(), message);
    }
}
