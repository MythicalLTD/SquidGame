package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;
import dev._2lstudios.squidgame.utils.MingleMaterials;

public class G8MingleGame extends ArenaGameBase {

    private enum Phase {
        MUSIC,
        ACTIVE
    }

    private final Random random;

    private int round;
    private int currentGroupSize;
    private boolean playing;
    private boolean platformLocked;
    private Phase phase;
    private BukkitTask phaseTask;
    private BukkitTask roomStatusTask;
    private long activePhaseStartedAt;

    public G8MingleGame(final Arena arena, final int durationTime) {
        super("§dMingle", "mingle", durationTime, arena);
        this.random = new Random();
    }

    @Override
    public Location getLobbyPosition() {
        return this.resolveArenaLocation("games.mingle.lobby", "arena.waiting_room", "arena.prelobby");
    }

    @Override
    public Location getPlaySpawnPosition() {
        return this.getPlatformPosition();
    }

    @Override
    protected boolean delaysPlaySpawnTeleport() {
        return true;
    }

    private Location getPlatformPosition() {
        final Location location = this.getArena().getConfig().getLocation("games.mingle.spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    @Override
    public void onStart() {
        this.round = 0;
        this.currentGroupSize = 0;
        this.playing = true;
        this.getArena().setPvPAllowed(false);

        if (this.getRooms().isEmpty()) {
            this.getArena().broadcastTitle("games.mingle.not-configured.title",
                    "games.mingle.not-configured.subtitle");
            this.abortGame();
            return;
        }

        if (!this.getArena().getConfig().contains("games.mingle.spawn.x")) {
            this.getArena().broadcastTitle("games.mingle.platform-missing.title",
                    "games.mingle.platform-missing.subtitle");
        }

        this.teleportToPlaySpawn();
        this.getArena().broadcastTitle("games.mingle.game-begin.title", "games.mingle.game-begin.subtitle");
        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), this::startRound, 20L);
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.stopMusic();
        this.cancelPhaseTask();
        this.stopRoomStatusUpdates();
        this.getArena().setPvPAllowed(false);
        this.closeAllDoors();
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public void onTimeUp() {
        this.finishGame();
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public boolean isPlatformLocked() {
        return this.playing && this.platformLocked;
    }

    public boolean isActivePhase() {
        return this.playing && this.phase == Phase.ACTIVE;
    }

    public boolean shouldCancelFallDamage() {
        return this.playing;
    }

    public boolean handleKnockback(final SquidPlayer attacker, final SquidPlayer victim,
            final EntityDamageByEntityEvent event) {
        if (!this.isActivePhase() || attacker == null || victim == null || attacker == victim) {
            return false;
        }

        event.setDamage(0);

        final Vector direction = victim.getBukkitPlayer().getLocation().toVector()
                .subtract(attacker.getBukkitPlayer().getLocation().toVector());
        if (direction.lengthSquared() > 0) {
            direction.normalize().multiply(this.getKnockbackPower()).setY(this.getKnockbackVertical());
            victim.getBukkitPlayer().setVelocity(direction);
        }

        return true;
    }

    public boolean handleMove(final SquidPlayer player, final Location to) {
        if (!this.isPlatformLocked()) {
            return false;
        }

        return !MingleMaterials.isOnPlatform(to);
    }

    public boolean handleDoorInteract(final SquidPlayer player, final Block block) {
        if (!this.isActivePhase() || player == null || block == null) {
            return false;
        }

        if (!CompatibilityUtils.isOpenableDoor(block)) {
            return false;
        }

        final int roomIndex = this.getDoorRoomIndex(block.getLocation());
        if (roomIndex == -1) {
            return true;
        }

        final int roomNumber = roomIndex + 1;
        final Cuboid room = this.getArena().getConfig().getCuboid("games.mingle.rooms." + roomNumber);

        if (room == null || !this.isValidRoom(room)
                || this.resolvePlayerRoom(player.getBukkitPlayer().getLocation()) != roomNumber) {
            return true;
        }

        return false;
    }

    private void startRound() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        if (this.round >= this.getMaxRounds()) {
            this.finishGame();
            return;
        }

        if (this.getArena().getPlayers().size() <= 1) {
            if (this.round > 0) {
                this.finishGame();
            } else {
                this.getArena().killAllPlayers();
            }

            return;
        }

        this.round++;
        this.currentGroupSize = 0;
        this.getArena().setPvPAllowed(false);
        this.closeAllDoors();

        MessageUtils.broadcastTitle(SquidGame.getInstance(), this.getArena(), "games.mingle.round-label.title",
                "games.mingle.round-label.subtitle", "{round}", String.valueOf(this.round));

        if (this.round == 1) {
            this.beginMusicPhase();
        } else {
            this.getArena().teleportAllPlayers(this.getPlatformPosition());
            this.beginMusicPhase();
        }
    }

    private void beginMusicPhase() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        this.phase = Phase.MUSIC;
        this.platformLocked = true;
        this.closeAllDoors();
        this.startMusic();
        this.scheduleMusicInterrupt();
    }

