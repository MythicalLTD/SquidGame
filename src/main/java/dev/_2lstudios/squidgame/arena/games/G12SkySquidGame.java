package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaFinishReason;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.ArenaZoneUtils;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;
import dev._2lstudios.squidgame.utils.SkySquidSetupHelper;

public class G12SkySquidGame extends ArenaGameBase {

    private enum Phase {
        WELCOME,
        WAITING_BUTTON,
        FIGHTING,
        BRIDGE
    }

    private final Map<Integer, List<Vector3>> bridgeSlotCache;

    private Phase phase;
    private int round;
    private int activeBridgeRound;
    private boolean playing;
    private boolean killThisRound;
    private boolean gameBegun;
    private BukkitTask phaseTask;
    private BukkitTask hintTask;

    public G12SkySquidGame(final Arena arena, final int durationTime) {
        super("§6Sky Squid Game", "sky-squid", durationTime, arena);
        this.bridgeSlotCache = new HashMap<>();
        this.activeBridgeRound = 0;
    }

    @Override
    public Location getLobbyPosition() {
        return this.resolveArenaLocation("games.sky-squid.lobby", "arena.waiting_room", "arena.prelobby");
    }

    @Override
    protected boolean delaysPlaySpawnTeleport() {
        return true;
    }

    @Override
    public void onExplainStart() {
        this.broadcastTitleAfterSeconds(1, "games.sky-squid.welcome.title", "games.sky-squid.welcome.subtitle");
        this.broadcastTitleAfterSeconds(4, "games.sky-squid.tutorial.1.title", "games.sky-squid.tutorial.1.subtitle");
        this.broadcastTitleAfterSeconds(7, "games.sky-squid.tutorial.2.title", "games.sky-squid.tutorial.2.subtitle");
        this.broadcastTitleAfterSeconds(10, "games.sky-squid.tutorial.3.title", "games.sky-squid.tutorial.3.subtitle");
        this.broadcastTitleAfterSeconds(13, "games.sky-squid.tutorial.4.title", "games.sky-squid.tutorial.4.subtitle");
        this.broadcastTitleAfterSeconds(15, "events.game-start.title", "events.game-start.subtitle");
    }

    @Override
    public void onStart() {
        this.round = 0;
        this.playing = true;
        this.gameBegun = false;
        this.killThisRound = false;
        this.phase = Phase.WELCOME;
        this.getArena().setPvPAllowed(false);

        if (!this.isConfigured()) {
            this.getArena().broadcastTitle("games.sky-squid.not-configured.title",
                    "games.sky-squid.not-configured.subtitle");
            this.playing = false;
            if (this.getArena().getState() == ArenaState.IN_GAME) {
                this.getArena().setInternalTime(1);
            }
            return;
        }

        this.cacheBridgeMarkerSlots();
        this.hideBridgeMarkers();

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
                return;
            }

