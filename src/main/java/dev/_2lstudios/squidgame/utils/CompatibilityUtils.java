package dev._2lstudios.squidgame.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

public final class CompatibilityUtils {

    private static final boolean HAS_BLOCK_DATA = classExists("org.bukkit.block.data.BlockData");

    private CompatibilityUtils() {
    }

    public static void setType(final Block block, final Material material) {
        block.setType(material);
    }

    public static boolean isOpenableDoor(final Block block) {
        if (block == null) {
            return false;
        }

        if (HAS_BLOCK_DATA) {
            try {
                final Object data = block.getClass().getMethod("getBlockData").invoke(block);
                return Class.forName("org.bukkit.block.data.Openable").isInstance(data);
            } catch (final ReflectiveOperationException exception) {
                return false;
            }
        }

        try {
            final BlockState state = block.getState();
            return Class.forName("org.bukkit.material.Openable").isInstance(state.getData());
        } catch (final ReflectiveOperationException exception) {
            return false;
        }
    }

    public static void setDoorOpen(final Block block, final boolean open) {
        if (block == null) {
            return;
        }

        if (HAS_BLOCK_DATA) {
            try {
                final Object data = block.getClass().getMethod("getBlockData").invoke(block);
                final Class<?> openableClass = Class.forName("org.bukkit.block.data.Openable");

                if (!openableClass.isInstance(data)) {
                    return;
                }

                openableClass.getMethod("setOpen", boolean.class).invoke(data, open);
                block.getClass().getMethod("setBlockData", Class.forName("org.bukkit.block.data.BlockData"))
                        .invoke(block, data);
            } catch (final ReflectiveOperationException exception) {
                exception.printStackTrace();
            }

            return;
        }

        try {
            final BlockState state = block.getState();
            final Object data = state.getData();
            final Class<?> openableClass = Class.forName("org.bukkit.material.Openable");

            if (!openableClass.isInstance(data)) {
                return;
            }

            openableClass.getMethod("setOpen", boolean.class).invoke(data, open);
            state.getClass().getMethod("setData", Class.forName("org.bukkit.material.MaterialData")).invoke(state, data);
            state.update();
        } catch (final ReflectiveOperationException exception) {
            exception.printStackTrace();
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

    public static void setUnbreakable(final org.bukkit.inventory.meta.ItemMeta meta, final boolean unbreakable) {
        if (meta == null) {
            return;
        }

        try {
            meta.getClass().getMethod("setUnbreakable", boolean.class).invoke(meta, unbreakable);
        } catch (final ReflectiveOperationException exception) {
            // Unbreakable flag is unavailable on older server builds.
        }
    }

    public static Material material(final String... names) {
        for (final String name : names) {
            final Material material = Material.getMaterial(name);

            if (material != null) {
                return material;
            }
        }

        return Material.STONE;
    }

    private static final boolean HAS_PARTICLE_API = classExists("org.bukkit.Particle");

    public static Location cosmeticTrailLocation(final Player player) {
        final Location location = player.getLocation().clone();
        org.bukkit.util.Vector direction = location.getDirection();

        if (direction.lengthSquared() > 0.0001D) {
            direction = direction.setY(0);

            if (direction.lengthSquared() > 0.0001D) {
                location.add(direction.normalize().multiply(-0.55D));
            }
        }

        location.add((Math.random() - 0.5D) * 0.18D, 0.1D, (Math.random() - 0.5D) * 0.18D);
        return location;
    }

    private static Location jitterTrail(final Location location) {
        return location.clone().add((Math.random() - 0.5D) * 0.14D, Math.random() * 0.06D,
                (Math.random() - 0.5D) * 0.14D);
    }

    private static void playLegacyEffect(final org.bukkit.World world, final Location location,
            final String... effectNames) {
        playLegacyEffectWithData(world, location, 0, effectNames);
    }

    private static void playLegacyEffectWithData(final org.bukkit.World world, final Location location,
            final int data, final String... effectNames) {
        for (final String effectName : effectNames) {
            try {
                world.playEffect(location, org.bukkit.Effect.valueOf(effectName), data);
                return;
            } catch (final IllegalArgumentException exception) {
                // Try the next legacy effect name.
            }
        }
    }

    private static void playLegacyEffects(final org.bukkit.World world, final Location location, final int count,
            final String... effectNames) {
        for (int i = 0; i < count; i++) {
            playLegacyEffect(world, jitterTrail(location), effectNames);
        }
    }

    private static int legacyDustColorData(final org.bukkit.Color color) {
        return (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    public static void spawnHeartParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "HEART");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.05D, 0.12D, 0.0D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "HEART");
    }

    public static void spawnFlameParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "FLAME");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.04D, 0.12D, 0.01D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "MOBSPAWNER_FLAMES");
    }

    public static void spawnLargeSmokeParticles(final org.bukkit.World world, final Location location,
            final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "SMOKE_LARGE");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.05D, 0.12D, 0.01D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "LARGE_SMOKE", "SMOKE");
    }

    public static void spawnHappyVillagerParticles(final org.bukkit.World world, final Location location,
            final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"),
                        "VILLAGER_HAPPY");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.05D, 0.12D, 0.0D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "VILLAGER_PLANT_GROW");
    }

    public static void spawnEnchantmentParticles(final org.bukkit.World world, final Location location,
            final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"),
                        "ENCHANTMENT_TABLE");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.15D, 0.08D, 0.15D, 0.2D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "SPELL", "INSTANT_SPELL");
    }

    public static void spawnNoteParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "NOTE");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.05D, 0.12D, 1.0D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "NOTE");
    }

    public static void spawnColoredDustParticles(final org.bukkit.World world, final Location location,
            final int count, final org.bukkit.Color color) {
        if (HAS_PARTICLE_API) {
            try {
                final Class<?> particleClass = Class.forName("org.bukkit.Particle");
                final Object particle = Enum.valueOf((Class<Enum>) particleClass, "REDSTONE");
                final Class<?> dustOptionsClass = Class.forName("org.bukkit.Particle$DustOptions");
                final Object dustOptions = dustOptionsClass.getConstructor(org.bukkit.Color.class, float.class)
                        .newInstance(color, 0.8F);
                world.getClass()
                        .getMethod("spawnParticle", particleClass, Location.class, int.class, double.class,
                                double.class, double.class, double.class, Object.class)
                        .invoke(world, particle, location, count, 0.14D, 0.05D, 0.14D, 0.0D, dustOptions);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        final int data = legacyDustColorData(color);

        for (int i = 0; i < count; i++) {
            playLegacyEffectWithData(world, jitterTrail(location), data, "COLOURED_DUST", "REDSTONE");
        }
    }

    public static void spawnPortalParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "PORTAL");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.08D, 0.12D, 0.05D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "ENDER_SIGNAL");
    }

    public static void spawnMagicParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "SPELL_WITCH");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.06D, 0.12D, 0.0D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "SPELL", "INSTANT_SPELL");
    }

    public static void spawnCritParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "CRIT");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.05D, 0.12D, 0.02D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "CRIT", "MAGIC_CRIT");
    }

    public static void spawnSnowParticles(final org.bukkit.World world, final Location location, final int count) {
        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "SNOWBALL");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.12D, 0.05D, 0.12D, 0.0D);
            } catch (final ReflectiveOperationException exception) {
                // Ignore and fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "SNOWBALL_BREAK", "SNOW_SHOVEL");
    }

    public static void setLeverPowered(final Block block, final boolean powered) {
        if (block == null || block.getType() != Material.LEVER) {
            return;
        }

        if (HAS_BLOCK_DATA) {
            try {
                final Object data = block.getClass().getMethod("getBlockData").invoke(block);
                final Class<?> powerableClass = Class.forName("org.bukkit.block.data.Powerable");

                if (!powerableClass.isInstance(data)) {
                    return;
                }

                powerableClass.getMethod("setPowered", boolean.class).invoke(data, powered);
                block.getClass().getMethod("setBlockData", Class.forName("org.bukkit.block.data.BlockData"))
                        .invoke(block, data);
            } catch (final ReflectiveOperationException exception) {
                exception.printStackTrace();
            }

            return;
        }

        try {
            final org.bukkit.block.BlockState state = block.getState();
            final Object data = state.getData();
            final Class<?> leverClass = Class.forName("org.bukkit.material.Lever");

            if (!leverClass.isInstance(data)) {
                return;
            }

            leverClass.getMethod("setPowered", boolean.class).invoke(data, powered);
            state.getClass().getMethod("setData", Class.forName("org.bukkit.material.MaterialData")).invoke(state, data);
            state.update(true, false);
        } catch (final ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public static boolean isAirMaterial(final Material material) {
        if (material == null || material == Material.AIR) {
            return material == Material.AIR;
        }

        final String name = material.name();
        return "CAVE_AIR".equals(name) || "VOID_AIR".equals(name);
    }

    public static boolean isIronBar(final Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }

        final String name = material.name();
        return "IRON_FENCE".equals(name) || "IRON_BARS".equals(name);
    }

    public static boolean isButtonBlock(final Block block) {
        return block != null && isButtonMaterial(block.getType());
    }

    public static boolean isButtonMaterial(final Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }

        final String name = material.name();
        return name.endsWith("_BUTTON") || "STONE_BUTTON".equals(name) || "WOOD_BUTTON".equals(name);
    }

    public static boolean isSameBlock(final Location first, final Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }

        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    @SuppressWarnings("deprecation")
    public static Block getInteractionBlock(final Player player) {
        if (player == null) {
            return null;
        }

        try {
            final Object result = player.getClass().getMethod("getTargetBlock", java.util.HashSet.class, int.class)
                    .invoke(player, new java.util.HashSet<Byte>(), 5);

            if (result instanceof Block) {
                final Block target = (Block) result;

                if (target.getType() != Material.AIR) {
                    return target;
                }
            }
        } catch (final ReflectiveOperationException exception) {
            // Fall back to the block below the player.
        }

        return player.getLocation().getBlock();
    }

    public static void sendActionBar(final org.bukkit.entity.Player player, final String message) {
        if (player == null) {
            return;
        }

        final String formatted = message == null || message.isEmpty() ? ""
                : org.bukkit.ChatColor.translateAlternateColorCodes('&', message);

        if (trySpigotActionBar(player, formatted)) {
            return;
        }

        sendActionBarPacket(player, formatted);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean trySpigotActionBar(final org.bukkit.entity.Player player, final String message) {
        try {
            final Class<?> chatMessageTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            final Object actionBar = Enum.valueOf((Class<Enum>) chatMessageTypeClass.asSubclass(Enum.class), "ACTION_BAR");
            final Object spigot = player.getClass().getMethod("spigot").invoke(player);
            final Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            final Object component = textComponentClass.getConstructor(String.class).newInstance(message);

            for (final java.lang.reflect.Method method : spigot.getClass().getMethods()) {
                if (!"sendMessage".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }

                final Class<?>[] parameters = method.getParameterTypes();

                if (!parameters[0].isAssignableFrom(chatMessageTypeClass)) {
                    continue;
                }

                if (parameters[1].isArray()) {
                    method.invoke(spigot, actionBar, new Object[] { component });
                } else {
                    method.invoke(spigot, actionBar, component);
                }

                return true;
            }
        } catch (final ReflectiveOperationException exception) {
            // Fall back to NMS packets on older 1.8 builds.
        }

        return false;
    }

    private static void sendActionBarPacket(final org.bukkit.entity.Player player, final String message) {
        try {
            final String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            final String json = "{\"text\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
            final Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            final Object craftPlayer = craftPlayerClass.cast(player);
            final Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            final Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            final Class<?> chatSerializerClass = Class
                    .forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            final Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            final Object chatComponent = chatSerializerClass.getMethod("a", String.class).invoke(null, json);
            final Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            final Object packet = packetClass.getConstructor(chatComponentClass, byte.class).newInstance(chatComponent,
                    (byte) 2);
            final Class<?> packetBase = Class.forName("net.minecraft.server." + version + ".Packet");
            playerConnection.getClass().getMethod("sendPacket", packetBase).invoke(playerConnection, packet);
        } catch (final ReflectiveOperationException exception) {
            // Action bar is unavailable on this server build.
        }
    }

    public static void spawnCritBurst(final org.bukkit.World world, final Location location, final int count) {
        if (world == null || location == null || count <= 0) {
            return;
        }

        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "CRIT");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.2D, 0.3D, 0.2D, 0.05D);
            } catch (final ReflectiveOperationException exception) {
                // Fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "CRIT", "CRIT_MAGIC");
    }

    public static void spawnBlockBreakBurst(final org.bukkit.World world, final Location location,
            final Material block, final int count) {
        if (world == null || location == null || block == null || count <= 0) {
            return;
        }

        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "BLOCK_CRACK");
                final Object blockData = block.getClass().getMethod("createBlockData").invoke(block);
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class, Object.class)
                        .invoke(world, particle, location, count, 0.2D, 0.2D, 0.2D, 0.1D, blockData);
            } catch (final ReflectiveOperationException exception) {
                // Fall back below.
            }

            return;
        }

        @SuppressWarnings("deprecation")
        final int legacyId = block.getId();

        for (int index = 0; index < count; index++) {
            playLegacyEffectWithData(world, jitterTrapLocation(location), legacyId, "STEP_SOUND");
        }
    }

    public static void spawnSmokeBurst(final org.bukkit.World world, final Location location, final int count) {
        if (world == null || location == null || count <= 0) {
            return;
        }

        if (HAS_PARTICLE_API) {
            try {
                final Object particle = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.Particle"), "SMOKE_NORMAL");
                world.getClass()
                        .getMethod("spawnParticle", particle.getClass(), Location.class, int.class, double.class,
                                double.class, double.class, double.class)
                        .invoke(world, particle, location, count, 0.1D, 0.1D, 0.1D, 0.01D);
            } catch (final ReflectiveOperationException exception) {
                // Fall back below.
            }

            return;
        }

        playLegacyEffects(world, location, count, "SMOKE", "LARGE_SMOKE");
    }

    public static org.bukkit.entity.Arrow spawnTrapArrow(final org.bukkit.World world, final Location muzzle,
            final org.bukkit.util.Vector direction, final float speed, final Player shooter) {
        final org.bukkit.entity.Arrow arrow = world.spawnArrow(muzzle, direction, speed, 0.0F);
        arrow.setShooter(shooter);
        arrow.setCritical(true);

        try {
            arrow.getClass().getMethod("setPickupStatus", Class.forName("org.bukkit.entity.AbstractArrow$PickupStatus"))
                    .invoke(arrow, Enum.valueOf(
                            (Class<Enum>) Class.forName("org.bukkit.entity.AbstractArrow$PickupStatus"), "DISALLOWED"));
        } catch (final ReflectiveOperationException exception) {
            // 1.8 has no pickup status API.
        }

        try {
            arrow.getClass().getMethod("setKnockbackStrength", int.class).invoke(arrow, 0);
        } catch (final ReflectiveOperationException exception) {
            // Optional on older builds.
        }

        try {
            arrow.getClass().getMethod("setDamage", double.class).invoke(arrow, 0.0D);
        } catch (final ReflectiveOperationException exception) {
            // Optional on older builds.
        }

        return arrow;
    }

    public static void playSound(final Player player, final Location location, final String soundName,
            final float volume, final float pitch) {
        dev._2lstudios.jelly.utils.PlayerCompat.playSound(player, location, soundName, volume, pitch);
    }

    private static Location jitterTrapLocation(final Location location) {
        return location.clone().add((Math.random() - 0.5D) * 0.2D, (Math.random() - 0.5D) * 0.2D,
                (Math.random() - 0.5D) * 0.2D);
    }
}
