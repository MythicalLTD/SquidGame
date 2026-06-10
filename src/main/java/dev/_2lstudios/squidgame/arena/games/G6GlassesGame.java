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
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class G6GlassesGame extends ArenaGameBase {

    private static final int SOLVER_SLOT = 8;
    private static final long SOLVER_CONFIRM_MS = 8000L;

    private Cuboid glassZone;
    private Cuboid goalZone;

    private List<Block> fakeBlocks;
    private List<Block> safeBlocks;
    private List<Block> bridgeBlocks;
    private final Map<UUID, Long> solverConfirmations;

    public G6GlassesGame(final Arena arena, final int durationTime) {
        super("§bGlasses", "sixth", durationTime, arena);

        this.fakeBlocks = new ArrayList<>();
        this.safeBlocks = new ArrayList<>();
        this.bridgeBlocks = new ArrayList<>();
        this.solverConfirmations = new HashMap<>();
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

    public List<Block> getSafeBlocks() {
        if (this.safeBlocks.isEmpty()) {
            this.setupBridgePath();
        }

        return new ArrayList<>(this.safeBlocks);
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
            current.setType(Material.AIR);

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
        final List<Block> safeBlocks = this.getSafeBlocks();

        for (int i = 0; i < safeBlocks.size(); i++) {
            final Block block = safeBlocks.get(i);
            final int delay = i * 4;

            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                block.setType(Material.GOLD_BLOCK);
            }, delay);

            Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
                if (block.getType() == Material.GOLD_BLOCK) {
                    block.setType(Material.EMERALD_BLOCK);
                }
            }, delay + 12L);
        }
    }

    private void setupBridgePath() {
        this.fakeBlocks.clear();
        this.safeBlocks.clear();
        this.bridgeBlocks = this.findBridgeBlocks();

        final List<List<Block>> panels = this.findGlassPanels(this.bridgeBlocks);
        final List<List<List<Block>>> steps = this.groupPanelsByBridgeStep(panels);

        for (final List<List<Block>> step : steps) {
            if (step.size() < 2) {
                for (final List<Block> panel : step) {
                    this.safeBlocks.addAll(panel);
                }

                continue;
            }

            Collections.sort(step, (firstPanel, secondPanel) -> Double.compare(
                    this.getPanelAverage(firstPanel, !this.useZAsBridgeAxis()),
                    this.getPanelAverage(secondPanel, !this.useZAsBridgeAxis())));

            final int fakeIndex = step.size() == 2 && BooleanUtils.randomBoolean() ? 0 : step.size() - 1;

            for (int i = 0; i < step.size(); i++) {
                if (i == fakeIndex) {
                    this.fakeBlocks.addAll(step.get(i));
                } else {
                    this.safeBlocks.addAll(step.get(i));
                }
            }
        }
    }

    private List<Block> findBridgeBlocks() {
        final List<Block> blocks = new ArrayList<>();
        final World world = this.getArena().getWorld();
        final Vector3 first = this.getGlassZone().getFirstPoint();
        final Vector3 second = this.getGlassZone().getSecondPoint();
        final int minX = (int) Math.min(first.getX(), second.getX());
        final int maxX = (int) Math.max(first.getX(), second.getX());
        final int minY = (int) Math.min(first.getY(), second.getY());
        final int maxY = (int) Math.max(first.getY(), second.getY());
        final int minZ = (int) Math.min(first.getZ(), second.getZ());
        final int maxZ = (int) Math.max(first.getZ(), second.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = world.getBlockAt(x, y, z);

                    if (this.isGlass(block)) {
                        blocks.add(block);
                    }
                }
            }
        }

        return blocks;
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
        double currentStepPosition = 0.0D;

        for (final List<Block> panel : panels) {
            final double panelPosition = this.getPanelAverage(panel, useZAsBridgeAxis);

            if (currentStep == null || Math.abs(panelPosition - currentStepPosition) > 1.5D) {
                currentStep = new ArrayList<>();
                steps.add(currentStep);
                currentStepPosition = panelPosition;
            }

            currentStep.add(panel);
        }

        return steps;
    }

    private List<Block> getPanelNeighbors(final Block block) {
        final List<Block> neighbors = new ArrayList<>();

        neighbors.add(block.getRelative(1, 0, 0));
        neighbors.add(block.getRelative(-1, 0, 0));
        neighbors.add(block.getRelative(0, 0, 1));
        neighbors.add(block.getRelative(0, 0, -1));
        neighbors.add(block.getRelative(0, 1, 0));
        neighbors.add(block.getRelative(0, -1, 0));

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
        final Vector3 first = this.getGlassZone().getFirstPoint();
        final Vector3 second = this.getGlassZone().getSecondPoint();

        return Math.abs(first.getZ() - second.getZ()) >= Math.abs(first.getX() - second.getX());
    }

    private double getPanelAverage(final List<Block> panel, final boolean useZ) {
        double total = 0.0D;

        for (final Block block : panel) {
            total += useZ ? block.getZ() : block.getX();
        }

        return total / panel.size();
    }

    private boolean isGlass(final Block block) {
        return block.getType() == Material.GLASS || block.getType().name().contains("STAINED_GLASS");
    }

    private void setBridgeBlocks(final Material material) {
        for (final Block block : this.bridgeBlocks) {
            block.setType(material);
        }
    }

    @Override
    public void onExplainStart() {
        super.onExplainStart();
        this.setupBridgePath();
        this.setBridgeBlocks(Material.AIR);
    }

    @Override
    public void onStart() {
        if (this.bridgeBlocks.isEmpty()) {
            this.setupBridgePath();
        }

        this.setBridgeBlocks(Material.GLASS);

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.giveSolverItem(player.getBukkitPlayer());
        }
    }

    @Override
    public void onTimeUp() {
        this.removeSolverItems();
        this.setBridgeBlocks(Material.AIR);

        this.getArena().broadcastTitle("events.game-timeout.title", "events.game-timeout.subtitle");

        final List<SquidPlayer> alive = new ArrayList<>();
        final List<SquidPlayer> death = new ArrayList<>();

        for (final SquidPlayer squidPlayer : this.getArena().getPlayers()) {
            final Player player = squidPlayer.getBukkitPlayer();
            final Location location = player.getLocation();
            final Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());

            if (this.getGoalZone().isBetween(position)) {
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
            for (final SquidPlayer squidPlayer : death) {
                this.getArena().killPlayer(squidPlayer);
            }
        }, 80L);
    }

    @Override
    public void onStop() {
        this.removeSolverItems();
        this.setBridgeBlocks(Material.GLASS);
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