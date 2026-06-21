package dev._2lstudios.squidgame.commands.game;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.player.PlayerProfile;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "stats", usage = "/squid stats", description = "View your SquidGame stats and coins", permission = "squidgame.stats", target = CommandExecutionTarget.ONLY_PLAYER)
public class SquidStatsCommand extends CommandListener {

    @Override
    public void handle(final CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final SquidPlayer player = (SquidPlayer) context.getPluginPlayer();

        if (!plugin.getPlayerDataManager().isEnabled()) {
            MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.disabled");
            return;
        }

        final PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getBukkitPlayer().getUniqueId());

        if (profile == null) {
            MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.profile-loading");
            return;
        }

        MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.stats.header");
        MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.stats.coins", "{coins}",
                String.valueOf(profile.getCoins()));
        MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.stats.wins", "{wins}",
                String.valueOf(profile.getWins()));
        MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.stats.deaths", "{deaths}",
                String.valueOf(profile.getDeaths()));
        MessageUtils.send(plugin, player.getBukkitPlayer(), "economy.stats.games", "{games}",
                String.valueOf(profile.getGamesPlayed()));
    }
}
