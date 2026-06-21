package dev._2lstudios.squidgame.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.PlayerProfile;
import dev._2lstudios.squidgame.player.SquidPlayer;

public final class SquidPlaceholders {

    private SquidPlaceholders() {
    }

    public static String format(final SquidGame plugin, final Player player, final String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String formatted = text;
        final SquidPlayer squidPlayer = player == null ? null
                : (SquidPlayer) plugin.getPlayerManager().getPlayer(player);

        formatted = formatted.replace("%squidgame_server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        formatted = formatted.replace("%squidgame_arenas%", String.valueOf(plugin.getArenaManager().getArenas().size()));

        if (player != null) {
            formatted = formatted.replace("%squidgame_player_name%", player.getName());
        }

        formatted = replacePlayerPlaceholders(plugin, squidPlayer, formatted);

        if (squidPlayer != null && squidPlayer.getArena() != null) {
            formatted = replaceArenaPlaceholders(squidPlayer.getArena(), formatted);
        }

        return PlaceholderAPIHook.formatString(formatted, player);
    }

    private static String replacePlayerPlaceholders(final SquidGame plugin, final SquidPlayer player,
            final String text) {
        String formatted = text;
        final PlayerProfile profile = player == null || !plugin.getPlayerDataManager().isEnabled() ? null
                : plugin.getPlayerDataManager().getProfile(player.getBukkitPlayer().getUniqueId());

        final int wins = profile == null ? 0 : profile.getWins();
        final int deaths = profile == null ? 0 : profile.getDeaths();
        final int games = profile == null ? 0 : profile.getGamesPlayed();
        final int coins = profile == null ? 0 : profile.getCoins();

        formatted = formatted.replace("%squidgame_player_wins%", String.valueOf(wins));
        formatted = formatted.replace("%squidgame_player_deaths%", String.valueOf(deaths));
        formatted = formatted.replace("%squidgame_player_games%", String.valueOf(games));
        formatted = formatted.replace("%squidgame_player_coins%", String.valueOf(coins));
        formatted = formatted.replace("%squidgame_player_kdr%", formatKdr(wins, deaths));
        return formatted;
    }

    private static String replaceArenaPlaceholders(final Arena arena, final String text) {
        String formatted = text;

        formatted = formatted.replace("%squidgame_arena_death%",
                arena.getDeathPlayer() != null ? arena.getDeathPlayer() : "None");
        formatted = formatted.replace("%squidgame_arena_joined%",
                arena.getJoinedPlayer() != null ? arena.getJoinedPlayer() : "None");
        formatted = formatted.replace("%squidgame_arena_leaved%",
                arena.getLeavedPlayer() != null ? arena.getLeavedPlayer() : "None");
        formatted = formatted.replace("%squidgame_arena_players%", String.valueOf(arena.getPlayers().size()));
        formatted = formatted.replace("%squidgame_arena_spectators%", String.valueOf(arena.getSpectators().size()));
        formatted = formatted.replace("%squidgame_arena_required%", String.valueOf(arena.getMinPlayers()));
        formatted = formatted.replace("%squidgame_arena_maxplayers%", String.valueOf(arena.getMaxPlayers()));
        formatted = formatted.replace("%squidgame_arena_time%", String.valueOf(Math.max(0, arena.getInternalTime())));

        final SquidPlayer winner = arena.calculateWinner();
        formatted = formatted.replace("%squidgame_arena_winner%",
                winner != null ? winner.getBukkitPlayer().getName() : "None");
        formatted = formatted.replace("%squidgame_arena_game%",
                arena.getCurrentGame() == null ? "None" : arena.getCurrentGame().getName());
        formatted = formatted.replace("%squidgame_arena_name%", arena.getName());

        return formatted;
    }

    private static String formatKdr(final int wins, final int deaths) {
        if (deaths <= 0) {
            return String.valueOf(wins);
        }

        return String.format("%.2f", wins / (double) deaths);
    }
}
