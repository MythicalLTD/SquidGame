package dev._2lstudios.squidgame.arena;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev._2lstudios.jelly.config.Configuration;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.music.NbsMusicManager;
import dev._2lstudios.squidgame.arena.games.ArenaGameBase;
import dev._2lstudios.squidgame.arena.games.G1RedGreenLightGame;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.arena.games.G12FinalDinnerGame;
import dev._2lstudios.squidgame.arena.games.G12SkySquidGame;
import dev._2lstudios.squidgame.arena.games.G3BattleGame;
import dev._2lstudios.squidgame.arena.games.G5TugOfWarGame;
import dev._2lstudios.squidgame.arena.games.G6GlassesGame;
import dev._2lstudios.squidgame.arena.games.G8MingleGame;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.LobbyItems;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class Arena {
    private final List<SquidPlayer> players;
    private final List<SquidPlayer> spectators;
    private final List<ArenaGameBase> games;

    private final Configuration mainConfig;
    private final Configuration arenaConfig;
    private final ArenaHandler handler;
    private final World world;
    private final String name;

    private String joined, leaved, death;

    private ArenaState state = ArenaState.WAITING;
    private ArenaGameBase currentGame;
    private int internalTime;
    private boolean allowPvP;
    private boolean forceStarted;
    private boolean resetting;
    private boolean winRewardGranted;
    private final Set<UUID> announcedEliminations = new HashSet<>();

    public Arena(final World world, final String name, final Configuration arenaConfig) {
        this.players = new ArrayList<>();
        this.spectators = new ArrayList<>();
        this.games = new ArrayList<>();

        this.mainConfig = SquidGame.getInstance().getMainConfig();
        this.arenaConfig = arenaConfig;
        this.handler = new ArenaHandler(this);
        this.world = world;
        this.name = name;

        this.resetArena();
    }

    public void resetArena() {
        this.resetting = true;

        try {
            if (this.currentGame != null) {
                this.currentGame.prepareStop();
            }

            for (final SquidPlayer player : this.getAllPlayers()) {
                this.removePlayer(player);
            }

            this.state = ArenaState.WAITING;
            this.currentGame = null;
            this.internalTime = -1;
            this.allowPvP = false;
            this.forceStarted = false;

            this.leaved = null;
            this.joined = null;
            this.death = null;
            this.winRewardGranted = false;
            this.announcedEliminations.clear();

            this.players.clear();
            this.spectators.clear();
            this.games.clear();

            final NbsMusicManager musicManager = SquidGame.getInstance().getNbsMusicManager();

            if (musicManager != null) {
                musicManager.stopLobbyMusicForArena(this);
            }

            final int game1Time = mainConfig.getInt("game-settings.game-time.1", 60);
            final int game3Time = mainConfig.getInt("game-settings.game-time.3", 60);
            final int game5Time = mainConfig.getInt("game-settings.game-time.5", 120);
            final int game6Time = mainConfig.getInt("game-settings.game-time.6", 180);
            final int game8Time = mainConfig.getInt("game-settings.game-time.8", 180);
            final int game10Time = mainConfig.getInt("game-settings.game-time.10", 1800);
            final int game12Time = mainConfig.getInt("game-settings.game-time.12", 180);
            final int game13Time = mainConfig.getInt("game-settings.game-time.13", 900);

            if (game1Time > 0)
                this.games.add(new G1RedGreenLightGame(this, game1Time));
            if (game8Time > 0)
                this.games.add(new G8MingleGame(this, game8Time));
            if (game3Time > 0)
                this.games.add(new G3BattleGame(this, game3Time));
            if (game5Time > 0)
                this.games.add(new G5TugOfWarGame(this, game5Time));
            if (game6Time > 0)
                this.games.add(new G6GlassesGame(this, game6Time));
            if (game10Time > 0)
                this.games.add(new G10HideAndSeekGame(this, game10Time));
            if (game12Time > 0)
                this.games.add(new G12FinalDinnerGame(this, game12Time));
            if (game13Time > 0)
                this.games.add(new G12SkySquidGame(this, game13Time));
        } finally {
            this.resetting = false;
        }
    }

    public Configuration getMainConfig() {
        return this.mainConfig;
    }

    public void broadcastPotionEffect(final PotionEffect e) {
        for (final SquidPlayer player : this.getPlayers()) {
            player.getBukkitPlayer().addPotionEffect(e);
        }
    }

    public void broadcastRemovePotionEffect(final PotionEffectType e) {
        for (final SquidPlayer player : this.getPlayers()) {
            player.getBukkitPlayer().removePotionEffect(e);
        }
    }

    public void broadcastMessage(final String message) {
        for (final SquidPlayer player : this.getAllPlayers()) {
            player.sendMessage(message);
        }
    }

    public void broadcastSound(final Sound sound) {
        for (final SquidPlayer player : this.getAllPlayers()) {
            player.playSound(sound);
        }
    }

    public void broadcastSound(final String soundName, final Location location) {
        if (soundName == null || soundName.isEmpty() || location == null) {
            return;
        }

        for (final SquidPlayer player : this.getAllPlayers()) {
            dev._2lstudios.squidgame.utils.CompatibilityUtils.playSound(player.getBukkitPlayer(), location,
                    soundName, 1.0F, 1.0F);
        }
    }

    public void broadcastTitle(final String title, final String subtitle) {
        for (final SquidPlayer player : this.getAllPlayers()) {
            player.sendTitle(title, subtitle, 2);
        }
    }

    public void broadcastScoreboard(final String scoreboardKey) {
        for (final SquidPlayer player : this.getAllPlayers()) {
            player.sendScoreboard(scoreboardKey);
        }
    }

    public int getMinPlayers() {
        return this.mainConfig.getInt("game-settings.min-players");
    }

    public int getForceStartMinPlayers() {
        return this.mainConfig.getInt("game-settings.force-start-min-players", 2);
    }

    public int getMaxPlayers() {
        return this.mainConfig.getInt("game-settings.max-players");
    }

    public Configuration getConfig() {
        return this.arenaConfig;
    }

    public boolean isLobbyConfigured() {
        return this.arenaConfig.contains("arena.prelobby.x") && this.arenaConfig.contains("arena.waiting_room.x");
    }

    public Location getArenaWaitingPosition() {
        if (this.arenaConfig.contains("arena.waiting_room.x")) {
            final Location location = this.arenaConfig.getLocation("arena.waiting_room", false);
            location.setWorld(this.world);
            return location;
        }

        final Location location = this.arenaConfig.getLocation("arena.prelobby", false);
        location.setWorld(this.world);
        return location;
    }

    public Location getSpawnPosition() {
        if (this.getState() == ArenaState.FINISHING_ARENA) {
            final Location loc = this.arenaConfig.getLocation("arena.waiting_room", false);
            loc.setWorld(this.world);
            return loc;
        } else if (this.getState() == ArenaState.EXPLAIN_GAME) {
            return this.getArenaWaitingPosition();
        } else if (this.getCurrentGame() != null) {
            return this.getCurrentGame().getSpawnPosition();
        } else {
            final Location loc = this.arenaConfig.getLocation("arena.prelobby", false);
            loc.setWorld(this.world);
            return loc;
        }
    }

    public void teleportAllPlayers(final Location location) {
        for (final SquidPlayer player : this.getAllPlayers()) {
            player.teleport(location);
        }
    }

    public void finishArena(final ArenaFinishReason reason) {
        if (this.state == ArenaState.FINISHING_ARENA) {
            return;
        }

        if (this.currentGame != null) {
            this.currentGame.prepareStop();
        }

        this.setState(ArenaState.FINISHING_ARENA);
        this.handler.handleArenaFinish(reason);
        this.teleportAllPlayers(this.getSpawnPosition());
    }

    public boolean canEliminatePlayers() {
        return this.state == ArenaState.IN_GAME || this.state == ArenaState.FINISHING_GAME;
    }

    public boolean hasGrantedWinReward() {
        return this.winRewardGranted;
    }

    public void markWinRewardGranted() {
        this.winRewardGranted = true;
    }

    public Arena addPlayer(final SquidPlayer player) {
        if (!this.players.contains(player) && !this.spectators.contains(player)) {
            if (this.players.size() >= this.getMaxPlayers()) {
                player.sendMessage("arena.full");
                return this;
            }

            this.joined = player.getBukkitPlayer().getName();
            this.players.add(player);
            this.preparePlayerForArena(player);
            this.giveStartItem(player.getBukkitPlayer());
            SquidGame.getInstance().getCosmeticManager().loadPlayerCosmetic(player);
            LobbyItems.giveLobbyItems(player);
            player.getBukkitPlayer().teleport(this.getArenaWaitingPosition());
            player.setArena(this);
            this.handler.handlePlayerJoin(player);
            SquidGame.getInstance().getNbsMusicManager().refreshLobbyMusic(player);
        }

        return this;
    }

    public boolean forceStart() {
        if (!this.canForceStart()) {
            return false;
        }

        this.forceStarted = true;
        this.removeStartItems();
        this.handler.handleArenaStart();
        return true;
    }

    public boolean canForceStart() {
        return (this.state == ArenaState.WAITING || this.state == ArenaState.STARTING)
                && this.players.size() >= this.getForceStartMinPlayers();
    }

    public boolean isForceStarted() {
        return this.forceStarted;
    }

    public void killAllPlayers() {
        if (this.state == ArenaState.FINISHING_ARENA) {
            return;
        }

        final List<SquidPlayer> list = new ArrayList<>(this.players);

        for (final SquidPlayer player : list) {
            this.death = player.getBukkitPlayer().getName();
            this.broadcastSound(this.getMainConfig().getSound("game-settings.sounds.player-death", "EXPLODE"));
            this.broadcastMessage("arena.death");
        }

        this.finishArena(ArenaFinishReason.ALL_PLAYERS_DEATH);
    }

    public boolean killPlayer(final SquidPlayer player, boolean setSpectator) {
        if (player == null || !this.canEliminatePlayers() || player.isSpectator()) {
            return false;
        }

        if (!this.players.contains(player)) {
            return false;
        }

        final UUID uuid = player.getBukkitPlayer().getUniqueId();

        if (!this.announcedEliminations.add(uuid)) {
            return false;
        }

        if (setSpectator) {
            player.setSpectator(true);

            if (!this.spectators.contains(player)) {
                this.spectators.add(player);
            }

            player.setArena(this);
        }

        if (!this.players.remove(player)) {
            this.announcedEliminations.remove(uuid);
            return false;
        }

        this.announceElimination(player);
        this.checkArenaFinishAfterElimination();
        this.notifyPlayerEliminated(player);
        return true;
    }

    public boolean killPlayer(final SquidPlayer player) {
        return this.killPlayer(player, true);
    }

    private void announceElimination(final SquidPlayer player) {
        SquidGame.getInstance().getPlayerDataManager().recordDeath(player);
        this.death = player.getBukkitPlayer().getName();
        this.broadcastSound(this.getMainConfig().getSound("game-settings.sounds.player-death", "EXPLODE"));
        this.broadcastMessage("arena.death");
    }

    private void checkArenaFinishAfterElimination() {
        if (!this.canEliminatePlayers()) {
            return;
        }

        if (this.isAllPlayersDeath()) {
            this.finishArena(ArenaFinishReason.ALL_PLAYERS_DEATH);
        } else if (this.calculateWinner() != null) {
            if (this.currentGame != null && this.currentGame.shouldPreventSinglePlayerFinish()) {
                return;
            }

            this.finishArena(ArenaFinishReason.ONE_PLAYER_IN_ARENA);
        }
    }

    private void notifyPlayerEliminated(final SquidPlayer player) {
        if (this.currentGame != null) {
            this.currentGame.onPlayerEliminated(player);
        }
    }

    public Arena addSpectator(final SquidPlayer player) {
        if (!this.spectators.contains(player)) {
            if (this.players.contains(player)) {
                this.players.remove(player);
                this.spectators.add(player);
                player.setSpectator(true);
                player.setArena(this);
            }
        }

        return this;
    }

    public boolean revivePlayer(final SquidPlayer player) {
        if (this.state == ArenaState.FINISHING_ARENA || !this.spectators.contains(player)) {
            return false;
        }

        final Player bukkitPlayer = player.getBukkitPlayer();

        this.spectators.remove(player);
        this.players.add(player);

        if (bukkitPlayer.isDead()) {
            bukkitPlayer.spigot().respawn();
        }

        player.setSpectator(false);
        player.setArena(this);
        this.announcedEliminations.remove(player.getBukkitPlayer().getUniqueId());
        this.preparePlayerForArena(player);
        player.teleport(this.getSpawnPosition());
        player.sendScoreboard(this.getState().toString().toLowerCase());

        return true;
    }

    @SuppressWarnings("deprecation")
    private void preparePlayerForArena(final SquidPlayer player) {
        final Player bukkitPlayer = player.getBukkitPlayer();

        bukkitPlayer.setGameMode(GameMode.SURVIVAL);
        bukkitPlayer.setAllowFlight(false);
        bukkitPlayer.setFlying(false);
        bukkitPlayer.setFoodLevel(20);
        bukkitPlayer.setSaturation(20.0F);
        bukkitPlayer.setFireTicks(0);
        bukkitPlayer.setFallDistance(0.0F);
        bukkitPlayer.setLevel(0);
        bukkitPlayer.setExp(0.0F);
        bukkitPlayer.getInventory().clear();
        bukkitPlayer.getInventory().setArmorContents(new ItemStack[4]);

        for (final PotionEffect effect : bukkitPlayer.getActivePotionEffects()) {
            bukkitPlayer.removePotionEffect(effect.getType());
        }

        if (!bukkitPlayer.isDead()) {
            bukkitPlayer.setHealth(bukkitPlayer.getMaxHealth());
        }

        bukkitPlayer.updateInventory();

        if (LobbyItems.canUseLobbyFeatures(player)) {
            LobbyItems.giveLobbyItems(player);
        }
    }

    public boolean isStartItem(final ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()
                || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        return item.getItemMeta().getDisplayName()
                .equals(MessageUtils.format(SquidGame.getInstance(), "items.start-game"));
    }

    public void removeStartItems() {
        for (final SquidPlayer player : this.getAllPlayers()) {
            this.removeStartItem(player.getBukkitPlayer());
        }
    }

    private void giveStartItem(final Player player) {
        if (!player.hasPermission("squidgame.start")) {
            return;
        }

        final ItemStack item = new ItemStack(Material.NETHER_STAR);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.start-game"));
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        player.updateInventory();
    }

    private void removeStartItem(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (this.isStartItem(item)) {
                player.getInventory().remove(item);
            }
        }

        player.updateInventory();
    }

    public void removePlayer(final SquidPlayer player) {
        this.removeStartItem(player.getBukkitPlayer());

        if (this.players.contains(player)) {
            this.leaved = player.getBukkitPlayer().getName();
            final boolean midGame = this.getState() != ArenaState.WAITING && this.getState() != ArenaState.STARTING
                    && this.getState() != ArenaState.FINISHING_ARENA;

            this.players.remove(player);
            this.handler.handlePlayerLeave(player);

            if (midGame && !this.resetting
                    && this.announcedEliminations.add(player.getBukkitPlayer().getUniqueId())) {
                this.announceElimination(player);
                this.checkArenaFinishAfterElimination();
                this.notifyPlayerEliminated(player);
            }
        } else if (this.spectators.contains(player)) {
            this.spectators.remove(player);
            player.setSpectator(false);
        } else {
            return;
        }

        player.getBukkitPlayer().setGameMode(GameMode.SURVIVAL);
        player.teleportToLobby();
        player.refreshScoreboard();
        player.setArena(null);
        SquidGame.getInstance().getCosmeticManager().loadPlayerCosmetic(player);
        LobbyItems.giveLobbyItems(player);
    }

    public List<SquidPlayer> getAllPlayers() {
        final Map<UUID, SquidPlayer> unique = new LinkedHashMap<>();

        for (final SquidPlayer player : this.players) {
            unique.put(player.getBukkitPlayer().getUniqueId(), player);
        }

        for (final SquidPlayer spectator : this.spectators) {
            unique.putIfAbsent(spectator.getBukkitPlayer().getUniqueId(), spectator);
        }

        return new ArrayList<>(unique.values());
    }

    public List<SquidPlayer> getPlayers() {
        return this.players;
    }

    public List<SquidPlayer> getSpectators() {
        return this.spectators;
    }

    public ArenaState getState() {
        return this.state;
    }

    public World getWorld() {
        return this.world;
    }

    public ArenaGameBase getCurrentGame() {
        return this.currentGame;
    }

    public boolean hasNextGame() {
        return !this.games.isEmpty();
    }

    public ArenaGameBase getNextGame() {
        return this.games.isEmpty() ? null : this.games.get(0);
    }

    public String getName() {
        return this.name;
    }

    public int getInternalTime() {
        return this.internalTime;
    }

    public void setState(final ArenaState newState) {
        this.state = newState;
        this.handler.handleArenaSwitchState();
    }

    public void setInternalTime(final int time) {
        if (time >= 0) {
            this.internalTime = time;
        }
    }

    public String getJoinedPlayer() {
        return this.joined;
    }

    public String getLeavedPlayer() {
        return this.leaved;
    }

    public String getDeathPlayer() {
        return this.death;
    }

    public SquidPlayer calculateWinner() {
        if (this.players.size() == 1) {
            return this.players.get(0);
        } else {
            return null;
        }
    }

    public boolean isAllPlayersDeath() {
        return this.players.isEmpty();
    }

    public boolean isPvPAllowed() {
        return this.allowPvP;
    }

    public void setPvPAllowed(final boolean result) {
        this.allowPvP = result;
    }

    public void doArenaTick() {
        if (this.internalTime > 0) {
            this.internalTime--;
        }

        this.handler.handleArenaTick();
        this.broadcastScoreboard(this.state.toString().toLowerCase());
    }

    public void nextGame() {
        this.announcedEliminations.clear();
        final boolean stoppedPreviousGame = this.currentGame != null;

        if (this.currentGame != null) {
            this.setPvPAllowed(false);
            this.currentGame.prepareStop();
            this.currentGame = null;
        }

        if (stoppedPreviousGame && this.calculateWinner() != null) {
            this.finishArena(ArenaFinishReason.ONE_PLAYER_IN_ARENA);
            return;
        }

        this.setPvPAllowed(false);
        final ArenaGameBase nextGame = this.getNextPlayableGame();

        if (nextGame == null) {
            if (this.calculateWinner() != null) {
                this.finishArena(ArenaFinishReason.ONE_PLAYER_IN_ARENA);
            } else {
                this.finishArena(ArenaFinishReason.ALL_PLAYERS_DEATH);
            }

            return;
        }

        this.currentGame = nextGame;

        this.setInternalTime(15);
        this.setState(ArenaState.EXPLAIN_GAME);
        this.setPvPAllowed(false);

        this.teleportAllPlayers(this.getArenaWaitingPosition());

        MessageUtils.broadcastTitle(SquidGame.getInstance(), this, "events.intermission.title",
                "events.intermission.subtitle", "{game}", nextGame.getName(), "{time}",
                String.valueOf(this.getInternalTime()));
        this.broadcastIntermissionTip(nextGame);
        this.prepareIntermissionLobby();
        nextGame.onExplainStart();
    }

    private void broadcastIntermissionTip(final ArenaGameBase nextGame) {
        final SquidGame plugin = SquidGame.getInstance();
        final String tip = MessageUtils.format(plugin, "events.intermission.tip", "{tip}", nextGame.getIntermissionTip());

        for (final SquidPlayer player : this.getAllPlayers()) {
            player.getBukkitPlayer().sendMessage(tip);
        }
    }

    private void prepareIntermissionLobby() {
        final SquidGame plugin = SquidGame.getInstance();

        for (final SquidPlayer player : this.getAllPlayers()) {
            LobbyItems.giveLobbyItems(player);
            plugin.getCosmeticManager().refreshCosmetic(player);
        }

        plugin.getNbsMusicManager().resumeArenaMusic(this);
    }

    private ArenaGameBase getNextPlayableGame() {
        while (!this.games.isEmpty()) {
            final ArenaGameBase nextGame = this.games.remove(0);

            if (this.players.size() >= nextGame.getMinPlayers()) {
                return nextGame;
            }

            for (final SquidPlayer player : this.getAllPlayers()) {
                MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "events.game-skipped-min-players",
                        "{game}", nextGame.getName(), "{players}", String.valueOf(this.players.size()), "{min}",
                        String.valueOf(nextGame.getMinPlayers()));
            }
        }

        return null;
    }
}
