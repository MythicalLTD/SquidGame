package dev._2lstudios.squidgame.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.jelly.math.Cuboid;

public final class IronBarBarrierSession {

    private final Map<Block, BlockState> savedStates = new HashMap<>();
    private boolean hidden;

    public void hide(final Configuration config, final World world, final String configKey) {
        if (this.hidden || world == null) {
            return;
        }

        for (final Cuboid region : loadRegions(config, configKey)) {
            this.hideInRegion(world, region);
        }

        this.hidden = true;
    }

    public void restore() {
        if (!this.hidden) {
            return;
        }

        for (final Map.Entry<Block, BlockState> entry : this.savedStates.entrySet()) {
            entry.getValue().update(true, false);
        }

        this.savedStates.clear();
        this.hidden = false;
    }

    public static List<Cuboid> loadRegions(final Configuration config, final String configKey) {
        final List<Cuboid> regions = new ArrayList<>();
        final String base = "games." + configKey;
        final int count = config.getInt(base + ".iron-bar-count", 0);

        if (count > 0) {
            for (int index = 1; index <= count; index++) {
                final Cuboid cuboid = config.getCuboid(base + ".iron-bars." + index);

                if (cuboid != null) {
                    regions.add(cuboid);
                }
            }
        } else {
            final Cuboid single = config.getCuboid(base + ".iron-bars");

            if (single != null) {
                regions.add(single);
            }
        }

        return regions;
    }

    private void hideInRegion(final World world, final Cuboid region) {
        if (region == null) {
            return;
        }

        final int minX = (int) Math.min(region.getFirstPoint().getX(), region.getSecondPoint().getX());
        final int maxX = (int) Math.max(region.getFirstPoint().getX(), region.getSecondPoint().getX());
        final int minY = (int) Math.min(region.getFirstPoint().getY(), region.getSecondPoint().getY());
        final int maxY = (int) Math.max(region.getFirstPoint().getY(), region.getSecondPoint().getY());
        final int minZ = (int) Math.min(region.getFirstPoint().getZ(), region.getSecondPoint().getZ());
        final int maxZ = (int) Math.max(region.getFirstPoint().getZ(), region.getSecondPoint().getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = world.getBlockAt(x, y, z);

                    if (!CompatibilityUtils.isIronBar(block.getType()) || this.savedStates.containsKey(block)) {
                        continue;
                    }

                    this.savedStates.put(block, block.getState());
                    CompatibilityUtils.setType(block, Material.AIR);
                }
            }
        }
    }
}
