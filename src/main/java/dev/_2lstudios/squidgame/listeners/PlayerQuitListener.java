package dev._2lstudios.squidgame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class PlayerQuitListener implements Listener {
    private final SquidGame plugin;

    public PlayerQuitListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        if (this.isProxySilent()) {
            e.setQuitMessage(null);
        }

        final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(e.getPlayer());
        if (squidPlayer != null && squidPlayer.getArena() != null) {
            squidPlayer.getArena().removePlayer(squidPlayer);
        }

        if (squidPlayer != null) {
            this.plugin.getCosmeticManager().savePlayerPreferences(squidPlayer);
            this.plugin.getNbsMusicManager().stopLobbyMusic(squidPlayer);
            this.plugin.getPlayerDataManager().unloadPlayer(e.getPlayer().getUniqueId());
        }
    }

    private boolean isProxySilent() {
        return this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.enabled", false)
                && this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.silent-arena-messages", true);
    }
}