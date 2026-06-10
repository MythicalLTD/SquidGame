package dev._2lstudios.squidgame.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class MessageUtils {

    public static String format(final SquidGame plugin, final String key, final String... replacements) {
        String message = plugin.getMessagesConfig().getString(key);

        if (message == null) {
            message = "&6&lWARNING: &eMissing translation key &7" + key + " &ein messages.yml file";
        }

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(final SquidGame plugin, final CommandSender sender, final String key,
            final String... replacements) {
        sender.sendMessage(format(plugin, key, replacements));
    }

    @SuppressWarnings("deprecation")
    public static void broadcastTitle(final SquidGame plugin, final Arena arena, final String titleKey,
            final String subtitleKey, final String... replacements) {
        final String title = format(plugin, titleKey, replacements);
        final String subtitle = format(plugin, subtitleKey, replacements);

        for (final SquidPlayer player : arena.getAllPlayers()) {
            player.getBukkitPlayer().sendTitle(title, subtitle);
        }
    }
}
