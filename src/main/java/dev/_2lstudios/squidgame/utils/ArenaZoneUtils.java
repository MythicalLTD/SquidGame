package dev._2lstudios.squidgame.utils;

import org.bukkit.Location;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;

public final class ArenaZoneUtils {

    private ArenaZoneUtils() {
    }

    public static boolean isInsideHorizontally(final Location location, final Cuboid zone, final double padding) {
        if (location == null || zone == null) {
            return false;
        }

        final Vector3 first = zone.getFirstPoint();
        final Vector3 second = zone.getSecondPoint();
        final double minX = Math.min(first.getX(), second.getX()) - padding;
        final double maxX = Math.max(first.getX(), second.getX()) + padding;
        final double minZ = Math.min(first.getZ(), second.getZ()) - padding;
        final double maxZ = Math.max(first.getZ(), second.getZ()) + padding;

        return location.getX() >= minX && location.getX() <= maxX
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public static boolean isInsideWithVerticalPadding(final Location location, final Cuboid zone,
            final double horizontalPadding, final double verticalPadding) {
        if (location == null || zone == null) {
            return false;
        }

        if (!isInsideHorizontally(location, zone, horizontalPadding)) {
            return false;
        }

        final Vector3 first = zone.getFirstPoint();
        final Vector3 second = zone.getSecondPoint();
        final double minY = Math.min(first.getY(), second.getY()) - verticalPadding;
        final double maxY = Math.max(first.getY(), second.getY()) + verticalPadding;

        return location.getY() >= minY && location.getY() <= maxY;
    }
}
