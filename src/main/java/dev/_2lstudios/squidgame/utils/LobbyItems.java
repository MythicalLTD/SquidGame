package dev._2lstudios.squidgame.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;

public final class LobbyItems {

    private static final int COSMETICS_SLOT = 4;
    private static final int MUTE_MUSIC_SLOT = 8;

    private LobbyItems() {
    }

    public static boolean isInLobbyWaiting(final SquidPlayer player) {
        if (player == null) {
            return false;
        }

        final Arena arena = player.getArena();

        if (arena == null) {
            return true;
        }

        final ArenaState state = arena.getState();

        return state == ArenaState.WAITING || state == ArenaState.STARTING || state == ArenaState.INTERMISSION
                || state == ArenaState.EXPLAIN_GAME;
    }

    public static boolean isCosmeticsItem(final ItemStack item) {
        if (item == null || item.getType() != Material.CHEST || !item.hasItemMeta()
                || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        return item.getItemMeta().getDisplayName()
                .equals(MessageUtils.format(SquidGame.getInstance(), "items.cosmetics"));
    }

    public static boolean isMusicMenuItem(final ItemStack item) {
        if (item == null || item.getType() != Material.NOTE_BLOCK || !item.hasItemMeta()
                || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        final String name = item.getItemMeta().getDisplayName();
        final SquidGame plugin = SquidGame.getInstance();

        return name.equals(MessageUtils.format(plugin, "items.music-menu"))
                || name.equals(MessageUtils.format(plugin, "items.mute-music"))
                || name.equals(MessageUtils.format(plugin, "items.unmute-music"));
    }

    public static boolean isMuteMusicItem(final ItemStack item) {
        return isMusicMenuItem(item);
    }

    public static boolean canUseLobbyFeatures(final SquidPlayer player) {
        if (isInLobbyWaiting(player)) {
            return true;
        }

        if (player == null) {
            return false;
        }

        final Arena arena = player.getArena();

        return arena != null && arena.getState() == ArenaState.IN_GAME && arena.getCurrentGame() != null
                && arena.getCurrentGame().keepsGameLobbyFeatures();
    }

    public static boolean hasLobbyAccess(final SquidPlayer player) {
        if (canUseLobbyFeatures(player)) {
            return true;
        }

        if (player == null || player.getBukkitPlayer() == null) {
            return false;
        }

        for (final ItemStack item : player.getBukkitPlayer().getInventory().getContents()) {
            if (item != null && (isCosmeticsItem(item) || isMusicMenuItem(item))) {
                return true;
            }
        }

        return false;
    }

    public static void giveLobbyItems(final SquidPlayer player) {
        final SquidGame plugin = SquidGame.getInstance();

        if (!canUseLobbyFeatures(player)) {
            return;
        }

        if (plugin.getMainConfig().getBoolean("lobby.give-cosmetics-item", true)) {
            giveCosmeticsItem(player.getBukkitPlayer());
            plugin.getCosmeticManager().refreshCosmetic(player);
        }

        if ((plugin.getMainConfig().getBoolean("lobby.music.enabled", true)
                || plugin.getMainConfig().getBoolean("lobby.music.cosmetic-enabled", true))
                && plugin.getMainConfig().getBoolean("lobby.give-mute-music-item", true)) {
            giveMusicMenuItem(player);
        }

        plugin.getNbsMusicManager().refreshLobbyMusic(player);
    }

    public static void removeLobbyItems(final Player player) {
        removeCosmeticsItem(player);
        removeMusicMenuItem(player);
    }

    public static void removeCosmeticsItem(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (isCosmeticsItem(item)) {
                player.getInventory().remove(item);
            }
        }

        player.updateInventory();
    }

    public static void removeMusicMenuItem(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (isMusicMenuItem(item)) {
                player.getInventory().remove(item);
            }
        }

        player.updateInventory();
    }

    public static void removeMuteMusicItem(final Player player) {
        removeMusicMenuItem(player);
    }

    public static void updateMusicMenuItem(final SquidPlayer player) {
        if (!SquidGame.getInstance().getMainConfig().getBoolean("lobby.give-mute-music-item", true)) {
            return;
        }

        giveMusicMenuItem(player);
    }

    public static void updateMuteMusicItem(final SquidPlayer player) {
        updateMusicMenuItem(player);
    }

    public static void openMusicMenu(final SquidPlayer player) {
        new dev._2lstudios.squidgame.gui.MusicMenuGUI(player).open(player.getBukkitPlayer());
    }

    private static void giveCosmeticsItem(final Player player) {
        removeCosmeticsItem(player);

        final ItemStack item = new ItemStack(Material.CHEST);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.cosmetics"));
        meta.setLore(java.util.Arrays.asList(
                MessageUtils.format(SquidGame.getInstance(), "items.cosmetics-lore").split("\n")));
        item.setItemMeta(meta);

        final int slot = SquidGame.getInstance().getMainConfig().getInt("lobby.cosmetics-slot", COSMETICS_SLOT);
        player.getInventory().setItem(slot, item);
        player.updateInventory();
    }

    private static void giveMusicMenuItem(final SquidPlayer player) {
        removeMusicMenuItem(player.getBukkitPlayer());

        final SquidGame plugin = SquidGame.getInstance();
        final ItemStack item = new ItemStack(Material.NOTE_BLOCK);
        final ItemMeta meta = item.getItemMeta();
        final String nameKey = player.isMusicMuted() ? "items.unmute-music" : "items.mute-music";
        meta.setDisplayName(MessageUtils.format(plugin, nameKey));
        meta.setLore(java.util.Arrays.asList(MessageUtils.format(plugin, "items.mute-music-lore").split("\n")));
        item.setItemMeta(meta);

        final int slot = plugin.getMainConfig().getInt("lobby.mute-music-slot", MUTE_MUSIC_SLOT);
        player.getBukkitPlayer().getInventory().setItem(slot, item);
        player.getBukkitPlayer().updateInventory();
    }
}
