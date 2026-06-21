package dev._2lstudios.squidgame;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import dev._2lstudios.jelly.JellyPlugin;
import dev._2lstudios.jelly.config.Configuration;

import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaManager;
import dev._2lstudios.squidgame.cosmetics.CosmeticManager;
import dev._2lstudios.squidgame.economy.ShopManager;
import dev._2lstudios.squidgame.music.NbsMusicManager;
import dev._2lstudios.squidgame.commands.SquidGameCommand;
import dev._2lstudios.squidgame.commands.admin.BuildCommand;
import dev._2lstudios.squidgame.commands.admin.FlyCommand;
import dev._2lstudios.squidgame.commands.admin.GamemodeCreativeCommand;
import dev._2lstudios.squidgame.commands.admin.GamemodeSurvivalCommand;
import dev._2lstudios.squidgame.hooks.PlaceholderAPIHook;
import dev._2lstudios.squidgame.hooks.ScoreboardHook;
import dev._2lstudios.squidgame.listeners.AsyncPlayerChatListener;
import dev._2lstudios.squidgame.listeners.BlockBreakListener;
import dev._2lstudios.squidgame.listeners.BlockPlaceListener;
import dev._2lstudios.squidgame.listeners.EntityDamageListener;
import dev._2lstudios.squidgame.listeners.FireProtectionListener;
import dev._2lstudios.squidgame.listeners.FoodLevelChangeListener;
import dev._2lstudios.squidgame.listeners.PlayerDeathListener;
import dev._2lstudios.squidgame.listeners.PlayerDropItemListener;
import dev._2lstudios.squidgame.listeners.PlayerInteractEntityListener;
import dev._2lstudios.squidgame.listeners.PlayerInteractListener;
import dev._2lstudios.squidgame.listeners.PlayerJoinListener;
import dev._2lstudios.squidgame.listeners.PlayerMoveListener;
import dev._2lstudios.squidgame.listeners.PlayerPickupItemListener;
import dev._2lstudios.squidgame.listeners.PlayerQuitListener;
import dev._2lstudios.squidgame.listeners.PlayerToggleSneakListener;
import dev._2lstudios.squidgame.listeners.WeatherLockListener;
import dev._2lstudios.squidgame.player.PlayerDataManager;
import dev._2lstudios.squidgame.player.PlayerManager;
import dev._2lstudios.squidgame.tasks.ArenaTickTask;
import dev._2lstudios.squidgame.tasks.WorldTimeLockTask;
import dev._2lstudios.squidgame.utils.WorldTimeUtils;

public class SquidGame extends JellyPlugin {

    private ScoreboardHook scoreboardHook;
    private ArenaManager arenaManger;
    private PlayerManager playerManager;
    private CosmeticManager cosmeticManager;
    private NbsMusicManager nbsMusicManager;
    private PlayerDataManager playerDataManager;
    private ShopManager shopManager;

    private boolean usePAPI;

    @Override
    public void onEnable() {
        final PluginManager pluginManager = getServer().getPluginManager();

        // Save current plugin instance as static instance
        SquidGame.instance = this;

        // Instantiate hooks
        this.scoreboardHook = new ScoreboardHook(pluginManager);

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            this.usePAPI = true;
        }

        // Instantiate managers (music/cosmetics before arenas — Arena.resetArena uses them)
        this.playerManager = new PlayerManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.playerDataManager.initialize();
        this.shopManager = new ShopManager(this, this.playerDataManager);
        this.cosmeticManager = new CosmeticManager(this);
        this.nbsMusicManager = new NbsMusicManager(this);
        this.arenaManger = new ArenaManager(this);

        // Register commands
        this.addCommand(new SquidGameCommand());
        this.addCommand(new GamemodeCreativeCommand());
        this.addCommand(new GamemodeSurvivalCommand());
        this.addCommand(new FlyCommand());
        this.addCommand(new BuildCommand());

        // Register listeners
        this.addEventListener(new AsyncPlayerChatListener(this));
        this.addEventListener(new BlockBreakListener(this));
        this.addEventListener(new BlockPlaceListener(this));
        this.addEventListener(new EntityDamageListener(this));
        this.addEventListener(new FireProtectionListener(this));
        this.addEventListener(new FoodLevelChangeListener(this));
        this.addEventListener(new PlayerDeathListener(this));
        this.addEventListener(new PlayerInteractEntityListener(this));
        this.addEventListener(new PlayerInteractListener(this));
        this.addEventListener(new PlayerJoinListener(this));
        this.addEventListener(new PlayerMoveListener(this));
        this.addEventListener(new PlayerDropItemListener(this));
        this.addEventListener(new PlayerPickupItemListener(this));
        this.addEventListener(new PlayerQuitListener(this));
        this.addEventListener(new PlayerToggleSneakListener(this));
        this.addEventListener(new WeatherLockListener(this));

