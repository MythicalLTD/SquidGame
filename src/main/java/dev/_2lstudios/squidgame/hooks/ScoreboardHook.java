package dev._2lstudios.squidgame.hooks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.swiftboard.SwiftBoard;

public class ScoreboardHook {
    private final PluginManager pluginManager;

    public ScoreboardHook(final PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void request(final Player player, final List<String> lines) {
        if (pluginManager.isPluginEnabled("SwiftBoard")) {
            SwiftBoard.getSwiftSidebar().setLines(player, this.formatLines(player, lines));
        }
    }

    public void request(final SquidPlayer squidPlayer, final List<String> lines) {
        request(squidPlayer.getBukkitPlayer(), lines);
    }

    public boolean canHook() {
        return pluginManager.isPluginEnabled("SwiftBoard");
    }

    private List<String> formatLines(final Player player, final List<String> lines) {
        final List<String> formatted = new ArrayList<>();

        for (final String line : lines) {
            formatted.add(ChatColor.translateAlternateColorCodes('&', PlaceholderAPIHook.formatString(line, player)));
        }

        return formatted;
    }
}
