package dev._2lstudios.squidgame.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G1RedGreenLightGame;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.arena.games.G11JumpRopeGame;
import dev._2lstudios.squidgame.arena.games.G6GlassesGame;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class PlayerMoveListener implements Listener {

    private final SquidGame plugin;

    public PlayerMoveListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent e) {
        if (e.getFrom().distance(e.getTo()) <= 0.015) {
            return;
        }

        final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(e.getPlayer());
        final Arena arena = player.getArena();

        if (arena == null || player.isSpectator()) {
            return;
        }

        /* Game 1: Handling */
        if (arena.getCurrentGame() instanceof G1RedGreenLightGame) {
            final G1RedGreenLightGame game = (G1RedGreenLightGame) arena.getCurrentGame();

            if (arena.getState() == ArenaState.EXPLAIN_GAME) {
                if (game.getBarrier().isBetween(e.getTo())) {
                    e.setCancelled(true);
                    e.setTo(e.getFrom());
                }
            }

            else if (arena.getState() == ArenaState.IN_GAME) {
                game.handleMove(player, e.getTo());

                if (!game.isCanWalk()) {
                    final Vector3 playerPosition = new Vector3(e.getTo().getX(), e.getTo().getY(), e.getTo().getZ());
                    if (game.getKillZone().isBetween(playerPosition)) {
                        arena.killPlayer(player);
                    }
                }
            }
        }

        /* Game 6: Handling */
        else if (arena.getCurrentGame() instanceof G6GlassesGame) {
            final Location loc = e.getTo().clone().subtract(0, 1, 0);
            final Block block = loc.getBlock();

            if (block != null && block.getType() == Material.GLASS) {
                final G6GlassesGame game = (G6GlassesGame) arena.getCurrentGame();

                if (game.breakFakeBlock(loc.getBlock())) {
                    arena.broadcastSound(
                            this.plugin.getMainConfig().getSound("game-settings.sounds.glass-break", "GLASS"));
                }
            }
        }

        /* Game 10: Handling */
        else if (arena.getCurrentGame() instanceof G10HideAndSeekGame && arena.getState() == ArenaState.IN_GAME) {
            ((G10HideAndSeekGame) arena.getCurrentGame()).handleMove(player, e.getTo());
        }

        /* Game 11: Handling */
        else if (arena.getCurrentGame() instanceof G11JumpRopeGame && arena.getState() == ArenaState.IN_GAME) {
            ((G11JumpRopeGame) arena.getCurrentGame()).handleMove(player, e.getFrom(), e.getTo());
        }
    }
}
