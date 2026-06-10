package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaGame5GUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaGame5GUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eSpawn point", Material.COMPASS, "§r\n§7Set at your current location\n§r"), 3,
                2);
        this.addItem(1, this.createItem("§eTeam 1 platform", Material.IRON_BLOCK,
                "§r\n§7Set at one end of the rope.\n§7Players face Team 2 from here.\n§r"), 5, 2);
        this.addItem(2, this.createItem("§eTeam 2 platform", Material.IRON_BLOCK,
                "§r\n§7Set at the other end of the rope.\n§7Players face Team 1 from here.\n§r"), 7, 2);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(int id, Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.fifth.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Tug of War spawn");
        } else if (id == 1) {
            this.arena.getConfig().setLocation("games.fifth.team1", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Tug of War team 1 platform");
        } else if (id == 2) {
            this.arena.getConfig().setLocation("games.fifth.team2", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Tug of War team 2 platform");
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }
}