        // Register player manager
        this.setPluginPlayerManager(this.playerManager);

        // Register tasks
        Bukkit.getScheduler().runTaskTimer(this, new ArenaTickTask(this), 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, new WorldTimeLockTask(), 20L, 20L);

        // Enable inventory API
        this.useInventoryAPI();

        // Generate config files
        this.getMainConfig();
        this.getMessagesConfig();
        this.getScoreboardConfig();
        this.applyWeatherLock();
        this.applyFireProtection();
        this.applyChatNoiseProtection();
        WorldTimeUtils.applyLockToManagedWorlds();

        // Banner
        this.getLogger().log(Level.INFO, "§7§m==========================================================");
        this.getLogger().log(Level.INFO,
                "                §d§lSquid§f§lGame§r §a(v" + this.getDescription().getVersion() + ")");
        this.getLogger().log(Level.INFO, "§r");
        this.getLogger().log(Level.INFO, "§7- §dArena loaded: §7" + this.arenaManger.getArenas().size());
        this.getLogger().log(Level.INFO, "§7- §dPlaceholderAPI Hook: "
                + (this.usePAPI ? "§aYes" : "§cNo §7(Placeholders option will be disabled)"));
        this.getLogger().log(Level.INFO, "§7- §dScoreboard Hook: "
                + (this.scoreboardHook.canHook() ? "§aYes" : "§cNo §7(The scoreboards option will be disabled)"));
        if (this.playerDataManager.isEnabled()) {
            this.getLogger().log(Level.INFO, "§7- §dPlayer database: "
                    + (this.playerDataManager.isDatabaseReady()
                            ? "§a" + this.playerDataManager.getDatabaseType().name().toLowerCase()
                            : "§cFailed"));
        }
        this.getLogger().log(Level.INFO, "§r");
        this.getLogger().log(Level.INFO, "§7§m==========================================================");

    }

    @Override
    public void onDisable() {
        if (this.playerDataManager != null) {
            this.playerDataManager.shutdown();
        }

        if (this.cosmeticManager != null) {
            this.cosmeticManager.shutdown();
        }

        if (this.nbsMusicManager != null) {
            this.nbsMusicManager.shutdown();
        }

        if (this.arenaManger != null) {
            for (final Arena arena : this.arenaManger.getArenas()) {
                arena.resetArena();
            }
        }
    }

    /* Configuration */
    public Configuration getMainConfig() {
        return this.getConfig("config.yml");
    }

    public Configuration getMessagesConfig() {
        return this.getConfig("messages.yml");
    }

    public Configuration getScoreboardConfig() {
        return this.getConfig("scoreboard.yml");
    }

    /* Managers */
    public ArenaManager getArenaManager() {
        return this.arenaManger;
    }

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public ScoreboardHook getScoreboardHook() {
        return scoreboardHook;
    }

    public CosmeticManager getCosmeticManager() {
        return this.cosmeticManager;
    }

    public NbsMusicManager getNbsMusicManager() {
        return this.nbsMusicManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    public ShopManager getShopManager() {
        return this.shopManager;
    }

    private void applyWeatherLock() {
        if (!this.getMainConfig().getBoolean("game-settings.proxy-mode.enabled", false)
                || !this.getMainConfig().getBoolean("game-settings.proxy-mode.lock-weather", true)) {
            return;
        }

        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
            world.setThunderDuration(0);
        }
    }

    @SuppressWarnings("deprecation")
    private void applyFireProtection() {
        if (!this.getMainConfig().getBoolean("game-settings.disable-fire-spread", true)) {
            return;
        }

        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            world.setGameRuleValue("doFireTick", "false");
        }
    }

    @SuppressWarnings("deprecation")
    private void applyChatNoiseProtection() {
        if (!this.getMainConfig().getBoolean("game-settings.disable-advancement-announcements", true)) {
            return;
        }

        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            world.setGameRuleValue("announceAdvancements", "false");
        }
    }

    /* Static instance */
    private static SquidGame instance;

    public static SquidGame getInstance() {
        return instance;
    }
}