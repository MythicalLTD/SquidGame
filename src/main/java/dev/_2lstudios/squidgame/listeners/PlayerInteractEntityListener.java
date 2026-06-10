package dev._2lstudios.squidgame.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G4MarblesGame;
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
        final Arena arena = player.getArena();
        final ItemStack item = e.getPlayer().getItemInHand();

        if (arena != null && arena.getState() == ArenaState.EXPLAIN_GAME
                && arena.getCurrentGame() instanceof G4MarblesGame && item != null
                && item.getType().equals(Material.NAME_TAG)) {
            final SquidPlayer target = (SquidPlayer) this.plugin.getPlayerManager()
                    .getPlayer((Player) e.getRightClicked());
            ((G4MarblesGame) arena.getCurrentGame()).selectPartner(player, target);
            e.setCancelled(true);
        }
    }
}
