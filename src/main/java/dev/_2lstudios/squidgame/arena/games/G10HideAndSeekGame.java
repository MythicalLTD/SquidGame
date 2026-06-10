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
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Openable;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class G10HideAndSeekGame extends ArenaGameBase {

    private static final int TEAM_SEEKER = 1;
    private static final int TEAM_HIDER = 2;

    private final Map<UUID, Integer> teams;
    private final Set<UUID> seekerPassed;
    private final Set<UUID> hiderEscaped;

    private boolean playing;
    private boolean finished;
    private boolean seekersReleased;

    public G10HideAndSeekGame(final Arena arena, final int durationTime) {
        super("§9Hide and Seek", "hide-and-seek", durationTime, arena);

        this.teams = new HashMap<>();
        this.seekerPassed = new HashSet<>();
        this.hiderEscaped = new HashSet<>();
    }

    @Override
    public Location getSpawnPosition() {
        final Location location = this.getArena().getConfig().getLocation("games.hide-and-seek.hider-spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    @Override
    public void onStart() {
        this.teams.clear();
        this.seekerPassed.clear();
        this.hiderEscaped.clear();
        this.playing = true;
        this.finished = false;
        this.seekersReleased = false;
        this.getArena().setPvPAllowed(true);

        if (this.getExitZone() == null) {
            this.getArena().broadcastTitle("games.hide-and-seek.not-configured.title",
                    "games.hide-and-seek.not-configured.subtitle");
            this.finishGame();
            return;
        }

        this.assignTeams();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (this.isSeeker(player)) {
                player.teleport(this.getWaitingRoom());
                player.sendTitle("games.hide-and-seek.seeker.title", "games.hide-and-seek.seeker.subtitle", 3);
            } else {
                player.teleport(this.getHiderSpawn());
                this.giveHiderKey(player.getBukkitPlayer());
                player.sendTitle("games.hide-and-seek.hider.title", "games.hide-and-seek.hider.subtitle", 3);
            }
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            this.releaseSeekers();
        }, this.getHideTime() * 20L);
        this.scheduleExitCheck();
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.getArena().setPvPAllowed(false);

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removeGameItems(player.getBukkitPlayer());
        }
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public void onTimeUp() {
        this.finishGame();
    }

    public boolean handlePlayerAttack(final SquidPlayer attacker, final SquidPlayer victim, final boolean fatal) {
        if (!this.playing || this.finished || attacker == null || victim == null) {
            return false;
        }

        if (this.isHider(victim) && this.hiderEscaped.contains(victim.getBukkitPlayer().getUniqueId())) {
            return true;
        }

        if (this.isSeeker(attacker) && !this.seekersReleased) {
            return true;
        }

        if (this.isSeeker(attacker) && this.isSeeker(victim)) {
            if (fatal) {
                attacker.sendTitle("games.hide-and-seek.lost.title", "games.hide-and-seek.lost.subtitle", 3);
                this.getArena().killPlayer(attacker);
            }

            return true;
        }

        if (this.isHider(attacker) && this.isHider(victim)) {
            return true;
        }

        if (fatal && this.isSeeker(attacker) && this.isHider(victim)) {
            this.seekerPassed.add(attacker.getBukkitPlayer().getUniqueId());
            attacker.sendTitle("games.hide-and-seek.seeker-pass.title", "games.hide-and-seek.seeker-pass.subtitle", 3);
        }

        return false;
    }

    public void handleMove(final SquidPlayer player, final Location to) {
        if (!this.playing || this.finished || !this.getArena().getPlayers().contains(player)) {
            return;
        }

        final Block block = to.clone().subtract(0, 1, 0).getBlock();

        if (this.isBlueStainedGlass(block)) {
            player.sendTitle("games.hide-and-seek.trap.title", "games.hide-and-seek.trap.subtitle", 3);
            this.removeGameItems(player.getBukkitPlayer());
            this.getArena().killPlayer(player);
        }
    }

    public boolean handleDoorUnlock(final SquidPlayer player, final Block block, final ItemStack item) {
        if (!this.playing || this.finished || !this.getArena().getPlayers().contains(player)) {
            return false;
        }

        if (!this.isHider(player) || !this.isHiderKey(item) || !this.isIronDoor(block)) {
            return false;
        }

        this.openDoor(block);
        this.consumeKey(item, player.getBukkitPlayer());
        MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.hide-and-seek.door-opened");
        return true;
    }

    private void releaseSeekers() {
        if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        this.seekersReleased = true;
        this.getArena().broadcastTitle("games.hide-and-seek.release.title", "games.hide-and-seek.release.subtitle");

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (this.isSeeker(player)) {
                player.teleport(this.getSeekerSpawn());
                this.giveSeekerKnife(player.getBukkitPlayer());
            }
        }
    }

    private void scheduleExitCheck() {
        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
                return;
            }

            this.checkHiderExits();
            this.scheduleExitCheck();
        }, 40L);
    }

    private void checkHiderExits() {
        final Cuboid exit = this.getExitZone();

        if (exit == null) {
            return;
        }

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (this.isHider(player) && !this.hiderEscaped.contains(player.getBukkitPlayer().getUniqueId())
                    && exit.isBetween(player.getBukkitPlayer().getLocation())) {
                this.hiderEscaped.add(player.getBukkitPlayer().getUniqueId());
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
            }
        }

        if (this.haveAllHidersEscaped()) {
            this.finishGame();
        }
    }

    private boolean haveAllHidersEscaped() {
        boolean hasHiders = false;

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (this.isHider(player)) {
                hasHiders = true;

                if (!this.hiderEscaped.contains(player.getBukkitPlayer().getUniqueId())) {
                    return false;
                }
            }
        }

        return hasHiders;
    }

    private void finishGame() {
        if (this.finished) {
            return;
        }

        this.finished = true;
        this.playing = false;
        this.getArena().setPvPAllowed(false);
        this.checkHiderExits();

        final List<SquidPlayer> alive = new ArrayList<>();
        final List<SquidPlayer> death = new ArrayList<>();

        for (final SquidPlayer player : new ArrayList<>(this.getArena().getPlayers())) {
            if (this.isHider(player)) {
                alive.add(player);
            } else if (this.seekerPassed.contains(player.getBukkitPlayer().getUniqueId())) {
                alive.add(player);
            } else {
                death.add(player);
            }

            this.removeGameItems(player.getBukkitPlayer());
        }

        this.getArena().broadcastTitle("games.hide-and-seek.finish.title", "games.hide-and-seek.finish.subtitle");

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : alive) {
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
            }

            for (final SquidPlayer player : death) {
                player.sendTitle("games.hide-and-seek.lost.title", "games.hide-and-seek.lost.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                if (this.getArena().getPlayers().contains(player)) {
                    this.getArena().killPlayer(player);
                }
            }
        }, 80L);

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }
    }

    private void assignTeams() {
        final List<SquidPlayer> players = new ArrayList<>(this.getArena().getPlayers());
        Collections.shuffle(players);

        final int seekerCount = Math.max(1, players.size() / 3);

        for (int i = 0; i < players.size(); i++) {
            this.teams.put(players.get(i).getBukkitPlayer().getUniqueId(), i < seekerCount ? TEAM_SEEKER : TEAM_HIDER);
        }
    }

    private boolean isSeeker(final SquidPlayer player) {
        return this.getTeam(player) == TEAM_SEEKER;
    }

    private boolean isHider(final SquidPlayer player) {
        return this.getTeam(player) == TEAM_HIDER;
    }

    private int getTeam(final SquidPlayer player) {
        final Integer team = this.teams.get(player.getBukkitPlayer().getUniqueId());
        return team == null ? 0 : team;
    }

    private Location getHiderSpawn() {
        final Location location = this.getArena().getConfig().getLocation("games.hide-and-seek.hider-spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private Location getSeekerSpawn() {
        final Location location = this.getArena().getConfig().getLocation("games.hide-and-seek.seeker-spawn", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private Location getWaitingRoom() {
        final Location location = this.getArena().getConfig().getLocation("arena.waiting_room", false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private Cuboid getExitZone() {
        return this.getArena().getConfig().getCuboid("games.hide-and-seek.exit");
    }

    @SuppressWarnings("deprecation")
    private boolean isBlueStainedGlass(final Block block) {
        if (block == null || block.getType() == null) {
            return false;
        }

        final String materialName = block.getType().name();

        if ("BLUE_STAINED_GLASS".equals(materialName)) {
            return true;
        }

        return materialName.contains("STAINED_GLASS") && block.getData() == 11;
    }

    private boolean isHiderKey(final ItemStack item) {
        return item != null && item.getType() == Material.TRIPWIRE_HOOK;
    }

    private boolean isIronDoor(final Block block) {
        return block != null && block.getType() != null && block.getType().name().contains("IRON_DOOR");
    }

    private void openDoor(final Block block) {
        this.setDoorOpen(block);
        this.setDoorOpen(block.getRelative(0, 1, 0));
        this.setDoorOpen(block.getRelative(0, -1, 0));
    }

    @SuppressWarnings("deprecation")
    private void setDoorOpen(final Block block) {
        if (!this.isIronDoor(block)) {
            return;
        }

        final BlockState state = block.getState();
        final org.bukkit.material.MaterialData data = state.getData();

        if (data instanceof Openable) {
            ((Openable) data).setOpen(true);
            state.setData(data);
            state.update(true);
        }
    }

    @SuppressWarnings("deprecation")
    private void consumeKey(final ItemStack item, final Player player) {
        if (item.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        player.updateInventory();
    }

    private int getHideTime() {
        return this.getArena().getMainConfig().getInt("game-settings.hide-and-seek-hide-time", 120);
    }

    private void giveHiderKey(final Player player) {
        final ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, 3);
        final ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.hide-and-seek-keys"));
        key.setItemMeta(meta);
        player.getInventory().addItem(key);
    }

    private void giveSeekerKnife(final Player player) {
        final ItemStack knife = new ItemStack(Material.IRON_SWORD);
        final ItemMeta meta = knife.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.seeker-knife"));
        knife.setItemMeta(meta);
        player.getInventory().addItem(knife);
    }

    private void removeGameItems(final Player player) {
        player.getInventory().remove(Material.TRIPWIRE_HOOK);
        player.getInventory().remove(Material.IRON_SWORD);
        player.updateInventory();
    }
}
