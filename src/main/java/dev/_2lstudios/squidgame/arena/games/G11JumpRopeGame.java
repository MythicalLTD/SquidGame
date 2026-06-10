package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class G11JumpRopeGame extends ArenaGameBase {

    private final Set<UUID> completed;
    private final Random random;

    private boolean playing;
    private boolean ropeSweeping;

    public G11JumpRopeGame(final Arena arena, final int durationTime) {
        super("§eJump Rope", "jump-rope", durationTime, arena);

        this.completed = new HashSet<>();
        this.random = new Random();
    }

    @Override
    public Location getSpawnPosition() {
        final Location location = this.getArena().getConfig().getLocation("games.jump-rope.spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    @Override
    public void onStart() {
        this.completed.clear();
        this.playing = true;
        this.ropeSweeping = false;
        this.getArena().setPvPAllowed(false);

        if (this.getBridgeZone() == null || this.getGoalZone() == null) {
            this.getArena().broadcastTitle("games.jump-rope.not-configured.title",
                    "games.jump-rope.not-configured.subtitle");
            this.finishGame();
            return;
        }

        this.scheduleNextSweep();
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.ropeSweeping = false;
        this.getArena().setPvPAllowed(false);
    }

    @Override
    public void onTimeUp() {
        this.finishGame();
    }

    public void handleMove(final SquidPlayer player, final Location from, final Location to) {
        if (!this.playing || this.completed.contains(player.getBukkitPlayer().getUniqueId())) {
            return;
        }

        final Cuboid voidZone = this.getVoidZone();
        if (voidZone != null && voidZone.isBetween(to)) {
            this.getArena().killPlayer(player);
            return;
        }

        if (this.getGoalZone().isBetween(to)) {
            this.completed.add(player.getBukkitPlayer().getUniqueId());
            player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
            player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));

            if (this.haveAllPlayersFinished()) {
                this.finishGame();
            }

            return;
        }

        if (this.ropeSweeping && this.getBridgeZone().isBetween(to) && Math.abs(to.getY() - from.getY()) < 0.05) {
            player.sendTitle("games.jump-rope.hit.title", "games.jump-rope.hit.subtitle", 3);
            player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
            this.getArena().killPlayer(player);
        }
    }

    private void scheduleNextSweep() {
        final int delay = this.randomNumber(this.getMinSweepDelay(), this.getMaxSweepDelay());

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.startSweepWarning();
        }, delay * 20L);
    }

    private void startSweepWarning() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        this.broadcastSweepCountdown(this.getSweepWarningTime());
    }

    private void broadcastSweepCountdown(final int secondsLeft) {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        if (secondsLeft <= 0) {
            this.startSweep();
            return;
        }

        this.getArena().broadcastTitle("games.jump-rope.warning.title", "games.jump-rope.warning.subtitle");
        this.getArena().broadcastSound(this.getArena().getMainConfig().getSound("game-settings.sounds.arena-countdown",
                "NOTE_PLING"));

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.broadcastSweepCountdown(secondsLeft - 1);
        }, 20L);
    }

    private void startSweep() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        this.ropeSweeping = true;
        this.getArena().broadcastTitle("games.jump-rope.swing.title", "games.jump-rope.swing.subtitle");
        this.getArena().broadcastSound(this.getArena().getMainConfig().getSound("game-settings.sounds.arena-countdown",
                "NOTE_PLING"));

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.ropeSweeping = false;

            if (this.playing && this.getArena().getState() == ArenaState.IN_GAME) {
                this.scheduleNextSweep();
            }
        }, 20L);
    }

    private void finishGame() {
        if (!this.playing && this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
            return;
        }

        this.playing = false;
        this.ropeSweeping = false;
        this.getArena().setPvPAllowed(false);

        final ArrayList<SquidPlayer> death = new ArrayList<>();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (!this.completed.contains(player.getBukkitPlayer().getUniqueId())) {
                death.add(player);
            }
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                if (this.getArena().getPlayers().contains(player)) {
                    player.sendTitle("games.jump-rope.lost.title", "games.jump-rope.lost.subtitle", 3);
                    this.getArena().killPlayer(player);
                }
            }
        }, 40L);

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }
    }

    private boolean haveAllPlayersFinished() {
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (!this.completed.contains(player.getBukkitPlayer().getUniqueId())) {
                return false;
            }
        }

        return true;
    }

    private Cuboid getBridgeZone() {
        return this.getArena().getConfig().getCuboid("games.jump-rope.bridge");
    }

    private Cuboid getGoalZone() {
        return this.getArena().getConfig().getCuboid("games.jump-rope.goal");
    }

    private Cuboid getVoidZone() {
        return this.getArena().getConfig().getCuboid("games.jump-rope.void");
    }

    private int getMinSweepDelay() {
        return this.getArena().getMainConfig().getInt("game-settings.jump-rope-min-sweep-delay", 2);
    }

    private int getMaxSweepDelay() {
        return this.getArena().getMainConfig().getInt("game-settings.jump-rope-max-sweep-delay", 5);
    }

    private int getSweepWarningTime() {
        return this.getArena().getMainConfig().getInt("game-settings.jump-rope-warning-time", 3);
    }

    private int randomNumber(final int min, final int max) {
        if (max <= min) {
            return min;
        }

        return this.random.nextInt((max - min) + 1) + min;
    }
}
