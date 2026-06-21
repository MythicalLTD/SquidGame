package dev._2lstudios.squidgame.arena.games;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.IronBarBarrierSession;
import dev._2lstudios.squidgame.utils.LobbyItems;

public abstract class ArenaGameBase {

    private final String name;
    private final String configKey;
    private final Arena arena;
    private final int gameTime;
    private final IronBarBarrierSession ironBarSession = new IronBarBarrierSession();
    private boolean finishedEarly;
    private boolean gameLobbyActive = true;

    public ArenaGameBase(final String name, final String configKey, final int gameTime, final Arena arena) {
        this.name = name;
        this.configKey = configKey;
        this.arena = arena;
        this.gameTime = gameTime;
    }

    public void onExplainStart() {
        this.gameLobbyActive = true;
        final String key = "games." + this.configKey + ".tutorial";
        this.broadcastTitleAfterSeconds(3, key + ".1.title", key + ".1.subtitle");
        this.broadcastTitleAfterSeconds(6, key + ".2.title", key + ".2.subtitle");
        this.broadcastTitleAfterSeconds(9, key + ".3.title", key + ".3.subtitle");
        this.broadcastTitleAfterSeconds(12, key + ".4.title", key + ".4.subtitle");
        this.broadcastTitleAfterSeconds(15, "events.game-start.title", "events.game-start.subtitle");
    }

    public void onStart() {
    }

    protected void finishEarly() {
        this.finishEarly("events.all-players-finished.title", "events.all-players-finished.subtitle");
    }

    protected void finishEarly(final String titleKey, final String subtitleKey) {
        if (this.finishedEarly || this.arena.getState() != ArenaState.IN_GAME) {
            return;
        }

        this.finishedEarly = true;
        this.arena.broadcastTitle(titleKey, subtitleKey);
        this.arena.setInternalTime(1);
    }

    protected boolean hasFinishedEarly() {
        return this.finishedEarly;
    }

    public void onTimeUp() {
    }

    public void onStop() {

    }

    public void onPlayerEliminated(final SquidPlayer player) {
    }

    public boolean shouldPreventSinglePlayerFinish() {
        return false;
    }

    public final void prepareStop() {
        this.ironBarSession.restore();
        this.onStop();
    }

    public String getConfigKey() {
        return this.configKey;
    }

    protected final Location resolveArenaLocation(final String primaryKey, final String... fallbackKeys) {
        final Configuration config = this.arena.getConfig();

        if (config.contains(primaryKey + ".x")) {
            final Location location = config.getLocation(primaryKey, false);
            location.setWorld(this.arena.getWorld());
            return location;
        }

        for (final String fallbackKey : fallbackKeys) {
            if (config.contains(fallbackKey + ".x")) {
                final Location location = config.getLocation(fallbackKey, false);
                location.setWorld(this.arena.getWorld());
                return location;
            }
        }

        final Location location = config.getLocation(primaryKey, false);
        location.setWorld(this.arena.getWorld());
        return location;
    }

    public Location getLobbyPosition() {
        final String prefix = "games." + this.configKey;

        return this.resolveArenaLocation(prefix + ".lobby", "arena.waiting_room", "arena.prelobby");
    }

    public Location getPlaySpawnPosition() {
        return this.resolveArenaLocation("games." + this.configKey + ".spawn");
    }

    public Location getSpawnPosition() {
        return this.getLobbyPosition();
    }

    protected boolean hasDistinctPlaySpawn() {
        final Location lobby = this.getLobbyPosition();
        final Location play = this.getPlaySpawnPosition();

        return lobby.getBlockX() != play.getBlockX() || lobby.getBlockY() != play.getBlockY()
                || lobby.getBlockZ() != play.getBlockZ();
    }

    protected boolean delaysPlaySpawnTeleport() {
        return false;
    }

    public boolean delaysLobbyRemovalUntilPlayBegins() {
        return false;
    }

    public boolean keepsGameLobbyFeatures() {
        return this.delaysLobbyRemovalUntilPlayBegins() && this.gameLobbyActive;
    }

    protected void refreshGameLobby() {
        final SquidGame plugin = SquidGame.getInstance();

        for (final SquidPlayer player : this.arena.getAllPlayers()) {
            LobbyItems.giveLobbyItems(player);
            plugin.getCosmeticManager().refreshCosmetic(player);
        }

        plugin.getNbsMusicManager().resumeArenaMusic(this.arena);
    }

    public final void endGameLobby() {
        this.gameLobbyActive = false;
        final SquidGame plugin = SquidGame.getInstance();

        for (final SquidPlayer player : this.arena.getPlayers()) {
            LobbyItems.removeLobbyItems(player.getBukkitPlayer());
            plugin.getCosmeticManager().refreshCosmeticsForGameStart(player);
        }

        plugin.getNbsMusicManager().pauseArenaMusic(this.arena);
    }

    protected void teleportToPlaySpawn() {
        this.arena.teleportAllPlayers(this.getPlaySpawnPosition());
    }

    public final void prepareStart() {
        this.finishedEarly = false;
        this.ironBarSession.hide(this.arena.getConfig(), this.arena.getWorld(), this.configKey);

        if (this.hasDistinctPlaySpawn() && !this.delaysPlaySpawnTeleport()) {
            this.teleportToPlaySpawn();
        }

        this.onStart();
    }

    public void broadcastTitleAfterSeconds(int seconds, final String title, final String subtitle) {
        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.arena.broadcastTitle(title, subtitle);
        }, seconds * 20L);
    }

    public void broadcastMessageAfterSeconds(int seconds, final String message) {
        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.arena.broadcastMessage(message);
        }, seconds * 20L);
    }

    public int getGameTime() {
        return this.gameTime;
    }

    public int getMinPlayers() {
        return 1;
    }

    public Arena getArena() {
        return this.arena;
    }

    public String getName() {
        return this.name;
    }

    public String getIntermissionTip() {
        final String key = "games." + this.configKey + ".intermission-tip";
        final String configured = SquidGame.getInstance().getMessagesConfig().getString(key);

        if (configured != null && !configured.isEmpty()) {
            return configured;
        }

        return SquidGame.getInstance().getMessagesConfig().getString("events.intermission.default-tip",
                "&7Get ready for the next minigame!");
    }
}
