package dev._2lstudios.squidgame.arena;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.games.ArenaGameBase;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class ArenaHandler {
    private final Arena arena;
    private final Configuration mainConfig;

    public ArenaHandler(final Arena arena) {
        this.arena = arena;
        this.mainConfig = SquidGame.getInstance().getMainConfig();
    }

    public void handleArenaSwitchState() {
        final ArenaState state = this.arena.getState();
        final String scoreboardKey = state.toString().toLowerCase();
        this.arena.broadcastScoreboard(scoreboardKey);
    }

    public void handlePlayerJoin(final SquidPlayer player) {
        if (!this.isProxySilent()) {
            arena.broadcastMessage("arena.join");
        }

        player.sendScoreboard(this.arena.getState().toString().toLowerCase());

        if (arena.getState() == ArenaState.WAITING) {
            if (arena.getPlayers().size() >= arena.getMinPlayers()) {
                arena.setInternalTime(this.mainConfig.getInt("game-settings.starting-time", 30));
                arena.setState(ArenaState.STARTING);
                if (!this.isProxySilent()) {
                    arena.broadcastMessage("arena.starting");
                }
            }
        }
    }

    public void handlePlayerLeave(final SquidPlayer player) {
        if (this.arena.getState() == ArenaState.FINISHING_ARENA) {
            return;
        }

        else if (this.arena.getState() == ArenaState.WAITING || this.arena.getState() == ArenaState.STARTING) {
            if (!this.isProxySilent()) {
                arena.broadcastMessage("arena.leave");
            }

            if (!arena.isForceStarted() && arena.getPlayers().size() < arena.getMinPlayers()
                    && arena.getState() == ArenaState.STARTING) {
                arena.setState(ArenaState.WAITING);
                if (!this.isProxySilent()) {
                    arena.broadcastMessage("arena.no-enough-players");
                }
                arena.setInternalTime(this.mainConfig.getInt("game-settings.starting-time", 30));
            }
        }
    }

    public void handleArenaStart() {
        arena.removeStartItems();

        final SquidGame plugin = SquidGame.getInstance();
        final int participationCoins = plugin.getMainConfig().getInt("economy.rewards.participation-coins", 15);

        for (final SquidPlayer player : this.arena.getPlayers()) {
            plugin.getPlayerDataManager().recordGamePlayed(player, participationCoins);
        }

        if (!this.isProxySilent()) {
            arena.broadcastMessage("arena.started");
        }
        arena.nextGame();
        arena.broadcastSound(this.mainConfig.getSound("game-settings.sounds.arena-start", "ORB_PICKUP"));
    }

    public void handleArenaTick() {
        if (arena.getState() == ArenaState.STARTING) {
            if (arena.getInternalTime() == 10) {
                if (!this.isProxySilent()) {
                    arena.broadcastMessage("arena.starting");
                }
                arena.broadcastSound(this.mainConfig.getSound("game-settings.sounds.arena-starting", "CLICK"));
            }

            else if (arena.getInternalTime() <= 5 && arena.getInternalTime() > 0) {
                if (!this.isProxySilent()) {
                    arena.broadcastMessage("arena.starting");
                }
                arena.broadcastSound(this.mainConfig.getSound("game-settings.sounds.arena-countdown", "NOTE_PLING"));
            }

            else if (arena.getInternalTime() == 0) {
                this.handleArenaStart();
            }
        }

        else if (arena.getState() == ArenaState.EXPLAIN_GAME) {
            if (arena.getInternalTime() == 0) {
                final ArenaGameBase game = arena.getCurrentGame();

                this.arena.setInternalTime(game.getGameTime());
                this.arena.setState(ArenaState.IN_GAME);
                this.arena.setPvPAllowed(false);
                game.prepareStart();

                if (!game.delaysLobbyRemovalUntilPlayBegins()) {
                    game.endGameLobby();
                }
            }
        }

        else if (arena.getState() == ArenaState.IN_GAME) {
            if (arena.getInternalTime() == 0) {
                this.arena.setInternalTime(this.mainConfig.getInt("game-settings.finishing-time", 5));
                this.arena.setState(ArenaState.FINISHING_GAME);
                this.arena.getCurrentGame().onTimeUp();
            }
        }

        else if (arena.getState() == ArenaState.FINISHING_GAME) {
            if (arena.getInternalTime() == 0) {
                this.arena.nextGame();
            }
        }

        else if (arena.getState() == ArenaState.FINISHING_ARENA) {
            this.playFinishCelebration();

            if (arena.getInternalTime() == 0) {
                this.kickProxyPlayers();
                arena.resetArena();
            }
        }
    }

    private boolean isProxySilent() {
        return this.mainConfig.getBoolean("game-settings.proxy-mode.enabled", false)
                && this.mainConfig.getBoolean("game-settings.proxy-mode.silent-arena-messages", true);
    }

    public void handleArenaFinish(final ArenaFinishReason reason) {
        this.arena.setInternalTime(this.getFinishingTime());

        switch (reason) {
        case ALL_PLAYERS_DEATH:
            this.arena.broadcastTitle("events.finish.draw.title", "events.finish.draw.subtitle");
            return;
        case ONE_PLAYER_IN_ARENA:
            this.arena.broadcastTitle("events.finish.winner.title", "events.finish.winner.subtitle");

            final SquidPlayer winner = this.arena.calculateWinner();
            if (winner == null || this.arena.hasGrantedWinReward()) {
                break;
            }

            this.arena.markWinRewardGranted();

            final SquidGame plugin = SquidGame.getInstance();
            final int winCoins = plugin.getMainConfig().getInt("economy.rewards.win-coins", 100);
            plugin.getPlayerDataManager().recordWin(winner, winCoins);
            MessageUtils.send(plugin, winner.getBukkitPlayer(), "economy.win-reward", "{coins}",
                    String.valueOf(winCoins));

            // Give rewards
            final List<String> rewardCommands = this.mainConfig.getStringList("game-settings.rewards",
                    new ArrayList<>());

            for (final String reward : rewardCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        reward.replace("{winner}", winner.getBukkitPlayer().getName()));
            }

            break;
        case PLUGIN_STOP:
            break;
        }
    }

    private int getFinishingTime() {
        if (this.mainConfig.getBoolean("game-settings.proxy-mode.enabled", false)) {
            return this.mainConfig.getInt("game-settings.proxy-mode.finish-kick-delay", 8);
        }

        return this.mainConfig.getInt("game-settings.finishing-time", 5);
    }

    private void playFinishCelebration() {
        if (!this.mainConfig.getBoolean("game-settings.spawn-fireworks-on-win", true)) {
            return;
        }

        final SquidPlayer winner = this.arena.calculateWinner();
        final Color color = winner == null ? Color.AQUA : Color.RED;

        for (final SquidPlayer player : this.arena.getAllPlayers()) {
            player.spawnFirework(1, 1, color, true);
        }
    }

    private void kickProxyPlayers() {
        if (!this.mainConfig.getBoolean("game-settings.proxy-mode.enabled", false)
                || !this.mainConfig.getBoolean("game-settings.proxy-mode.kick-on-finish", true)) {
            return;
        }

        for (final SquidPlayer player : new ArrayList<>(this.arena.getAllPlayers())) {
            player.getBukkitPlayer()
                    .kickPlayer(MessageUtils.format(SquidGame.getInstance(), "events.finish.proxy-kick"));
        }
    }
}
