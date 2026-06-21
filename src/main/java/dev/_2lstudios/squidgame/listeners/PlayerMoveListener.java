package dev._2lstudios.squidgame.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G1RedGreenLightGame;
import dev._2lstudios.squidgame.arena.games.G12SkySquidGame;
import dev._2lstudios.squidgame.arena.games.G5TugOfWarGame;
import dev._2lstudios.squidgame.arena.games.G8MingleGame;
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
        if (player == null) {
            return;
        }

        final Arena arena = player.getArena();

        if (arena == null || player.isSpectator()) {
            return;
        }

        /* Game 1: Handling */
        if (arena.getCurrentGame() instanceof G1RedGreenLightGame) {
            final G1RedGreenLightGame game = (G1RedGreenLightGame) arena.getCurrentGame();

            if (arena.getState() == ArenaState.EXPLAIN_GAME && game.isBarrierActive()) {
                final Cuboid barrier = game.getBarrier();

                if (barrier != null && barrier.isBetween(e.getTo())) {
                    e.setCancelled(true);
                    e.setTo(e.getFrom());
                }
            }

            else if (arena.getState() == ArenaState.IN_GAME) {
                if (game.keepsGameLobbyFeatures()) {
                    return;
                }

                game.handleMove(player, e.getTo());

                if (!game.isCanWalk() && !game.hasPlayerCrossed(player)) {
                    final Cuboid killZone = game.getKillZone();

                    if (killZone != null) {
                        final Vector3 playerPosition = new Vector3(e.getTo().getX(), e.getTo().getY(),
                                e.getTo().getZ());

                        if (killZone.isBetween(playerPosition)) {
                            game.handleRedLightViolation(player, e.getTo());
                        }
                    }
                }
            }
        }

        /* Game 6: Handling */
        else if (arena.getCurrentGame() instanceof G6GlassesGame && arena.getState() == ArenaState.IN_GAME) {
            final G6GlassesGame game = (G6GlassesGame) arena.getCurrentGame();

            game.handleMove(player, e.getTo());

            if (!game.isBridgeActive()) {
                return;
            }

            final Location loc = e.getTo().clone().subtract(0, 1, 0);
            final Block block = loc.getBlock();

            if (block != null && block.getType() == Material.GLASS) {
                if (game.breakFakeBlock(loc.getBlock())) {
                    arena.broadcastSound(
                            this.plugin.getMainConfig().getSound("game-settings.sounds.glass-break", "GLASS"));
                }
            }
        }

        /* Game 5: Handling */
        else if (arena.getCurrentGame() instanceof G5TugOfWarGame && arena.getState() == ArenaState.IN_GAME) {
            final G5TugOfWarGame game = (G5TugOfWarGame) arena.getCurrentGame();

            if (game.shouldLockMovement(player, e.getFrom(), e.getTo())) {
                e.setTo(game.getLockedLocation(player, e.getTo()));
            }
        }

        /* Game 12: Handling */
        else if (arena.getCurrentGame() instanceof G12SkySquidGame && arena.getState() == ArenaState.IN_GAME) {
            final G12SkySquidGame game = (G12SkySquidGame) arena.getCurrentGame();

            if (game.isBridgePhase()) {
                game.handleBridgeMove(player, e.getTo());
            }
        }

        /* Game 8: Handling */
        else if (arena.getCurrentGame() instanceof G8MingleGame && arena.getState() == ArenaState.IN_GAME) {
            final G8MingleGame game = (G8MingleGame) arena.getCurrentGame();

            if (game.handleMove(player, e.getTo())) {
                e.setCancelled(true);
                e.setTo(e.getFrom());
            }
        }
    }
}
