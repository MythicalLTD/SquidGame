package dev._2lstudios.squidgame.gui;

import java.io.IOException;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;
import dev._2lstudios.squidgame.utils.SkySquidSetupHelper;

public class EditArenaSkySquidGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaSkySquidGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§6§lSky Squid §8| §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        final SquidGame plugin = SquidGame.getInstance();
        final double horizontalPadding = plugin.getMainConfig()
                .getDouble("game-settings.sky-squid-bridge-marker-scan-horizontal", 14.0D);
        final int verticalPadding = plugin.getMainConfig().getInt("game-settings.sky-squid-bridge-marker-scan-vertical", 4);
        final String status = SkySquidSetupHelper.getSetupStatus(this.arena.getConfig(), this.arena.getWorld(),
                horizontalPadding, verticalPadding);

        this.addItem(11, this.createItem("§aSetup guide", Material.BOOK,
                "§r\n§71. §fSet each tower§7 — stand in the center\n§72. §fBuild the bridges§7 in-world:\n§7   Square→Triangle: §credstone blocks\n§7   Triangle→Circle: §6gold blocks\n§73. §fScan below§7 to save them\n§r"), 2, 1);
        this.addItem(12, this.createItem("§eSetup status", Material.PAPER,
                "§r\n" + status + "\n§r"), 6, 1);

        this.addItem(10, this.createItem("§eWaiting lobby", CompatibilityUtils.material("WOODEN_DOOR", "OAK_DOOR"),
                "§r\n§7Set where players wait during the tutorial\n§r"), 2, 2);

        this.addItem(1, this.createItem("§eSet §6Square §etower", Material.GOLD_BLOCK,
                "§r\n§7Stand in the middle of the square tower\n§7Sets spawn, safe zone, and finds the button\n§r"), 4, 2);
        this.addItem(2, this.createItem("§eSet §aTriangle §etower", Material.EMERALD_BLOCK,
                "§r\n§7Stand in the middle of the triangle tower\n§7Sets spawn, safe zone, and finds the button\n§r"), 6, 2);
        this.addItem(3, this.createItem("§eSet §cCircle §etower", Material.REDSTONE_BLOCK,
                "§r\n§7Stand in the middle of the circle tower\n§7Sets spawn, safe zone, and finds the button\n§r"), 8, 2);

        this.addItem(6, this.createItem("§eScan §cRedstone §ebridge", Material.REDSTONE_BLOCK,
                "§r\n§7Square → Triangle path\n§7Build it with redstone blocks,\n§7then click here to save it.\n§r"), 3, 3);
        this.addItem(7, this.createItem("§eScan §6Gold §ebridge", Material.GOLD_BLOCK,
                "§r\n§7Triangle → Circle path\n§7Build it with gold blocks,\n§7then click here to save it.\n§r"), 7, 3);

        final int ironBarCount = EditArenaIronBars.getCount(this.arena, "sky-squid");
        this.addItem(EditArenaIronBars.ADD_ID,
                this.createItem("§eAdd iron bar zone", EditArenaIronBars.buttonMaterial(),
                        EditArenaIronBars.addButtonLore(ironBarCount)),
                2, 4);
        this.addItem(EditArenaIronBars.CLEAR_ID,
                this.createItem("§cClear iron bar zones", Material.TNT, EditArenaIronBars.clearButtonLore()), 6, 4);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(final int id, final Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 10) {
            this.arena.getConfig().setLocation("games.sky-squid.lobby", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Sky Squid lobby");
        } else if (id >= 1 && id <= 3) {
            this.setTower(player, id);
        } else if (id == 6 || id == 7) {
            this.scanBridgeMarkers(player, id == 6 ? 1 : 2);
        } else if (EditArenaIronBars.handle(this.arena, player, "sky-squid", "Sky Squid Game", id)) {
        }

        try {
            this.arena.getConfig().save();
        } catch (final IOException exception) {
            exception.printStackTrace();
        }

        this.close(player);
    }

    private void setTower(final Player player, final int tower) {
        final SquidGame plugin = SquidGame.getInstance();
        final Location location = player.getLocation();
        final int radius = plugin.getMainConfig().getInt("game-settings.sky-squid-tower-radius", 6);
        final int heightBelow = plugin.getMainConfig().getInt("game-settings.sky-squid-tower-height-below", 1);
        final int heightAbove = plugin.getMainConfig().getInt("game-settings.sky-squid-tower-height-above", 5);
        final int buttonRadius = plugin.getMainConfig().getInt("game-settings.sky-squid-button-search-radius", 8);
        final String towerName = SkySquidSetupHelper.getTowerName(tower);

        this.arena.getConfig().setLocation(SkySquidSetupHelper.getSpawnKey(tower), location, false);

        final String safeZoneKey = SkySquidSetupHelper.getSafeZoneKey(tower);

        if (safeZoneKey != null) {
            this.arena.getConfig().setCuboid(safeZoneKey,
                    SkySquidSetupHelper.createTowerZone(location, radius, heightBelow, heightAbove));
        }

        final Location button = SkySquidSetupHelper.findNearbyButton(location, buttonRadius);

        if (button != null) {
            this.arena.getConfig().setLocation(SkySquidSetupHelper.getButtonKey(tower), button, false);
            MessageUtils.send(plugin, player, "setup.sky-squid-tower-set", "{tower}", towerName, "{button}",
                    "auto-found");
        } else {
            MessageUtils.send(plugin, player, "setup.sky-squid-tower-set", "{tower}", towerName, "{button}",
                    "missing — place a button nearby and set the tower again");
        }
    }

    private void scanBridgeMarkers(final Player player, final int bridge) {
        final SquidGame plugin = SquidGame.getInstance();
        final double horizontalPadding = plugin.getMainConfig()
                .getDouble("game-settings.sky-squid-bridge-marker-scan-horizontal", 14.0D);
        final int verticalPadding = plugin.getMainConfig().getInt("game-settings.sky-squid-bridge-marker-scan-vertical", 4);
        final String name = bridge == 1 ? "Square → Triangle bridge" : "Triangle → Circle bridge";
        final List<Vector3> slots = SkySquidSetupHelper.scanBridgeMarkerSlots(this.arena.getConfig(),
                this.arena.getWorld(), bridge, horizontalPadding, verticalPadding);

        MessageUtils.send(plugin, player, "setup.sky-squid-bridge-marker-hint");

        if (slots.isEmpty()) {
            MessageUtils.send(plugin, player, "setup.sky-squid-bridge-empty", "{name}", name);
            return;
        }

        SkySquidSetupHelper.saveBridgeSlots(this.arena.getConfig(), bridge, slots);
        MessageUtils.send(plugin, player, "setup.sky-squid-bridge-ready", "{name}", name, "{count}",
                String.valueOf(slots.size()));
    }
}
