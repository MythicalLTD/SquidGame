package dev._2lstudios.squidgame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.utils.LobbyItems;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class PlayerDropItemListener implements Listener {

    private final SquidGame plugin;

    public PlayerDropItemListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(event.getPlayer());

        if (player == null) {
            return;
        }

        final Arena arena = player.getArena();

        if (LobbyItems.hasLobbyAccess(player) && LobbyItems.isCosmeticsItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            return;
        }

        if (LobbyItems.hasLobbyAccess(player) && LobbyItems.isMusicMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            return;
        }

        if (arena == null || arena.getState() != ArenaState.IN_GAME
                || !(arena.getCurrentGame() instanceof G10HideAndSeekGame)) {
            return;
        }

        if (!((G10HideAndSeekGame) arena.getCurrentGame()).canDropItem(player, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
