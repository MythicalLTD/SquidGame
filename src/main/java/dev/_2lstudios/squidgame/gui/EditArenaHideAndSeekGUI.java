package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaHideAndSeekGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaHideAndSeekGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§9Hider spawn", Material.COMPASS, "§r\n§7Set at your current location\n§r"), 3,
                2);
        this.addItem(1, this.createItem("§cSeeker spawn", Material.IRON_SWORD,
                "§r\n§7Set at your current location\n§r"), 5, 2);
        this.addItem(2, this.createItem("§eWaiting lobby", CompatibilityUtils.material("WOODEN_DOOR", "OAK_DOOR"),
                "§r\n§7Use Intermission GUI for waiting room\n§r"), 7, 2);

        final int ironBarCount = EditArenaIronBars.getCount(this.arena, "hide-and-seek");
        this.addItem(EditArenaIronBars.ADD_ID,
                this.createItem("§eAdd iron bar zone", EditArenaIronBars.buttonMaterial(),
                        EditArenaIronBars.addButtonLore(ironBarCount)),
                2, 3);
        this.addItem(EditArenaIronBars.CLEAR_ID,
                this.createItem("§cClear iron bar zones", Material.TNT, EditArenaIronBars.clearButtonLore()), 8, 3);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(int id, Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.hide-and-seek.hider-spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Hide and Seek hider spawn");
        } else if (id == 1) {
            this.arena.getConfig().setLocation("games.hide-and-seek.seeker-spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Hide and Seek seeker spawn");
        } else if (EditArenaIronBars.handle(this.arena, player, "hide-and-seek", "Hide and Seek", id)) {
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }
}
