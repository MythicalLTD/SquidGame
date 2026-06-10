package dev._2lstudios.squidgame.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import dev._2lstudios.squidgame.SquidGame;

public class WeatherLockListener implements Listener {
    private final SquidGame plugin;

    public WeatherLockListener(final SquidGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWeatherChange(final WeatherChangeEvent event) {
        if (this.isWeatherLocked() && event.toWeatherState()) {
            event.setCancelled(true);
            this.clearWeather(event.getWorld());
        }
    }

    @EventHandler
    public void onThunderChange(final ThunderChangeEvent event) {
        if (this.isWeatherLocked() && event.toThunderState()) {
            event.setCancelled(true);
            this.clearWeather(event.getWorld());
        }
    }

    private boolean isWeatherLocked() {
        return this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.enabled", false)
                && this.plugin.getMainConfig().getBoolean("game-settings.proxy-mode.lock-weather", true);
    }

    private void clearWeather(final World world) {
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setThunderDuration(0);
    }
}
