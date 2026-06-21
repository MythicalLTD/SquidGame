package dev._2lstudios.squidgame.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G5TugOfWarGame;
import dev._2lstudios.squidgame.arena.games.G6GlassesGame;
import dev._2lstudios.squidgame.arena.games.G12SkySquidGame;
import dev._2lstudios.squidgame.arena.games.G8MingleGame;
import dev._2lstudios.squidgame.gui.CosmeticsGUI;
import dev._2lstudios.squidgame.player.PlayerWand;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.LobbyItems;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class PlayerInteractListener implements Listener {

    private final SquidGame plugin;

    public PlayerInteractListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(e.getPlayer());

        if (player == null) {
            return;
        }

        final Arena arena = player.getArena();

        if (LobbyItems.hasLobbyAccess(player) && e.getItem() != null && LobbyItems.isCosmeticsItem(e.getItem())
                && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            e.setCancelled(true);
            new CosmeticsGUI(player).open(e.getPlayer());
            return;
        }

        if (LobbyItems.hasLobbyAccess(player) && e.getItem() != null && LobbyItems.isMusicMenuItem(e.getItem())
                && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            e.setCancelled(true);
            LobbyItems.openMusicMenu(player);
            return;
        }

        if (player.getWand() != null && e.getItem() != null && e.getItem().getType().equals(Material.BLAZE_ROD)
                && (arena == null || arena.getState() != ArenaState.IN_GAME) && e.getClickedBlock() != null) {
            final PlayerWand wand = player.getWand();

            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                wand.setFirstPoint(e.getClickedBlock().getLocation());
                MessageUtils.send(this.plugin, e.getPlayer(), "setup.wand.first-point", "{point}",
                        wand.getFirstPoint().toString());
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                wand.setSecondPoint(e.getClickedBlock().getLocation());
                MessageUtils.send(this.plugin, e.getPlayer(), "setup.wand.second-point", "{point}",
                        wand.getSecondPoint().toString());
            }

            e.setCancelled(true);
        }

        if (arena != null && arena.isStartItem(e.getItem())) {
            e.setCancelled(true);

            if (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING) {
                if (!arena.forceStart()) {
                    MessageUtils.send(this.plugin, e.getPlayer(), "commands.start.not-enough", "{players}",
                            String.valueOf(arena.getPlayers().size()), "{min}",
                            String.valueOf(arena.getForceStartMinPlayers()));
                }
            }

            return;
        }

        if (arena != null && arena.getState() == ArenaState.IN_GAME
                && arena.getCurrentGame() instanceof G5TugOfWarGame && e.getItem() != null
                && e.getItem().getType().equals(Material.STRING)) {
            ((G5TugOfWarGame) arena.getCurrentGame()).handlePullAction(player);
            e.setCancelled(true);
        }

        if (arena != null && arena.getCurrentGame() instanceof G6GlassesGame && e.getItem() != null
                && ((G6GlassesGame) arena.getCurrentGame()).isSolverItem(e.getItem())) {
            ((G6GlassesGame) arena.getCurrentGame()).handleSolverUse(player);
            e.setCancelled(true);
        }

        if (arena != null && arena.getState() == ArenaState.IN_GAME
                && arena.getCurrentGame() instanceof G12SkySquidGame
                && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            final org.bukkit.Location clicked = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;

            if (((G12SkySquidGame) arena.getCurrentGame()).handleButtonPress(player, clicked)) {
                e.setCancelled(true);
            }
        }

        if (arena != null && arena.getState() == ArenaState.IN_GAME
                && arena.getCurrentGame() instanceof G8MingleGame && e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock() != null) {
            if (((G8MingleGame) arena.getCurrentGame()).handleDoorInteract(player, e.getClickedBlock())) {
                e.setCancelled(true);
            }
        }
    }
}
