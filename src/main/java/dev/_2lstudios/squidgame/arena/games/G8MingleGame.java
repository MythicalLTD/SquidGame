package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.gui.MingleVoteGUI;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class G8MingleGame extends ArenaGameBase {

    private final Random random;
    private final Set<UUID> playVotes;
    private final Set<UUID> skipVotes;

    private int round;
    private int currentGroupSize;
    private boolean playing;

    public G8MingleGame(final Arena arena, final int durationTime) {
        super("§dMingle", "mingle", durationTime, arena);
        this.random = new Random();
        this.playVotes = new HashSet<>();
        this.skipVotes = new HashSet<>();
    }

    @Override
    public Location getSpawnPosition() {
        final Location location = this.getArena().getConfig().getLocation("games.mingle.spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    @Override
    public void onExplainStart() {
        super.onExplainStart();
        this.broadcastMingleMessage("games.mingle.explain");
        this.broadcastMingleMessage("games.mingle.vote.info", "{needed}", String.valueOf(this.getSkipVotesNeeded()));
        this.openVoteGui();
    }

    @Override
    public void onStart() {
        this.round = 0;
        this.currentGroupSize = 0;
        this.playing = true;
        this.getArena().setPvPAllowed(false);

        if (this.shouldSkipByVote()) {
            this.playing = false;
            this.getArena().broadcastTitle("games.mingle.vote.skipped.title", "games.mingle.vote.skipped.subtitle");
            this.broadcastMingleMessage("games.mingle.vote.skipped.chat");
            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> this.getArena().nextGame(), 20L);
            return;
        }

        if (this.getRooms().isEmpty()) {
            this.getArena().broadcastTitle("games.mingle.not-configured.title", "games.mingle.not-configured.subtitle");
            this.finishGame();
            return;
        }

        this.broadcastMingleMessage("games.mingle.started", "{rounds}", String.valueOf(this.getMaxRounds()));
        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.startRound();
        }, 40L);
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.playVotes.clear();
        this.skipVotes.clear();
        this.getArena().setPvPAllowed(false);
    }

    @Override
    public int getMinPlayers() {
        return this.getMinGroupSize();
    }

    @Override
    public void onTimeUp() {
        this.finishGame();
    }

    public void vote(final Player player, final boolean skip) {
        final UUID uuid = player.getUniqueId();
        this.playVotes.remove(uuid);
        this.skipVotes.remove(uuid);

        if (skip) {
            this.skipVotes.add(uuid);
        } else {
            this.playVotes.add(uuid);
        }

        MessageUtils.send(SquidGame.getInstance(), player, skip ? "games.mingle.vote.voted-skip"
                : "games.mingle.vote.voted-play", "{skip}", String.valueOf(this.skipVotes.size()), "{play}",
                String.valueOf(this.playVotes.size()), "{needed}", String.valueOf(this.getSkipVotesNeeded()));
        this.broadcastMingleMessage("games.mingle.vote.status", "{skip}", String.valueOf(this.skipVotes.size()),
                "{play}", String.valueOf(this.playVotes.size()), "{needed}", String.valueOf(this.getSkipVotesNeeded()));
    }

    private void openVoteGui() {
        this.playVotes.clear();
        this.skipVotes.clear();

        if (!this.getArena().getMainConfig().getBoolean("game-settings.mingle-vote-enabled", true)) {
            return;
        }

        final MingleVoteGUI gui = new MingleVoteGUI(this);
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            gui.open(player.getBukkitPlayer());
        }
    }

    private boolean shouldSkipByVote() {
        return this.getArena().getMainConfig().getBoolean("game-settings.mingle-vote-enabled", true)
                && this.skipVotes.size() >= this.getSkipVotesNeeded();
    }

    private int getSkipVotesNeeded() {
        final int percent = Math.max(1,
                this.getArena().getMainConfig().getInt("game-settings.mingle-skip-vote-percent", 50));
        return Math.max(1, (int) Math.ceil(this.getArena().getPlayers().size() * (percent / 100.0D)));
    }

    private void startRound() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        if (this.round >= this.getMaxRounds() || this.getArena().getPlayers().size() <= 1) {
            this.finishGame();
            return;
        }

        this.round++;
        this.currentGroupSize = this.pickGroupSize();
        MessageUtils.broadcastTitle(SquidGame.getInstance(), this.getArena(), "games.mingle.round.title",
                "games.mingle.round.subtitle", "{size}", String.valueOf(this.currentGroupSize));
        this.broadcastMingleMessage("games.mingle.round.chat", "{round}", String.valueOf(this.round), "{rounds}",
                String.valueOf(this.getMaxRounds()), "{size}", String.valueOf(this.currentGroupSize), "{time}",
                String.valueOf(this.getRoundTime()));
        this.getArena().broadcastSound(this.getArena().getMainConfig().getSound("game-settings.sounds.arena-countdown",
                "NOTE_PLING"));
        this.startRoundWarnings();

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.checkRound();
        }, this.getRoundTime() * 20L);
    }

    private void checkRound() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        final Map<Integer, List<SquidPlayer>> roomPlayers = new HashMap<>();
        final List<SquidPlayer> death = new ArrayList<>();
        final List<Cuboid> rooms = this.getRooms();

        for (final SquidPlayer player : new ArrayList<>(this.getArena().getPlayers())) {
            final int room = this.getPlayerRoom(player, rooms);

            if (room == -1) {
                MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.mingle.fail.outside");
                death.add(player);
            } else {
                if (!roomPlayers.containsKey(room)) {
                    roomPlayers.put(room, new ArrayList<>());
                }

                roomPlayers.get(room).add(player);
            }
        }

        for (final List<SquidPlayer> group : roomPlayers.values()) {
            if (group.size() != this.currentGroupSize) {
                for (final SquidPlayer player : group) {
                    MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.mingle.fail.wrong-size",
                            "{size}", String.valueOf(group.size()), "{needed}",
                            String.valueOf(this.currentGroupSize));
                }
                death.addAll(group);
            } else {
                for (final SquidPlayer player : group) {
                    MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.mingle.pass.chat",
                            "{size}", String.valueOf(this.currentGroupSize));
                }
            }
        }

        if (death.isEmpty()) {
            this.getArena().broadcastTitle("games.mingle.pass.title", "games.mingle.pass.subtitle");
        } else {
            this.getArena().broadcastTitle("games.mingle.eliminated.title", "games.mingle.eliminated.subtitle");
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                if (this.getArena().getPlayers().contains(player)) {
                    this.getArena().killPlayer(player);
                }
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.startRound();
        }, 80L);
    }

    private void startRoundWarnings() {
        final int roundTime = this.getRoundTime();
        final int[] warnings = new int[] { 10, 5, 3, 2, 1 };

        for (final int warning : warnings) {
            if (roundTime <= warning) {
                continue;
            }

            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                if (this.playing && this.getArena().getState() == ArenaState.IN_GAME) {
                    this.broadcastMingleMessage("games.mingle.countdown", "{time}", String.valueOf(warning),
                            "{size}", String.valueOf(this.currentGroupSize));
                    this.getArena().broadcastSound(this.getArena().getMainConfig()
                            .getSound("game-settings.sounds.arena-countdown", "NOTE_PLING"));
                }
            }, (roundTime - warning) * 20L);
        }
    }

    private void finishGame() {
        if (!this.playing && this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
            return;
        }

        this.playing = false;
        this.getArena().setPvPAllowed(false);

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
            player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
        }

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }
    }

    private int pickGroupSize() {
        final int minGroupSize = this.getMinGroupSize();
        final int maxGroupSize = Math.min(this.getMaxGroupSize(), Math.max(minGroupSize, this.getArena().getPlayers().size()));
        return this.random.nextInt(maxGroupSize - minGroupSize + 1) + minGroupSize;
    }

    private int getPlayerRoom(final SquidPlayer player, final List<Cuboid> rooms) {
        final Location location = player.getBukkitPlayer().getLocation();

        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).isBetween(location)) {
                return i;
            }
        }

        return -1;
    }

    private List<Cuboid> getRooms() {
        final List<Cuboid> rooms = new ArrayList<>();
        final int roomCount = this.getArena().getConfig().getInt("games.mingle.room-count", 0);

        for (int i = 1; i <= roomCount; i++) {
            final Cuboid room = this.getArena().getConfig().getCuboid("games.mingle.rooms." + i);

            if (room != null) {
                rooms.add(room);
            }
        }

        return rooms;
    }

    private void broadcastMingleMessage(final String key, final String... replacements) {
        for (final SquidPlayer player : this.getArena().getAllPlayers()) {
            MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), key, replacements);
        }
    }

    private int getMaxRounds() {
        return this.getArena().getMainConfig().getInt("game-settings.mingle-rounds", 5);
    }

    private int getRoundTime() {
        return this.getArena().getMainConfig().getInt("game-settings.mingle-round-time", 30);
    }

    private int getMinGroupSize() {
        return Math.max(2, this.getArena().getMainConfig().getInt("game-settings.mingle-min-group-size", 2));
    }

    private int getMaxGroupSize() {
        return Math.max(this.getMinGroupSize(),
                this.getArena().getMainConfig().getInt("game-settings.mingle-max-group-size", 10));
    }
}
