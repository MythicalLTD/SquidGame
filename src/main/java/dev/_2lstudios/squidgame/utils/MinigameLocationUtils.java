package dev._2lstudios.squidgame.utils;

import org.bukkit.Location;
import org.bukkit.World;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.arena.Arena;

public final class MinigameLocationUtils {

    public static final String[] GAME_ALIASES = new String[] { "lobby", "waiting", "intermission", "1", "redlight",
            "redgreenlight", "red-light-green-light", "3", "battle", "lightsoff", "lights-off", "5", "tug", "tugofwar",
            "tug-of-war", "6", "glass", "glasses", "glassbridge", "glass-bridge", "8", "mingle", "10", "hideandseek",
            "hide-and-seek", "12", "finaldinner", "final-dinner", "13", "skysquid",
            "sky-squid" };

    private MinigameLocationUtils() {
    }

    public static String resolveGameKey(final String game) {
        if (game == null) {
            return null;
        }

        final String normalized = game.toLowerCase().replace("_", "-").replace(" ", "-");

        switch (normalized) {
        case "lobby":
        case "waiting":
        case "intermission":
            return "lobby";
        case "1":
        case "redlight":
        case "redgreenlight":
        case "red-light-green-light":
            return "first";
        case "3":
        case "battle":
        case "lightsoff":
        case "lights-off":
            return "third";
        case "5":
        case "tug":
        case "tugofwar":
        case "tug-of-war":
            return "fifth";
        case "6":
        case "glass":
        case "glasses":
        case "glassbridge":
        case "glass-bridge":
            return "sixth";
        case "8":
        case "mingle":
            return "mingle";
        case "10":
        case "hideandseek":
        case "hide-and-seek":
            return "hide-and-seek";
        case "12":
        case "finaldinner":
        case "final-dinner":
            return "final-dinner";
        case "13":
        case "skysquid":
        case "sky-squid":
            return "sky-squid";
        default:
            return null;
        }
    }

    public static Location resolveLocation(final Arena arena, final String gameKey) {
        final Configuration config = arena.getConfig();
        final World world = arena.getWorld();

        switch (gameKey) {
        case "lobby":
            return firstLocation(config, world, "arena.waiting_room", "arena.prelobby");
        case "first":
            return firstOf(config, world,
                    locationAt(config, world, "games.first.lobby"),
                    locationAt(config, world, "arena.waiting_room"),
                    locationAt(config, world, "arena.prelobby"),
                    centerOfCuboid(config, world, "games.first.killzone"),
                    locationAt(config, world, "games.first.spawn"));
        case "third":
            return firstOf(config, world,
                    locationAt(config, world, "games.third.spawn"),
                    locationAt(config, world, "arena.waiting_room"),
                    locationAt(config, world, "arena.prelobby"));
        case "fifth":
            return firstOf(config, world,
                    midpoint(config, world, "games.fifth.team1", "games.fifth.team2"),
                    locationAt(config, world, "games.fifth.team1"),
                    locationAt(config, world, "games.fifth.team2"),
                    locationAt(config, world, "games.fifth.spawn"));
        case "sixth":
            return firstOf(config, world,
                    centerOfCuboid(config, world, "games.sixth.glass"),
                    centerOfCuboid(config, world, "games.sixth.goal"),
                    locationAt(config, world, "games.sixth.spawn"));
        case "mingle":
            return firstOf(config, world,
                    centerOfCuboid(config, world, "games.mingle.platform"),
                    locationAt(config, world, "games.mingle.spawn"),
                    locationAt(config, world, "games.mingle.lobby"),
                    locationAt(config, world, "arena.waiting_room"));
        case "hide-and-seek":
            return firstOf(config, world,
                    midpoint(config, world, "games.hide-and-seek.hider-spawn", "games.hide-and-seek.seeker-spawn"),
                    centerOfCuboid(config, world, "games.hide-and-seek.exit"),
                    locationAt(config, world, "games.hide-and-seek.hider-spawn"),
                    locationAt(config, world, "games.hide-and-seek.seeker-spawn"));
        case "final-dinner":
            return firstOf(config, world, locationAt(config, world, "games.final-dinner.spawn"));
        case "sky-squid":
            return firstOf(config, world,
                    averageLocations(config, world,
                            "games.sky-squid.square-spawn",
                            "games.sky-squid.triangle-spawn",
                            "games.sky-squid.circle-spawn"),
                    centerOfCuboid(config, world, "games.sky-squid.triangle-safe-zone"),
                    centerOfCuboid(config, world, "games.sky-squid.circle-safe-zone"),
                    locationAt(config, world, "games.sky-squid.square-spawn"),
                    locationAt(config, world, "games.sky-squid.triangle-spawn"),
                    locationAt(config, world, "games.sky-squid.circle-spawn"));
        default:
            return null;
        }
    }

    @SafeVarargs
    private static Location firstOf(final Configuration config, final World world, final Location... locations) {
        for (final Location location : locations) {
            if (location != null) {
                return location;
            }
        }

        return null;
    }

    private static Location firstLocation(final Configuration config, final World world, final String... paths) {
        return firstOf(config, world, pathsToLocations(config, world, paths));
    }

    private static Location[] pathsToLocations(final Configuration config, final World world, final String[] paths) {
        final Location[] locations = new Location[paths.length];

        for (int i = 0; i < paths.length; i++) {
            locations[i] = locationAt(config, world, paths[i]);
        }

        return locations;
    }

    private static Location locationAt(final Configuration config, final World world, final String path) {
        if (!config.contains(path + ".x")) {
            return null;
        }

        final Location location = config.getLocation(path, false);
        location.setWorld(world);
        return location;
    }

    private static Location centerOfCuboid(final Configuration config, final World world, final String path) {
        final Cuboid cuboid = config.getCuboid(path);

        if (cuboid == null) {
            return null;
        }

        final Vector3 first = cuboid.getFirstPoint();
        final Vector3 second = cuboid.getSecondPoint();
        final Location location = new Location(world,
                (first.getX() + second.getX()) / 2.0D,
                (first.getY() + second.getY()) / 2.0D + 1.0D,
                (first.getZ() + second.getZ()) / 2.0D);
        location.setPitch(0.0F);
        location.setYaw(0.0F);
        return location;
    }

    private static Location midpoint(final Configuration config, final World world, final String firstPath,
            final String secondPath) {
        final Location first = locationAt(config, world, firstPath);
        final Location second = locationAt(config, world, secondPath);

        if (first == null || second == null) {
            return null;
        }

        final Location location = new Location(world,
                (first.getX() + second.getX()) / 2.0D,
                (first.getY() + second.getY()) / 2.0D,
                (first.getZ() + second.getZ()) / 2.0D,
                (first.getYaw() + second.getYaw()) / 2.0F,
                (first.getPitch() + second.getPitch()) / 2.0F);
        return location;
    }

    private static Location averageLocations(final Configuration config, final World world, final String... paths) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        float yaw = 0.0F;
        float pitch = 0.0F;
        int count = 0;

        for (final String path : paths) {
            final Location location = locationAt(config, world, path);

            if (location == null) {
                continue;
            }

            x += location.getX();
            y += location.getY();
            z += location.getZ();
            yaw += location.getYaw();
            pitch += location.getPitch();
            count++;
        }

        if (count == 0) {
            return null;
        }

        return new Location(world, x / count, y / count, z / count, yaw / count, pitch / count);
    }
}
