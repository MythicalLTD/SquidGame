package dev._2lstudios.squidgame.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.LobbyItems;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class MusicMenuGUI extends InventoryGUI {

    private static final int FILLER_ID = 1000;

    private final SquidPlayer owner;

    public MusicMenuGUI(final SquidPlayer owner) {
        super(MessageUtils.format(SquidGame.getInstance(), "music.menu.title"), 5 * 9);
        this.owner = owner;
    }

    @Override
    public void init() {
        final SquidGame plugin = SquidGame.getInstance();
        final boolean muted = this.owner.isMusicMuted();
        final boolean admin = plugin.getNbsMusicManager().canBypassMusicCooldown(this.owner);

        this.fillBackground();

        this.addItem(100, this.createItem(MessageUtils.format(plugin, "music.menu.info"), Material.BOOK,
                MessageUtils.format(plugin, "music.menu.info-lore", "{volume}",
                        String.valueOf(this.owner.getMusicVolume()), "{muted}",
                        MessageUtils.format(plugin, muted ? "music.menu.status-muted" : "music.menu.status-unmuted"))),
                5, 1);

        this.addItem(1, this.createItem(
                MessageUtils.format(plugin, muted ? "music.menu.unmute" : "music.menu.mute"),
                muted ? Material.EMERALD : Material.REDSTONE,
                MessageUtils.format(plugin, muted ? "music.menu.unmute-lore" : "music.menu.mute-lore")), 5, 2);

        this.addItem(2, this.createItem(MessageUtils.format(plugin, "music.menu.volume-down"), Material.REDSTONE_BLOCK,
                MessageUtils.format(plugin, "music.menu.volume-down-lore")), 3, 2);

        this.addItem(3, this.createItem(MessageUtils.format(plugin, "music.menu.volume-up"), Material.EMERALD_BLOCK,
                MessageUtils.format(plugin, "music.menu.volume-up-lore")), 7, 2);

        this.addItem(4, this.createItem(MessageUtils.format(plugin, "music.menu.pick-song"), Material.JUKEBOX,
                MessageUtils.format(plugin, "music.menu.pick-song-lore")), 4, 3);

        this.addItem(6, this.createItem(MessageUtils.format(plugin, "music.menu.random-dj"),
                CompatibilityUtils.material("MUSIC_DISC_11", "RECORD_11", "GOLD_RECORD"),
                MessageUtils.format(plugin, "music.menu.random-dj-lore")), 6, 3);

        if (admin) {
            this.addItem(11, this.createItem(MessageUtils.format(plugin, "music.menu.admin-force-dj"),
                    Material.DIAMOND_BLOCK, MessageUtils.format(plugin, "music.menu.admin-force-dj-lore")), 3, 4);

            this.addItem(13, this.createItem(MessageUtils.format(plugin, "music.menu.admin-random-dj"),
                    CompatibilityUtils.material("MUSIC_DISC_13", "RECORD_12", "GREEN_RECORD"),
                    MessageUtils.format(plugin, "music.menu.admin-random-dj-lore")), 5, 4);

            this.addItem(12, this.createItem(MessageUtils.format(plugin, "music.menu.admin-stop"),
                    Material.TNT, MessageUtils.format(plugin, "music.menu.admin-stop-lore")), 7, 4);
        }

        this.addItem(99, this.createItem(MessageUtils.format(plugin, "music.menu.close"), Material.ARROW,
                MessageUtils.format(plugin, "music.menu.close-lore")), 5, 5);
    }

    private void fillBackground() {
        final ItemStack filler = this.createFillerPane();

        for (int slot = 0; slot < this.getInventory().getSize(); slot++) {
            this.addItem(FILLER_ID, filler, slot);
        }
    }

    private ItemStack createFillerPane() {
        return this.createItem(" ",
                CompatibilityUtils.material("LIGHT_GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "THIN_GLASS"));
    }

    @Override
    public void handle(final int id, final Player player) {
        if (id == FILLER_ID || id == 100) {
            return;
        }

        if (id == 99) {
            player.closeInventory();
            return;
        }

        final SquidGame plugin = SquidGame.getInstance();

        if (id == 1) {
            plugin.getNbsMusicManager().toggleMusicMuted(this.owner);
            LobbyItems.updateMusicMenuItem(this.owner);
            new MusicMenuGUI(this.owner).open(player);
            return;
        }

        if (id == 2) {
            plugin.getNbsMusicManager().adjustPlayerMusicVolume(this.owner, -10);
            MessageUtils.send(plugin, player, "music.menu.volume-set", "{volume}",
                    String.valueOf(this.owner.getMusicVolume()));
            new MusicMenuGUI(this.owner).open(player);
            return;
        }

        if (id == 3) {
            plugin.getNbsMusicManager().adjustPlayerMusicVolume(this.owner, 10);
            MessageUtils.send(plugin, player, "music.menu.volume-set", "{volume}",
                    String.valueOf(this.owner.getMusicVolume()));
            new MusicMenuGUI(this.owner).open(player);
            return;
        }

        if (id == 4) {
            new MusicCosmeticsGUI(this.owner, true).open(player);
            return;
        }

        if (id == 6) {
            if (!plugin.getNbsMusicManager().playRandomCosmeticMusic(this.owner, false)) {
                MessageUtils.send(plugin, player, "music.menu.random-dj-failed");
            }
            new MusicMenuGUI(this.owner).open(player);
            return;
        }

        if (id == 11) {
            new MusicCosmeticsGUI(this.owner, true, true).open(player);
            return;
        }

        if (id == 12) {
            plugin.getNbsMusicManager().stopAllMusicInScope(this.owner);
            new MusicMenuGUI(this.owner).open(player);
            return;
        }

        if (id == 13) {
            if (!plugin.getNbsMusicManager().playRandomCosmeticMusic(this.owner, true)) {
                MessageUtils.send(plugin, player, "music.menu.random-dj-failed");
            }
            new MusicMenuGUI(this.owner).open(player);
        }
    }
}
