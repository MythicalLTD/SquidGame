package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.jelly.utils.BooleanUtils;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;
import dev._2lstudios.squidgame.utils.SkySquidSetupHelper;

public class G6GlassesGame extends ArenaGameBase {

    private static final int SOLVER_SLOT = 8;
    private static final long SOLVER_CONFIRM_MS = 8000L;

    private Cuboid glassZone;
    private Cuboid goalZone;

    private final List<Location> bridgeSlotLocations;
    private final List<Block> fakeBlocks;
    private final List<Block> safeBlocks;
    private final List<Block> bridgeBlocks;
    private final Set<UUID> finishedPlayers;
    private final Map<UUID, Long> solverConfirmations;

    private boolean bridgeActive;

    public G6GlassesGame(final Arena arena, final int durationTime) {
        super("§bGlasses", "sixth", durationTime, arena);

        this.bridgeSlotLocations = new ArrayList<>();
        this.fakeBlocks = new ArrayList<>();
        this.safeBlocks = new ArrayList<>();
        this.bridgeBlocks = new ArrayList<>();
        this.finishedPlayers = new HashSet<>();
        this.solverConfirmations = new HashMap<>();
    }

    @Override
    protected boolean delaysPlaySpawnTeleport() {
        return true;
    }

    private Cuboid getGlassZone() {
        if (this.glassZone == null) {
            this.glassZone = this.getArena().getConfig().getCuboid("games.sixth.glass");
        }

        return this.glassZone;
    }

    public Cuboid getGoalZone() {
        if (this.goalZone == null) {
            this.goalZone = this.getArena().getConfig().getCuboid("games.sixth.goal");
        }

        return this.goalZone;
    }

    public boolean isFakeBlock(final Block block) {
        return this.fakeBlocks.contains(block);
    }

    public boolean isBridgeActive() {
        return this.bridgeActive && this.getArena().getState() == ArenaState.IN_GAME;
    }

    public boolean isBelowDeathLevel(final Location location) {
        if (location == null) {
            return false;
        }

        return location.getY() <= this.getDeathY();
    }

    private int getDeathY() {
        return this.getArena().getMainConfig().getInt("game-settings.glass-bridge-death-y", 28);
    }

    public List<Block> getSafeBlocks() {
        if (this.safeBlocks.isEmpty()) {
            this.setupBridgePath();
        }

        return new ArrayList<>(this.safeBlocks);
    }

    public void handleMove(final SquidPlayer player, final Location to) {
        if (this.isBelowDeathLevel(to)) {
            this.getArena().killPlayer(player);
            return;
        }

        if (this.hasFinishedEarly() || this.getGoalZone() == null) {
            return;
        }

        final Vector3 position = new Vector3(to.getX(), to.getY(), to.getZ());

        if (!this.getGoalZone().isBetween(position)) {
            return;
        }

        if (!this.finishedPlayers.add(player.getBukkitPlayer().getUniqueId())) {
            return;
        }

        if (this.haveAllPlayersFinished()) {
            this.finishEarly();
        }
    }

    private boolean haveAllPlayersFinished() {
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (!this.finishedPlayers.contains(player.getBukkitPlayer().getUniqueId())) {
                return false;
            }
        }

