package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaFinishReason;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class G12SkySquidGame extends ArenaGameBase {

    private final Map<UUID, Integer> kills;

    private int round;
    private boolean playing;
    private boolean killThisRound;

    public G12SkySquidGame(final Arena arena, final int durationTime) {
        super("§6Sky Squid Game", "sky-squid", durationTime, arena);
        this.kills = new HashMap<>();
    }

    @Override
    public Location getSpawnPosition() {
        final Location location = this.getArena().getConfig().getLocation("games.sky-squid.square-spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    @Override
    public void onStart() {
        this.round = 0;
        this.playing = true;
        this.kills.clear();
        this.getArena().setPvPAllowed(true);
        this.startNextRound();
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.getArena().setPvPAllowed(false);
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public void onTimeUp() {
        if (!this.playing) {
            return;
        }

        if (!this.killThisRound) {
            this.getArena().broadcastTitle("games.sky-squid.no-kill.title", "games.sky-squid.no-kill.subtitle");
            this.getArena().killAllPlayers();
            return;
        }

        if (this.round >= 3 || this.getArena().getPlayers().size() <= 1) {
            this.finishFinalRound();
        } else {
            this.startNextRound();
        }
    }

    public boolean handlePlayerKill(final SquidPlayer killer, final SquidPlayer victim) {
        if (!this.playing || killer == null || victim == null || killer == victim) {
            return false;
        }

        this.killThisRound = true;
        final UUID uuid = killer.getBukkitPlayer().getUniqueId();
        this.kills.put(uuid, this.getKills(killer) + 1);
        killer.sendTitle("games.sky-squid.kill.title", "games.sky-squid.kill.subtitle", 2);
        this.eliminateToAdventureSpectator(victim);

        if (this.getArena().getPlayers().size() <= 1) {
            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                this.finishFinalRound();
            }, 2L);
        }

        return true;
    }

    private void startNextRound() {
        this.round++;
        this.killThisRound = false;

        if (this.getArena().getState() != ArenaState.IN_GAME) {
            this.getArena().setState(ArenaState.IN_GAME);
        }

        this.getArena().setInternalTime(this.round == 3 ? this.getFinalRoundTime() : this.getRoundTime());
        this.getArena().teleportAllPlayers(this.getRoundSpawn());
        this.getArena().broadcastTitle(this.getRoundTitle(), "games.sky-squid.round.subtitle");
    }

    private void finishFinalRound() {
        if (!this.playing) {
            return;
        }

        this.playing = false;
        this.getArena().setPvPAllowed(false);

        final SquidPlayer winner = this.findWinner();
        final List<SquidPlayer> death = new ArrayList<>(this.getArena().getPlayers());
        death.remove(winner);

        this.getArena().broadcastTitle("games.sky-squid.finish.title", "games.sky-squid.finish.subtitle");

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                if (this.getArena().getPlayers().contains(player)) {
                    this.eliminateToAdventureSpectator(player);
                }
            }

            if (winner != null && this.getArena().getPlayers().contains(winner)) {
                this.getArena().finishArena(ArenaFinishReason.ONE_PLAYER_IN_ARENA);
            }
        }, 40L);
    }

    @SuppressWarnings("deprecation")
    private void eliminateToAdventureSpectator(final SquidPlayer player) {
        final Player bukkitPlayer = player.getBukkitPlayer();

        this.getArena().killPlayer(player);

        if (bukkitPlayer.isDead()) {
            bukkitPlayer.spigot().respawn();
        }

        bukkitPlayer.setGameMode(GameMode.ADVENTURE);
        bukkitPlayer.setAllowFlight(false);
        bukkitPlayer.setFlying(false);
        bukkitPlayer.setFoodLevel(20);
        bukkitPlayer.setHealth(bukkitPlayer.getMaxHealth());
        bukkitPlayer.teleport(this.getSpectatorSpawn());
        player.sendTitle("games.sky-squid.spectator.title", "games.sky-squid.spectator.subtitle", 3);
    }

    private SquidPlayer findWinner() {
        final List<SquidPlayer> players = new ArrayList<>(this.getArena().getPlayers());

        if (players.isEmpty()) {
            return null;
        }

        Collections.sort(players, (first, second) -> Integer.compare(this.getKills(second), this.getKills(first)));
        return players.get(0);
    }

    private int getKills(final SquidPlayer player) {
        final Integer result = this.kills.get(player.getBukkitPlayer().getUniqueId());
        return result == null ? 0 : result;
    }

    private Location getRoundSpawn() {
        final String key;

        if (this.round == 1) {
            key = "games.sky-squid.square-spawn";
        } else if (this.round == 2) {
            key = "games.sky-squid.triangle-spawn";
        } else {
            key = "games.sky-squid.circle-spawn";
        }

        final Location location = this.getArena().getConfig().getLocation(key, false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private Location getSpectatorSpawn() {
        if (!this.getArena().getConfig().contains("games.sky-squid.spectator-spawn.x")) {
            return this.getRoundSpawn();
        }

        final Location location = this.getArena().getConfig().getLocation("games.sky-squid.spectator-spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private String getRoundTitle() {
        if (this.round == 1) {
            return "games.sky-squid.square.title";
        } else if (this.round == 2) {
            return "games.sky-squid.triangle.title";
        } else {
            return "games.sky-squid.circle.title";
        }
    }

    private int getRoundTime() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-round-time", 900);
    }

    private int getFinalRoundTime() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-final-round-time", 600);
    }
}
