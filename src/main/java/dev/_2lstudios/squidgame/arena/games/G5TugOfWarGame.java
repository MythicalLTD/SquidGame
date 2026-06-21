package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class G5TugOfWarGame extends ArenaGameBase {

    private final Map<UUID, Integer> teams;
    private final Map<UUID, Long> lastPulls;
    private final Map<UUID, Location> anchors;
    private final java.util.Set<UUID> completed;

    private int ropePosition;
    private int team1Pulls;
    private int team2Pulls;
    private boolean finished;
    private boolean suddenDeath;
    private BukkitTask displayTask;

    public G5TugOfWarGame(final Arena arena, final int durationTime) {
        super("§eTug of War", "fifth", durationTime, arena);

        this.teams = new HashMap<>();
        this.lastPulls = new HashMap<>();
        this.anchors = new HashMap<>();
        this.completed = new java.util.HashSet<>();
    }

    @Override
    public Location getLobbyPosition() {
        return this.resolveArenaLocation("games.fifth.lobby", "arena.waiting_room", "arena.prelobby");
    }

    @Override
    protected boolean delaysPlaySpawnTeleport() {
        return true;
    }

    @Override
    public void onStart() {
        this.teams.clear();
        this.lastPulls.clear();
        this.anchors.clear();
        this.completed.clear();
        this.ropePosition = 0;
        this.team1Pulls = 0;
        this.team2Pulls = 0;
        this.finished = false;
        this.suddenDeath = false;
        this.getArena().setPvPAllowed(false);

        if (!this.isConfigured()) {
            this.getArena().broadcastTitle("games.fifth.not-configured.title",
                    "games.fifth.not-configured.subtitle");
            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> this.getArena().setInternalTime(1), 20L);
            return;
        }

        this.assignTeams();
        this.startCountdown();
    }

    private void startCountdown() {
        if (this.finished || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.teleportToTeamAnchor(player);
        }

        this.runStartCountdown(3);
    }

    private void runStartCountdown(final int secondsLeft) {
        if (this.finished || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        if (secondsLeft <= 0) {
            this.beginTugOfWar();
            return;
        }

        MessageUtils.broadcastTitle(SquidGame.getInstance(), this.getArena(), "events.start-countdown.title",
                "events.start-countdown.subtitle", "{time}", String.valueOf(secondsLeft));
        this.getArena().broadcastSound(this.getArena().getMainConfig().getSound("game-settings.sounds.arena-countdown",
                "NOTE_PLING"));

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> this.runStartCountdown(secondsLeft - 1),
                20L);
    }

    private void beginTugOfWar() {
        if (this.finished || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.giveRope(player.getBukkitPlayer());
            this.applyInvisibility(player);
            player.sendTitle("games.fifth.start.title", "games.fifth.start.subtitle", 3);
        }

        this.startDisplayTask();
        this.broadcastRopeDisplay();
    }

    @Override
    public void onStop() {
        this.stopDisplayTask();
        this.clearInvisibility();

        for (final SquidPlayer player : this.getArena().getAllPlayers()) {
            this.removeRope(player.getBukkitPlayer());
            player.sendActionBar("");
        }
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public void onTimeUp() {
        if (!this.finished) {
            if (this.ropePosition > 0) {
                this.finishGame(1);
            } else if (this.ropePosition < 0) {
                this.finishGame(2);
            } else if (this.team1Pulls > this.team2Pulls) {
                this.finishGame(1);
            } else if (this.team2Pulls > this.team1Pulls) {
                this.finishGame(2);
            } else if (!this.suddenDeath) {
                this.suddenDeath = true;
                this.getArena().setState(ArenaState.IN_GAME);
                this.getArena().setInternalTime(this.getSuddenDeathTime());
                this.getArena().broadcastTitle("games.fifth.sudden-death.title", "games.fifth.sudden-death.subtitle");
            } else {
                this.finishGame(0);
            }
        }
    }

    public boolean isPlaying() {
        return !this.finished && this.isConfigured();
    }

    public boolean shouldLockMovement(final SquidPlayer player, final Location from, final Location to) {
        if (!this.isPlaying() || !this.getArena().getPlayers().contains(player)) {
            return false;
        }

        final Location anchor = this.anchors.get(player.getBukkitPlayer().getUniqueId());
        if (anchor == null) {
            return false;
        }

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return false;
        }

        return true;
    }

    public Location getLockedLocation(final SquidPlayer player, final Location lookTarget) {
        final Location anchor = this.anchors.get(player.getBukkitPlayer().getUniqueId());
        if (anchor == null) {
            return lookTarget;
        }

        final Location locked = anchor.clone();
        locked.setYaw(lookTarget.getYaw());
        locked.setPitch(lookTarget.getPitch());
        return locked;
    }

    public void handlePullAction(final SquidPlayer player) {
        if (!this.isPlaying() || !this.getArena().getPlayers().contains(player)) {
            return;
        }

        final long now = System.currentTimeMillis();
        final UUID uuid = player.getBukkitPlayer().getUniqueId();
        final long lastPull = this.lastPulls.containsKey(uuid) ? this.lastPulls.get(uuid) : 0L;

        if (now - lastPull < this.getPullCooldown()) {
            return;
        }

        this.lastPulls.put(uuid, now);
        this.pull(player);
    }

    private void pull(final SquidPlayer player) {
        final int team = this.getTeam(player);
        final boolean sneaking = player.getBukkitPlayer().isSneaking();
        final int pullPower = sneaking ? this.getSneakPullPower() : this.getPullPower();

        if (team == 1) {
            this.ropePosition += pullPower;
            this.team1Pulls++;
        } else if (team == 2) {
            this.ropePosition -= pullPower;
            this.team2Pulls++;
        } else {
            return;
        }

        player.playSound(this.getPullSound());
        player.sendActionBar(this.buildPersonalActionBar(player, sneaking));
        this.broadcastRopeDisplay();

        if (this.ropePosition >= this.getWinDistance()) {
            this.finishGame(1);
        } else if (this.ropePosition <= -this.getWinDistance()) {
            this.finishGame(2);
        }
    }

    private void finishGame(final int winningTeam) {
        if (this.finished) {
            return;
        }

        this.finished = true;
        this.stopDisplayTask();
        this.getArena().broadcastTitle("games.fifth.finish.title", "games.fifth.finish.subtitle");

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(6);
        }

        final List<SquidPlayer> winners = new ArrayList<>();
        final List<SquidPlayer> losers = new ArrayList<>();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removeRope(player.getBukkitPlayer());
            player.getBukkitPlayer().closeInventory();
            player.sendActionBar("");

            if (winningTeam != 0 && this.getTeam(player) == winningTeam) {
                this.completed.add(player.getBukkitPlayer().getUniqueId());
                winners.add(player);
            } else {
                losers.add(player);
            }
        }

        this.clearInvisibilityFor(winners);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : winners) {
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
            }

            for (final SquidPlayer player : losers) {
                player.sendTitle("games.fifth.lost.title", "games.fifth.lost.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
            }
        }, 20L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer loser : new ArrayList<>(losers)) {
                if (!this.getArena().getPlayers().contains(loser)) {
                    continue;
                }

                loser.teleport(this.getFallLocation(loser));
            }
        }, 30L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer loser : new ArrayList<>(losers)) {
                this.getArena().killPlayer(loser);
            }
        }, 60L);
    }

    private void assignTeams() {
        final List<SquidPlayer> players = new ArrayList<>(this.getArena().getPlayers());
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            this.teams.put(players.get(i).getBukkitPlayer().getUniqueId(), i % 2 == 0 ? 1 : 2);
        }
    }

    private void teleportToTeamAnchor(final SquidPlayer player) {
        final Location location = this.getTeamAnchor(player).clone();
        final Vector direction = this.getTeamLocation(this.getTeam(player) == 1 ? 2 : 1).toVector()
                .subtract(location.toVector());
        direction.setY(0);

        if (direction.lengthSquared() > 0) {
            location.setDirection(direction);
        }

        player.teleport(location);
        this.anchors.put(player.getBukkitPlayer().getUniqueId(), location.clone());
    }

    private Location getTeamAnchor(final SquidPlayer player) {
        return this.getTeamLocation(this.getTeam(player));
    }

    private Location getTeamLocation(final int team) {
        final String key = team == 1 ? "games.fifth.team1" : "games.fifth.team2";
        final Location location = this.getArena().getConfig().getLocation(key, false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private boolean isConfigured() {
        return this.getArena().getConfig().contains("games.fifth.team1.x")
                && this.getArena().getConfig().contains("games.fifth.team2.x");
    }

    private void startDisplayTask() {
        this.stopDisplayTask();
        this.displayTask = Bukkit.getScheduler().runTaskTimer(SquidGame.getInstance(), this::broadcastRopeDisplay, 0L,
                10L);
    }

    private void stopDisplayTask() {
        if (this.displayTask != null) {
            this.displayTask.cancel();
            this.displayTask = null;
        }
    }

    private void broadcastRopeDisplay() {
        if (!this.isPlaying()) {
            return;
        }

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.sendActionBar(this.buildPersonalActionBar(player, false));
        }
    }

    private String buildPersonalActionBar(final SquidPlayer player, final boolean pulled) {
        final int team = this.getTeam(player);
        final int winDistance = this.getWinDistance();
        final int clamped = Math.max(-winDistance, Math.min(winDistance, this.ropePosition));
        final int segments = 21;
        final int marker = (int) Math.round(((clamped + winDistance) / (double) (winDistance * 2)) * (segments - 1));
        final StringBuilder bar = new StringBuilder();

        if (pulled) {
            bar.append("§a§lPULL! §r");
        }

        bar.append(team == 1 ? "§9§lYOUR TEAM" : "§7Team 1");
        bar.append(" §8|");

        for (int i = 0; i < segments; i++) {
            if (i == marker) {
                bar.append("§e█");
            } else if (i < marker) {
                bar.append("§9▌");
            } else {
                bar.append("§c▌");
            }
        }

        bar.append("§8| ");
        bar.append(team == 2 ? "§c§lYOUR TEAM" : "§7Team 2");
        bar.append(" §7(").append(clamped > 0 ? "+" : "").append(clamped).append(")");

        return bar.toString();
    }

    private int getTeam(final SquidPlayer player) {
        final Integer team = this.teams.get(player.getBukkitPlayer().getUniqueId());
        return team == null ? 0 : team;
    }

    private int getWinDistance() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-win-distance", 20);
    }

    private int getPullCooldown() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-pull-cooldown-ms", 150);
    }

    private int getSuddenDeathTime() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-sudden-death-time", 15);
    }

    private int getPullPower() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-pull-power", 3);
    }

    private int getSneakPullPower() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-sneak-pull-power", 5);
    }

    private void applyInvisibility(final SquidPlayer player) {
        player.getBukkitPlayer().addPotionEffect(
                new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 60 * 60, 0), true);

        for (final SquidPlayer other : this.getArena().getPlayers()) {
            if (other == player) {
                continue;
            }

            player.getBukkitPlayer().hidePlayer(other.getBukkitPlayer());
            other.getBukkitPlayer().hidePlayer(player.getBukkitPlayer());
        }
    }

    private void clearInvisibilityFor(final List<SquidPlayer> players) {
        for (final SquidPlayer player : players) {
            player.getBukkitPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);

            for (final SquidPlayer other : this.getArena().getAllPlayers()) {
                if (other == player) {
                    continue;
                }

                player.getBukkitPlayer().showPlayer(other.getBukkitPlayer());
            }
        }
    }

    private void clearInvisibility() {
        for (final SquidPlayer player : this.getArena().getAllPlayers()) {
            player.getBukkitPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);

            for (final SquidPlayer other : this.getArena().getAllPlayers()) {
                if (other == player) {
                    continue;
                }

                player.getBukkitPlayer().showPlayer(other.getBukkitPlayer());
            }
        }
    }

    private Location getFallLocation(final SquidPlayer player) {
        final Location anchor = this.anchors.get(player.getBukkitPlayer().getUniqueId());

        if (anchor != null) {
            return anchor.clone().add(0, -20, 0);
        }

        return player.getBukkitPlayer().getLocation().clone().add(0, -20, 0);
    }

    private Sound getPullSound() {
        return this.getArena().getMainConfig().getSound("game-settings.sounds.tug-of-war-pull", "IRONGOLEM_HIT");
    }

    private void giveRope(final Player player) {
        final ItemStack rope = this.createRopeItem();

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, rope.clone());
        }

        player.updateInventory();
    }

    private ItemStack createRopeItem() {
        final ItemStack rope = new ItemStack(Material.STRING);
        final ItemMeta meta = rope.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.tug-of-war-rope"));
        rope.setItemMeta(meta);
        return rope;
    }

    private void removeRope(final Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            final ItemStack item = player.getInventory().getItem(slot);

            if (item != null && item.getType() == Material.STRING) {
                player.getInventory().setItem(slot, null);
            }
        }

        player.updateInventory();
    }
}