        return !this.getArena().getPlayers().isEmpty();
    }

    public boolean breakFakeBlock(final Block block) {
        if (!this.fakeBlocks.contains(block)) {
            return false;
        }

        final List<Block> queue = new ArrayList<>();
        final Set<Block> broken = new HashSet<>();

        queue.add(block);
        broken.add(block);

        while (!queue.isEmpty()) {
            final Block current = queue.remove(0);
            CompatibilityUtils.setType(current, Material.AIR);

            for (final Block neighbor : this.getPanelNeighbors(current)) {
                if (this.fakeBlocks.contains(neighbor) && broken.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return true;
    }

    public boolean isSolverItem(final ItemStack item) {
        if (item == null || item.getType() != Material.EMERALD || !item.hasItemMeta()
                || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        return item.getItemMeta().getDisplayName()
                .equals(MessageUtils.format(SquidGame.getInstance(), "items.glass-bridge-solver"));
    }

    public void handleSolverUse(final SquidPlayer player) {
        if (!player.getBukkitPlayer().hasPermission("squidgame.admin")) {
            return;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        final Long lastUse = this.solverConfirmations.get(uuid);

        if (lastUse == null || now - lastUse > SOLVER_CONFIRM_MS) {
            this.solverConfirmations.put(uuid, now);
            MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.sixth.solver.confirm");
            return;
        }

        this.solverConfirmations.remove(uuid);
        this.removeSolverItem(player.getBukkitPlayer());
        this.revealSafePath();
    }

    public void revealSafePath() {
        if (this.safeBlocks.isEmpty()) {
            this.setupBridgePath();
        }

        final Set<Block> remainingSafe = new HashSet<>(this.safeBlocks);
        int panelIndex = 0;

        while (!remainingSafe.isEmpty()) {
            final Block start = remainingSafe.iterator().next();
            final List<Block> panel = new ArrayList<>();
            final List<Block> queue = new ArrayList<>();

            queue.add(start);
            remainingSafe.remove(start);

            while (!queue.isEmpty()) {
                final Block block = queue.remove(0);
                panel.add(block);

                for (final Block neighbor : this.getPanelNeighbors(block)) {
                    if (remainingSafe.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }

            final int delay = panelIndex * 8;

            for (final Block block : panel) {
                Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                    if (this.getArena().getState() != ArenaState.IN_GAME || !this.bridgeBlocks.contains(block)) {
                        return;
                    }

                    CompatibilityUtils.setType(block, Material.GOLD_BLOCK);
                }, delay);

                Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                    if (this.getArena().getState() != ArenaState.IN_GAME) {
                        return;
                    }

                    if (block.getType() == Material.GOLD_BLOCK) {
                        CompatibilityUtils.setType(block, Material.EMERALD_BLOCK);
                    }
                }, delay + 12L);
            }

            panelIndex++;
        }
    }

    private void captureBridgeSlots() {
        if (!this.bridgeSlotLocations.isEmpty()) {
            return;
        }

        final Cuboid zone = this.getGlassZone();
        final World world = this.getArena().getWorld();

        if (zone == null || world == null) {
            return;
        }

        this.bridgeSlotLocations.addAll(SkySquidSetupHelper.scanGlassBridgeSlots(zone, world));
    }

    private void refreshBridgeBlocks() {
        this.bridgeBlocks.clear();
        final World world = this.getArena().getWorld();

        for (final Location location : this.bridgeSlotLocations) {
            location.setWorld(world);
            this.bridgeBlocks.add(world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        }
    }

    private void setupBridgePath() {
        this.fakeBlocks.clear();
        this.safeBlocks.clear();
        this.refreshBridgeBlocks();

        if (this.bridgeBlocks.isEmpty()) {
            return;
        }

        final List<List<Block>> panels = this.findGlassPanels(this.bridgeBlocks);
        final List<List<List<Block>>> steps = this.groupPanelsByBridgeStep(panels);
        final boolean crossUsesZ = !this.useZAsBridgeAxis();

        for (final List<List<Block>> step : this.normalizeSteps(steps)) {
            if (step.size() < 2) {
                for (final List<Block> panel : step) {
                    this.safeBlocks.addAll(panel);
                }

                continue;
            }

            Collections.sort(step, (firstPanel, secondPanel) -> Double.compare(
                    this.getPanelAverage(firstPanel, crossUsesZ),
                    this.getPanelAverage(secondPanel, crossUsesZ)));

            final int fakeIndex = BooleanUtils.randomBoolean() ? 0 : 1;

            for (int i = 0; i < step.size(); i++) {
                if (i == fakeIndex) {
                    this.fakeBlocks.addAll(step.get(i));
                } else {
                    this.safeBlocks.addAll(step.get(i));
                }
            }
        }
    }

    private List<List<List<Block>>> normalizeSteps(final List<List<List<Block>>> steps) {
        final List<List<List<Block>>> normalized = new ArrayList<>();
        final boolean crossUsesZ = !this.useZAsBridgeAxis();

        for (final List<List<Block>> step : steps) {
            if (step.size() <= 2) {
                normalized.add(step);
                continue;
            }

            step.sort((firstPanel, secondPanel) -> Double.compare(
                    this.getPanelAverage(firstPanel, crossUsesZ),
                    this.getPanelAverage(secondPanel, crossUsesZ)));

            final List<List<Block>> pair = new ArrayList<>();
            pair.add(step.get(0));
            pair.add(step.get(step.size() - 1));
            normalized.add(pair);
        }

        return normalized;
    }

    private List<List<Block>> findGlassPanels(final List<Block> blocks) {
        final List<List<Block>> panels = new ArrayList<>();
        final Set<Block> remaining = new HashSet<>(blocks);

        while (!remaining.isEmpty()) {
            final Block start = remaining.iterator().next();
            final List<Block> panel = new ArrayList<>();
            final List<Block> queue = new ArrayList<>();

            queue.add(start);
            remaining.remove(start);

            while (!queue.isEmpty()) {
                final Block block = queue.remove(0);
                panel.add(block);

                for (final Block neighbor : this.getPanelNeighbors(block)) {
                    if (remaining.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }

            panels.add(panel);
        }

        Collections.sort(panels, this.getPanelComparator());
        return panels;
    }

    private List<List<List<Block>>> groupPanelsByBridgeStep(final List<List<Block>> panels) {
        final List<List<List<Block>>> steps = new ArrayList<>();
        final boolean useZAsBridgeAxis = this.useZAsBridgeAxis();
        List<List<Block>> currentStep = null;
        int currentStepKey = Integer.MIN_VALUE;

        for (final List<Block> panel : panels) {
            final int panelStepKey = this.getPanelStepKey(panel, useZAsBridgeAxis);

            if (currentStep == null || panelStepKey != currentStepKey) {
                currentStep = new ArrayList<>();
                steps.add(currentStep);
                currentStepKey = panelStepKey;
            }

            currentStep.add(panel);
        }

        return steps;
    }

    private int getPanelStepKey(final List<Block> panel, final boolean useZAsBridgeAxis) {
        return (int) Math.round(this.getPanelAverage(panel, useZAsBridgeAxis));
    }

    private List<Block> getPanelNeighbors(final Block block) {
        final List<Block> neighbors = new ArrayList<>();

        neighbors.add(block.getRelative(1, 0, 0));
        neighbors.add(block.getRelative(-1, 0, 0));
        neighbors.add(block.getRelative(0, 0, 1));
        neighbors.add(block.getRelative(0, 0, -1));

        return neighbors;
    }

    private Comparator<List<Block>> getPanelComparator() {
        final boolean useZAsBridgeAxis = this.useZAsBridgeAxis();

        return (firstPanel, secondPanel) -> {
            final double firstBridge = this.getPanelAverage(firstPanel, useZAsBridgeAxis);
            final double secondBridge = this.getPanelAverage(secondPanel, useZAsBridgeAxis);

            if (firstBridge == secondBridge) {
                return Double.compare(this.getPanelAverage(firstPanel, !useZAsBridgeAxis),
                        this.getPanelAverage(secondPanel, !useZAsBridgeAxis));
            }

            return Double.compare(firstBridge, secondBridge);
        };
    }

    private boolean useZAsBridgeAxis() {
        final Cuboid zone = this.getGlassZone();

        if (zone == null) {
            return true;
        }

        final Vector3 first = zone.getFirstPoint();
        final Vector3 second = zone.getSecondPoint();

        return Math.abs(first.getZ() - second.getZ()) >= Math.abs(first.getX() - second.getX());
    }

    private double getPanelAverage(final List<Block> panel, final boolean useZ) {
        double total = 0.0D;

        for (final Block block : panel) {
            total += useZ ? block.getZ() : block.getX();
        }

        return total / panel.size();
    }

    private void setBridgeMaterial(final Material material) {
        final World world = this.getArena().getWorld();

        for (final Location location : this.bridgeSlotLocations) {
            CompatibilityUtils.setType(world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                    material);
        }
    }

    @Override
    public void onExplainStart() {
        super.onExplainStart();
        this.bridgeActive = false;
        this.finishedPlayers.clear();
        this.solverConfirmations.clear();
        this.captureBridgeSlots();
        this.setupBridgePath();
        this.setBridgeMaterial(Material.AIR);
    }

    @Override
    public void onStart() {
        this.bridgeActive = false;
        this.finishedPlayers.clear();
        this.captureBridgeSlots();

        if (this.bridgeSlotLocations.isEmpty()) {
            this.getArena().broadcastTitle("games.sixth.not-configured.title",
                    "games.sixth.not-configured.subtitle");
            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> this.getArena().setInternalTime(1), 20L);
            return;
        }

        this.setupBridgePath();
        this.setBridgeMaterial(Material.GLASS);

        final Location start = this.getBridgeStartLocation();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            player.teleport(start);
        }

        this.runStartCountdown(3);
    }

    private Location getBridgeStartLocation() {
        final Location spawn = this.getPlaySpawnPosition().clone();
        spawn.setWorld(this.getArena().getWorld());
        return spawn;
    }

    private void runStartCountdown(final int secondsLeft) {
        if (this.getArena().getState() != ArenaState.IN_GAME) {
            return;
        }

        if (secondsLeft <= 0) {
            this.bridgeActive = true;

            for (final SquidPlayer player : this.getArena().getPlayers()) {
                this.giveSolverItem(player.getBukkitPlayer());
            }

            this.getArena().broadcastTitle("events.game-start.title", "events.game-start.subtitle");
            return;
        }

        MessageUtils.broadcastTitle(SquidGame.getInstance(), this.getArena(), "events.start-countdown.title",
                "events.start-countdown.subtitle", "{time}", String.valueOf(secondsLeft));
        this.getArena().broadcastSound(
                this.getArena().getMainConfig().getSound("game-settings.sounds.arena-countdown", "NOTE_PLING"));

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> this.runStartCountdown(secondsLeft - 1),
                20L);
    }

    @Override
    public void onTimeUp() {
        this.bridgeActive = false;
        this.removeSolverItems();
        this.setBridgeMaterial(Material.AIR);

        if (!this.hasFinishedEarly()) {
            this.getArena().broadcastTitle("events.game-timeout.title", "events.game-timeout.subtitle");
        }

        final List<SquidPlayer> alive = new ArrayList<>();
        final List<SquidPlayer> death = new ArrayList<>();

        final Cuboid goal = this.getGoalZone();

        if (goal == null) {
            if (!this.hasFinishedEarly()) {
                this.getArena().broadcastTitle("games.sixth.not-configured.title",
                        "games.sixth.not-configured.subtitle");
            }

            return;
        }

        for (final SquidPlayer squidPlayer : this.getArena().getPlayers()) {
            final Player player = squidPlayer.getBukkitPlayer();
            final Location location = player.getLocation();
            final Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());

            if (goal.isBetween(position)) {
                alive.add(squidPlayer);
            } else {
                death.add(squidPlayer);
            }
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                player.sendTitle("events.game-timeout-died.title", "events.game-timeout-died.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
            }

            for (final SquidPlayer player : alive) {
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            if (!this.getArena().canEliminatePlayers()) {
                return;
            }

            for (final SquidPlayer squidPlayer : death) {
                this.getArena().killPlayer(squidPlayer);
            }
        }, 80L);
    }

    @Override
    public void onStop() {
        this.bridgeActive = false;
        this.removeSolverItems();
        this.setBridgeMaterial(Material.GLASS);
        this.fakeBlocks.clear();
        this.safeBlocks.clear();
        this.bridgeBlocks.clear();
        this.finishedPlayers.clear();
        this.solverConfirmations.clear();
        this.glassZone = null;
        this.goalZone = null;
    }

    private void giveSolverItem(final Player player) {
        if (!player.hasPermission("squidgame.admin")) {
            return;
        }

        final ItemStack item = new ItemStack(Material.EMERALD);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.glass-bridge-solver"));
        item.setItemMeta(meta);
        player.getInventory().setItem(SOLVER_SLOT, item);
        player.updateInventory();
    }

    private void removeSolverItems() {
        this.solverConfirmations.clear();

        for (final SquidPlayer player : this.getArena().getAllPlayers()) {
            this.removeSolverItem(player.getBukkitPlayer());
        }
    }

    private void removeSolverItem(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (this.isSolverItem(item)) {
                player.getInventory().remove(item);
            }
        }

        player.updateInventory();
    }
}
