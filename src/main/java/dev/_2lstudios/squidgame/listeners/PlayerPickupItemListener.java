package dev._2lstudios.squidgame.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class PlayerPickupItemListener implements Listener {

    private final SquidGame plugin;

    public PlayerPickupItemListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager()
                .getPlayer((Player) event.getEntity());

        if (player == null) {
            return;
        }

        final Arena arena = player.getArena();

        if (arena == null || arena.getState() != ArenaState.IN_GAME
                || !(arena.getCurrentGame() instanceof G10HideAndSeekGame)) {
            return;
        }

        if (!((G10HideAndSeekGame) arena.getCurrentGame()).canPickupItem(player,
                event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
