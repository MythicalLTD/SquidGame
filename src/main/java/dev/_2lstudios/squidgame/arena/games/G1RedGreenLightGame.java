package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.jelly.utils.NumberUtils;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class G1RedGreenLightGame extends ArenaGameBase {

    private Cuboid barrier;
    private Cuboid killZone;
    private Cuboid goalZone;
    private final Map<Block, Material> barrierBlocks = new HashMap<>();
    private final Set<UUID> crossedPlayers = new HashSet<>();

    private boolean canWalk = true;
    private boolean playing = false;
    private boolean allCrossed = false;

    public G1RedGreenLightGame(final Arena arena, final int durationTime) {
        super("§aGreen Light §7| §cRed Light", "first", durationTime, arena);
    }

    public Cuboid getBarrier() {
        if (this.barrier == null) {
            this.barrier = this.getArena().getConfig().getCuboid("games.first.barrier");
        }

        return this.barrier;
    }

    public Cuboid getKillZone() {
        if (this.killZone == null) {
            this.killZone = this.getArena().getConfig().getCuboid("games.first.killzone");
        }

        return this.killZone;
    }

    public Cuboid getGoalZone() {
        if (this.goalZone == null) {
            this.goalZone = this.getArena().getConfig().getCuboid("games.first.goal");
        }

        return this.goalZone;
    }

    private void spawnBarrier() {
        final Cuboid barrier = this.getBarrier();

        if (barrier == null || !this.barrierBlocks.isEmpty()) {
            return;
        }

        final int minX = (int) Math.min(barrier.getFirstPoint().getX(), barrier.getSecondPoint().getX());
        final int maxX = (int) Math.max(barrier.getFirstPoint().getX(), barrier.getSecondPoint().getX());
        final int minY = (int) Math.min(barrier.getFirstPoint().getY(), barrier.getSecondPoint().getY());
        final int maxY = (int) Math.max(barrier.getFirstPoint().getY(), barrier.getSecondPoint().getY());
        final int minZ = (int) Math.min(barrier.getFirstPoint().getZ(), barrier.getSecondPoint().getZ());
        final int maxZ = (int) Math.max(barrier.getFirstPoint().getZ(), barrier.getSecondPoint().getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = this.getArena().getWorld().getBlockAt(x, y, z);

                    if (!this.barrierBlocks.containsKey(block)) {
                        this.barrierBlocks.put(block, block.getType());
                    }

                    block.setType(Material.BARRIER);
                }
            }
        }
    }

    private void removeBarrier() {
        for (final Map.Entry<Block, Material> entry : this.barrierBlocks.entrySet()) {
            entry.getKey().setType(entry.getValue());
        }

        this.barrierBlocks.clear();
    }

    private void singDoll() {
        if (!this.playing) {
            return;
        }

        final int time = NumberUtils.randomNumber(2, 5);
        this.getArena().broadcastTitle("games.first.green-light.title", "games.first.green-light.subtitle");
        this.getArena().broadcastSound(
                this.getArena().getMainConfig().getSound("game-settings.sounds.green-light", "GHAST_MOAN"));
        this.canWalk = true;

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.getArena().broadcastTitle("games.first.red-light.title", "games.first.red-light.subtitle");
            this.getArena().broadcastSound(
                    this.getArena().getMainConfig().getSound("game-settings.sounds.red-light", "BLAZE_HIT"));
            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                this.canWalk = false;
                final int waitTime = NumberUtils.randomNumber(2, 5);
                Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                    singDoll();
                }, waitTime * 20);
            }, 20);
        }, time * 20);
    }

    @Override
    public void onExplainStart() {
        super.onExplainStart();
        this.spawnBarrier();
    }

    @Override
    public void onStart() {
        this.removeBarrier();
        this.crossedPlayers.clear();
        this.allCrossed = false;
        this.playing = true;
        this.singDoll();
        this.getArena().setPvPAllowed(false);
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.removeBarrier();
    }

    public void handleMove(final SquidPlayer player, final Location to) {
        if (!this.playing || this.allCrossed || !this.getGoalZone().isBetween(to)) {
            return;
        }

        this.crossedPlayers.add(player.getBukkitPlayer().getUniqueId());

        if (this.haveAllPlayersCrossed()) {
            this.allCrossed = true;
            this.getArena().broadcastMessage("games.first.all-crossed");
            this.getArena().setInternalTime(1);
        }
    }

    private boolean haveAllPlayersCrossed() {
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (!this.crossedPlayers.contains(player.getBukkitPlayer().getUniqueId())) {
                return false;
            }
        }

        return !this.getArena().getPlayers().isEmpty();
    }

    @Override
    public void onTimeUp() {
        this.getArena().setPvPAllowed(false);
        this.canWalk = false;
        this.playing = false;

        this.getArena().broadcastTitle("events.game-timeout.title", "events.game-timeout.subtitle");

        final List<SquidPlayer> death = new ArrayList<>();
        final List<SquidPlayer> alive = new ArrayList<>();

        for (final SquidPlayer squidPlayer : this.getArena().getPlayers()) {
            final Player player = squidPlayer.getBukkitPlayer();
            final Location location = player.getLocation();
            final Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());

            if (this.getGoalZone().isBetween(position)) {
                alive.add(squidPlayer);
            } else {
                death.add(squidPlayer);
            }
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                player.sendTitle("events.game-timeout-died.title", "events.game-timeout-died.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
            }

            for (final SquidPlayer player : alive) {
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer squidPlayer : death) {
                this.getArena().killPlayer(squidPlayer);
            }
        }, 80L);
    }

    public boolean isCanWalk() {
        return this.canWalk;
    }
}
