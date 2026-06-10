package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.PlayerWand;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaJumpRopeGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaJumpRopeGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eSpawn point", Material.COMPASS, "§r\n§7Set at your current location\n§r"), 2,
                2);
        this.addItem(1, this.createItem("§eBridge zone", Material.IRON_BLOCK, "§r\n§7Set with your location wand\n§r"), 4, 2);
        this.addItem(2, this.createItem("§eVoid / gap zone", Material.BEDROCK, "§r\n§7Set with your location wand\n§r"), 6,
                2);
        this.addItem(3, this.createItem("§eGoal zone", Material.ARMOR_STAND, "§r\n§7Set with your location wand\n§r"),
                8, 2);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(int id, Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.jump-rope.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Jump Rope spawn");
        } else {
            this.setRegion(player, id);
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }

    private void setRegion(final Player player, final int id) {
        final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
        final PlayerWand wand = squidPlayer.getWand();

        if (wand == null) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
        } else if (!wand.isComplete()) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
        } else {
            String key = "games.jump-rope.";
            String name = "Jump Rope";

            if (id == 1) {
                key += "bridge";
                name += " bridge zone";
            } else if (id == 2) {
                key += "void";
                name += " void zone";
            } else if (id == 3) {
                key += "goal";
                name += " goal zone";
            }

            this.arena.getConfig().setCuboid(key, wand.getCuboid());
            MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}", name, "{first}",
                    wand.getFirstPoint().toString(), "{second}", wand.getSecondPoint().toString());
        }
    }
}
