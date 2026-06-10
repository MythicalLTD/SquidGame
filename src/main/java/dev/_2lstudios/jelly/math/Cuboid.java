package dev._2lstudios.jelly.math;

import org.bukkit.Location;

public class Cuboid {
    private final Vector3 firstPoint;
    private final Vector3 secondPoint;

    public Cuboid(final Vector3 firstPoint, final Vector3 secondPoint) {
        this.firstPoint = firstPoint;
        this.secondPoint = secondPoint;
    }

    public boolean isBetween(final double xP, final double zP) {
        double minX = Math.min(this.firstPoint.getX(), this.secondPoint.getX());
        double maxX = Math.max(this.firstPoint.getX(), this.secondPoint.getX());

        double minZ = Math.min(this.firstPoint.getZ(), this.secondPoint.getZ());
        double maxZ = Math.max(this.firstPoint.getZ(), this.secondPoint.getZ());

        return minX <= xP && xP <= maxX && minZ <= zP && zP <= maxZ;
    }

    public boolean isBetween(final Location target) {
        double xP = target.getX();
        double zP = target.getZ();

        return this.isBetween(xP, zP);
    }

    public boolean isBetween(final Vector3 target) {
        double xP = target.getX();
        double zP = target.getZ();

        return this.isBetween(xP, zP);
    }

    public Vector3 getFirstPoint() {
        return this.firstPoint;
    }

    public Vector3 getSecondPoint() {
        return this.secondPoint;
    }
}
