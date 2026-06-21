package dev._2lstudios.squidgame.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.cosmetics.CosmeticType;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class CosmeticsGUI extends InventoryGUI {

    private static final int[] COSMETIC_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int PER_PAGE = COSMETIC_SLOTS.length;

    private final SquidPlayer owner;
    private final int page;

    public CosmeticsGUI(final SquidPlayer owner) {
        this(owner, 0);
    }

    public CosmeticsGUI(final SquidPlayer owner, final int page) {
        super(buildTitle(owner, page), 6 * 9);
        this.owner = owner;
        this.page = Math.max(0, page);
    }

    private static String buildTitle(final SquidPlayer owner, final int page) {
        final SquidGame plugin = SquidGame.getInstance();
        final int totalPages = totalPages();

        if (totalPages <= 1) {
            return MessageUtils.format(plugin, "cosmetics.gui.title");
        }

        return MessageUtils.format(plugin, "cosmetics.gui.title-page", "{page}", String.valueOf(page + 1), "{pages}",
                String.valueOf(totalPages));
    }

    private static int totalPages() {
        return Math.max(1, (int) Math.ceil(CosmeticType.selectable().length / (double) PER_PAGE));
    }

    @Override
    public void init() {
        final SquidGame plugin = SquidGame.getInstance();
        final CosmeticType selected = plugin.getCosmeticManager().getCosmetic(this.owner);
        final CosmeticType[] cosmetics = CosmeticType.selectable();
        final int start = this.page * PER_PAGE;
        final int end = Math.min(start + PER_PAGE, cosmetics.length);
        final int playerCoins = plugin.getShopManager().getPlayerCoins(this.owner);

        String infoLore = MessageUtils.format(plugin, "cosmetics.gui.info-lore");

        if (plugin.getShopManager().isEnabled()) {
            infoLore = infoLore + "\n" + MessageUtils.format(plugin, "economy.gui.coins", "{coins}",
                    String.valueOf(playerCoins));
        }

        this.addItem(100, this.createItem(MessageUtils.format(plugin, "cosmetics.gui.info"), Material.BOOK, infoLore),
                1, 1);

        this.addItem(101, this.createItem(MessageUtils.format(plugin, "cosmetics.gui.music-button"), Material.JUKEBOX,
                MessageUtils.format(plugin, "cosmetics.gui.music-button-lore")), 9, 1);

        for (int index = start; index < end; index++) {
            final CosmeticType cosmetic = cosmetics[index];
            final int slotIndex = index - start;
            final boolean unlocked = plugin.getShopManager().ownsCosmetic(this.owner, cosmetic);
            final int price = plugin.getShopManager().getCosmeticPrice(cosmetic.getId());
            this.addItem(cosmetic.ordinal(),
                    cosmetic.createMenuItem(cosmetic == selected, unlocked, price, playerCoins),
                    COSMETIC_SLOTS[slotIndex]);
        }

        if (this.page > 0) {
            this.addItem(200, this.createItem(MessageUtils.format(plugin, "cosmetics.gui.prev-page"), Material.ARROW,
                    MessageUtils.format(plugin, "cosmetics.gui.prev-page-lore")), 1, 6);
        }

        if (end < cosmetics.length) {
            this.addItem(201, this.createItem(MessageUtils.format(plugin, "cosmetics.gui.next-page"), Material.ARROW,
                    MessageUtils.format(plugin, "cosmetics.gui.next-page-lore")), 9, 6);
        }

        this.addItem(99, this.createItem("§cClose", Material.BARRIER), 5, 6);
    }

    @Override
    public void handle(final int id, final Player player) {
        if (id == 99) {
            player.closeInventory();
            return;
        }

        if (id == 100) {
            return;
        }

        if (id == 101) {
            new MusicCosmeticsGUI(this.owner).open(player);
            return;
        }

        if (id == 200) {
            new CosmeticsGUI(this.owner, this.page - 1).open(player);
            return;
        }

        if (id == 201) {
            new CosmeticsGUI(this.owner, this.page + 1).open(player);
            return;
        }

        final CosmeticType[] cosmetics = CosmeticType.selectable();

        if (id < 0 || id >= cosmetics.length) {
            return;
        }

        final CosmeticType cosmetic = cosmetics[id];
        final SquidGame plugin = SquidGame.getInstance();

        if (!plugin.getShopManager().ownsCosmetic(this.owner, cosmetic)) {
            plugin.getShopManager().tryPurchaseCosmetic(this.owner, cosmetic);
            new CosmeticsGUI(this.owner, this.page).open(player);
            return;
        }

        plugin.getCosmeticManager().setCosmetic(this.owner, cosmetic);
        MessageUtils.send(plugin, player, "cosmetics.equipped", "{cosmetic}", cosmetic.getDisplayName());
        new CosmeticsGUI(this.owner, this.page).open(player);
    }
}
