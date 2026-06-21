package dev._2lstudios.squidgame.arena.games.g1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;

public final class G1TrapKillAnimation {

    public static final String ARROW_METADATA = "squidgame-g1-trap";

    private static final Set<UUID> ACTIVE = new HashSet<>();

    private G1TrapKillAnimation() {
    }

    public static boolean isActive(final UUID uuid) {
        return ACTIVE.contains(uuid);
    }

    public static void play(final Arena arena, final List<G1WallTrap> traps, final Cuboid killZone,
            final SquidPlayer player, final Runnable onComplete) {
        final Player bukkitPlayer = player.getBukkitPlayer();
        final UUID uuid = bukkitPlayer.getUniqueId();

        if (!ACTIVE.add(uuid)) {
            return;
        }

        final SquidGame plugin = SquidGame.getInstance();
        player.sendTitle("games.first.trap-caught.title", "games.first.trap-caught.subtitle", 3);
        arena.broadcastSound(plugin.getMainConfig().getSound("game-settings.sounds.red-light", "BLAZE_HIT"));

        G1WallTrap trap = G1WallTrap.findNearest(traps, bukkitPlayer.getLocation());

        if (trap == null) {
            trap = G1TrapScanner.createFallbackTrap(killZone, bukkitPlayer.getLocation(), arena.getWorld());
        }

        if (trap == null) {
            finish(uuid, onComplete);
            return;
        }

        final G1WallTrap selectedTrap = trap;
        final int leverDelay = plugin.getMainConfig().getInt("game-settings.red-light-trap-lever-delay-ticks", 8);
        final int tripwireDelay = plugin.getMainConfig().getInt("game-settings.red-light-trap-tripwire-delay-ticks", 18);
        final int shootDelay = plugin.getMainConfig().getInt("game-settings.red-light-trap-shoot-delay-ticks", 30);
        final int killDelay = plugin.getMainConfig().getInt("game-settings.red-light-trap-kill-delay-ticks", 50);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isStillValid(arena, player)) {
                finish(uuid, onComplete);
                return;
            }

            pullLever(selectedTrap);
            final Location leverSound = selectedTrap.hasLever()
                    ? selectedTrap.getLever().getLocation().add(0.5, 0.5, 0.5)
                    : selectedTrap.getMuzzle();
            CompatibilityUtils.spawnFlameParticles(leverSound.getWorld(), leverSound, 12);
            arena.broadcastSound("CLICK", leverSound);
        }, leverDelay);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isStillValid(arena, player)) {
                finish(uuid, onComplete);
                return;
            }

            triggerTripwire(selectedTrap);
            arena.broadcastSound("CLICK", selectedTrap.getMuzzle());
        }, tripwireDelay);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isStillValid(arena, player)) {
                finish(uuid, onComplete);
                return;
            }

            fireArrow(arena.getWorld(), selectedTrap, bukkitPlayer);
            CompatibilityUtils.spawnFlameParticles(selectedTrap.getMuzzle().getWorld(), selectedTrap.getMuzzle(), 16);
            arena.broadcastSound("SHOOT_ARROW", selectedTrap.getMuzzle());
        }, shootDelay);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isStillValid(arena, player)) {
                final Location hit = bukkitPlayer.getLocation().add(0, 1.0, 0);
                final World world = bukkitPlayer.getWorld();
                CompatibilityUtils.spawnCritBurst(world, hit, 24);
                CompatibilityUtils.spawnBlockBreakBurst(world, hit,
                        CompatibilityUtils.material("REDSTONE_BLOCK", "REDSTONE_BLOCK"), 16);
                CompatibilityUtils.spawnSmokeBurst(world, hit, 12);
                arena.broadcastSound("HURT_FLESH", hit);
                bukkitPlayer.setVelocity(selectedTrap.getMuzzle().getDirection().multiply(0.2).setY(0.1));
            }

            resetLever(selectedTrap);
            finish(uuid, onComplete);
        }, killDelay);
    }

    private static boolean isStillValid(final Arena arena, final SquidPlayer player) {
        return arena.getState() == ArenaState.IN_GAME && arena.getPlayers().contains(player)
                && player.getBukkitPlayer().isOnline();
    }

    private static void finish(final UUID uuid, final Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }

        ACTIVE.remove(uuid);
    }

    private static void pullLever(final G1WallTrap trap) {
        if (!trap.hasLever()) {
            return;
        }

        CompatibilityUtils.setLeverPowered(trap.getLever(), true);
    }

    private static void resetLever(final G1WallTrap trap) {
        if (!trap.hasLever()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            CompatibilityUtils.setLeverPowered(trap.getLever(), false);
        }, 30L);
    }

    private static void triggerTripwire(final G1WallTrap trap) {
        final Location center = trap.getMuzzle().clone();
        final World world = center.getWorld();
        CompatibilityUtils.spawnCritBurst(world, center, 4);

        if (trap.getTripwireHook() == null) {
            return;
        }

        final Location hook = trap.getTripwireHook().getLocation().clone().add(0.5, 0.5, 0.5);
        final Material stringMaterial = CompatibilityUtils.material("TRIPWIRE", "STRING");
        CompatibilityUtils.spawnBlockBreakBurst(world, hook, stringMaterial, 8);
        CompatibilityUtils.spawnSmokeBurst(world, hook, 6);

        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {
                    final Block block = world.getBlockAt(hook.getBlockX() + x, hook.getBlockY() + y,
                            hook.getBlockZ() + z);

                    if (block.getType() == stringMaterial) {
                        CompatibilityUtils.spawnBlockBreakBurst(world, block.getLocation().add(0.5, 0.1, 0.5),
                                stringMaterial, 3);
                    }
                }
            }
        }
    }

    private static void fireArrow(final World world, final G1WallTrap trap, final Player target) {
        final Location muzzle = trap.getMuzzle().clone();
        final Vector direction = target.getEyeLocation().toVector().subtract(muzzle.toVector());

        if (direction.lengthSquared() == 0) {
            return;
        }

        direction.normalize();
        muzzle.setDirection(direction);

        final float speed = (float) SquidGame.getInstance().getMainConfig()
                .getDouble("game-settings.red-light-trap-arrow-speed", 3.2D);
        final org.bukkit.entity.Arrow arrow = CompatibilityUtils.spawnTrapArrow(world, muzzle, direction, speed, target);
        arrow.setMetadata(ARROW_METADATA, new FixedMetadataValue(SquidGame.getInstance(), target.getUniqueId()));

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!arrow.isDead()) {
                arrow.remove();
            }
        }, 30L);
    }
}
