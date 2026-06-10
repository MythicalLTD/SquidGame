package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaSkySquidGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaSkySquidGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eSquare tower", Material.GOLD_BLOCK,
                "§r\n§7Set at your current location\n§r"), 3, 2);
        this.addItem(1, this.createItem("§eTriangle tower", Material.EMERALD_BLOCK,
                "§r\n§7Set at your current location\n§r"), 5, 2);
        this.addItem(2, this.createItem("§eCircle tower", Material.REDSTONE_BLOCK,
                "§r\n§7Set at your current location\n§r"), 7, 2);
        this.addItem(3, this.createItem("§eSpectator spawn", Material.COMPASS,
                "§r\n§7Dead finalists watch from here\n§7in Adventure mode.\n§r"), 5, 3);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(int id, Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.sky-squid.square-spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Sky Squid square tower");
        } else if (id == 1) {
            this.arena.getConfig().setLocation("games.sky-squid.triangle-spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Sky Squid triangle tower");
        } else if (id == 2) {
            this.arena.getConfig().setLocation("games.sky-squid.circle-spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Sky Squid circle tower");
        } else if (id == 3) {
            this.arena.getConfig().setLocation("games.sky-squid.spectator-spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}",
                    "Sky Squid spectator spawn");
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }
}
