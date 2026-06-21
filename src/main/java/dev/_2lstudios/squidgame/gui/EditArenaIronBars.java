package dev._2lstudios.squidgame.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.PlayerWand;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;

public final class EditArenaIronBars {

    public static final int ADD_ID = 40;
    public static final int CLEAR_ID = 41;

    private EditArenaIronBars() {
    }

    public static Material buttonMaterial() {
        return CompatibilityUtils.material("IRON_FENCE", "IRON_BARS");
    }

    public static int getCount(final Arena arena, final String configKey) {
        return arena.getConfig().getInt("games." + configKey + ".iron-bar-count", 0);
    }

    public static String addButtonLore(final int count) {
        return "§r\n§7Wand zone containing iron bars.\n§7Bars are removed while the minigame runs.\n§7Zones: §f"
                + count + "\n§r";
    }

    public static String clearButtonLore() {
        return "§r\n§7Remove all iron bar zones for this minigame.\n§r";
    }

    public static boolean handle(final Arena arena, final Player player, final String configKey, final String gameName,
            final int id) {
        if (id == ADD_ID) {
            addRegion(arena, player, configKey, gameName);
            return true;
        }

        if (id == CLEAR_ID) {
            final String base = "games." + configKey;
            arena.getConfig().set(base + ".iron-bars", null);
            arena.getConfig().set(base + ".iron-bar-count", 0);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.iron-bars-cleared", "{name}", gameName);
            return true;
        }

        return false;
    }

    private static void addRegion(final Arena arena, final Player player, final String configKey,
            final String gameName) {
        final SquidPlayer squidPlayer = (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(player);
        final PlayerWand wand = squidPlayer.getWand();
        final String base = "games." + configKey;

        if (wand == null) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.no-wand");
        } else if (!wand.isComplete()) {
            MessageUtils.send(SquidGame.getInstance(), player, "setup.wand-incomplete");
        } else {
            final int nextZone = arena.getConfig().getInt(base + ".iron-bar-count", 0) + 1;
            arena.getConfig().setCuboid(base + ".iron-bars." + nextZone, wand.getCuboid());
            arena.getConfig().set(base + ".iron-bar-count", nextZone);
            MessageUtils.send(SquidGame.getInstance(), player, "setup.iron-bars-added", "{name}", gameName, "{index}",
                    String.valueOf(nextZone), "{first}", wand.getFirstPoint().toString(), "{second}",
                    wand.getSecondPoint().toString());
        }
    }
}
