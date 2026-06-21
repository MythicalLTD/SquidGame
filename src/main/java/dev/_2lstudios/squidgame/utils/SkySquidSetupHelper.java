package dev._2lstudios.squidgame.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;

public final class SkySquidSetupHelper {

    private SkySquidSetupHelper() {
    }

    public static Cuboid createTowerZone(final Location center, final int radius, final int heightBelow,
            final int heightAbove) {
        final Vector3 firstPoint = new Vector3(center.getX() - radius, center.getY() - heightBelow,
                center.getZ() - radius);
        final Vector3 secondPoint = new Vector3(center.getX() + radius, center.getY() + heightAbove,
                center.getZ() + radius);
        return new Cuboid(firstPoint, secondPoint);
    }

    public static Location findNearbyButton(final Location center, final int radius) {
        if (center == null || center.getWorld() == null) {
            return null;
        }

        final World world = center.getWorld();
        final int centerX = center.getBlockX();
        final int centerY = center.getBlockY();
        final int centerZ = center.getBlockZ();
        Location closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - 2; y <= centerY + 2; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    final Block block = world.getBlockAt(x, y, z);

                    if (!CompatibilityUtils.isButtonBlock(block)) {
                        continue;
                    }

                    final double distance = block.getLocation().distanceSquared(center);

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = block.getLocation();
                    }
                }
            }
        }

        return closest;
    }

    public static boolean hasTower(final Configuration config, final int tower) {
        return config.contains(getSpawnKey(tower) + ".x");
    }

    public static boolean hasBridge(final Configuration config, final int bridge) {
        return config.contains(getBridgeKey(bridge) + ".first_point.x");
    }

    public static String getSpawnKey(final int tower) {
        if (tower == 1) {
            return "games.sky-squid.square-spawn";
        }

        if (tower == 2) {
            return "games.sky-squid.triangle-spawn";
        }

        return "games.sky-squid.circle-spawn";
    }

    public static String getButtonKey(final int tower) {
        if (tower == 1) {
            return "games.sky-squid.square-button";
        }

        if (tower == 2) {
            return "games.sky-squid.triangle-button";
        }

        return "games.sky-squid.circle-button";
    }

    public static String getSafeZoneKey(final int tower) {
        if (tower == 2) {
            return "games.sky-squid.tower-triangle";
        }

        if (tower == 3) {
            return "games.sky-squid.tower-circle";
        }

        return null;
    }

    public static String getBridgeKey(final int bridge) {
        return bridge == 1 ? "games.sky-squid.bridge-square" : "games.sky-squid.bridge-triangle";
    }

    public static String getTowerName(final int tower) {
        if (tower == 1) {
            return "Square";
        }

        if (tower == 2) {
            return "Triangle";
        }

        return "Circle";
    }

    public static List<Location> collectCuboidLocations(final Cuboid cuboid, final World world) {
        final List<Location> locations = new ArrayList<>();

        if (cuboid == null || world == null) {
            return locations;
        }

        final Vector3 first = cuboid.getFirstPoint();
        final Vector3 second = cuboid.getSecondPoint();
        final int minX = (int) Math.floor(Math.min(first.getX(), second.getX()));
        final int maxX = (int) Math.floor(Math.max(first.getX(), second.getX()));
        final int minY = (int) Math.floor(Math.min(first.getY(), second.getY()));
        final int maxY = (int) Math.floor(Math.max(first.getY(), second.getY()));
        final int minZ = (int) Math.floor(Math.min(first.getZ(), second.getZ()));
        final int maxZ = (int) Math.floor(Math.max(first.getZ(), second.getZ()));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    locations.add(new Location(world, x, y, z));
                }
            }
        }

        return locations;
    }

    public static Cuboid expandCuboidY(final Cuboid cuboid, final int minY, final int maxY) {
        if (cuboid == null) {
            return null;
        }

        final Vector3 first = cuboid.getFirstPoint();
        final Vector3 second = cuboid.getSecondPoint();
        final double expandedMinY = Math.min(Math.min(first.getY(), second.getY()), minY);
        final double expandedMaxY = Math.max(Math.max(first.getY(), second.getY()), maxY);
        final Vector3 expandedFirst = new Vector3(
                Math.min(first.getX(), second.getX()),
                expandedMinY,
                Math.min(first.getZ(), second.getZ()));
        final Vector3 expandedSecond = new Vector3(
                Math.max(first.getX(), second.getX()),
                expandedMaxY,
                Math.max(first.getZ(), second.getZ()));
        return new Cuboid(expandedFirst, expandedSecond);
    }

    public static void placeBridgeBlocks(final World world, final List<Vector3> slots, final Material material) {
        if (world == null || slots == null || material == null) {
            return;
        }

        ensureChunksLoaded(slots, world);

        for (final Vector3 slot : slots) {
            CompatibilityUtils.setType(world.getBlockAt((int) Math.floor(slot.getX()), (int) Math.floor(slot.getY()),
                    (int) Math.floor(slot.getZ())), material);
        }
    }

    public static void clearBridgeBlocks(final World world, final List<Vector3> slots) {
        placeBridgeBlocks(world, slots, Material.AIR);
    }

    public static Material getBridgeMarkerMaterial(final int bridge) {
        return bridge == 1 ? Material.REDSTONE_BLOCK : Material.GOLD_BLOCK;
    }

    public static String getBridgeMarkerName(final int bridge) {
        return bridge == 1 ? "redstone" : "gold";
    }

    public static boolean isBridgeMarkerBlock(final Material material, final int bridge) {
        if (material == null) {
            return false;
        }

        return material == getBridgeMarkerMaterial(bridge);
    }

    public static Cuboid createPathSearchCuboid(final Location from, final Location to,
            final double horizontalPadding, final int verticalPadding) {
        final double minX = Math.min(from.getX(), to.getX()) - horizontalPadding;
        final double maxX = Math.max(from.getX(), to.getX()) + horizontalPadding;
        final double minY = Math.min(from.getY(), to.getY()) - verticalPadding;
        final double maxY = Math.max(from.getY(), to.getY()) + verticalPadding;
        final double minZ = Math.min(from.getZ(), to.getZ()) - horizontalPadding;
        final double maxZ = Math.max(from.getZ(), to.getZ()) + horizontalPadding;
        return new Cuboid(new Vector3(minX, minY, minZ), new Vector3(maxX, maxY, maxZ));
    }

    public static List<Vector3> scanBridgeMarkerSlots(final Configuration config, final World world, final int bridge,
            final double horizontalPadding, final int verticalPadding) {
        final List<Vector3> slots = new ArrayList<>();

        if (config == null || world == null) {
            return slots;
        }

        final int fromTower = bridge == 1 ? 1 : 2;
        final int toTower = bridge == 1 ? 2 : 3;
        final Location from = config.getLocation(getSpawnKey(fromTower), false);
        final Location to = config.getLocation(getSpawnKey(toTower), false);

        if (from == null || to == null) {
            return slots;
        }

        final Material marker = getBridgeMarkerMaterial(bridge);
        final Cuboid searchZone = createPathSearchCuboid(from, to, horizontalPadding, verticalPadding);

        for (final Location location : collectCuboidLocations(searchZone, world)) {
            if (world.getBlockAt(location).getType() == marker) {
                slots.add(new Vector3(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            }
        }

        slots.sort((first, second) -> Double.compare(distanceSquared(from, first), distanceSquared(from, second)));
        return slots;
    }

    private static double distanceSquared(final Location origin, final Vector3 point) {
        final double deltaX = origin.getX() - point.getX();
        final double deltaY = origin.getY() - point.getY();
        final double deltaZ = origin.getZ() - point.getZ();
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public static int countBridgeMarkers(final Configuration config, final World world, final int bridge,
            final double horizontalPadding, final int verticalPadding) {
        return scanBridgeMarkerSlots(config, world, bridge, horizontalPadding, verticalPadding).size();
    }

    public static boolean hasSavedBridgeMarkers(final Configuration config, final World world, final int bridge,
            final double horizontalPadding, final int verticalPadding) {
        if (!loadBridgeSlots(config, bridge).isEmpty()) {
            return true;
        }

        return world != null && countBridgeMarkers(config, world, bridge, horizontalPadding, verticalPadding) > 0;
    }

    public static List<Vector3> scanBridgeSpawnSlots(final Cuboid cuboid, final World world) {
        final List<Vector3> airSlots = new ArrayList<>();
        final List<Vector3> quartzSlots = new ArrayList<>();

        if (cuboid == null || world == null) {
            return airSlots;
        }

        final Vector3 first = cuboid.getFirstPoint();
        final Vector3 second = cuboid.getSecondPoint();
        final int minX = (int) Math.floor(Math.min(first.getX(), second.getX()));
        final int maxX = (int) Math.floor(Math.max(first.getX(), second.getX()));
        final int minY = (int) Math.floor(Math.min(first.getY(), second.getY()));
        final int maxY = (int) Math.floor(Math.max(first.getY(), second.getY()));
        final int minZ = (int) Math.floor(Math.min(first.getZ(), second.getZ()));
        final int maxZ = (int) Math.floor(Math.max(first.getZ(), second.getZ()));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Material material = world.getBlockAt(x, y, z).getType();

                    if (isSkySquidBridgeBlock(material)) {
                        quartzSlots.add(new Vector3(x, y, z));
                    } else if (isBridgeFillable(material)) {
                        airSlots.add(new Vector3(x, y, z));
                    }
                }
            }
        }

        return quartzSlots.isEmpty() ? airSlots : quartzSlots;
    }

    public static void saveBridgeSlots(final Configuration config, final int bridge, final List<Vector3> slots) {
        final String base = getBridgeKey(bridge);
        final int previousCount = config.getInt(base + ".slot-count", 0);

        config.set(base + ".first_point", null);
        config.set(base + ".second_point", null);

        for (int index = 1; index <= previousCount; index++) {
            config.set(base + ".slots." + index, null);
        }

        config.set(base + ".slot-count", slots.size());

        for (int index = 0; index < slots.size(); index++) {
            config.setVector3(base + ".slots." + (index + 1), slots.get(index));
        }
    }

    public static List<Vector3> loadBridgeSlots(final Configuration config, final int bridge) {
        final List<Vector3> slots = new ArrayList<>();
        final String base = getBridgeKey(bridge);
        final int count = config.getInt(base + ".slot-count", 0);

        for (int index = 1; index <= count; index++) {
            final Vector3 slot = config.getVector3(base + ".slots." + index);

            if (slot != null) {
                slots.add(slot);
            }
        }

        return slots;
    }

    public static boolean isSkySquidBridgeBlock(final Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }

        return material.name().contains("QUARTZ");
    }

    public static void ensureChunksLoaded(final List<Vector3> slots, final World world) {
        if (slots == null || slots.isEmpty() || world == null) {
            return;
        }

        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;

        for (final Vector3 slot : slots) {
            final int chunkX = (int) Math.floor(slot.getX()) >> 4;
            final int chunkZ = (int) Math.floor(slot.getZ()) >> 4;
            minChunkX = Math.min(minChunkX, chunkX);
            maxChunkX = Math.max(maxChunkX, chunkX);
            minChunkZ = Math.min(minChunkZ, chunkZ);
            maxChunkZ = Math.max(maxChunkZ, chunkZ);
        }

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ).load(true);
            }
        }
    }

    public static boolean isBridgeFillable(final Material material) {
        if (material == null) {
            return false;
        }

        return CompatibilityUtils.isAirMaterial(material);
    }

    public static boolean isNearBridgePath(final Configuration config, final int bridge, final Cuboid zone,
            final double padding) {
        if (zone == null) {
            return false;
        }

        final int fromTower = bridge == 1 ? 1 : 2;
        final int toTower = bridge == 1 ? 2 : 3;
        final Location from = config.getLocation(getSpawnKey(fromTower), false);
        final Location to = config.getLocation(getSpawnKey(toTower), false);

        if (from == null || to == null) {
            return true;
        }

        final Location midpoint = new Location(null,
                (from.getX() + to.getX()) / 2.0D,
                (from.getY() + to.getY()) / 2.0D,
                (from.getZ() + to.getZ()) / 2.0D);
        return isInsideHorizontal(zone, from, padding) || isInsideHorizontal(zone, to, padding)
                || isInsideHorizontal(zone, midpoint, padding);
    }

    private static boolean isInsideHorizontal(final Cuboid zone, final Location location, final double padding) {
        final Vector3 first = zone.getFirstPoint();
        final Vector3 second = zone.getSecondPoint();
        final double minX = Math.min(first.getX(), second.getX()) - padding;
        final double maxX = Math.max(first.getX(), second.getX()) + padding;
        final double minZ = Math.min(first.getZ(), second.getZ()) - padding;
        final double maxZ = Math.max(first.getZ(), second.getZ()) + padding;

        return location.getX() >= minX && location.getX() <= maxX
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public static List<Location> scanGlassBridgeSlots(final Cuboid cuboid, final World world) {
        final List<Location> locations = new ArrayList<>();

        if (cuboid == null || world == null) {
            return locations;
        }

        for (final Location location : collectCuboidLocations(cuboid, world)) {
            if (isGlassBridgeMaterial(world.getBlockAt(location).getType())) {
                locations.add(location.clone());
            }
        }

        return locations;
    }

    public static List<BlockState> captureBridgeTemplate(final Cuboid cuboid, final World world) {
        final List<BlockState> templates = new ArrayList<>();

        if (cuboid == null || world == null) {
            return templates;
        }

        for (final Vector3 slot : scanBridgeSpawnSlots(cuboid, world)) {
            final Block block = world.getBlockAt((int) Math.floor(slot.getX()), (int) Math.floor(slot.getY()),
                    (int) Math.floor(slot.getZ()));

            if (isSkySquidBridgeBlock(block.getType())) {
                templates.add(block.getState());
            }
        }

        return templates;
    }

    public static boolean isGlassBridgeMaterial(final Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }

        final String name = material.name();
        return material == Material.GLASS || name.contains("STAINED_GLASS");
    }

    public static boolean isSkySquidBridgeMaterial(final Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }

        final String name = material.name();
        return name.contains("QUARTZ") || name.contains("GLASS") || name.contains("STAINED_GLASS");
    }

    public static String getSetupStatus(final Configuration config, final World world, final double horizontalPadding,
            final int verticalPadding) {
        final String square = hasTower(config, 1) ? "&a✔" : "&c✘";
        final String triangle = hasTower(config, 2) ? "&a✔" : "&c✘";
        final String circle = hasTower(config, 3) ? "&a✔" : "&c✘";
        final String bridgeOne = hasSavedBridgeMarkers(config, world, 1, horizontalPadding, verticalPadding) ? "&a✔"
                : "&c✘";
        final String bridgeTwo = hasSavedBridgeMarkers(config, world, 2, horizontalPadding, verticalPadding) ? "&a✔"
                : "&c✘";
        final String lobby = config.contains("games.sky-squid.lobby.x") || config.contains("arena.waiting_room.x")
                ? "&a✔"
                : "&7~";

        return "&7Lobby " + lobby
                + " &8| &7Square " + square
                + " &8| &7Triangle " + triangle
                + " &8| &7Circle " + circle
                + "\n&7Bridge 1 &c(redstone) " + bridgeOne
                + " &8| &7Bridge 2 &6(gold) " + bridgeTwo;
    }

    public static String getSetupStatus(final Configuration config) {
        return getSetupStatus(config, null, 14.0D, 4);
    }
}
