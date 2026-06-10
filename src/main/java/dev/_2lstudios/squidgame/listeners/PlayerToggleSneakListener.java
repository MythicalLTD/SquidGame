package dev._2lstudios.squidgame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G5TugOfWarGame;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class PlayerToggleSneakListener implements Listener {
    private final SquidGame plugin;

    public PlayerToggleSneakListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(event.getPlayer());

        if (player == null || player.isSpectator()) {
            return;
        }

        final Arena arena = player.getArena();

        if (arena != null && arena.getState() == ArenaState.IN_GAME
                && arena.getCurrentGame() instanceof G5TugOfWarGame) {
            ((G5TugOfWarGame) arena.getCurrentGame()).handlePullAction(player);
        }
    }
}
