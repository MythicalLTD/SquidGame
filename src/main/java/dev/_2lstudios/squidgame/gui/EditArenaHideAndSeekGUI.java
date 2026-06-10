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

public class EditArenaHideAndSeekGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaHideAndSeekGUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eHider spawn", Material.COMPASS, "§r\n§7Set at your current location\n§r"), 3,
                2);
        this.addItem(1, this.createItem("§eSeeker spawn", Material.IRON_SWORD,
                "§r\n§7Set at your current location\n§r"), 5, 2);
        this.addItem(2, this.createItem("§eExit zone", Material.IRON_DOOR,
                "§r\n§7Set with your location wand\n§r"), 7, 2);

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
        } else if (id == 2) {
            this.setExitZone(player);
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }

    private void setExitZone(final Player player) {
        final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
        final PlayerWand wand = squidPlayer.getWand();

        if (wand == null) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
        } else if (!wand.isComplete()) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
        } else {
            this.arena.getConfig().setCuboid("games.hide-and-seek.exit", wand.getCuboid());
            MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}", "Hide and Seek exit zone",
                    "{first}", wand.getFirstPoint().toString(), "{second}", wand.getSecondPoint().toString());
        }
    }
}
