package dev._2lstudios.squidgame.arena.games.g1;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;

public final class G1TrapScanner {

    private G1TrapScanner() {
    }

    public static List<G1WallTrap> scan(final Cuboid killZone, final Cuboid barrier, final World world) {
        if (killZone == null || world == null) {
            return new ArrayList<>();
        }

        final Vector3 first = killZone.getFirstPoint();
        final Vector3 second = killZone.getSecondPoint();
        final int minX = (int) Math.floor(Math.min(first.getX(), second.getX()));
        final int maxX = (int) Math.floor(Math.max(first.getX(), second.getX()));
        final int minZ = (int) Math.floor(Math.min(first.getZ(), second.getZ()));
        final int maxZ = (int) Math.floor(Math.max(first.getZ(), second.getZ()));
        final int minY = (int) Math.floor(Math.min(first.getY(), second.getY()));
        final int maxY = getScanMaxY(minY, barrier);
        final List<G1WallTrap> traps = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = world.getBlockAt(x, y, z);

                    if (block.getType() != Material.LEVER) {
                        continue;
                    }

                    final Block tripwireHook = findNearestTripwireHook(block, world);
                    final Location muzzle = buildMuzzle(block, tripwireHook);
                    traps.add(new G1WallTrap(block, tripwireHook, muzzle));
                }
            }
        }

        return G1WallTrap.dedupe(traps);
    }

    public static G1WallTrap createFallbackTrap(final Cuboid killZone, final Location playerLocation,
            final World world) {
        if (killZone == null || playerLocation == null || world == null) {
            return null;
        }

        final Vector3 first = killZone.getFirstPoint();
        final Vector3 second = killZone.getSecondPoint();
        final double centerX = (first.getX() + second.getX()) / 2.0D;
        final double centerZ = (first.getZ() + second.getZ()) / 2.0D;
        final double playerX = playerLocation.getX();
        final double playerZ = playerLocation.getZ();

        final double wallX = Math.abs(playerX - first.getX()) < Math.abs(playerX - second.getX()) ? first.getX()
                : second.getX();
        final double wallZ = Math.abs(playerZ - first.getZ()) < Math.abs(playerZ - second.getZ()) ? first.getZ()
                : second.getZ();

        final boolean useXWall = Math.abs(playerX - wallX) <= Math.abs(playerZ - wallZ);
        final int blockX = (int) Math.floor(useXWall ? wallX : centerX);
        final int blockZ = (int) Math.floor(useXWall ? centerZ : wallZ);
        final int blockY = (int) Math.floor(playerLocation.getY() + 1.0D);
        final Location muzzle = new Location(world, blockX + 0.5, blockY + 0.5, blockZ + 0.5);
        final Vector direction = playerLocation.toVector().subtract(muzzle.toVector());

        if (direction.lengthSquared() > 0) {
            muzzle.setDirection(direction.normalize());
        }

        return new G1WallTrap(null, null, muzzle);
    }

    private static int getScanMaxY(final int minY, final Cuboid barrier) {
        final int scanHeight = SquidGame.getInstance().getMainConfig()
                .getInt("game-settings.red-light-trap-scan-height", 40);

        if (barrier == null) {
            return minY + scanHeight;
        }

        final Vector3 first = barrier.getFirstPoint();
        final Vector3 second = barrier.getSecondPoint();
        final int barrierMaxY = (int) Math.ceil(Math.max(first.getY(), second.getY()));

        return Math.max(minY + scanHeight, barrierMaxY);
    }

    private static Block findNearestTripwireHook(final Block lever, final World world) {
        final Material tripwireHookMaterial = CompatibilityUtils.material("TRIPWIRE_HOOK");
        final Material tripwireMaterial = CompatibilityUtils.material("TRIPWIRE", "STRING");
        final Location leverCenter = lever.getLocation().clone().add(0.5, 0.5, 0.5);
        Block closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -6; x <= 6; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -6; z <= 6; z++) {
                    final Block block = world.getBlockAt(lever.getX() + x, lever.getY() + y, lever.getZ() + z);
                    final Material type = block.getType();

                    if (type != tripwireHookMaterial && type != tripwireMaterial) {
                        continue;
                    }

                    final double distance = block.getLocation().add(0.5, 0.5, 0.5).distanceSquared(leverCenter);

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = type == tripwireHookMaterial ? block : findHookForTripwire(block, world);
                    }
                }
            }
        }

        return closest;
    }

    private static Block findHookForTripwire(final Block tripwire, final World world) {
        final Material tripwireHookMaterial = CompatibilityUtils.material("TRIPWIRE_HOOK");

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    final Block block = world.getBlockAt(tripwire.getX() + x, tripwire.getY() + y,
                            tripwire.getZ() + z);

                    if (block.getType() == tripwireHookMaterial) {
                        return block;
                    }
                }
            }
        }

        return tripwire;
    }

    private static Location buildMuzzle(final Block lever, final Block tripwireHook) {
        final Location leverCenter = lever.getLocation().clone().add(0.5, 0.5, 0.5);

        if (tripwireHook == null) {
            return leverCenter;
        }

        final Location hookCenter = tripwireHook.getLocation().clone().add(0.5, 0.5, 0.5);
        final Vector offset = hookCenter.toVector().subtract(leverCenter.toVector());

        if (offset.lengthSquared() == 0) {
            return leverCenter;
        }

        return leverCenter.setDirection(offset.normalize());
    }
}
