package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaFinalDinnerGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaFinalDinnerGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eDinner spawn", Material.CAKE,
                "§r\n§7Set at your current location\n§r"), 5, 2);

        final int ironBarCount = EditArenaIronBars.getCount(this.arena, "final-dinner");
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
            this.arena.getConfig().setLocation("games.final-dinner.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Final Dinner spawn");
        } else if (EditArenaIronBars.handle(this.arena, player, "final-dinner", "Final Dinner", id)) {
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }
}