    private void scheduleMusicInterrupt() {
        this.cancelPhaseTask();
        final int delay = this.randomNumber(this.getMusicMinDelay(), this.getMusicMaxDelay());
        this.phaseTask = Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), this::announceNumber, delay * 20L);
    }

    private void announceNumber() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        this.stopMusic();
        this.phase = Phase.ACTIVE;
        this.platformLocked = false;
        this.currentGroupSize = this.pickGroupSize();
        this.activePhaseStartedAt = System.currentTimeMillis();

        this.playAnnounceSound();
        MessageUtils.broadcastTitle(SquidGame.getInstance(), this.getArena(), "games.mingle.round.title",
                "games.mingle.round.subtitle", "{size}", String.valueOf(this.currentGroupSize));
        this.getArena().setPvPAllowed(true);
        this.startRoundWarnings();
        this.startRoomStatusUpdates();

        this.cancelPhaseTask();
        this.phaseTask = Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), this::closeDoorsAndCheckRound,
                this.getRoundTime() * 20L);
    }

    private void closeDoorsAndCheckRound() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        this.stopRoomStatusUpdates();
        this.getArena().setPvPAllowed(false);
        this.checkRound();
        this.closeAllDoors();
    }

    private void checkRound() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        final Map<Integer, List<SquidPlayer>> roomPlayers = new HashMap<>();
        final Set<SquidPlayer> death = new HashSet<>();

        for (final SquidPlayer player : new ArrayList<>(this.getArena().getPlayers())) {
            final int room = this.getPlayerRoomNumber(player);

            if (room == -1) {
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
                death.addAll(group);
            }
        }

        for (final SquidPlayer player : death) {
            player.sendTitle("games.mingle.eliminated.title", "games.mingle.eliminated.subtitle", 3);
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!this.getArena().canEliminatePlayers()) {
                return;
            }

            for (final SquidPlayer player : death) {
                if (this.getArena().getPlayers().contains(player)) {
                    this.getArena().killPlayer(player);
                }
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!this.playing || this.getArena().getPlayers().isEmpty()) {
                return;
            }

            if (this.getArena().getPlayers().size() <= 1) {
                this.finishGame();
                return;
            }

            this.startRound();
        }, 40L);
    }

    private void startRoomStatusUpdates() {
        this.stopRoomStatusUpdates();
        final int showFromSecond = Math.min(15, this.getRoundTime());

        this.roomStatusTask = Bukkit.getScheduler().runTaskTimer(SquidGame.getInstance(), () -> {
            if (!this.playing || this.phase != Phase.ACTIVE) {
                this.stopRoomStatusUpdates();
                return;
            }

            this.updateRoomStatusActionBars(showFromSecond);
        }, 0L, 20L);
    }

    private void updateRoomStatusActionBars(final int showFromSecond) {
        final int secondsLeft = this.getActiveSecondsLeft();

        if (secondsLeft > showFromSecond) {
            for (final SquidPlayer player : this.getArena().getPlayers()) {
                this.sendActionBar(player, "games.mingle.active-countdown", "{needed}",
                        String.valueOf(this.currentGroupSize), "{time}", String.valueOf(secondsLeft));
            }
            return;
        }

        final Map<Integer, Integer> roomCounts = this.getRoomPopulation();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            final int room = this.getPlayerRoomNumber(player);
            final int count = room > 0 ? roomCounts.getOrDefault(room, 0) : 0;

            if (room > 0) {
                final String color = MingleMaterials.getRoomColorName(player.getBukkitPlayer().getLocation());
                this.sendActionBar(player, "games.mingle.room-status", "{room}", String.valueOf(room), "{color}",
                        color == null ? "?" : color, "{count}", String.valueOf(count), "{needed}",
                        String.valueOf(this.currentGroupSize), "{time}", String.valueOf(secondsLeft));
            } else {
                this.sendActionBar(player, "games.mingle.room-status-none", "{needed}",
                        String.valueOf(this.currentGroupSize), "{time}", String.valueOf(secondsLeft));
            }
        }
    }

    private Map<Integer, Integer> getRoomPopulation() {
        final Map<Integer, Integer> roomCounts = new HashMap<>();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            final int room = this.getPlayerRoomNumber(player);

            if (room > 0) {
                roomCounts.put(room, roomCounts.getOrDefault(room, 0) + 1);
            }
        }

        return roomCounts;
    }

    private int getActiveSecondsLeft() {
        return Math.max(0, this.getRoundTime() - (int) ((System.currentTimeMillis() - this.activePhaseStartedAt) / 1000L));
    }

    private void stopRoomStatusUpdates() {
        if (this.roomStatusTask != null) {
            this.roomStatusTask.cancel();
            this.roomStatusTask = null;
        }
    }

    private void startRoundWarnings() {
        final int roundTime = this.getRoundTime();
        final int[] warnings = new int[] { 10, 5, 3 };

        for (final int warning : warnings) {
            if (roundTime <= warning) {
                continue;
            }

            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                if (this.playing && this.phase == Phase.ACTIVE) {
                    this.broadcastActionBar("games.mingle.countdown", "{time}", String.valueOf(warning), "{size}",
                            String.valueOf(this.currentGroupSize));
                    this.getArena().broadcastSound(this.getArena().getMainConfig()
                            .getSound("game-settings.sounds.arena-countdown", "NOTE_PLING"));
                }
            }, (roundTime - warning) * 20L);
        }
    }

    private void startMusic() {
        SquidGame.getInstance().getNbsMusicManager().playMingleMusic(this.getArena());
    }

    private void playAnnounceSound() {
        final Sound announceSound = this.getArena().getMainConfig().getSound("game-settings.sounds.mingle-announce",
                "NOTE_BASS");

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.playSound(announceSound, player.getBukkitPlayer().getLocation(), 1.0F);
        }
    }

    private void stopMusic() {
        SquidGame.getInstance().getNbsMusicManager().stopMingleMusic(this.getArena());
    }

    private void closeAllDoors() {
        this.setAllDoorsOpen(false);
    }

    private void openAllDoors() {
        this.setAllDoorsOpen(true);
    }

    private void setAllDoorsOpen(final boolean open) {
        final int roomCount = this.getArena().getConfig().getInt("games.mingle.room-count", 0);

        for (int i = 1; i <= roomCount; i++) {
            final Location door = this.getDoorLocation(i);
            if (door != null) {
                this.setDoorOpen(door, open);
            }
        }
    }

    private void setDoorOpen(final Location location, final boolean open) {
        CompatibilityUtils.setDoorOpen(location.getBlock(), open);
    }

    private int getDoorRoomIndex(final Location doorLocation) {
        final int roomCount = this.getArena().getConfig().getInt("games.mingle.room-count", 0);

        for (int i = 1; i <= roomCount; i++) {
            final Location door = this.getDoorLocation(i);
            if (door != null && this.isSameBlock(door, doorLocation)) {
                return i - 1;
            }
        }

        return -1;
    }

    private boolean isSameBlock(final Location first, final Location second) {
        return first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private Location getDoorLocation(final int roomIndex) {
        final String key = "games.mingle.rooms." + roomIndex + ".door";
        if (!this.getArena().getConfig().contains(key + ".x")) {
            return null;
        }

        final Location location = this.getArena().getConfig().getLocation(key, false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private void finishGame() {
        if (!this.playing && this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
            return;
        }

        this.playing = false;
        this.stopMusic();
        this.cancelPhaseTask();
        this.stopRoomStatusUpdates();
        this.getArena().setPvPAllowed(false);
        this.closeAllDoors();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
            player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game",
                    "LEVEL_UP"));
        }

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }
    }

    private int pickGroupSize() {
        final int alive = this.getArena().getPlayers().size();

        if (alive <= 1) {
            return 1;
        }

        if (alive == 2) {
            return 2;
        }

        final int minGroupSize = this.getMinGroupSize();
        final int maxGroupSize = Math.min(this.getMaxGroupSize(), alive);
        final int min = Math.min(minGroupSize, maxGroupSize);
        final int max = Math.max(minGroupSize, maxGroupSize);
        return this.random.nextInt(max - min + 1) + min;
    }

    private int getPlayerRoomNumber(final SquidPlayer player) {
        return this.resolvePlayerRoom(player.getBukkitPlayer().getLocation());
    }

    private int resolvePlayerRoom(final Location playerLocation) {
        final Block floorBlock = MingleMaterials.getRoomFloorBlock(playerLocation);

        if (floorBlock == null) {
            return -1;
        }

        final Location floorCenter = floorBlock.getLocation().add(0.5D, 0.0D, 0.5D);
        final int roomCount = this.getArena().getConfig().getInt("games.mingle.room-count", 0);
        final double padding = this.getRoomHorizontalPadding();
        final double nearRadius = this.getRoomNearRadius();

        for (int i = 1; i <= roomCount; i++) {
            final Cuboid room = this.getArena().getConfig().getCuboid("games.mingle.rooms." + i);

            if (room != null && this.isValidRoom(room)
                    && this.isInsideRoomHorizontally(floorCenter, room, padding)) {
                return i;
            }
        }

        int nearestRoom = -1;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 1; i <= roomCount; i++) {
            final Cuboid room = this.getArena().getConfig().getCuboid("games.mingle.rooms." + i);

            if (room == null || !this.isValidRoom(room)) {
                continue;
            }

            final double distance = this.getHorizontalDistanceOutsideCuboid(floorCenter, room);

            if (distance <= nearRadius && distance < nearestDistance) {
                nearestDistance = distance;
                nearestRoom = i;
            }
        }

        return nearestRoom;
    }

    private boolean isInsideRoomHorizontally(final Location location, final Cuboid room, final double padding) {
        final Vector3 first = room.getFirstPoint();
        final Vector3 second = room.getSecondPoint();
        final double minX = Math.min(first.getX(), second.getX()) - padding;
        final double maxX = Math.max(first.getX(), second.getX()) + padding;
        final double minZ = Math.min(first.getZ(), second.getZ()) - padding;
        final double maxZ = Math.max(first.getZ(), second.getZ()) + padding;

        return location.getX() >= minX && location.getX() <= maxX
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    private double getHorizontalDistanceOutsideCuboid(final Location location, final Cuboid room) {
        final Vector3 first = room.getFirstPoint();
        final Vector3 second = room.getSecondPoint();
        final double minX = Math.min(first.getX(), second.getX());
        final double maxX = Math.max(first.getX(), second.getX());
        final double minZ = Math.min(first.getZ(), second.getZ());
        final double maxZ = Math.max(first.getZ(), second.getZ());
        final double x = location.getX();
        final double z = location.getZ();
        final double dx;

        if (x < minX) {
            dx = minX - x;
        } else if (x > maxX) {
            dx = x - maxX;
        } else {
            dx = 0.0D;
        }

        final double dz;

        if (z < minZ) {
            dz = minZ - z;
        } else if (z > maxZ) {
            dz = z - maxZ;
        } else {
            dz = 0.0D;
        }

        return Math.sqrt((dx * dx) + (dz * dz));
    }

    private double getRoomHorizontalPadding() {
        return this.getArena().getMainConfig().getDouble("game-settings.mingle-room-horizontal-padding", 2.0D);
    }

    private double getRoomNearRadius() {
        return this.getArena().getMainConfig().getDouble("game-settings.mingle-room-near-radius", 3.0D);
    }

    private boolean isValidRoom(final Cuboid room) {
        final Location spawn = this.getPlatformPosition();
        final Vector3 first = room.getFirstPoint();
        final Vector3 second = room.getSecondPoint();
        final Location center = new Location(spawn.getWorld(),
                (first.getX() + second.getX()) / 2.0D,
                (first.getY() + second.getY()) / 2.0D,
                (first.getZ() + second.getZ()) / 2.0D);

        return center.distance(spawn) <= this.getMaxRoomDistance();
    }

    private double getMaxRoomDistance() {
        return this.getArena().getMainConfig().getDouble("game-settings.mingle-room-max-distance", 200.0D);
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

    private void abortGame() {
        this.playing = false;
        this.stopMusic();
        this.cancelPhaseTask();
        this.stopRoomStatusUpdates();
        this.getArena().setPvPAllowed(false);
        this.closeAllDoors();

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }
    }

    private void sendActionBar(final SquidPlayer player, final String key, final String... replacements) {
        player.sendActionBar(MessageUtils.format(SquidGame.getInstance(), key, replacements));
    }

    private void broadcastActionBar(final String key, final String... replacements) {
        final String message = MessageUtils.format(SquidGame.getInstance(), key, replacements);

        for (final SquidPlayer player : this.getArena().getAllPlayers()) {
            player.sendActionBar(message);
        }
    }

    private int getMaxRounds() {
        return this.getArena().getMainConfig().getInt("game-settings.mingle-rounds", 3);
    }

    private int getRoundTime() {
        return this.getArena().getMainConfig().getInt("game-settings.mingle-round-time", 30);
    }

    private int getMinGroupSize() {
        return Math.max(2, this.getArena().getMainConfig().getInt("game-settings.mingle-min-group-size", 2));
    }

    private int getMaxGroupSize() {
        return Math.max(this.getMinGroupSize(),
                this.getArena().getMainConfig().getInt("game-settings.mingle-max-group-size", 8));
    }

    private int getMusicMinDelay() {
        return this.getArena().getMainConfig().getInt("game-settings.mingle-music-min-delay", 5);
    }

    private int getMusicMaxDelay() {
        return this.getArena().getMainConfig().getInt("game-settings.mingle-music-max-delay", 12);
    }

    private double getKnockbackPower() {
        return this.getArena().getMainConfig().getDouble("game-settings.mingle-knockback-power", 1.0D);
    }

    private double getKnockbackVertical() {
        return this.getArena().getMainConfig().getDouble("game-settings.mingle-knockback-vertical", 0.35D);
    }

    private int randomNumber(final int min, final int max) {
        return this.random.nextInt(max - min + 1) + min;
    }

    private void cancelPhaseTask() {
        if (this.phaseTask != null) {
            this.phaseTask.cancel();
            this.phaseTask = null;
        }
    }
}
