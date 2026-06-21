package dev._2lstudios.squidgame.utils;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G1RedGreenLightGame;

public final class WorldTimeUtils {

    public static final long DEFAULT_LOCKED_TIME = 18000L;
    public static final long GREEN_LIGHT_TIME = 6000L;
    public static final long RED_LIGHT_TIME = 18000L;

    private WorldTimeUtils() {
    }

    public static boolean isLockEnabled() {
        return SquidGame.getInstance().getMainConfig().getBoolean("game-settings.lock-world-time", true);
    }

    public static long getLockedTime() {
        return SquidGame.getInstance().getMainConfig().getInt("game-settings.locked-world-time",
                (int) DEFAULT_LOCKED_TIME);
    }

    public static Set<World> getManagedWorlds() {
        final Set<World> worlds = new HashSet<>();
        final SquidGame plugin = SquidGame.getInstance();
        final String lobbyWorld = plugin.getMainConfig().getString("lobby.world", "");

        if (lobbyWorld != null && !lobbyWorld.isEmpty()) {
            final World lobby = Bukkit.getWorld(lobbyWorld);

            if (lobby != null) {
                worlds.add(lobby);
            }
        }

        for (final Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getWorld() != null) {
                worlds.add(arena.getWorld());
            }
        }

        return worlds;
    }

    public static boolean isG1ActiveOnWorld(final World world) {
        if (world == null) {
            return false;
        }

        for (final Arena arena : SquidGame.getInstance().getArenaManager().getArenas()) {
            if (!world.equals(arena.getWorld())) {
                continue;
            }

            if (arena.getCurrentGame() instanceof G1RedGreenLightGame
                    && (arena.getState() == ArenaState.EXPLAIN_GAME
                            || arena.getState() == ArenaState.IN_GAME)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    public static void lockWorldTime(final World world) {
        if (world == null) {
            return;
        }

        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(getLockedTime());
    }

    @SuppressWarnings("deprecation")
    public static void setWorldTime(final World world, final long time) {
        if (world == null) {
            return;
        }

        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(time);
    }

    public static void applyLockToManagedWorlds() {
        if (!isLockEnabled()) {
            return;
        }

        for (final World world : getManagedWorlds()) {
            if (!isG1ActiveOnWorld(world)) {
                lockWorldTime(world);
            }
        }
    }
}
