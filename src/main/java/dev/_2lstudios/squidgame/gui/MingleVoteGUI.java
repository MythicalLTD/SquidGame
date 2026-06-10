package dev._2lstudios.squidgame.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.games.G8MingleGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class MingleVoteGUI extends InventoryGUI {
    private static final int PLAY = 1;
    private static final int SKIP = 2;

    private final G8MingleGame game;

    public MingleVoteGUI(final G8MingleGame game) {
        super(MessageUtils.format(SquidGame.getInstance(), "games.mingle.vote.gui-title"), 9);
        this.game = game;
    }

    @Override
    public void init() {
        final SquidGame plugin = SquidGame.getInstance();

        this.addItem(PLAY,
                this.createItem(MessageUtils.format(plugin, "games.mingle.vote.play.name"), Material.EMERALD_BLOCK,
                        MessageUtils.format(plugin, "games.mingle.vote.play.lore")),
                3, 1);
        this.addItem(SKIP,
                this.createItem(MessageUtils.format(plugin, "games.mingle.vote.skip.name"), Material.REDSTONE_BLOCK,
                        MessageUtils.format(plugin, "games.mingle.vote.skip.lore")),
                7, 1);
    }

    @Override
    public void handle(final int id, final Player player) {
        if (id == SKIP) {
            this.game.vote(player, true);
        } else if (id == PLAY) {
            this.game.vote(player, false);
        }

        this.close(player);
    }
}
