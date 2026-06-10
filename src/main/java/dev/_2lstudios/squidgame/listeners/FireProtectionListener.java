package dev._2lstudios.squidgame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import dev._2lstudios.squidgame.SquidGame;

public class FireProtectionListener implements Listener {
    private final SquidGame plugin;

    public FireProtectionListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBurn(final BlockBurnEvent event) {
        if (this.isFireProtectionEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(final BlockIgniteEvent event) {
        if (!this.isFireProtectionEnabled()) {
            return;
        }

        switch (event.getCause()) {
        case LAVA:
        case SPREAD:
        case LIGHTNING:
            event.setCancelled(true);
            break;
        default:
            break;
        }
    }

    @EventHandler
    public void onBlockSpread(final BlockSpreadEvent event) {
        if (this.isFireProtectionEnabled() && event.getNewState().getType().name().contains("FIRE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(final BlockFadeEvent event) {
        if (this.isBlockFadeProtectionEnabled() && this.isSnowOrIce(event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockForm(final BlockFormEvent event) {
        if (this.isBlockFadeProtectionEnabled() && this.isSnowOrIce(event.getNewState().getType().name())) {
            event.setCancelled(true);
        }
    }

    private boolean isFireProtectionEnabled() {
        return this.plugin.getMainConfig().getBoolean("game-settings.disable-fire-spread", true);
    }

    private boolean isBlockFadeProtectionEnabled() {
        return this.plugin.getMainConfig().getBoolean("game-settings.disable-ice-snow-melt", true);
    }

    private boolean isSnowOrIce(final String materialName) {
        return materialName.contains("ICE") || materialName.contains("SNOW");
    }
}
