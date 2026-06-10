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

public class EditArenaGame6GUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaGame6GUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eSpawn point", Material.COMPASS, "§r\n§7Set at your current location\n§r"), 3,
                2);
        this.addItem(1, this.createItem("§eGlass Bridge Blocks", Material.GLASS,
                "§r\n§7Build the bridge with glass first.\n§7Select all bridge glass with your wand.\n§r"), 5, 2);
        this.addItem(2, this.createItem("§eGoal Zone", Material.ARMOR_STAND, "§r\n§7Set with your location wand\n§r"),
                7, 2);

        this.addItem(99, this.createItem("§cBack", Material.BARRIER), 5, 4);
    }

    @Override
    public void handle(int id, Player player) {
        if (id == 99) {
            this.back(player);
            return;
        } else if (id == 0) {
            this.arena.getConfig().setLocation("games.sixth.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Glass Bridge spawn");
        }

        else if (id == 1 || id == 2) {
            final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
            final PlayerWand wand = squidPlayer.getWand();

            if (wand == null) {
                MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
            } else if (!wand.isComplete()) {
                MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
            } else {

                if (id == 1) {
                    this.arena.getConfig().setCuboid("games.sixth.glass", wand.getCuboid());
                    MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}",
                            "Glass Bridge blocks", "{first}", wand.getFirstPoint().toString(), "{second}",
                            wand.getSecondPoint().toString());
                }

                else if (id == 2) {
                    this.arena.getConfig().setCuboid("games.sixth.goal", wand.getCuboid());
                    MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}",
                            "Glass Bridge goal zone", "{first}", wand.getFirstPoint().toString(), "{second}",
                            wand.getSecondPoint().toString());
                }
            }
        }

        try {
            this.arena.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.close(player);
    }
}
