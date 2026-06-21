package dev._2lstudios.squidgame.music;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.LobbyItems;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class NbsMusicManager {

    public static final String MAIN_LOBBY_SCOPE = "main";

    private final SquidGame plugin;
    private final Map<String, SquidNbsSongPlayer> sharedLobbyPlayers;
    private final Map<String, SquidNbsSongPlayer> minglePlayers;
    private final Map<String, SquidNbsSongPlayer> sharedCosmeticPlayers;
    private final Map<UUID, Long> cosmeticCooldowns;
    private final LobbyMusicCosmeticRegistry cosmeticRegistry;

    private Song lobbySong;
    private Song mingleSong;

    public NbsMusicManager(final SquidGame plugin) {
        this.plugin = plugin;
        this.sharedLobbyPlayers = new HashMap<>();
        this.minglePlayers = new HashMap<>();
        this.sharedCosmeticPlayers = new HashMap<>();
        this.cosmeticCooldowns = new HashMap<>();
        this.cosmeticRegistry = new LobbyMusicCosmeticRegistry(plugin);
        this.reloadSongs();
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshDefaultMusicForEveryone, 20L);
    }

    public void reloadSongs() {
        this.ensureSongFiles();
        this.lobbySong = this.loadSong("lobby.nbs");
        this.mingleSong = this.loadSong("mingle.nbs");
        this.cosmeticRegistry.reload();
    }

    public LobbyMusicCosmeticRegistry getCosmeticRegistry() {
        return this.cosmeticRegistry;
    }

    public void shutdown() {
        for (final SquidNbsSongPlayer player : new HashMap<>(this.sharedLobbyPlayers).values()) {
            player.stop();
        }

        for (final SquidNbsSongPlayer player : new HashMap<>(this.minglePlayers).values()) {
            player.stop();
        }

        for (final SquidNbsSongPlayer player : new HashMap<>(this.sharedCosmeticPlayers).values()) {
            player.stop();
        }

        this.sharedLobbyPlayers.clear();
        this.minglePlayers.clear();
        this.sharedCosmeticPlayers.clear();
        this.cosmeticCooldowns.clear();
    }

    public void refreshDefaultMusicForEveryone() {
        for (final Player online : Bukkit.getOnlinePlayers()) {
            final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(online);

            if (squidPlayer != null) {
                this.refreshLobbyMusic(squidPlayer);
            }
        }
    }

    public void refreshLobbyMusic(final SquidPlayer player) {
        final String scope = this.getMusicScope(player);

        this.removePlayerFromOtherScopeMusic(player, scope);

        if (scope == null) {
            this.removePlayerFromAllScopeLobbyMusic(player);
            return;
        }

        final SquidNbsSongPlayer sharedCosmetic = this.sharedCosmeticPlayers.get(scope);

        if (sharedCosmetic != null && sharedCosmetic.isPlaying()) {
            this.removePlayerFromScopeLobbyMusic(player, scope);
            this.syncPlayerToSharedMusic(player, sharedCosmetic, this.getListenerCosmeticVolume(player, false));
            return;
        }

        if (sharedCosmetic != null) {
            this.sharedCosmeticPlayers.remove(scope);
        }

        this.addPlayerToScopeLobbyMusic(player, scope);
    }

    public void stopLobbyMusic(final SquidPlayer player) {
        this.removePlayerFromAllScopeLobbyMusic(player);
        this.removePlayerFromAllScopeCosmeticMusic(player);
    }

    public void stopLobbyMusicForArena(final Arena arena) {
        final String scope = this.getArenaScope(arena);
        this.stopSharedCosmeticMusic(scope);
        this.stopScopeLobbyMusic(scope);
    }

    public void pauseArenaMusic(final Arena arena) {
        if (arena == null) {
            return;
        }

        final String scope = this.getArenaScope(arena);

        for (final SquidPlayer player : arena.getAllPlayers()) {
            this.detachPlayerFromScopeMusic(player, scope);
        }
    }

    public void resumeArenaMusic(final Arena arena) {
        if (arena == null) {
            return;
        }

        for (final SquidPlayer player : arena.getAllPlayers()) {
            this.refreshLobbyMusic(player);
        }
    }

    private void detachPlayerFromScopeMusic(final SquidPlayer player, final String scope) {
        if (player == null || scope == null) {
            return;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();
        final SquidNbsSongPlayer lobby = this.sharedLobbyPlayers.get(scope);

        if (lobby != null) {
            lobby.removePlayer(uuid);
        }

        final SquidNbsSongPlayer cosmetic = this.sharedCosmeticPlayers.get(scope);

        if (cosmetic != null) {
            cosmetic.removePlayer(uuid);
        }
    }

    public boolean toggleMusicMuted(final SquidPlayer player) {
        this.plugin.getCosmeticManager().setMusicMuted(player, !player.isMusicMuted());
        this.refreshLobbyMusic(player);
        player.sendMessage(player.isMusicMuted() ? "music.muted" : "music.unmuted");
        LobbyItems.updateMusicMenuItem(player);

        return player.isMusicMuted();
    }

    public long getCosmeticCooldownRemaining(final SquidPlayer player) {
        final Long lastPlayed = this.cosmeticCooldowns.get(player.getBukkitPlayer().getUniqueId());

        if (lastPlayed == null) {
            return 0L;
        }

        final long cooldownMs = this.getCosmeticCooldownSeconds() * 1000L;
        final long remaining = cooldownMs - (System.currentTimeMillis() - lastPlayed);

        return Math.max(0L, remaining);
    }

    public boolean tryPlayCosmeticMusic(final SquidPlayer requester, final LobbyMusicCosmetic cosmetic) {
        return this.playCosmeticMusic(requester, cosmetic, false);
    }

    public boolean playCosmeticMusic(final SquidPlayer requester, final LobbyMusicCosmetic cosmetic,
            final boolean forceEveryone) {
        if (!this.isCosmeticMusicEnabled() || requester == null) {
            return false;
        }

        final String scope = this.getMusicScope(requester);

        if (scope == null) {
            MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "music.cosmetic.not-waiting");
            return false;
        }

        if (cosmetic == null) {
            this.plugin.getCosmeticManager().setMusicCosmetic(requester, "none");
            this.stopSharedCosmeticMusic(scope);
            this.refreshLobbyMusicForScope(scope);
            MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "music.cosmetic.cleared");
            return true;
        }

        if (!this.plugin.getShopManager().ownsMusic(requester, cosmetic)) {
            MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "economy.music-locked", "{track}",
                    cosmetic.getDisplayName());
            return false;
        }

        if (!this.plugin.getShopManager().tryChargeMusicPlay(requester, cosmetic)) {
            return false;
        }

        final boolean bypassCooldown = this.canBypassMusicCooldown(requester);

        if (!bypassCooldown) {
            final long cooldownRemaining = this.getCosmeticCooldownRemaining(requester);

            if (cooldownRemaining > 0L) {
                MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "music.cosmetic.cooldown", "{seconds}",
                        String.valueOf((cooldownRemaining + 999L) / 1000L));
                return false;
            }
        }

        final List<SquidPlayer> listeners = this.getMusicListeners(requester);

        if (listeners.isEmpty()) {
            MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "music.cosmetic.no-listeners");
            return false;
        }

        this.plugin.getCosmeticManager().setMusicCosmetic(requester, cosmetic.getId());
        this.stopSharedCosmeticMusic(scope);

        for (final SquidPlayer listener : listeners) {
            this.removePlayerFromScopeLobbyMusic(listener, scope);
        }

        final SquidNbsSongPlayer songPlayer = new SquidNbsSongPlayer(this.plugin, cosmetic.getSong(),
                this.getCosmeticVolume(), this.getVolumeMultiplier(), this.getTempoMultiplier());
        final List<SquidPlayer> activeListeners = new ArrayList<>();

        for (final SquidPlayer listener : listeners) {
            final UUID uuid = listener.getBukkitPlayer().getUniqueId();
            final byte listenerVolume = this.getListenerCosmeticVolume(listener, forceEveryone);
            songPlayer.addPlayer(listener.getBukkitPlayer(), listenerVolume);

            if (listenerVolume > 0) {
                activeListeners.add(listener);
            }
        }

        if (activeListeners.isEmpty()) {
            MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "music.cosmetic.no-listeners");
            this.refreshLobbyMusicForScope(scope);
            return false;
        }

        if (!bypassCooldown) {
            this.cosmeticCooldowns.put(requester.getBukkitPlayer().getUniqueId(), System.currentTimeMillis());
        }

        this.sharedCosmeticPlayers.put(scope, songPlayer);
        songPlayer.start(false, () -> this.handleSharedCosmeticFinished(scope));
        this.broadcastNowPlaying(requester, cosmetic, activeListeners, forceEveryone);

        if (forceEveryone) {
            MessageUtils.send(this.plugin, requester.getBukkitPlayer(), "music.admin.forced");
        }

        return true;
    }

    public void stopAllMusicInScope(final SquidPlayer context) {
        final String scope = this.getMusicScope(context);

        if (scope == null) {
            return;
        }

        this.stopSharedCosmeticMusic(scope);
        this.refreshLobbyMusicForScope(scope);
        MessageUtils.send(this.plugin, context.getBukkitPlayer(), "music.admin.stopped");
    }

    public void setPlayerMusicVolume(final SquidPlayer player, final int volume) {
        this.plugin.getCosmeticManager().setMusicVolume(player, volume);
        this.updateActiveSongVolume(player);

        if (!player.isMusicMuted()) {
            this.refreshLobbyMusic(player);
        }
    }

    public void adjustPlayerMusicVolume(final SquidPlayer player, final int delta) {
        this.setPlayerMusicVolume(player, player.getMusicVolume() + delta);
    }

    public boolean canBypassMusicCooldown(final SquidPlayer player) {
        return player != null && player.getBukkitPlayer().hasPermission("squidgame.admin");
    }

    private void updateActiveSongVolume(final SquidPlayer player) {
        final UUID uuid = player.getBukkitPlayer().getUniqueId();
        final String scope = this.getMusicScope(player);

        if (scope == null) {
            return;
        }

        final SquidNbsSongPlayer lobby = this.sharedLobbyPlayers.get(scope);

        if (lobby != null && lobby.hasPlayer(uuid)) {
            lobby.setPlayerVolume(uuid, this.getListenerLobbyVolume(player));
        }

        final SquidNbsSongPlayer shared = this.sharedCosmeticPlayers.get(scope);

        if (shared != null && shared.hasPlayer(uuid)) {
            shared.setPlayerVolume(uuid, this.getListenerCosmeticVolume(player, false));
        }
    }

    private byte getListenerLobbyVolume(final SquidPlayer player) {
        if (player.isMusicMuted()) {
            return 0;
        }

        return this.getEffectiveLobbyVolume(player);
    }

    private byte getListenerCosmeticVolume(final SquidPlayer player, final boolean forceEveryone) {
        if (player.isMusicMuted() && !forceEveryone) {
            return 0;
        }

        return this.getEffectiveCosmeticVolume(player);
    }

    private byte getEffectiveLobbyVolume(final SquidPlayer player) {
        return (byte) Math.max(1, this.getLobbyVolume() * player.getMusicVolume() / 100);
    }

    private byte getEffectiveCosmeticVolume(final SquidPlayer player) {
        return (byte) Math.max(1, this.getCosmeticVolume() * player.getMusicVolume() / 100);
    }

    public void playMingleMusic(final Arena arena) {
        if (arena == null || this.mingleSong == null || !this.isMingleMusicEnabled()) {
            return;
        }

        this.stopMingleMusic(arena);

        final SquidNbsSongPlayer songPlayer = new SquidNbsSongPlayer(this.plugin, this.mingleSong,
                this.getMingleVolume(), this.getVolumeMultiplier(), this.getTempoMultiplier());

        for (final SquidPlayer player : arena.getPlayers()) {
            songPlayer.addPlayer(player.getBukkitPlayer());
        }

        songPlayer.start(false);
        this.minglePlayers.put(arena.getName(), songPlayer);
    }

    public void stopMingleMusic(final Arena arena) {
        if (arena == null) {
            return;
        }

        final SquidNbsSongPlayer songPlayer = this.minglePlayers.remove(arena.getName());

        if (songPlayer != null) {
            songPlayer.stop();
        }
    }

    public String getMusicScope(final SquidPlayer player) {
        final Arena arena = player.getArena();

        if (arena == null) {
            return MAIN_LOBBY_SCOPE;
        }

        if (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING
                || arena.getState() == ArenaState.INTERMISSION
                || arena.getState() == ArenaState.EXPLAIN_GAME
                || this.shouldKeepArenaLobbyMusic(arena)) {
            return this.getArenaScope(arena);
        }

        return null;
    }

    private boolean shouldKeepArenaLobbyMusic(final Arena arena) {
        return arena.getState() == ArenaState.IN_GAME && arena.getCurrentGame() != null
                && arena.getCurrentGame().keepsGameLobbyFeatures();
    }

    private String getArenaScope(final Arena arena) {
        return "arena:" + arena.getName();
    }

    private List<SquidPlayer> getMusicListeners(final SquidPlayer context) {
        final List<SquidPlayer> listeners = new ArrayList<>();
        final Arena arena = context.getArena();

        if (arena == null) {
            for (final Player online : Bukkit.getOnlinePlayers()) {
                final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(online);

                if (squidPlayer != null && squidPlayer.getArena() == null) {
                    listeners.add(squidPlayer);
                }
            }

            return listeners;
        }

        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING
                && arena.getState() != ArenaState.INTERMISSION
                && arena.getState() != ArenaState.EXPLAIN_GAME
                && !this.shouldKeepArenaLobbyMusic(arena)) {
            return listeners;
        }

        for (final SquidPlayer squidPlayer : arena.getAllPlayers()) {
            listeners.add(squidPlayer);
        }

        return listeners;
    }

    private void handleSharedCosmeticFinished(final String scope) {
        this.sharedCosmeticPlayers.remove(scope);
        this.refreshLobbyMusicForScope(scope);
    }

    private void refreshLobbyMusicForScope(final String scope) {
        if (MAIN_LOBBY_SCOPE.equals(scope)) {
            for (final Player online : Bukkit.getOnlinePlayers()) {
                final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(online);

                if (squidPlayer != null && squidPlayer.getArena() == null) {
                    this.refreshLobbyMusic(squidPlayer);
                }
            }

            return;
        }

        if (!scope.startsWith("arena:")) {
            return;
        }

        final String arenaName = scope.substring("arena:".length());
        final Arena arena = this.plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            return;
        }

        for (final SquidPlayer squidPlayer : arena.getAllPlayers()) {
            this.refreshLobbyMusic(squidPlayer);
        }
    }

    private void addPlayerToScopeLobbyMusic(final SquidPlayer player, final String scope) {
        if (player == null || scope == null || this.lobbySong == null || !this.isLobbyMusicEnabled()) {
            this.removePlayerFromScopeLobbyMusic(player, scope);
            return;
        }

        final SquidNbsSongPlayer activeDj = this.sharedCosmeticPlayers.get(scope);

        if (activeDj != null && activeDj.isPlaying()) {
            this.removePlayerFromScopeLobbyMusic(player, scope);
            this.syncPlayerToSharedMusic(player, activeDj, this.getListenerCosmeticVolume(player, false));
            return;
        }

        SquidNbsSongPlayer shared = this.sharedLobbyPlayers.get(scope);
        final UUID uuid = player.getBukkitPlayer().getUniqueId();

        if (shared != null && !shared.isPlaying()) {
            this.sharedLobbyPlayers.remove(scope);
            shared = null;
        }

        if (shared == null) {
            shared = new SquidNbsSongPlayer(this.plugin, this.lobbySong, this.getLobbyVolume(),
                    this.getVolumeMultiplier(), this.getTempoMultiplier());
            this.sharedLobbyPlayers.put(scope, shared);
            shared.start(true);
        }

        if (!shared.hasPlayer(uuid)) {
            shared.addPlayer(player.getBukkitPlayer(), this.getListenerLobbyVolume(player));
        } else {
            shared.setPlayerVolume(uuid, this.getListenerLobbyVolume(player));
        }
    }

    private void removePlayerFromScopeLobbyMusic(final SquidPlayer player, final String scope) {
        if (player == null || scope == null) {
            return;
        }

        final SquidNbsSongPlayer shared = this.sharedLobbyPlayers.get(scope);

        if (shared == null) {
            return;
        }

        shared.removePlayer(player.getBukkitPlayer().getUniqueId());
    }

    private void removePlayerFromAllScopeLobbyMusic(final SquidPlayer player) {
        if (player == null) {
            return;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();

        for (final String scope : new java.util.ArrayList<>(this.sharedLobbyPlayers.keySet())) {
            final SquidNbsSongPlayer shared = this.sharedLobbyPlayers.get(scope);

            if (shared != null && shared.hasPlayer(uuid)) {
                shared.removePlayer(uuid);
            }
        }
    }

    private void removePlayerFromAllScopeCosmeticMusic(final SquidPlayer player) {
        if (player == null) {
            return;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();

        for (final String scope : new java.util.ArrayList<>(this.sharedCosmeticPlayers.keySet())) {
            final SquidNbsSongPlayer shared = this.sharedCosmeticPlayers.get(scope);

            if (shared != null && shared.hasPlayer(uuid)) {
                shared.removePlayer(uuid);
            }
        }
    }

    private void removePlayerFromOtherScopeMusic(final SquidPlayer player, final String activeScope) {
        if (player == null) {
            return;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();

        for (final String scope : new java.util.ArrayList<>(this.sharedLobbyPlayers.keySet())) {
            if (scope.equals(activeScope)) {
                continue;
            }

            final SquidNbsSongPlayer shared = this.sharedLobbyPlayers.get(scope);

            if (shared != null && shared.hasPlayer(uuid)) {
                shared.removePlayer(uuid);
            }
        }

        for (final String scope : new java.util.ArrayList<>(this.sharedCosmeticPlayers.keySet())) {
            if (scope.equals(activeScope)) {
                continue;
            }

            final SquidNbsSongPlayer shared = this.sharedCosmeticPlayers.get(scope);

            if (shared != null && shared.hasPlayer(uuid)) {
                shared.removePlayer(uuid);
            }
        }
    }

    private void stopScopeLobbyMusic(final String scope) {
        final SquidNbsSongPlayer shared = this.sharedLobbyPlayers.remove(scope);

        if (shared != null) {
            shared.stop();
        }
    }

    private void syncPlayerToSharedMusic(final SquidPlayer player, final SquidNbsSongPlayer shared,
            final byte volume) {
        if (player == null || shared == null || !shared.isPlaying()) {
            return;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();
        final byte listenerVolume = player.isMusicMuted() ? 0 : volume;

        if (!shared.hasPlayer(uuid)) {
            shared.addPlayer(player.getBukkitPlayer(), listenerVolume);
            return;
        }

        shared.setPlayerVolume(uuid, listenerVolume);
    }

    private void stopSharedCosmeticMusic(final String scope) {
        final SquidNbsSongPlayer current = this.sharedCosmeticPlayers.remove(scope);

        if (current != null) {
            current.stop();
        }
    }

    private void broadcastNowPlaying(final SquidPlayer requester, final LobbyMusicCosmetic cosmetic,
            final List<SquidPlayer> listeners, final boolean forced) {
        final String key = forced ? "music.cosmetic.now-playing-forced" : "music.cosmetic.now-playing";

        for (final SquidPlayer listener : listeners) {
            MessageUtils.send(this.plugin, listener.getBukkitPlayer(), key, "{song}", cosmetic.getDisplayName(),
                    "{player}", requester.getBukkitPlayer().getName());
        }
    }

    private boolean isLobbyMusicEnabled() {
        return this.plugin.getMainConfig().getBoolean("lobby.music.enabled", true);
    }

    private boolean isCosmeticMusicEnabled() {
        return this.plugin.getMainConfig().getBoolean("lobby.music.cosmetic-enabled", true);
    }

    private boolean isMingleMusicEnabled() {
        return this.plugin.getMainConfig().getBoolean("game-settings.mingle-nbs-music-enabled", true);
    }

    private int getCosmeticCooldownSeconds() {
        return Math.max(5, this.plugin.getMainConfig().getInt("lobby.music.cosmetic-cooldown-seconds", 120));
    }

    private byte getLobbyVolume() {
        return (byte) Math.max(1, Math.min(100, this.plugin.getMainConfig().getInt("lobby.music.volume", 100)));
    }

    private byte getCosmeticVolume() {
        return (byte) Math.max(1,
                Math.min(100, this.plugin.getMainConfig().getInt("lobby.music.cosmetic-volume", 100)));
    }

    private byte getMingleVolume() {
        return (byte) Math.max(1,
                Math.min(100, this.plugin.getMainConfig().getInt("game-settings.mingle-nbs-music-volume", 100)));
    }

    private float getVolumeMultiplier() {
        return (float) Math.max(0.5D, this.plugin.getMainConfig().getDouble("lobby.music.volume-multiplier", 2.0D));
    }

    private float getTempoMultiplier() {
        return (float) Math.max(0.25D, this.plugin.getMainConfig().getDouble("lobby.music.tempo-multiplier", 1.0D));
    }

    public boolean playRandomCosmeticMusic(final SquidPlayer requester, final boolean forceEveryone) {
        final java.util.List<LobbyMusicCosmetic> songs = this.cosmeticRegistry.getAll();

        if (songs.isEmpty()) {
            return false;
        }

        final LobbyMusicCosmetic cosmetic = songs.get(new java.util.Random().nextInt(songs.size()));
        return this.playCosmeticMusic(requester, cosmetic, forceEveryone);
    }

    private void ensureSongFiles() {
        final File songsDirectory = new File(this.plugin.getDataFolder(), "songs");

        if (!songsDirectory.exists()) {
            songsDirectory.mkdirs();
        }

        for (final String songName : new String[] { "lobby.nbs", "mingle.nbs" }) {
            final File songFile = new File(songsDirectory, songName);

            if (!songFile.exists()) {
                this.plugin.saveResource("songs/" + songName, false);
            }
        }
    }

    private Song loadSong(final String fileName) {
        final File songFile = new File(this.plugin.getDataFolder(), "songs/" + fileName);

        if (!songFile.exists()) {
            this.plugin.getLogger().warning("Missing song file: " + songFile.getPath());
            return null;
        }

        try {
            return NBSDecoder.parse(songFile);
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Could not load song '" + fileName + "': " + exception.getMessage());
            return null;
        }
    }
}
