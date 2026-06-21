package dev._2lstudios.squidgame.tasks;

import org.bukkit.World;

import dev._2lstudios.squidgame.utils.WorldTimeUtils;

public class WorldTimeLockTask implements Runnable {

    @Override
    public void run() {
        if (!WorldTimeUtils.isLockEnabled()) {
            return;
        }

        for (final World world : WorldTimeUtils.getManagedWorlds()) {
            if (!WorldTimeUtils.isG1ActiveOnWorld(world)) {
                WorldTimeUtils.lockWorldTime(world);
            }
        }
    }
}
