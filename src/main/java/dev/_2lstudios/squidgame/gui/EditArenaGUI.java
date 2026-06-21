package dev._2lstudios.squidgame.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class EditArenaGUI extends InventoryGUI {

    private final Arena arena;

    public EditArenaGUI(final Arena arena) {
        super("§d§lArena §f" + arena.getName(), 5 * 9);
        this.arena = arena;
    }

    @Override
    public void init() {
        final SquidGame plugin = SquidGame.getInstance();

        this.addItem(1,
                this.createItem(MessageUtils.format(plugin, "games.first.name"), Material.ENDER_PEARL,
                        "§r\n§aGreen Light§7, §cRed Light §7game.\n§r"),
                2, 2);

        this.addItem(3, this.createItem(MessageUtils.format(plugin, "games.third.name"), Material.DIAMOND_SWORD,
                "§r\n§8Lights Off §7spawn setup.\n§r"),
                3, 2);
        this.addItem(5, this.createItem(MessageUtils.format(plugin, "games.fifth.name"), Material.STRING,
                "§r\n§eTug of War §7game.\n§r"), 5, 2);

        this.addItem(6, this.createItem(MessageUtils.format(plugin, "games.sixth.name"), Material.GLASS,
                "§r\n§bGlass §fGame§7.\n§r"), 6, 2);
        this.addItem(8, this.createItem(MessageUtils.format(plugin, "games.mingle.name"), Material.EMERALD,
                "§r\n§dMingle §7game.\n§r"), 7, 2);
        this.addItem(10, this.createItem(MessageUtils.format(plugin, "games.hide-and-seek.name"), Material.IRON_SWORD,
                "§r\n§9Hide and Seek §7game.\n§r"), 3, 3);
        this.addItem(12, this.createItem(MessageUtils.format(plugin, "games.final-dinner.name"), Material.CAKE,
                "§r\n§6Final Dinner §7interlude.\n§r"), 5, 3);
        this.addItem(13, this.createItem(MessageUtils.format(plugin, "games.sky-squid.name"), Material.GOLD_BLOCK,
                "§r\n§6Sky Squid Game §7final.\n§r"), 6, 3);
        this.addItem(0,
                this.createItem("§bIntermission", Material.COMPASS, "§r\n§7Where players spawn after each game.\n§r"),
                4, 4);
        this.addItem(99, this.createItem("§cExit", Material.BARRIER), 6, 4);
    }

    @Override
    public void handle(int id, Player player) {
        switch (id) {
        case 0:
            new EditArenaWaitingLobbyGUI(this.arena, this).open(player);
            break;
        case 1:
            new EditArenaGame1GUI(this.arena, this).open(player);
            break;
        case 3:
            new EditArenaGame3GUI(this.arena, this).open(player);
            break;
        case 5:
            new EditArenaGame5GUI(this.arena, this).open(player);
            break;
        case 6:
            new EditArenaGame6GUI(this.arena, this).open(player);
            break;
        case 8:
            new EditArenaMingleGUI(this.arena, this).open(player);
            break;
        case 10:
            new EditArenaHideAndSeekGUI(this.arena, this).open(player);
            break;
        case 12:
            new EditArenaFinalDinnerGUI(this.arena, this).open(player);
            break;
        case 13:
            new EditArenaSkySquidGUI(this.arena, this).open(player);
            break;
        default:
            this.close(player);
            break;
        }
    }
}
