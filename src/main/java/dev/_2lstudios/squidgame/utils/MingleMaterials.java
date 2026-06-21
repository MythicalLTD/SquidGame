package dev._2lstudios.squidgame.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class MingleMaterials {

    private static final boolean HAS_BLOCK_DATA = classExists("org.bukkit.block.data.BlockData");

    private MingleMaterials() {
    }

    public static boolean isOnPlatform(final Location location) {
        if (isPlatformBlock(getSupportBlock(location, true))) {
            return true;
        }

        return isPlatformBlock(getSupportBlock(location, false));
    }

    public static boolean isOnRoomFloor(final Location location) {
        return getRoomFloorBlock(location) != null;
    }

    public static Block getRoomFloorBlock(final Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        final Block feet = location.getBlock();
        final Block[] preferred = new Block[] {
                feet.getRelative(BlockFace.DOWN),
                feet,
                feet.getRelative(BlockFace.DOWN, 2),
                feet.getRelative(BlockFace.DOWN, 3),
                feet.getRelative(BlockFace.UP)
        };

        for (final Block block : preferred) {
            if (isRoomBlock(block)) {
                return block;
            }
        }

        final int centerX = feet.getX();
        final int centerY = feet.getY();
        final int centerZ = feet.getZ();

        for (int dy = 0; dy >= -4; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    final Block block = feet.getWorld().getBlockAt(centerX + dx, centerY + dy, centerZ + dz);

                    if (isRoomBlock(block)) {
                        return block;
                    }
                }
            }
        }

        return null;
    }

    public static String getRoomColorName(final Location location) {
        final Block block = getRoomFloorBlock(location);

        if (block == null) {
            return null;
        }

        return getRoomColorName(block);
    }

    public static boolean isPlatformBlock(final Block block) {
        if (block == null) {
            return false;
        }

        final Material type = block.getType();
        final String name = type.name();

        if ("ORANGE_TERRACOTTA".equals(name) || "ORANGE_GLAZED_TERRACOTTA".equals(name)) {
            return true;
        }

        if (name.startsWith("ACACIA_")
                && (name.contains("PLANK") || name.contains("SLAB") || name.contains("STAIRS"))) {
            return true;
        }

        if ("DIAMOND_BLOCK".equals(name)) {
            return true;
        }

        if ("LANTERN".equals(name) || "SOUL_LANTERN".equals(name) || "SEA_LANTERN".equals(name)
                || "JACK_O_LANTERN".equals(name)) {
            return true;
        }

        if (isLitLamp(block, name)) {
            return true;
        }

        if (!HAS_BLOCK_DATA) {
            final byte data = block.getData();

            if ("STAINED_CLAY".equals(name) && data == 1) {
                return true;
            }

            if ("WOOD".equals(name) && data == 4) {
                return true;
            }

            if (("WOOD_STEP".equals(name) || "WOOD_DOUBLE_STEP".equals(name)) && (data & 0x7) == 4) {
                return true;
            }

            if ("WOOD_STAIRS".equals(name) && data == 4) {
                return true;
            }
        }

        return false;
    }

    public static boolean isRoomBlock(final Block block) {
        return getRoomColorName(block) != null;
    }

    public static String getRoomColorName(final Block block) {
        if (block == null) {
            return null;
        }

        final Material type = block.getType();
        final String name = type.name();

        if (name.endsWith("_WOOL") || name.endsWith("_CARPET")) {
            return colorFromModernName(name);
        }

        if (name.endsWith("_CONCRETE") && !name.endsWith("_CONCRETE_POWDER")) {
            return colorFromModernName(name);
        }

        if (name.endsWith("_TERRACOTTA") || name.endsWith("_GLAZED_TERRACOTTA")) {
            return colorFromModernName(name);
        }

        if (!HAS_BLOCK_DATA) {
            final byte data = block.getData();

            if ("WOOL".equals(name) || "CARPET".equals(name) || "STAINED_CLAY".equals(name)) {
                return colorFromLegacyData(data);
            }
        }

        return null;
    }

    private static Block getSupportBlock(final Location location, final boolean includeFeetBlock) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        final Block feet = location.getBlock();

        if (includeFeetBlock && isSolidSupport(feet)) {
            return feet;
        }

        final Block below = feet.getRelative(BlockFace.DOWN);

        if (isSolidSupport(below)) {
            return below;
        }

        final Block twoBelow = below.getRelative(BlockFace.DOWN);

        if (isSolidSupport(twoBelow)) {
            return twoBelow;
        }

        return below;
    }

    private static boolean isSolidSupport(final Block block) {
        return block != null && block.getType() != Material.AIR;
    }

    @SuppressWarnings("deprecation")
    private static boolean isLitLamp(final Block block, final String name) {
        if ("REDSTONE_LAMP_ON".equals(name)) {
            return true;
        }

        if (!"REDSTONE_LAMP".equals(name)) {
            return false;
        }

        if (HAS_BLOCK_DATA) {
            try {
                final Object data = block.getClass().getMethod("getBlockData").invoke(block);
                final Object lit = data.getClass().getMethod("isLit").invoke(data);
                return lit instanceof Boolean && (Boolean) lit;
            } catch (final ReflectiveOperationException exception) {
                return false;
            }
        }

        return (block.getData() & 0x8) != 0;
    }

    private static String colorFromModernName(final String materialName) {
        if (materialName.startsWith("LIME_")) {
            return "lime";
        }

        if (materialName.startsWith("YELLOW_")) {
            return "yellow";
        }

        if (materialName.startsWith("WHITE_")) {
            return "white";
        }

        if (materialName.startsWith("RED_")) {
            return "red";
        }

        if (materialName.startsWith("BLUE_")) {
            return "blue";
        }

        if (materialName.startsWith("PURPLE_")) {
            return "purple";
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    private static String colorFromLegacyData(final byte data) {
        switch (data) {
            case 5:
                return "lime";
            case 4:
                return "yellow";
            case 0:
                return "white";
            case 14:
                return "red";
            case 11:
                return "blue";
            case 10:
                return "purple";
            default:
                return null;
        }
    }

    private static boolean classExists(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException exception) {
            return false;
        }
    }
}
