package dev._2lstudios.jelly.utils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class PlayerCompat {

    private PlayerCompat() {
    }

    public static void playSound(final Player player, final Location location, final String soundName,
            final float volume, final float pitch) {
        if (player == null || location == null || soundName == null || soundName.isEmpty()) {
            return;
        }

        final Sound enumSound = resolveSoundEnum(soundName);

        if (enumSound != null) {
            player.playSound(location, enumSound, volume, pitch);
            return;
        }

        if (tryStringPlaySound(player, location, soundName, volume, pitch)) {
            return;
        }

        playNamedSoundPacket(player, location, soundName, volume, pitch);
    }

    public static void sendTitle(final Player player, final String title, final String subtitle, final int fadeInTicks,
            final int stayTicks, final int fadeOutTicks) {
        if (player == null) {
            return;
        }

        final String safeTitle = title == null ? "" : title;
        final String safeSubtitle = subtitle == null ? "" : subtitle;

        if (tryTimedTitle(player, safeTitle, safeSubtitle, fadeInTicks, stayTicks, fadeOutTicks)) {
            return;
        }

        if (trySimpleTitle(player, safeTitle, safeSubtitle)) {
            return;
        }

        sendTitlePacket(player, safeTitle, safeSubtitle, fadeInTicks, stayTicks, fadeOutTicks);
    }

    private static Sound resolveSoundEnum(final String soundName) {
        if (soundName == null || soundName.contains(".")) {
            return null;
        }

        for (final Sound sound : Sound.values()) {
            if (sound.name().equalsIgnoreCase(soundName)) {
                return sound;
            }
        }

        return null;
    }

    private static boolean tryStringPlaySound(final Player player, final Location location, final String soundName,
            final float volume, final float pitch) {
        try {
            player.getClass().getMethod("playSound", Location.class, String.class, float.class, float.class)
                    .invoke(player, location, soundName, volume, pitch);
            return true;
        } catch (final ReflectiveOperationException exception) {
            return false;
        }
    }

    private static void playNamedSoundPacket(final Player player, final Location location, final String soundName,
            final float volume, final float pitch) {
        try {
            final String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            final Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            final Object craftPlayer = craftPlayerClass.cast(player);
            final Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            final Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            final Class<?> packetClass = Class
                    .forName("net.minecraft.server." + version + ".PacketPlayOutNamedSoundEffect");
            final Object packet = packetClass
                    .getConstructor(String.class, double.class, double.class, double.class, float.class, float.class)
                    .newInstance(soundName, location.getX(), location.getY(), location.getZ(), volume, pitch);
            final Class<?> packetBase = Class.forName("net.minecraft.server." + version + ".Packet");
            playerConnection.getClass().getMethod("sendPacket", packetBase).invoke(playerConnection, packet);
        } catch (final ReflectiveOperationException exception) {
            // Sound is unavailable on this server build.
        }
    }

    private static boolean tryTimedTitle(final Player player, final String title, final String subtitle,
            final int fadeInTicks, final int stayTicks, final int fadeOutTicks) {
        try {
            player.getClass()
                    .getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class)
                    .invoke(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
            return true;
        } catch (final ReflectiveOperationException exception) {
            return false;
        }
    }

    private static boolean trySimpleTitle(final Player player, final String title, final String subtitle) {
        try {
            try {
                player.getClass().getMethod("resetTitle").invoke(player);
            } catch (final ReflectiveOperationException exception) {
                // Optional on older builds.
            }

            player.getClass().getMethod("sendTitle", String.class, String.class).invoke(player, title, subtitle);
            return true;
        } catch (final ReflectiveOperationException exception) {
            return false;
        }
    }

    private static void sendTitlePacket(final Player player, final String title, final String subtitle,
            final int fadeInTicks, final int stayTicks, final int fadeOutTicks) {
        try {
            final String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            final Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            final Object craftPlayer = craftPlayerClass.cast(player);
            final Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            final Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            final Class<?> packetBase = Class.forName("net.minecraft.server." + version + ".Packet");
            final Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle");
            final Class<?> enumClass = Class
                    .forName("net.minecraft.server." + version + ".PacketPlayOutTitle$EnumTitleAction");
            final Class<?> chatSerializerClass = Class
                    .forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            final Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            final Object timesAction = Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), "TIMES");
            final Object titleAction = Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), "TITLE");
            final Object subtitleAction = Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), "SUBTITLE");
            final Object timesPacket = packetClass
                    .getConstructor(enumClass, chatComponentClass, int.class, int.class, int.class)
                    .newInstance(timesAction, null, fadeInTicks, stayTicks, fadeOutTicks);
            final Object titleComponent = chatSerializerClass.getMethod("a", String.class).invoke(null,
                    "{\"text\":\"" + escapeJson(ChatColor.translateAlternateColorCodes('&', title)) + "\"}");
            final Object subtitleComponent = chatSerializerClass.getMethod("a", String.class).invoke(null,
                    "{\"text\":\"" + escapeJson(ChatColor.translateAlternateColorCodes('&', subtitle)) + "\"}");
            final Object titlePacket = packetClass.getConstructor(enumClass, chatComponentClass)
                    .newInstance(titleAction, titleComponent);
            final Object subtitlePacket = packetClass.getConstructor(enumClass, chatComponentClass)
                    .newInstance(subtitleAction, subtitleComponent);
            playerConnection.getClass().getMethod("sendPacket", packetBase).invoke(playerConnection, timesPacket);
            playerConnection.getClass().getMethod("sendPacket", packetBase).invoke(playerConnection, titlePacket);
            playerConnection.getClass().getMethod("sendPacket", packetBase).invoke(playerConnection, subtitlePacket);
        } catch (final ReflectiveOperationException exception) {
            // Titles are unavailable on this server build.
        }
    }

    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