            this.beginRound(1);
        }, this.getStartTeleportDelayTicks());
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.cancelPhaseTask();
        this.stopPhaseHints();
        this.shrinkBridge();
        this.restoreBridgeMarkers();
        this.bridgeSlotCache.clear();
        this.activeBridgeRound = 0;
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

        this.finishWithWinners();
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public boolean isFightingPhase() {
        return this.playing && this.phase == Phase.FIGHTING;
    }

    public boolean isBridgePhase() {
        return this.playing && this.phase == Phase.BRIDGE;
    }

    public void handleBridgeMove(final SquidPlayer player, final Location to) {
        if (!this.isBridgePhase() || this.getArena().getPlayers().size() != 1) {
            return;
        }

        final Cuboid safeZone = this.getDestinationTowerZone();
        if (safeZone == null || !this.isInsideSafeZone(safeZone, to)) {
            return;
        }

        this.cancelPhaseTask();
        this.shrinkBridge();
        this.finishWithWinners();
    }

    public boolean handleButtonPress(final SquidPlayer player, final Location clickedBlock) {
        if (!this.playing || player == null) {
            return false;
        }

        if (this.phase != Phase.WELCOME && this.phase != Phase.WAITING_BUTTON) {
            return false;
        }

        if (!this.matchesRoundButton(clickedBlock) && !this.matchesRoundButton(
                player.getBukkitPlayer().getLocation().getBlock().getLocation())) {
            return false;
        }

        if (!this.gameBegun) {
            this.gameBegun = true;
            this.getArena().broadcastTitle("games.sky-squid.game-begin.title",
                    "games.sky-squid.game-begin.subtitle");
        }

        this.startFightingPhase();
        return true;
    }

    public boolean handleKnockback(final SquidPlayer attacker, final SquidPlayer victim,
            final EntityDamageByEntityEvent event) {
        if (!this.isFightingPhase() || attacker == null || victim == null || attacker == victim) {
            return false;
        }

        event.setDamage(0);

        final Vector direction = victim.getBukkitPlayer().getLocation().toVector()
                .subtract(attacker.getBukkitPlayer().getLocation().toVector());
        if (direction.lengthSquared() > 0) {
            direction.normalize().multiply(this.getKnockbackPower()).setY(this.getKnockbackVertical());
            victim.getBukkitPlayer().setVelocity(direction);
        }

        return true;
    }

    public void registerFallKill() {
        this.markEliminationAndAdvance();
    }

    public void registerRoundKill() {
        if (this.isFightingPhase()) {
            this.killThisRound = true;
        }
    }

    public void markEliminationAndAdvance() {
        if (!this.playing || this.phase != Phase.FIGHTING) {
            return;
        }

        this.killThisRound = true;
        this.cancelPhaseTask();
        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), this::completeFightingPhase, 40L);
    }

    @Override
    public void onPlayerEliminated(final SquidPlayer player) {
        if (!this.playing) {
            return;
        }

        if (this.phase == Phase.FIGHTING) {
            this.markEliminationAndAdvance();
            return;
        }

        if (this.phase == Phase.BRIDGE && this.getArena().getPlayers().size() <= 1) {
            this.cancelPhaseTask();
            this.shrinkBridge();
            this.finishWithWinners();
        }
    }

    @Override
    public boolean shouldPreventSinglePlayerFinish() {
        return this.playing;
    }

    private void beginRound(final int nextRound) {
        this.round = nextRound;
        this.killThisRound = false;
        this.phase = Phase.WAITING_BUTTON;
        this.getArena().setPvPAllowed(false);
        this.getArena().teleportAllPlayers(this.getRoundSpawn());
        this.getArena().broadcastTitle(this.getRoundTitle(), "games.sky-squid.press-button.subtitle");
        this.startPhaseHints("games.sky-squid.hints.press-button");
    }

    private void startFightingPhase() {
        this.phase = Phase.FIGHTING;
        this.killThisRound = false;
        this.getArena().setPvPAllowed(true);
        this.getArena().broadcastTitle(this.getRoundTitle(), "games.sky-squid.round.subtitle");
        this.startPhaseHints("games.sky-squid.hints.fighting");
        this.schedulePhaseEnd(this.getRoundTime());
    }

    private void onRoundTimeUp() {
        this.completeFightingPhase();
    }

    private void completeFightingPhase() {
        if (!this.playing || this.phase != Phase.FIGHTING) {
            return;
        }

        this.getArena().setPvPAllowed(false);

        if (!this.killThisRound && this.getArena().getPlayers().size() <= 1) {
            this.killThisRound = true;
        }

        if (!this.killThisRound) {
            this.getArena().broadcastTitle("games.sky-squid.no-kill.title", "games.sky-squid.no-kill.subtitle");
            this.getArena().killAllPlayers();
            this.playing = false;
            return;
        }

        if (this.round >= 3) {
            this.finishWithWinners();
            return;
        }

        this.startBridgePhase();
    }

    private void startBridgePhase() {
        if (this.getArena().getPlayers().size() <= 1) {
            this.finishWithWinners();
            return;
        }

        this.phase = Phase.BRIDGE;
        this.getArena().setPvPAllowed(false);
        this.extendBridge();
        this.getArena().broadcastTitle("games.sky-squid.bridge.title", "games.sky-squid.bridge.subtitle");
        this.startPhaseHints("games.sky-squid.hints.bridge");
        this.schedulePhaseEnd(this.getBridgeCooldown());
    }

    private void onBridgeTimeUp() {
        if (!this.playing || this.phase != Phase.BRIDGE) {
            return;
        }

        this.shrinkBridge();
        this.eliminatePlayersLeftBehind();

        if (!this.playing) {
            return;
        }

        if (this.getArena().getPlayers().isEmpty()) {
            this.playing = false;
            this.getArena().finishArena(ArenaFinishReason.ALL_PLAYERS_DEATH);
            return;
        }

        if (this.getArena().getPlayers().size() <= 1) {
            this.finishWithWinners();
            return;
        }

        this.beginRound(this.round + 1);
    }

    private void eliminatePlayersLeftBehind() {
        final Cuboid safeZone = this.getDestinationTowerZone();
        if (safeZone == null) {
            return;
        }

        for (final SquidPlayer player : new ArrayList<>(this.getArena().getPlayers())) {
            if (!this.isInsideSafeZone(safeZone, player.getBukkitPlayer().getLocation())) {
                this.getArena().killPlayer(player);
            }
        }
    }

    private boolean isInsideSafeZone(final Cuboid safeZone, final Location location) {
        return ArenaZoneUtils.isInsideWithVerticalPadding(location, safeZone, this.getSafeZonePadding(), 2.5D);
    }

    private boolean isConfigured() {
        final Configuration config = this.getArena().getConfig();

        return SkySquidSetupHelper.hasTower(config, 1)
                && SkySquidSetupHelper.hasTower(config, 2)
                && SkySquidSetupHelper.hasTower(config, 3);
    }

    private double getBridgeMarkerScanHorizontal() {
        return this.getArena().getMainConfig().getDouble("game-settings.sky-squid-bridge-marker-scan-horizontal", 14.0D);
    }

    private int getBridgeMarkerScanVertical() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-bridge-marker-scan-vertical", 4);
    }

    private double getSafeZonePadding() {
        return this.getArena().getMainConfig().getDouble("game-settings.sky-squid-safe-zone-padding", 2.0D);
    }

    private void finishWithWinners() {
        if (!this.playing) {
            return;
        }

        this.playing = false;
        this.cancelPhaseTask();
        this.stopPhaseHints();
        this.shrinkBridge();
        this.getArena().setPvPAllowed(false);

        if (this.getArena().getPlayers().isEmpty()) {
            return;
        }

        this.getArena().broadcastTitle("games.sky-squid.finish.title", "games.sky-squid.finish.subtitle");

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
            player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game",
                    "LEVEL_UP"));
        }

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().finishArena(ArenaFinishReason.ONE_PLAYER_IN_ARENA);
        }
    }

    private void extendBridge() {
        this.shrinkBridge();

        final int bridgeRound = this.round;
        final List<Vector3> slots = this.getBridgeSlots(bridgeRound);

        if (slots.isEmpty()) {
            SquidGame.getInstance().getLogger().warning("Sky Squid bridge for round " + this.round
                    + " has no saved marker blocks — place "
                    + SkySquidSetupHelper.getBridgeMarkerName(this.round)
                    + " blocks and scan in the Sky Squid edit menu.");
            return;
        }

        SkySquidSetupHelper.placeBridgeBlocks(this.getArena().getWorld(), slots,
                SkySquidSetupHelper.getBridgeMarkerMaterial(bridgeRound));
        this.activeBridgeRound = bridgeRound;
        SquidGame.getInstance().getLogger().info("Sky Squid bridge for round " + this.round + " revealed "
                + slots.size() + " "
                + SkySquidSetupHelper.getBridgeMarkerName(this.round) + " blocks.");
    }

    private void cacheBridgeMarkerSlots() {
        this.bridgeSlotCache.clear();
        final Configuration config = this.getArena().getConfig();
        final org.bukkit.World world = this.getArena().getWorld();
        final double horizontalPadding = this.getBridgeMarkerScanHorizontal();
        final int verticalPadding = this.getBridgeMarkerScanVertical();

        for (int bridgeRound = 1; bridgeRound <= 2; bridgeRound++) {
            List<Vector3> slots = SkySquidSetupHelper.loadBridgeSlots(config, bridgeRound);
            final List<Vector3> liveMarkers = SkySquidSetupHelper.scanBridgeMarkerSlots(config, world, bridgeRound,
                    horizontalPadding, verticalPadding);

            if (!liveMarkers.isEmpty()) {
                slots = liveMarkers;
                SkySquidSetupHelper.saveBridgeSlots(config, bridgeRound, slots);
                config.safeSave();
            }

            this.bridgeSlotCache.put(bridgeRound, new ArrayList<>(slots));
        }
    }

    private List<Vector3> getBridgeSlots(final int bridgeRound) {
        final List<Vector3> cached = this.bridgeSlotCache.get(bridgeRound);
        return cached == null ? new ArrayList<>() : cached;
    }

    private void hideBridgeMarkers() {
        final org.bukkit.World world = this.getArena().getWorld();

        for (int bridgeRound = 1; bridgeRound <= 2; bridgeRound++) {
            SkySquidSetupHelper.clearBridgeBlocks(world, this.getBridgeSlots(bridgeRound));
        }
    }

    private void restoreBridgeMarkers() {
        final org.bukkit.World world = this.getArena().getWorld();

        for (int bridgeRound = 1; bridgeRound <= 2; bridgeRound++) {
            SkySquidSetupHelper.placeBridgeBlocks(world, this.getBridgeSlots(bridgeRound),
                    SkySquidSetupHelper.getBridgeMarkerMaterial(bridgeRound));
        }
    }

    private void shrinkBridge() {
        if (this.activeBridgeRound <= 0) {
            return;
        }

        SkySquidSetupHelper.clearBridgeBlocks(this.getArena().getWorld(),
                this.getBridgeSlots(this.activeBridgeRound));
        this.activeBridgeRound = 0;
    }

    private boolean matchesRoundButton(final Location location) {
        if (location == null || !CompatibilityUtils.isButtonBlock(location.getBlock())) {
            return false;
        }

        final Location configured = this.getRoundButton();
        if (configured != null) {
            return CompatibilityUtils.isSameBlock(location, configured);
        }

        final Location spawn = this.getRoundSpawn();
        if (spawn == null || spawn.getWorld() == null) {
            return false;
        }

        return location.getWorld().equals(spawn.getWorld()) && location.distanceSquared(spawn) <= 36.0D;
    }

    private Location getRoundButton() {
        final String key;

        if (this.round == 1) {
            key = "games.sky-squid.square-button";
        } else if (this.round == 2) {
            key = "games.sky-squid.triangle-button";
        } else {
            key = "games.sky-squid.circle-button";
        }

        if (!this.getArena().getConfig().contains(key + ".x")) {
            return null;
        }

        final Location location = this.getArena().getConfig().getLocation(key, false);
        location.setWorld(this.getArena().getWorld());
        return location;
    }

    private Cuboid getBridgeZone() {
        final String key = this.round == 1 ? "games.sky-squid.bridge-square" : "games.sky-squid.bridge-triangle";
        return this.getArena().getConfig().getCuboid(key);
    }

    private Cuboid getDestinationTowerZone() {
        final String key = this.round == 1 ? "games.sky-squid.tower-triangle" : "games.sky-squid.tower-circle";
        Cuboid zone = this.getArena().getConfig().getCuboid(key);

        if (zone != null) {
            return zone;
        }

        final int tower = this.round == 1 ? 2 : 3;
        final String spawnKey = SkySquidSetupHelper.getSpawnKey(tower);

        if (!this.getArena().getConfig().contains(spawnKey + ".x")) {
            return null;
        }

        final Location spawn = this.getArena().getConfig().getLocation(spawnKey, false);
        spawn.setWorld(this.getArena().getWorld());
        final int radius = this.getArena().getMainConfig().getInt("game-settings.sky-squid-tower-radius", 6);
        final int heightBelow = this.getArena().getMainConfig().getInt("game-settings.sky-squid-tower-height-below", 1);
        final int heightAbove = this.getArena().getMainConfig().getInt("game-settings.sky-squid-tower-height-above", 5);
        return SkySquidSetupHelper.createTowerZone(spawn, radius, heightBelow, heightAbove);
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
        return this.round == 3 ? this.getFinalRoundTime() : this.getRegularRoundTime();
    }

    private int getRegularRoundTime() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-round-time", 30);
    }

    private int getFinalRoundTime() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-final-round-time", 60);
    }

    private int getBridgeCooldown() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-bridge-cooldown", 15);
    }

    private double getKnockbackPower() {
        return this.getArena().getMainConfig().getDouble("game-settings.sky-squid-knockback-power", 1.2D);
    }

    private double getKnockbackVertical() {
        return this.getArena().getMainConfig().getDouble("game-settings.sky-squid-knockback-vertical", 0.4D);
    }

    private long getStartTeleportDelayTicks() {
        return this.getArena().getMainConfig().getInt("game-settings.sky-squid-start-teleport-delay-ticks", 40);
    }

    private void schedulePhaseEnd(final int seconds) {
        this.cancelPhaseTask();
        this.phaseTask = Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (this.phase == Phase.FIGHTING) {
                this.onRoundTimeUp();
            } else if (this.phase == Phase.BRIDGE) {
                this.onBridgeTimeUp();
            }
        }, seconds * 20L);
    }

    private void cancelPhaseTask() {
        if (this.phaseTask != null) {
            this.phaseTask.cancel();
            this.phaseTask = null;
        }
    }

    private void startPhaseHints(final String messageKey) {
        this.stopPhaseHints();
        this.hintTask = Bukkit.getScheduler().runTaskTimer(SquidGame.getInstance(), () -> {
            if (!this.playing || this.getArena().getState() != ArenaState.IN_GAME) {
                return;
            }

            this.broadcastActionBar(messageKey);
        }, 0L, 40L);
    }

    private void stopPhaseHints() {
        if (this.hintTask != null) {
            this.hintTask.cancel();
            this.hintTask = null;
        }
    }

    private void broadcastActionBar(final String key, final String... replacements) {
        final String message = MessageUtils.format(SquidGame.getInstance(), key, replacements);

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.sendActionBar(message);
        }
    }
}
