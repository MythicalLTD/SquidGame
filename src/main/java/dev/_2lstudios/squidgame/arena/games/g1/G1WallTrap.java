package dev._2lstudios.squidgame.arena.games.g1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;

public final class G1WallTrap {

    private final Block lever;
    private final Block tripwireHook;
    private final Location muzzle;

    public G1WallTrap(final Block lever, final Block tripwireHook, final Location muzzle) {
        this.lever = lever;
        this.tripwireHook = tripwireHook;
        this.muzzle = muzzle;
    }

    public boolean hasLever() {
        return this.lever != null;
    }

    public Block getLever() {
        return this.lever;
    }

    public Block getTripwireHook() {
        return this.tripwireHook;
    }

    public Location getMuzzle() {
        return this.muzzle;
    }

    public double distanceSquared(final Location playerLocation) {
        return this.muzzle.distanceSquared(playerLocation);
    }

    public static G1WallTrap findNearest(final List<G1WallTrap> traps, final Location playerLocation) {
        if (traps == null || traps.isEmpty()) {
            return null;
        }

        return traps.stream()
                .min(Comparator.comparingDouble(trap -> trap.distanceSquared(playerLocation)))
                .orElse(null);
    }

    public static List<G1WallTrap> dedupe(final List<G1WallTrap> traps) {
        final List<G1WallTrap> unique = new ArrayList<>();

        for (final G1WallTrap trap : traps) {
            boolean duplicate = false;

            for (final G1WallTrap existing : unique) {
                if (existing.hasLever() && trap.hasLever()
                        && existing.getLever().getLocation().equals(trap.getLever().getLocation())) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                unique.add(trap);
            }
        }

        return unique;
    }
}
