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
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.jelly.utils.NumberUtils;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.WorldTimeUtils;

public class G1RedGreenLightGame extends ArenaGameBase {

    private Cuboid barrier;
    private Cuboid killZone;
    private Cuboid goalZone;
    private final Map<String, BlockState> barrierBlocks = new HashMap<>();
    private final Set<UUID> crossedPlayers = new HashSet<>();
    private final Set<UUID> redLightViolators = new HashSet<>();

    private boolean barrierActive = false;

    private boolean canWalk = true;
    private boolean playing = false;
    private boolean allCrossed = false;
    private int dollRoundId = 0;
    private BukkitTask eliminationTask;

    public G1RedGreenLightGame(final Arena arena, final int durationTime) {
        super("§aGreen Light §7| §cRed Light", "first", durationTime, arena);
    }

    @Override
    public Location getLobbyPosition() {
        return this.resolveArenaLocation("games.first.lobby", "arena.waiting_room", "arena.prelobby");
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

    public boolean isBarrierActive() {
        return this.barrierActive;
    }

    private void spawnBarrier() {
        this.removeBarrier();

        final Cuboid barrier = this.getBarrier();

        if (barrier == null) {
            return;
        }

        final Material barrierMaterial = CompatibilityUtils.material("BARRIER");
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
                    final String key = blockKey(block);

                    if (!this.barrierBlocks.containsKey(key)) {
                        this.barrierBlocks.put(key, block.getState());
                    }

                    if (barrierMaterial != null) {
                        CompatibilityUtils.setType(block, barrierMaterial);
                    }
                }
            }
        }

        this.barrierActive = true;
    }

    private void removeBarrier() {
        for (final BlockState state : this.barrierBlocks.values()) {
            state.update(true, false);
        }

        this.barrierBlocks.clear();
        this.clearBarrierCuboidFallback();
        this.barrierActive = false;
    }

    private void clearBarrierCuboidFallback() {
        final Cuboid barrier = this.getBarrier();
        final Material barrierMaterial = CompatibilityUtils.material("BARRIER");

        if (barrier == null || barrierMaterial == null) {
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

                    if (block.getType() == barrierMaterial) {
                        CompatibilityUtils.setType(block, Material.AIR);
                    }
                }
            }
        }
    }

    private static String blockKey(final Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private void singDoll() {
        if (!this.playing) {
            return;
        }

        this.removeBarrier();

        final int round = this.dollRoundId;
        final int time = NumberUtils.randomNumber(2, 5);
        this.getArena().broadcastTitle("games.first.green-light.title", "games.first.green-light.subtitle");
        this.getArena().broadcastSound(
                this.getArena().getMainConfig().getSound("game-settings.sounds.green-light", "GHAST_MOAN"));
        WorldTimeUtils.setWorldTime(this.getArena().getWorld(), WorldTimeUtils.GREEN_LIGHT_TIME);
        this.canWalk = true;

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!this.playing || this.dollRoundId != round) {
                return;
            }

            this.getArena().broadcastTitle("games.first.red-light.title", "games.first.red-light.subtitle");
            this.getArena().broadcastSound(
                    this.getArena().getMainConfig().getSound("game-settings.sounds.red-light", "BLAZE_HIT"));
            WorldTimeUtils.setWorldTime(this.getArena().getWorld(), WorldTimeUtils.RED_LIGHT_TIME);
            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                if (!this.playing || this.dollRoundId != round) {
                    return;
                }

                this.canWalk = false;
                final int waitTime = NumberUtils.randomNumber(2, 5);
                Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                    if (!this.playing || this.dollRoundId != round) {
                        return;
                    }

                    this.singDoll();
                }, waitTime * 20);
            }, 20);
        }, time * 20);
    }

    @Override
    public boolean delaysLobbyRemovalUntilPlayBegins() {
        return true;
    }

    @Override
    protected boolean delaysPlaySpawnTeleport() {
        return true;
    }

    @Override
    public void onExplainStart() {
        super.onExplainStart();
        this.spawnBarrier();
        this.refreshGameLobby();
        WorldTimeUtils.setWorldTime(this.getArena().getWorld(), WorldTimeUtils.GREEN_LIGHT_TIME);
    }

    public boolean handleRedLightViolation(final SquidPlayer player, final Location at) {
        if (!this.playing || this.canWalk || this.allCrossed || this.keepsGameLobbyFeatures()
                || !this.getArena().canEliminatePlayers()) {
            return false;
        }

        if (player.isSpectator() || !this.getArena().getPlayers().contains(player)) {
            return true;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();

        if (this.hasPlayerCrossed(uuid) || this.isInsideGoal(at)) {
            return false;
        }

        if (this.redLightViolators.contains(uuid)) {
            return true;
        }

        if (!this.getArena().killPlayer(player)) {
            return false;
        }

        this.redLightViolators.add(uuid);
        return true;
    }

    public boolean hasPlayerCrossed(final SquidPlayer player) {
        if (player == null || player.getBukkitPlayer() == null) {
            return false;
        }

        return this.hasPlayerCrossed(player.getBukkitPlayer().getUniqueId());
    }

    private boolean hasPlayerCrossed(final UUID uuid) {
        return this.crossedPlayers.contains(uuid);
    }

    private boolean isInsideGoal(final Location location) {
        final Cuboid goal = this.getGoalZone();

        if (goal == null || location == null) {
            return false;
        }

        return goal.isBetween(new Vector3(location.getX(), location.getY(), location.getZ()));
    }

    private void stopDollCycle() {
        this.dollRoundId++;
        this.canWalk = true;
        this.playing = false;
        WorldTimeUtils.setWorldTime(this.getArena().getWorld(), WorldTimeUtils.GREEN_LIGHT_TIME);
    }

    @Override
    public void onStart() {
        this.redLightViolators.clear();
        this.removeBarrier();
        this.refreshGameLobby();
        this.crossedPlayers.clear();
        this.allCrossed = false;
        this.playing = false;
        this.canWalk = true;
        this.getArena().setPvPAllowed(false);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (this.getArena().getState() != ArenaState.IN_GAME) {
                return;
            }

            this.getArena().teleportAllPlayers(this.getPlaySpawnPosition());
            this.endGameLobby();
            this.playing = true;
            this.dollRoundId++;
            this.singDoll();
        }, this.getStartTeleportDelayTicks());
    }

    private long getStartTeleportDelayTicks() {
        return this.getArena().getMainConfig().getInt("game-settings.red-light-start-teleport-delay-ticks", 40);
    }

    @Override
    public void onStop() {
        this.cancelEliminationTask();
        this.redLightViolators.clear();
        this.playing = false;
        this.dollRoundId++;
        this.removeBarrier();
        WorldTimeUtils.lockWorldTime(this.getArena().getWorld());
    }

    private void cancelEliminationTask() {
        if (this.eliminationTask != null) {
            this.eliminationTask.cancel();
            this.eliminationTask = null;
        }
    }

    public void handleMove(final SquidPlayer player, final Location to) {
        final Cuboid goal = this.getGoalZone();

        if (!this.playing || this.allCrossed || goal == null) {
            return;
        }

        if (!this.isInsideGoal(to)) {
            return;
        }

        this.crossedPlayers.add(player.getBukkitPlayer().getUniqueId());

        if (this.haveAllPlayersCrossed()) {
            this.completeAllPlayersCrossed();
        }
    }

    private void completeAllPlayersCrossed() {
        if (this.allCrossed || this.hasFinishedEarly()) {
            return;
        }

        this.allCrossed = true;
        this.stopDollCycle();
        this.finishEarly();

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (this.getArena().getState() != ArenaState.IN_GAME) {
                return;
            }

            for (final SquidPlayer player : this.getArena().getPlayers()) {
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game",
                        "LEVEL_UP"));
            }

            this.getArena().setInternalTime(0);
        }, 1L);
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

        if (!this.hasFinishedEarly()) {
            this.getArena().broadcastTitle("events.game-timeout.title", "events.game-timeout.subtitle");
        }

        final List<SquidPlayer> death = new ArrayList<>();
        final List<SquidPlayer> alive = new ArrayList<>();

        final Cuboid goal = this.getGoalZone();

        if (goal == null) {
            return;
        }

        for (final SquidPlayer squidPlayer : this.getArena().getPlayers()) {
            final Player player = squidPlayer.getBukkitPlayer();
            final UUID uuid = player.getUniqueId();
            final Location location = player.getLocation();
            final Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());

            if (this.crossedPlayers.contains(uuid) || goal.isBetween(position)) {
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

            if (!this.allCrossed) {
                for (final SquidPlayer player : alive) {
                    player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                    player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game",
                            "LEVEL_UP"));
                }
            }
        }, 40L);

        this.cancelEliminationTask();
        this.eliminationTask = Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.eliminationTask = null;

            if (this.getArena().getState() != ArenaState.IN_GAME) {
                return;
            }

            for (final SquidPlayer squidPlayer : new ArrayList<>(death)) {
                this.getArena().killPlayer(squidPlayer);
            }
        }, 80L);
    }

    public boolean isCanWalk() {
        return this.canWalk;
    }
}
