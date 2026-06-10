package dev._2lstudios.jelly.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import dev._2lstudios.jelly.JellyPlugin;

public class ConfigManager {

    private final Map<String, Configuration> configs;
    private final JellyPlugin plugin;

    public ConfigManager(final JellyPlugin plugin) {
        this.configs = new HashMap<>();
        this.plugin = plugin;
    }

    public Configuration getConfig(final String name) {
        if (this.configs.containsKey(name)) {
            return configs.get(name);
        }

        final File configFile = new File(this.plugin.getDataFolder(), name);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            this.plugin.saveResource(name, false);
        }

        final Configuration config = new Configuration(configFile);
        try {
            config.load();
            if (this.mergeDefaults(name, config)) {
                config.save();
            }
            this.configs.put(name, config);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        return config;
    }

    public void reloadConfigs() {
        this.configs.clear();
    }

    private boolean mergeDefaults(final String name, final Configuration config) {
        try (final InputStream inputStream = this.plugin.getResource(name)) {
            if (inputStream == null) {
                return false;
            }

            final YamlConfiguration defaults = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return this.mergeSection(config, defaults, "");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean mergeSection(final Configuration config, final ConfigurationSection defaults, final String path) {
        boolean changed = false;

        for (final String key : defaults.getKeys(false)) {
            final String fullPath = path.isEmpty() ? key : path + "." + key;

            if (defaults.isConfigurationSection(key)) {
                if (!config.isConfigurationSection(fullPath)) {
                    config.createSection(fullPath);
                    changed = true;
                }

                changed = this.mergeSection(config, defaults.getConfigurationSection(key), fullPath) || changed;
            } else if (!config.contains(fullPath)) {
                config.set(fullPath, defaults.get(key));
                changed = true;
            }
        }

        return changed;
    }
}
