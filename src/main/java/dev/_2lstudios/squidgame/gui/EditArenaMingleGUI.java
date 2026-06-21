package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.math.Cuboid;
import dev._2lstudios.jelly.math.Vector3;
import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.PlayerWand;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaMingleGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaMingleGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 54, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        final int roomCount = this.arena.getConfig().getInt("games.mingle.room-count", 0);

        this.addItem(0, this.createItem("§eCenter platform spawn", Material.COMPASS,
                "§r\n§7Set at your current location\n§7Platform blocks: §6orange terracotta§7, §aacacia§7 wood,\n§b§7diamond block§7, §elit lanterns§7\n§r"), 2, 1);
        this.addItem(1, this.createItem("§ePlatform zone (optional)", Material.IRON_BLOCK,
                "§r\n§7Legacy wand region — detection uses platform blocks now\n§r"), 4, 1);
        this.addItem(2, this.createItem("§eAdd room region", Material.IRON_DOOR,
                "§r\n§7Set with your location wand\n§7Room floors: §alime§7, §eyellow§7, §fwhite§7, §cred§7, §9blue§7, §5purple\n§7Current rooms: §f"
                        + roomCount + "\n§r"), 6, 1);
        this.addItem(3, this.createItem("§eSet door for last room", CompatibilityUtils.material("WOODEN_DOOR", "OAK_DOOR"),
                "§r\n§7Look at a door block and click.\n§7Assigns it to room §f" + roomCount + "\n§r"), 2, 2);
        this.addItem(4, this.createItem("§aQuick add room", Material.EMERALD,
                "§r\n§7Stand in the middle of a room.\n§7Creates a room around you.\n§7Current rooms: §f"
                        + roomCount + "\n§r"),
                4, 2);
        this.addItem(5, this.createItem("§cClear room regions", Material.TNT,
                "§r\n§7Remove all configured Mingle rooms\n§r"), 6, 2);

        final int ironBarCount = EditArenaIronBars.getCount(this.arena, "mingle");
        this.addItem(EditArenaIronBars.ADD_ID,
                this.createItem("§eAdd iron bar zone", EditArenaIronBars.buttonMaterial(),
                        EditArenaIronBars.addButtonLore(ironBarCount)),
                2, 3);
        this.addItem(EditArenaIronBars.CLEAR_ID,
                this.createItem("§cClear iron bar zones", Material.TNT, EditArenaIronBars.clearButtonLore()), 6, 3);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(int id, Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.mingle.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Mingle center platform spawn");
        } else if (id == 1) {
            this.setPlatform(player);
        } else if (id == 2) {
            this.addRoom(player);
        } else if (id == 3) {
            this.setDoor(player);
        } else if (id == 4) {
            this.addQuickRoom(player);
        } else if (id == 5) {
            this.arena.getConfig().set("games.mingle.rooms", null);
            this.arena.getConfig().set("games.mingle.room-count", 0);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.rooms-cleared", "{name}", "Mingle");
        } else if (EditArenaIronBars.handle(this.arena, player, "mingle", "Mingle", id)) {
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }

    private void setPlatform(final Player player) {
        final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
        final PlayerWand wand = squidPlayer.getWand();

        if (wand == null) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
        } else if (!wand.isComplete()) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
        } else {
            this.arena.getConfig().setCuboid("games.mingle.platform", wand.getCuboid());
            MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}", "Mingle platform",
                    "{first}", wand.getFirstPoint().toString(), "{second}", wand.getSecondPoint().toString());
        }
    }

    private void setDoor(final Player player) {
        final int roomCount = this.arena.getConfig().getInt("games.mingle.room-count", 0);

        if (roomCount <= 0) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.no-rooms");
            return;
        }

        final Location target = player.getTargetBlockExact(5) != null
                ? player.getTargetBlockExact(5).getLocation()
                : player.getLocation().getBlock().getLocation();
        this.arena.getConfig().setLocation("games.mingle.rooms." + roomCount + ".door", target, false);
        MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                "Mingle room " + roomCount + " door");
    }

    private void addRoom(final Player player) {
        final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
        final PlayerWand wand = squidPlayer.getWand();

        if (wand == null) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
        } else if (!wand.isComplete()) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
        } else {
            final int nextRoom = this.arena.getConfig().getInt("games.mingle.room-count", 0) + 1;
            this.arena.getConfig().setCuboid("games.mingle.rooms." + nextRoom, wand.getCuboid());
            this.arena.getConfig().set("games.mingle.room-count", nextRoom);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}", "Mingle room "
                    + nextRoom, "{first}", wand.getFirstPoint().toString(), "{second}", wand.getSecondPoint().toString());
        }
    }

    private void addQuickRoom(final Player player) {
        final SquidGame plugin = SquidGame.getInstance();
        final Location location = player.getLocation();
        final int radius = plugin.getMainConfig().getInt("game-settings.mingle-quick-room-radius", 3);
        final int height = plugin.getMainConfig().getInt("game-settings.mingle-quick-room-height", 4);
        final Vector3 firstPoint = new Vector3(location.getBlockX() - radius, location.getBlockY() - 1,
                location.getBlockZ() - radius);
        final Vector3 secondPoint = new Vector3(location.getBlockX() + radius, location.getBlockY() + height,
                location.getBlockZ() + radius);
        final int nextRoom = this.arena.getConfig().getInt("games.mingle.room-count", 0) + 1;

        this.arena.getConfig().setCuboid("games.mingle.rooms." + nextRoom, new Cuboid(firstPoint, secondPoint));
        this.arena.getConfig().set("games.mingle.room-count", nextRoom);
        MessageUtils.send(plugin, player, "setup.region-set", "{name}", "Mingle room " + nextRoom, "{first}",
                firstPoint.toString(), "{second}", secondPoint.toString());
    }
}
