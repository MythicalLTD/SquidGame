package dev._2lstudios.squidgame.music;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.xxmicloxx.NoteBlockAPI.model.Layer;
import com.xxmicloxx.NoteBlockAPI.model.Note;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.SoundCategory;
import com.xxmicloxx.NoteBlockAPI.model.playmode.ChannelMode;
import com.xxmicloxx.NoteBlockAPI.model.playmode.StereoMode;

import dev._2lstudios.squidgame.SquidGame;

public class SquidNbsSongPlayer {

    private final SquidGame plugin;
    private final Song song;
    private final Set<UUID> players;
    private final Map<UUID, Byte> playerVolumes;
    private final ChannelMode channelMode;
    private final byte volume;
    private final float volumeMultiplier;
    private final float tempoMultiplier;

    private BukkitTask task;
    private int tick;
    private float tickAccumulator;
    private boolean playing;
    private boolean looping;
    private Runnable onFinish;

    public SquidNbsSongPlayer(final SquidGame plugin, final Song song, final byte volume,
            final float volumeMultiplier) {
        this(plugin, song, volume, volumeMultiplier, 1.0F);
    }

    public SquidNbsSongPlayer(final SquidGame plugin, final Song song, final byte volume,
            final float volumeMultiplier, final float tempoMultiplier) {
        this.plugin = plugin;
        this.song = song;
        this.volume = volume;
        this.volumeMultiplier = volumeMultiplier;
        this.tempoMultiplier = Math.max(0.25F, tempoMultiplier);
        this.players = new HashSet<>();
        this.playerVolumes = new HashMap<>();
        this.channelMode = new StereoMode();
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public boolean hasPlayers() {
        return !this.players.isEmpty();
    }

    public void setPlayerVolume(final UUID uuid, final byte volume) {
        if (!this.players.contains(uuid)) {
            return;
        }

        this.playerVolumes.put(uuid, volume);
    }

    public void addPlayer(final Player player) {
        this.addPlayer(player, this.volume);
    }

    public void addPlayer(final Player player, final byte volume) {
        final UUID uuid = player.getUniqueId();
        this.players.add(uuid);
        this.playerVolumes.put(uuid, volume);
    }

    public void removePlayer(final UUID uuid) {
        this.players.remove(uuid);
        this.playerVolumes.remove(uuid);
    }

    public boolean hasPlayer(final UUID uuid) {
        return this.players.contains(uuid);
    }

    public void start(final boolean loop) {
        this.start(loop, null);
    }

    public void start(final boolean loop, final Runnable onFinish) {
        if (this.playing || this.song == null) {
            return;
        }

        this.playing = true;
        this.looping = loop;
        this.onFinish = onFinish;
        this.tick = 0;
        this.tickAccumulator = 0.0F;
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::advance, 0L, 1L);
    }

    public void stop() {
        this.playing = false;

        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }

        this.onFinish = null;
    }

    private void finishNaturally() {
        final Runnable callback = this.onFinish;
        this.stop();

        if (callback != null) {
            callback.run();
        }
    }

    private float getTicksPerNbsTick() {
        return Math.max(0.05F, this.song.getDelay() * this.tempoMultiplier);
    }

    private void advance() {
        if (!this.playing) {
            return;
        }

        this.tickAccumulator += 1.0F;

        while (this.tickAccumulator >= this.getTicksPerNbsTick()) {
            this.tickAccumulator -= this.getTicksPerNbsTick();
            this.playCurrentTick();

            this.tick++;

            if (this.tick >= this.song.getLength()) {
                if (this.looping) {
                    this.tick = 0;
                } else {
                    this.finishNaturally();
                    return;
                }
            }
        }

        if (!this.playing) {
            return;
        }
    }

    private void playCurrentTick() {
        for (final UUID uuid : new HashSet<>(this.players)) {
            final Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                this.players.remove(uuid);
                this.playerVolumes.remove(uuid);
                continue;
            }

            this.playTick(player, this.tick);
        }
    }

    private void playTick(final Player player, final int currentTick) {
        for (final Layer layer : this.song.getLayerHashMap().values()) {
            final Note note = layer.getNote(currentTick);

            if (note == null) {
                continue;
            }

            final byte resolvedVolume = this.playerVolumes.getOrDefault(player.getUniqueId(), this.volume);

            if (resolvedVolume <= 0) {
                continue;
            }

            final float volumeValue = Math.min(1.0F,
                    layer.getVolume() * resolvedVolume * note.getVelocity() / 1_000_000F * this.volumeMultiplier);
            this.channelMode.play(player, player.getLocation(), this.song, layer, note, SoundCategory.MASTER,
                    volumeValue, false);
        }
    }
}
