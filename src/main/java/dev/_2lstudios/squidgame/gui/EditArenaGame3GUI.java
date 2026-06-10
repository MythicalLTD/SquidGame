package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaGame3GUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaGame3GUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eBattle spawn", Material.DIAMOND_SWORD,
                "§r\n§7Set where players spawn for Battle.\n§7If unset, intermission lobby is used.\n§r"), 5, 2);
        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(final int id, final Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.third.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Battle spawn");
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        this.close(player);
    }
}
