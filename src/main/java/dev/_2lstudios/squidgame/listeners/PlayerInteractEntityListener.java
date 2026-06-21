package dev._2lstudios.squidgame.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class PlayerInteractEntityListener implements Listener {

    private final SquidGame plugin;

    public PlayerInteractEntityListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player)) {
            return;
        }

        final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(e.getPlayer());
        if (player == null) {
            return;
        }

        final Arena arena = player.getArena();

        if (arena != null && arena.getState() == ArenaState.EXPLAIN_GAME
                && arena.getCurrentGame() instanceof G10HideAndSeekGame) {
            final SquidPlayer target = (SquidPlayer) this.plugin.getPlayerManager()
                    .getPlayer((Player) e.getRightClicked());
            ((G10HideAndSeekGame) arena.getCurrentGame()).handleTeamSwitchRequest(player, target);
            e.setCancelled(true);
        }
    }
}
