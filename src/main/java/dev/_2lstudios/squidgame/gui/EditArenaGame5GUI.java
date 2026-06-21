package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaGame5GUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaGame5GUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eWaiting lobby", CompatibilityUtils.material("WOODEN_DOOR", "OAK_DOOR"),
                "§r\n§7Where players wait during the tutorial\n§r"), 2, 2);
        this.addItem(1, this.createItem("§9Team 1 pull spot", CompatibilityUtils.material("WOOL", "BLUE_WOOL"),
                "§r\n§7Stand where Team 1 players should be locked.\n§7They face Team 2 and can only pull from here.\n§r"),
                4, 2);
        this.addItem(2, this.createItem("§cTeam 2 pull spot", CompatibilityUtils.material("WOOL", "RED_WOOL"),
                "§r\n§7Stand where Team 2 players should be locked.\n§7They face Team 1 and can only pull from here.\n§r"),
                8, 2);

        final int ironBarCount = EditArenaIronBars.getCount(this.arena, "fifth");
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
            this.arena.getConfig().setLocation("games.fifth.lobby", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Tug of War lobby");
        } else if (id == 1) {
            this.arena.getConfig().setLocation("games.fifth.team1", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Tug of War team 1 pull spot");
        } else if (id == 2) {
            this.arena.getConfig().setLocation("games.fifth.team2", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Tug of War team 2 pull spot");
        } else if (EditArenaIronBars.handle(this.arena, player, "fifth", "Tug of War", id)) {
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }
}
