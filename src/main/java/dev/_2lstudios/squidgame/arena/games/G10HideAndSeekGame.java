package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class G10HideAndSeekGame extends ArenaGameBase {

    private static final int TEAM_SEEKER = 1;
    private static final int TEAM_HIDER = 2;
    private static final double PUNCH_DAMAGE = 2.0D;
    private static final long DOUBLE_CLICK_MS = 500L;
    private static final long SWAP_REQUEST_EXPIRE_MS = 30000L;

    private final Map<UUID, Integer> teams;
    private final Set<UUID> seekerPassed;
    private final Map<UUID, UUID> lastInteractTarget;
    private final Map<UUID, Long> lastInteractTime;
    private final Map<String, SwapRequest> pendingSwaps;

    private boolean playing;
    private boolean finished;
    private boolean seekersReleased;
    private BukkitTask releaseTask;

    public G10HideAndSeekGame(final Arena arena, final int durationTime) {
        super("§9Hide and Seek", "hide-and-seek", durationTime, arena);

        this.teams = new HashMap<>();
        this.seekerPassed = new HashSet<>();
        this.lastInteractTarget = new HashMap<>();
        this.lastInteractTime = new HashMap<>();
        this.pendingSwaps = new HashMap<>();
    }

    @Override
    protected boolean delaysPlaySpawnTeleport() {
        return true;
    }

    @Override
    public void onExplainStart() {
        this.teams.clear();
        this.seekerPassed.clear();
        this.pendingSwaps.clear();
        this.lastInteractTarget.clear();
        this.lastInteractTime.clear();
        this.assignTeams();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.applyTeamTunic(player);
            this.sendRoleTitle(player);
        }

        this.broadcastTitleAfterSeconds(1, "games.hide-and-seek.welcome.title",
                "games.hide-and-seek.welcome.subtitle");
        this.broadcastTitleAfterSeconds(4, "games.hide-and-seek.tutorial.1.title",
                "games.hide-and-seek.tutorial.1.subtitle");
        this.broadcastTitleAfterSeconds(7, "games.hide-and-seek.tutorial.2.title",
                "games.hide-and-seek.tutorial.2.subtitle");
        this.broadcastTitleAfterSeconds(10, "games.hide-and-seek.tutorial.3.title",
                "games.hide-and-seek.tutorial.3.subtitle");
        this.broadcastTitleAfterSeconds(13, "games.hide-and-seek.tutorial.4.title",
                "games.hide-and-seek.tutorial.4.subtitle");
        this.broadcastTitleAfterSeconds(15, "events.game-start.title", "events.game-start.subtitle");
    }

    @Override
    public void onStart() {
        this.seekerPassed.clear();
        this.pendingSwaps.clear();
        this.lastInteractTarget.clear();
        this.lastInteractTime.clear();
        this.playing = true;
        this.finished = false;
        this.seekersReleased = false;
        this.getArena().setPvPAllowed(true);

        if (!this.isConfigured()) {
            this.getArena().broadcastTitle("games.hide-and-seek.not-configured.title",
                    "games.hide-and-seek.not-configured.subtitle");
            this.finishGame();
            return;
        }

        if (this.teams.isEmpty()) {
            this.assignTeams();
        }

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.applyTeamTunic(player);

            if (this.isSeeker(player)) {
                player.teleport(this.getWaitingRoom());
                player.sendTitle("games.hide-and-seek.seeker.title", "games.hide-and-seek.seeker.subtitle", 3);
            } else {
                player.teleport(this.getHiderSpawn());
                player.sendTitle("games.hide-and-seek.hider.title", "games.hide-and-seek.hider.subtitle", 3);
            }
        }

        this.releaseTask = Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), this::releaseSeekers,
                this.getHideTime() * 20L);
    }

    @Override
    public void onPlayerEliminated(final SquidPlayer player) {
        this.checkEarlyFinish();
    }

    private void checkEarlyFinish() {
        if (!this.playing || this.finished || this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        boolean hiderAlive = false;
        boolean seekerAlive = false;

        for (final SquidPlayer arenaPlayer : this.getArena().getPlayers()) {
            if (this.isHider(arenaPlayer)) {
                hiderAlive = true;
            } else if (this.isSeeker(arenaPlayer)) {
                seekerAlive = true;
            }
        }

        if (hiderAlive ^ seekerAlive) {
            this.finishGame();
        }
    }

    @Override
    public void onStop() {
        this.cancelReleaseTask();
        this.playing = false;
        this.getArena().setPvPAllowed(false);
        this.pendingSwaps.clear();

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

    public void handleTeamSwitchRequest(final SquidPlayer requester, final SquidPlayer target) {
        if (this.getArena().getState() != ArenaState.EXPLAIN_GAME || requester == null || target == null
                || requester.equals(target)) {
            return;
        }

        if (this.isSameTeam(requester, target)) {
            MessageUtils.send(SquidGame.getInstance(), requester.getBukkitPlayer(),
                    "games.hide-and-seek.switch.same-team");
            return;
        }

        final long now = System.currentTimeMillis();
        final UUID requesterId = requester.getBukkitPlayer().getUniqueId();
        final UUID targetId = target.getBukkitPlayer().getUniqueId();
        final UUID previousTarget = this.lastInteractTarget.get(requesterId);
        final Long previousTime = this.lastInteractTime.get(requesterId);

        this.lastInteractTarget.put(requesterId, targetId);
        this.lastInteractTime.put(requesterId, now);

        if (previousTarget == null || !previousTarget.equals(targetId) || previousTime == null
                || now - previousTime > DOUBLE_CLICK_MS) {
            return;
        }

        this.lastInteractTarget.remove(requesterId);
        this.lastInteractTime.remove(requesterId);
        this.sendSwapRequest(requester, target);
    }

    public boolean acceptSwapRequest(final SquidPlayer target, final String token) {
        if (this.getArena().getState() != ArenaState.EXPLAIN_GAME || target == null || token == null) {
            return false;
        }

        this.expireSwapRequests();
        final SwapRequest request = this.pendingSwaps.remove(token);

        if (request == null || !request.getTargetId().equals(target.getBukkitPlayer().getUniqueId())) {
            MessageUtils.send(SquidGame.getInstance(), target.getBukkitPlayer(), "games.hide-and-seek.switch.expired");
            return false;
        }

        final SquidPlayer requester = this.findPlayer(request.getRequesterId());

        if (requester == null || !this.getArena().getPlayers().contains(requester)) {
            MessageUtils.send(SquidGame.getInstance(), target.getBukkitPlayer(), "games.hide-and-seek.switch.expired");
            return false;
        }

        this.swapTeams(requester, target);
        MessageUtils.send(SquidGame.getInstance(), requester.getBukkitPlayer(), "games.hide-and-seek.switch.success");
        MessageUtils.send(SquidGame.getInstance(), target.getBukkitPlayer(), "games.hide-and-seek.switch.success");
        return true;
    }

    public boolean handlePlayerDamage(final SquidPlayer attacker, final SquidPlayer victim,
            final EntityDamageByEntityEvent event) {
        if (!this.playing || this.finished || attacker == null || victim == null) {
            return false;
        }

        if (this.isSameTeam(attacker, victim)) {
            event.setCancelled(true);
            return true;
        }

        if (this.isSeeker(attacker) && !this.seekersReleased) {
            event.setCancelled(true);
            return true;
        }

        if (!this.isSeekerSword(attacker.getBukkitPlayer().getItemInHand())) {
            event.setDamage(PUNCH_DAMAGE);
        }

        return false;
    }

    public boolean handlePlayerAttack(final SquidPlayer attacker, final SquidPlayer victim, final boolean fatal) {
        if (!this.playing || this.finished || attacker == null || victim == null) {
            return false;
        }

        if (fatal && this.isSeeker(attacker) && this.isHider(victim)) {
            this.seekerPassed.add(attacker.getBukkitPlayer().getUniqueId());
            attacker.sendTitle("games.hide-and-seek.seeker-pass.title", "games.hide-and-seek.seeker-pass.subtitle", 3);
        }

        return false;
    }

    public void handlePlayerDeath(final SquidPlayer victim, final SquidPlayer killer) {
        if (!this.playing || this.finished || victim == null) {
            return;
        }

        this.dropSeekerSword(victim.getBukkitPlayer());
    }

    public boolean isSeekerSword(final ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SWORD || !item.hasItemMeta()
                || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        return item.getItemMeta().getDisplayName()
                .equals(MessageUtils.format(SquidGame.getInstance(), "items.seeker-knife"));
    }

    public boolean canDropItem(final SquidPlayer player, final ItemStack item) {
        return this.playing && !this.finished && this.getArena().getPlayers().contains(player)
                && this.isSeekerSword(item);
    }

    public boolean canPickupItem(final SquidPlayer player, final ItemStack item) {
        return this.playing && !this.finished && this.getArena().getPlayers().contains(player)
                && this.isSeekerSword(item);
    }

    private void sendSwapRequest(final SquidPlayer requester, final SquidPlayer target) {
        this.expireSwapRequests();

        final String token = UUID.randomUUID().toString().substring(0, 8);
        this.pendingSwaps.put(token, new SwapRequest(requester.getBukkitPlayer().getUniqueId(),
                target.getBukkitPlayer().getUniqueId(), System.currentTimeMillis()));

        final TextComponent prefix = new TextComponent(MessageUtils.format(SquidGame.getInstance(),
                "games.hide-and-seek.switch.request", "{player}", requester.getBukkitPlayer().getName()));
        final TextComponent button = new TextComponent(
                MessageUtils.format(SquidGame.getInstance(), "games.hide-and-seek.switch.accept-button"));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/squid hideseekswap " + token));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(MessageUtils.format(SquidGame.getInstance(), "games.hide-and-seek.switch.hover"))
                        .create()));

        target.getBukkitPlayer().spigot().sendMessage(prefix, button);
        MessageUtils.send(SquidGame.getInstance(), requester.getBukkitPlayer(), "games.hide-and-seek.switch.sent",
                "{player}", target.getBukkitPlayer().getName());
    }

    private void swapTeams(final SquidPlayer first, final SquidPlayer second) {
        final int firstTeam = this.getTeam(first);
        this.teams.put(first.getBukkitPlayer().getUniqueId(), this.getTeam(second));
        this.teams.put(second.getBukkitPlayer().getUniqueId(), firstTeam);
        this.applyTeamTunic(first);
        this.applyTeamTunic(second);
        this.sendRoleTitle(first);
        this.sendRoleTitle(second);
    }

    private void expireSwapRequests() {
        final long now = System.currentTimeMillis();

        for (final Iterator<Map.Entry<String, SwapRequest>> iterator = this.pendingSwaps.entrySet().iterator(); iterator
                .hasNext();) {
            if (now - iterator.next().getValue().getCreatedAt() > SWAP_REQUEST_EXPIRE_MS) {
                iterator.remove();
            }
        }
    }

    private SquidPlayer findPlayer(final UUID playerId) {
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (player.getBukkitPlayer().getUniqueId().equals(playerId)) {
                return player;
            }
        }

        return null;
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
                this.giveSeekerSword(player.getBukkitPlayer());
            }
        }
    }

    private void finishGame() {
        if (this.finished) {
            return;
        }

        this.cancelReleaseTask();
        this.finished = true;
        this.playing = false;
        this.getArena().setPvPAllowed(false);

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
            this.getArena().setInternalTime(6);
        }
    }

    private void cancelReleaseTask() {
        if (this.releaseTask != null) {
            this.releaseTask.cancel();
            this.releaseTask = null;
        }
    }

    private void assignTeams() {
        final List<SquidPlayer> players = new ArrayList<>(this.getArena().getPlayers());
        Collections.shuffle(players);

        final int seekerCount = players.size() / 2;

        for (int i = 0; i < players.size(); i++) {
            this.teams.put(players.get(i).getBukkitPlayer().getUniqueId(), i < seekerCount ? TEAM_SEEKER : TEAM_HIDER);
        }
    }

    private boolean isConfigured() {
        return this.getArena().getConfig().contains("games.hide-and-seek.hider-spawn.x")
                && this.getArena().getConfig().contains("games.hide-and-seek.seeker-spawn.x")
                && this.getArena().getConfig().contains("arena.waiting_room.x");
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

    private boolean isSameTeam(final SquidPlayer first, final SquidPlayer second) {
        return this.getTeam(first) != 0 && this.getTeam(first) == this.getTeam(second);
    }

    private void sendRoleTitle(final SquidPlayer player) {
        if (this.isSeeker(player)) {
            player.sendTitle("games.hide-and-seek.seeker.title", "games.hide-and-seek.seeker.subtitle", 3);
        } else {
            player.sendTitle("games.hide-and-seek.hider.title", "games.hide-and-seek.hider.subtitle", 3);
        }
    }

    private void applyTeamTunic(final SquidPlayer player) {
        final ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        final LeatherArmorMeta meta = (LeatherArmorMeta) chestplate.getItemMeta();
        meta.setColor(this.isSeeker(player) ? Color.RED : Color.BLUE);
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(),
                this.isSeeker(player) ? "items.seeker-tunic" : "items.hider-tunic"));
        chestplate.setItemMeta(meta);
        player.getBukkitPlayer().getInventory().setChestplate(chestplate);
        player.getBukkitPlayer().updateInventory();
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

    private int getHideTime() {
        return this.getArena().getMainConfig().getInt("game-settings.hide-and-seek-hide-time", 15);
    }

    private void giveSeekerSword(final Player player) {
        this.removeSeekerSword(player);
        final ItemStack sword = new ItemStack(Material.IRON_SWORD);
        final ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.seeker-knife"));
        sword.setItemMeta(meta);
        player.getInventory().addItem(sword);
        player.updateInventory();
    }

    private void dropSeekerSword(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (this.isSeekerSword(item)) {
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                player.getInventory().remove(item);
            }
        }

        player.updateInventory();
    }

    private void removeSeekerSword(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (this.isSeekerSword(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    private void removeGameItems(final Player player) {
        this.removeSeekerSword(player);
        player.getInventory().setChestplate(null);
        player.updateInventory();
    }

    private static final class SwapRequest {
        private final UUID requesterId;
        private final UUID targetId;
        private final long createdAt;

        private SwapRequest(final UUID requesterId, final UUID targetId, final long createdAt) {
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.createdAt = createdAt;
        }

        private UUID getRequesterId() {
            return this.requesterId;
        }

        private UUID getTargetId() {
            return this.targetId;
        }

        private long getCreatedAt() {
            return this.createdAt;
        }
    }
}
