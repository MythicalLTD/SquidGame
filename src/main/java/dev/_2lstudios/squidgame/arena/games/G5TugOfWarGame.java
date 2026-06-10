package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class G5TugOfWarGame extends ArenaGameBase {

    private final Map<UUID, Integer> teams;
    private final Map<UUID, Long> lastPulls;
    private final Map<Block, Material> ropeBlocks;
    private final Set<UUID> completed;

    private int ropePosition;
    private int team1Pulls;
    private int team2Pulls;
    private boolean finished;
    private boolean suddenDeath;
    private Block ropeMarker;

    public G5TugOfWarGame(final Arena arena, final int durationTime) {
        super("§eTug of War", "fifth", durationTime, arena);

        this.teams = new HashMap<>();
        this.lastPulls = new HashMap<>();
        this.ropeBlocks = new HashMap<>();
        this.completed = new HashSet<>();
    }

    @Override
    public Location getSpawnPosition() {
        final Configuration config = this.getArena().getConfig();
        final Location location = config.getLocation("games.fifth.spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    @Override
    public void onStart() {
        this.teams.clear();
        this.lastPulls.clear();
        this.completed.clear();
        this.ropePosition = 0;
        this.team1Pulls = 0;
        this.team2Pulls = 0;
        this.finished = false;
        this.suddenDeath = false;
        this.ropeMarker = null;
        this.assignTeams();
        this.spawnVisualRope();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.teleportToTeamPlatform(player);
            this.giveRope(player.getBukkitPlayer());
            player.sendTitle("games.fifth.start.title", "games.fifth.start.subtitle", 3);
        }
    }

    @Override
    public void onStop() {
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removeRope(player.getBukkitPlayer());
        }

        this.removeVisualRope();
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

    public void openChallenge(final SquidPlayer player) {
        if (!this.finished && this.getArena().getPlayers().contains(player)) {
            new TugOfWarGUI(this, player).open(player.getBukkitPlayer());
        }
    }

    public void handleMove(final SquidPlayer player, final Location from, final Location to) {
        if (this.finished || !this.getArena().getPlayers().contains(player) || !this.isPullMovement(player, from, to)) {
            return;
        }

        this.handlePullAction(player);
    }

    public void handlePullAction(final SquidPlayer player) {
        if (this.finished || !this.getArena().getPlayers().contains(player)) {
            return;
        }

        final long now = System.currentTimeMillis();
        final long lastPull = this.lastPulls.containsKey(player.getBukkitPlayer().getUniqueId())
                ? this.lastPulls.get(player.getBukkitPlayer().getUniqueId())
                : 0L;

        if (now - lastPull < this.getPullCooldown()) {
            return;
        }

        this.lastPulls.put(player.getBukkitPlayer().getUniqueId(), now);
        this.pull(player, false);
    }

    private boolean isPullMovement(final SquidPlayer player, final Location from, final Location to) {
        final int team = this.getTeam(player);

        if (team == 0) {
            return false;
        }

        final Vector movement = to.toVector().subtract(from.toVector());
        movement.setY(0);

        if (movement.lengthSquared() < 0.0025) {
            return false;
        }

        final Location ownPlatform = this.getTeamLocation(team);
        final Location enemyPlatform = this.getTeamLocation(team == 1 ? 2 : 1);
        final Vector towardEnemy = enemyPlatform.toVector().subtract(ownPlatform.toVector());
        final Vector awayFromEnemy = ownPlatform.toVector().subtract(enemyPlatform.toVector());
        towardEnemy.setY(0);
        awayFromEnemy.setY(0);

        if (towardEnemy.lengthSquared() <= 0 || awayFromEnemy.lengthSquared() <= 0) {
            return false;
        }

        final Vector facing = to.getDirection();
        facing.setY(0);

        if (facing.lengthSquared() <= 0 || facing.normalize().dot(towardEnemy.normalize()) < 0.25) {
            return false;
        }

        return movement.normalize().dot(awayFromEnemy.normalize()) > 0.45;
    }

    private void pull(final SquidPlayer player, final boolean reopenGui) {
        if (this.finished || !this.getArena().getPlayers().contains(player)) {
            return;
        }

        final int team = this.getTeam(player);
        final int pullPower = player.getBukkitPlayer().isSneaking() ? this.getSneakPullPower() : this.getPullPower();

        if (team == 1) {
            this.ropePosition += pullPower;
            this.team1Pulls++;
        } else if (team == 2) {
            this.ropePosition -= pullPower;
            this.team2Pulls++;
        }

        this.updateVisualRope();

        if (this.ropePosition >= this.getWinDistance()) {
            this.finishGame(1);
        } else if (this.ropePosition <= -this.getWinDistance()) {
            this.finishGame(2);
        } else if (reopenGui) {
            this.openChallenge(player);
        }
    }

    private void finishGame(final int winningTeam) {
        if (this.finished) {
            return;
        }

        this.finished = true;
        this.removeVisualRope();
        this.getArena().broadcastTitle("games.fifth.finish.title", "games.fifth.finish.subtitle");

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }

        final List<SquidPlayer> winners = new ArrayList<>();
        final List<SquidPlayer> losers = new ArrayList<>();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removeRope(player.getBukkitPlayer());
            player.getBukkitPlayer().closeInventory();

            if (this.getTeam(player) == winningTeam) {
                this.completed.add(player.getBukkitPlayer().getUniqueId());
                winners.add(player);
            } else {
                losers.add(player);
            }
        }

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
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer loser : losers) {
                this.getArena().killPlayer(loser);
            }
        }, 80L);
    }

    private void assignTeams() {
        final List<SquidPlayer> players = new ArrayList<>(this.getArena().getPlayers());
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            this.teams.put(players.get(i).getBukkitPlayer().getUniqueId(), i % 2 == 0 ? 1 : 2);
        }
    }

    private void teleportToTeamPlatform(final SquidPlayer player) {
        final String key = this.getTeam(player) == 1 ? "games.fifth.team1" : "games.fifth.team2";
        final Location location = this.getArena().getConfig().getLocation(key, false);
        location.setWorld(this.getArena().getWorld());
        final Vector direction = this.getTeamLocation(this.getTeam(player) == 1 ? 2 : 1).toVector()
                .subtract(location.toVector());
        direction.setY(0);
        location.setDirection(direction);
        player.teleport(location);
    }

    private void spawnVisualRope() {
        if (!this.ropeBlocks.isEmpty()) {
            return;
        }

        final Location first = this.getTeamLocation(1).clone();
        final Location second = this.getTeamLocation(2).clone();
        final int steps = Math.max(1, (int) first.distance(second));

        for (int i = 0; i <= steps; i++) {
            final double percentage = (double) i / (double) steps;
            final int x = (int) Math.round(first.getX() + (second.getX() - first.getX()) * percentage);
            final int y = (int) Math.round((first.getY() + second.getY()) / 2.0D) + 1;
            final int z = (int) Math.round(first.getZ() + (second.getZ() - first.getZ()) * percentage);
            final Block block = this.getArena().getWorld().getBlockAt(x, y, z);

            if (!this.ropeBlocks.containsKey(block)) {
                this.ropeBlocks.put(block, block.getType());
                block.setType(Material.COAL_BLOCK);
            }
        }

        this.updateVisualRope();
    }

    private void removeVisualRope() {
        for (final Map.Entry<Block, Material> entry : this.ropeBlocks.entrySet()) {
            entry.getKey().setType(entry.getValue());
        }

        this.ropeBlocks.clear();
        this.ropeMarker = null;
    }

    private void updateVisualRope() {
        if (this.ropeBlocks.isEmpty()) {
            return;
        }

        if (this.ropeMarker != null && this.ropeMarker.getType() == Material.GOLD_BLOCK) {
            this.ropeMarker.setType(Material.COAL_BLOCK);
        }

        final Location first = this.getTeamLocation(1).clone();
        final Location second = this.getTeamLocation(2).clone();
        final double clampedPosition = Math.max(-this.getWinDistance(), Math.min(this.getWinDistance(), this.ropePosition));
        final double percentage = Math.max(0.0D, Math.min(1.0D, 0.5D - (clampedPosition / (this.getWinDistance() * 2.0D))));
        final int x = (int) Math.round(first.getX() + (second.getX() - first.getX()) * percentage);
        final int y = (int) Math.round((first.getY() + second.getY()) / 2.0D) + 1;
        final int z = (int) Math.round(first.getZ() + (second.getZ() - first.getZ()) * percentage);

        this.ropeMarker = this.getArena().getWorld().getBlockAt(x, y, z);

        if (this.ropeBlocks.containsKey(this.ropeMarker)) {
            this.ropeMarker.setType(Material.GOLD_BLOCK);
        }
    }

    private Location getTeamLocation(final int team) {
        final String key = team == 1 ? "games.fifth.team1" : "games.fifth.team2";
        final Location location = this.getArena().getConfig().getLocation(key, false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private int getTeam(final SquidPlayer player) {
        final Integer team = this.teams.get(player.getBukkitPlayer().getUniqueId());
        return team == null ? 0 : team;
    }

    private int getWinDistance() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-win-distance", 20);
    }

    private int getPullCooldown() {
        return this.getArena().getMainConfig().getInt("game-settings.tug-of-war-pull-cooldown-ms", 350);
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

    private int getRopePosition() {
        return this.ropePosition;
    }

    private boolean isCompleted(final SquidPlayer player) {
        return this.completed.contains(player.getBukkitPlayer().getUniqueId());
    }

    private void giveRope(final Player player) {
        final ItemStack rope = new ItemStack(Material.STRING);
        final ItemMeta meta = rope.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.tug-of-war-rope"));
        rope.setItemMeta(meta);
        player.getInventory().addItem(rope);
    }

    private void removeRope(final Player player) {
        player.getInventory().remove(Material.STRING);
        player.updateInventory();
    }

    private static class TugOfWarGUI extends InventoryGUI {
        private final G5TugOfWarGame game;
        private final SquidPlayer player;

        public TugOfWarGUI(final G5TugOfWarGame game, final SquidPlayer player) {
            super("§e§lTug of War", 27);
            this.game = game;
            this.player = player;
        }

        @Override
        public void init() {
            final int team = this.game.getTeam(this.player);
            final int ropePosition = this.game.getRopePosition();
            final String leader = ropePosition == 0 ? "Even" : ropePosition > 0 ? "Team 1" : "Team 2";

            this.addItem(0, this.createItem("§eTeam §f" + team, Material.STRING,
                    "§r\n§7Rope position: §f" + ropePosition + "\n§7Leader: §f" + leader
                            + "\n§7Win distance: §f" + this.game.getWinDistance() + "\n§r"),
                    5, 2);
            this.addItem(1, this.createItem("§aHow to pull", Material.LEVER,
                    "§r\n§7Right-click the rope item to pull.\n§7You can also spam sneak.\n§7Sneaking gives extra force.\n§7The gold block shows rope control.\n§r"), 5, 3);
        }

        @Override
        public void handle(final int id, final Player player) {
            if (id == 1 && !this.game.isCompleted(this.player)) {
                this.game.handlePullAction(this.player);
            }
        }
    }
}
