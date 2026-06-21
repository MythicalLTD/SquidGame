package dev._2lstudios.squidgame.gui;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.PlayerWand;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaGame6GUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaGame6GUI(final Arena arena, final InventoryGUI prevGui) {
        super("§d§lArena §f" + arena.getName(), 45, prevGui);
        this.arena = arena;
    }

    @Override
    public void init() {
        this.addItem(0, this.createItem("§eWaiting lobby", CompatibilityUtils.material("WOODEN_DOOR", "OAK_DOOR"),
                "§r\n§7Where players wait during the tutorial\n§r"), 2, 2);
        this.addItem(1, this.createItem("§eBridge start", Material.COMPASS,
                "§r\n§7Where players teleport when the game begins\n§r"), 5, 2);
        this.addItem(2, this.createItem("§eBridge zone", Material.GLASS,
                "§r\n§7Region containing the pre-built glass bridge.\n§7Place glass panels in the world first, then set this zone with your wand.\n§r"), 8, 2);
        this.addItem(3, this.createItem("§eGoal Zone", Material.ARMOR_STAND, "§r\n§7Set with your location wand\n§r"),
                5, 3);

        final int ironBarCount = EditArenaIronBars.getCount(this.arena, "sixth");
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
            this.arena.getConfig().setLocation("games.sixth.lobby", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Glass Bridge lobby");
        } else if (id == 1) {
            this.arena.getConfig().setLocation("games.sixth.spawn", player.getLocation(), false);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.location-set", "{name}", "Glass Bridge start");
        } else if (EditArenaIronBars.handle(this.arena, player, "sixth", "Glass Bridge", id)) {
        }

        else if (id == 2 || id == 3) {
            final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
            final PlayerWand wand = squidPlayer.getWand();

            if (wand == null) {
                MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
            } else if (!wand.isComplete()) {
                MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
            } else {

                if (id == 2) {
                    this.arena.getConfig().setCuboid("games.sixth.glass", wand.getCuboid());
                    MessageUtils.send(SquidGame.getInstance(), player, "setup.region-set", "{name}",
                            "Glass Bridge blocks", "{first}", wand.getFirstPoint().toString(), "{second}",
                            wand.getSecondPoint().toString());
                }

                else if (id == 3) {
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
