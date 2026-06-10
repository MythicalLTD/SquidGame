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

                if (player.isSpectator()) {
                    e.setCancelled(true);
                    return;
                }

                if (e.getCause() == DamageCause.FALL && arena.getState() != ArenaState.IN_GAME) {
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
                    final Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

                    if (damager instanceof Player) {
                        final SquidPlayer attacker = (SquidPlayer) this.plugin.getPlayerManager()
                                .getPlayer((Player) damager);

                        if (((G10HideAndSeekGame) arena.getCurrentGame()).handlePlayerAttack(attacker, player,
                                fatalDamage)) {
                            e.setCancelled(true);
                        }
                    }
                }

                if (!e.isCancelled() && fatalDamage && e instanceof EntityDamageByEntityEvent
                        && arena.getCurrentGame() instanceof G12SkySquidGame) {
                    final Entity damager = ((EntityDamageByEntityEvent) e).getDamager();

                    if (damager instanceof Player) {
                        final SquidPlayer attacker = (SquidPlayer) this.plugin.getPlayerManager()
                                .getPlayer((Player) damager);

                        if (((G12SkySquidGame) arena.getCurrentGame()).handlePlayerKill(attacker, player)) {
                            e.setCancelled(true);
                        }
                    }
                }

                if (!e.isCancelled() && player.getBukkitPlayer().getHealth() - e.getDamage() <= 0
                        && !player.isSpectator()) {
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
