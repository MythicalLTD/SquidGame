package dev._2lstudios.squidgame.music;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.xxmicloxx.NoteBlockAPI.model.Song;

import dev._2lstudios.squidgame.utils.CompatibilityUtils;

public class LobbyMusicCosmetic {

    private final String id;
    private final String fileName;
    private final String displayName;
    private final Song song;

    public LobbyMusicCosmetic(final String id, final String fileName, final String displayName, final Song song) {
        this.id = id;
        this.fileName = fileName;
        this.displayName = displayName;
        this.song = song;
    }

    public String getId() {
        return this.id;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Song getSong() {
        return this.song;
    }

    public ItemStack createMenuItem(final boolean selected, final String extraLore) {
        return this.createMenuItem(selected, extraLore, true, 0, 0, 0);
    }

    public ItemStack createMenuItem(final boolean selected, final String extraLore, final boolean unlocked,
            final int unlockPrice, final int playCost, final int playerCoins) {
        final ItemStack item = new ItemStack(
                CompatibilityUtils.material("MUSIC_DISC_CAT", "RECORD_11", "GOLD_RECORD", "JUKEBOX"));
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l" + this.displayName);

        final StringBuilder lore = new StringBuilder("§7DJ this track for everyone\n§7in the lobby while waiting.");
        if (extraLore != null && !extraLore.isEmpty()) {
            lore.append('\n').append(extraLore);
        }

        if (!unlocked) {
            lore.append("\n§c§lLOCKED");
            lore.append("\n§eUnlock: §f").append(unlockPrice).append(" coins");

            if (playerCoins < unlockPrice) {
                lore.append("\n§cYou need §f").append(unlockPrice - playerCoins).append(" §cmore coins");
            }

            lore.append("\n§7Click to purchase");
        } else {
            if (playCost > 0) {
                lore.append("\n§ePlay cost: §f").append(playCost).append(" coins");
            }

            if (selected) {
                lore.append("\n§a§lEQUIPPED");
            } else {
                lore.append("\n§7Click to play");
            }
        }

        meta.setLore(java.util.Arrays.asList(lore.toString().split("\n")));
        item.setItemMeta(meta);
        return item;
    }

    public static String toId(final String fileName) {
        return fileName.replace(".nbs", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    public static String toDisplayName(final String fileName) {
        if (!fileName.toLowerCase().endsWith(".nbs")) {
            return fileName;
        }

        return fileName.substring(0, fileName.length() - 4);
    }
}
