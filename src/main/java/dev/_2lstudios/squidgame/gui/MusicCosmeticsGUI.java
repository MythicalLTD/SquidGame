package dev._2lstudios.squidgame.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.music.LobbyMusicCosmetic;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class MusicCosmeticsGUI extends InventoryGUI {

    private final SquidPlayer owner;
    private final boolean returnToMusicMenu;
    private final boolean forceEveryone;

    public MusicCosmeticsGUI(final SquidPlayer owner) {
        this(owner, false, false);
    }

    public MusicCosmeticsGUI(final SquidPlayer owner, final boolean returnToMusicMenu) {
        this(owner, returnToMusicMenu, false);
    }

    public MusicCosmeticsGUI(final SquidPlayer owner, final boolean returnToMusicMenu, final boolean forceEveryone) {
        super(MessageUtils.format(SquidGame.getInstance(), "cosmetics.music.gui.title"), 6 * 9);
        this.owner = owner;
        this.returnToMusicMenu = returnToMusicMenu;
        this.forceEveryone = forceEveryone;
    }

    @Override
    public void init() {
        final SquidGame plugin = SquidGame.getInstance();
        final String selectedId = plugin.getCosmeticManager().getMusicCosmeticId(this.owner);
        final boolean admin = plugin.getNbsMusicManager().canBypassMusicCooldown(this.owner);
        final int playerCoins = plugin.getShopManager().getPlayerCoins(this.owner);
        final long cooldownSeconds = admin ? 0L
                : (plugin.getNbsMusicManager().getCosmeticCooldownRemaining(this.owner) + 999L) / 1000L;

        String infoLore = MessageUtils.format(plugin, "cosmetics.music.gui.info-lore");
        if (plugin.getShopManager().isEnabled()) {
            infoLore = infoLore + "\n" + MessageUtils.format(plugin, "economy.gui.coins", "{coins}",
                    String.valueOf(playerCoins));
        }
        if (admin) {
            infoLore = infoLore + "\n" + MessageUtils.format(plugin, "cosmetics.music.gui.admin-bypass");
        }
        if (this.forceEveryone) {
            infoLore = infoLore + "\n" + MessageUtils.format(plugin, "cosmetics.music.gui.force-mode");
        }

        this.addItem(100, this.createItem(MessageUtils.format(plugin, "cosmetics.music.gui.info"), Material.JUKEBOX,
                infoLore), 1, 1);

        this.addItem(99, this.createItem("§eBack", Material.ARROW), 9, 1);
        this.addItem(98, this.createItem("§cClose", Material.BARRIER), 5, 6);

        final String cooldownLore = cooldownSeconds > 0L
                ? MessageUtils.format(plugin, "cosmetics.music.gui.cooldown-lore", "{seconds}",
                        String.valueOf(cooldownSeconds))
                : "";

        this.addItem(0, this.createNoneItem("none".equalsIgnoreCase(selectedId), cooldownLore), 3, 2);

        int slot = 10;
        int index = 1;

        for (final LobbyMusicCosmetic cosmetic : plugin.getNbsMusicManager().getCosmeticRegistry().getAll()) {
            final boolean selected = cosmetic.getId().equalsIgnoreCase(selectedId);
            final boolean unlocked = plugin.getShopManager().ownsMusic(this.owner, cosmetic);
            final int unlockPrice = plugin.getShopManager().getMusicUnlockPrice(cosmetic.getId());
            final int playCost = plugin.getShopManager().getMusicPlayCost(cosmetic.getId());
            this.addItem(index,
                    cosmetic.createMenuItem(selected, cooldownLore, unlocked, unlockPrice, playCost, playerCoins),
                    slot);
            index++;
            slot++;

            if (slot == 17) {
                slot = 19;
            } else if (slot == 26) {
                slot = 28;
            } else if (slot == 35) {
                slot = 37;
            } else if (slot == 44) {
                break;
            }
        }
    }

    private org.bukkit.inventory.ItemStack createNoneItem(final boolean selected, final String cooldownLore) {
        final SquidGame plugin = SquidGame.getInstance();
        final org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.BARRIER);
        final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.format(plugin, "cosmetics.music.gui.none"));

        final StringBuilder lore = new StringBuilder(MessageUtils.format(plugin, "cosmetics.music.gui.none-lore"));
        if (cooldownLore != null && !cooldownLore.isEmpty()) {
            lore.append('\n').append(cooldownLore);
        }
        if (selected) {
            lore.append("\n§a§lEQUIPPED");
        }

        meta.setLore(java.util.Arrays.asList(lore.toString().split("\n")));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handle(final int id, final Player player) {
        if (id == 98) {
            player.closeInventory();
            return;
        }

        if (id == 99) {
            if (this.returnToMusicMenu) {
                new MusicMenuGUI(this.owner).open(player);
            } else {
                new CosmeticsGUI(this.owner).open(player);
            }

            return;
        }

        if (id == 100) {
            return;
        }

        final SquidGame plugin = SquidGame.getInstance();
        final boolean force = this.forceEveryone
                || (plugin.getNbsMusicManager().canBypassMusicCooldown(this.owner) && this.returnToMusicMenu);

        if (id == 0) {
            plugin.getNbsMusicManager().playCosmeticMusic(this.owner, null, false);
            new MusicCosmeticsGUI(this.owner, this.returnToMusicMenu, this.forceEveryone).open(player);
            return;
        }

        final java.util.List<LobbyMusicCosmetic> cosmetics = plugin.getNbsMusicManager().getCosmeticRegistry()
                .getAll();

        if (id < 1 || id > cosmetics.size()) {
            return;
        }

        final LobbyMusicCosmetic cosmetic = cosmetics.get(id - 1);

        if (!plugin.getShopManager().ownsMusic(this.owner, cosmetic)) {
            plugin.getShopManager().tryPurchaseMusic(this.owner, cosmetic);
            new MusicCosmeticsGUI(this.owner, this.returnToMusicMenu, this.forceEveryone).open(player);
            return;
        }

        plugin.getNbsMusicManager().playCosmeticMusic(this.owner, cosmetic, force);
        new MusicCosmeticsGUI(this.owner, this.returnToMusicMenu, this.forceEveryone).open(player);
    }
}
