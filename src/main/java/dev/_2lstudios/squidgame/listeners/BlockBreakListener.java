package dev._2lstudios.squidgame.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class BlockBreakListener implements Listener {
    private final SquidGame plugin;

    public BlockBreakListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        final Player bukkitPlayer = e.getPlayer();
        final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(bukkitPlayer);
        final Arena arena = squidPlayer.getArena();

        if (arena != null && !this.canBuildInArena(bukkitPlayer)) {
            e.setCancelled(true);
        }
    }

    private boolean canBuildInArena(final Player player) {
        return player.getGameMode() == GameMode.CREATIVE && player.hasPermission("squidgame.admin");
    }
}