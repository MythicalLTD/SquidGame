package dev._2lstudios.squidgame.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.arena.games.G12SkySquidGame;
import dev._2lstudios.squidgame.arena.games.G6GlassesGame;
import dev._2lstudios.squidgame.arena.games.G8MingleGame;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class EntityDamageListener implements Listener {

    private final SquidGame plugin;

    public EntityDamageListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent e) {
        final Entity entity = e.getEntity();

        if (this.isProtectedLobbyPet(entity)) {
            e.setCancelled(true);
            return;
        }

        if (entity instanceof Player) {
            final SquidPlayer player = (SquidPlayer) this.plugin.getPlayerManager().getPlayer((Player) entity);

            if (player != null && player.getArena() != null) {
                final Arena arena = player.getArena();

                if (player.isSpectator() || !arena.getPlayers().contains(player)) {
                    e.setCancelled(true);
                    return;
                }

                if (e.getCause() == DamageCause.FALL && arena.getState() != ArenaState.IN_GAME) {
                    e.setCancelled(true);
                }

                if (e.getCause() == DamageCause.FALL && arena.getState() == ArenaState.IN_GAME
                        && arena.getCurrentGame() instanceof G8MingleGame
                        && ((G8MingleGame) arena.getCurrentGame()).shouldCancelFallDamage()) {
                    e.setCancelled(true);
                }

                if (e.getCause() == DamageCause.ENTITY_EXPLOSION) {
                    e.setCancelled(true);
                }

                if (!arena.isPvPAllowed() && e instanceof EntityDamageByEntityEvent) {
                    final Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

                    if (damager instanceof Player || damager instanceof Projectile
                            && ((Projectile) damager).getShooter() instanceof Player) {
                        e.setCancelled(true);
                    }
                }

                final boolean fatalDamage = player.getBukkitPlayer().getHealth() - e.getDamage() <= 0;

                if (!e.isCancelled() && e instanceof EntityDamageByEntityEvent
                        && arena.getCurrentGame() instanceof G10HideAndSeekGame) {
                    final EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) e;
                    final Entity damager = damageEvent.getDamager();

                    if (damager instanceof Player) {
                        final SquidPlayer attacker = (SquidPlayer) this.plugin.getPlayerManager()
                                .getPlayer((Player) damager);
                        final G10HideAndSeekGame game = (G10HideAndSeekGame) arena.getCurrentGame();

                        if (game.handlePlayerDamage(attacker, player, damageEvent)) {
                            return;
                        }

                        final boolean updatedFatalDamage = player.getBukkitPlayer().getHealth()
                                - damageEvent.getDamage() <= 0;
                        game.handlePlayerAttack(attacker, player, updatedFatalDamage);
                    }
                }

                if (!e.isCancelled() && arena.getCurrentGame() instanceof G8MingleGame) {
                    final G8MingleGame mingleGame = (G8MingleGame) arena.getCurrentGame();

                    if (e instanceof EntityDamageByEntityEvent) {
                        final Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

                        if (damager instanceof Player) {
                            final SquidPlayer attacker = (SquidPlayer) this.plugin.getPlayerManager()
                                    .getPlayer((Player) damager);

                            if (mingleGame.handleKnockback(attacker, player, (EntityDamageByEntityEvent) e)) {
                                return;
                            }
                        }
                    }
                }

                if (!e.isCancelled() && arena.getState() == ArenaState.IN_GAME
                        && arena.getCurrentGame() instanceof G6GlassesGame) {
                    final G6GlassesGame glassesGame = (G6GlassesGame) arena.getCurrentGame();

                    if (glassesGame.isBelowDeathLevel(player.getBukkitPlayer().getLocation())
                            || e.getCause() == DamageCause.VOID) {
                        arena.killPlayer(player);
                        e.setCancelled(true);
                        return;
                    }
                }

                if (!e.isCancelled() && arena.getCurrentGame() instanceof G12SkySquidGame) {
                    final G12SkySquidGame skySquidGame = (G12SkySquidGame) arena.getCurrentGame();

                    if (e instanceof EntityDamageByEntityEvent) {
                        final Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

                        if (damager instanceof Player) {
                            final SquidPlayer attacker = (SquidPlayer) this.plugin.getPlayerManager()
                                    .getPlayer((Player) damager);

                            if (skySquidGame.handleKnockback(attacker, player, (EntityDamageByEntityEvent) e)) {
                                return;
                            }
                        }
                    }

                    if (skySquidGame.isFightingPhase()) {
                        final boolean lethal = player.getBukkitPlayer().getHealth() - e.getDamage() <= 0;

                        if (lethal && (e.getCause() == DamageCause.FALL || e.getCause() == DamageCause.VOID)) {
                            skySquidGame.markEliminationAndAdvance();
                        }
                    }
                }

                if (!e.isCancelled() && arena.canEliminatePlayers() && arena.getPlayers().contains(player)
                        && !player.isSpectator()
                        && player.getBukkitPlayer().getHealth() - e.getDamage() <= 0) {
                    if (arena.getCurrentGame() instanceof G10HideAndSeekGame) {
                        SquidPlayer attacker = null;

                        if (e instanceof EntityDamageByEntityEvent) {
                            final Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

                            if (damager instanceof Player) {
                                attacker = (SquidPlayer) this.plugin.getPlayerManager().getPlayer((Player) damager);
                            }
                        }

                        ((G10HideAndSeekGame) arena.getCurrentGame()).handlePlayerDeath(player, attacker);
                    }

                    arena.killPlayer(player);
                    e.setCancelled(true);
                }
            }
        }
    }

    private boolean isProtectedLobbyPet(final Entity entity) {
        return entity instanceof Wolf || entity instanceof Ocelot;
    }
}
