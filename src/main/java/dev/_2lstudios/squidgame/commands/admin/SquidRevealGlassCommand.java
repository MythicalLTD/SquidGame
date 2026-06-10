package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.games.G6GlassesGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "revealglass", usage = "/squid revealglass [arena]", description = "Reveal correct Glass Bridge tiles", permission = "squidgame.admin", arguments = {
        String.class })
public class SquidRevealGlassCommand extends CommandListener {
    @Override
    public void handle(final CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final String arenaName = context.getArguments().getString(0);
        final Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            MessageUtils.send(plugin, context.getSender(), "setup.arena-not-exist");
            return;
        }

        if (!(arena.getCurrentGame() instanceof G6GlassesGame)) {
            MessageUtils.send(plugin, context.getSender(), "commands.revealglass.not-glass");
            return;
        }

        final G6GlassesGame game = (G6GlassesGame) arena.getCurrentGame();
        final List<Block> safeBlocks = game.getSafeBlocks();

        if (safeBlocks.isEmpty()) {
            MessageUtils.send(plugin, context.getSender(), "commands.revealglass.no-tiles");
            return;
        }

        game.revealSafePath();
        MessageUtils.send(plugin, context.getSender(), "commands.revealglass.done", "{arena}", arena.getName(), "{count}",
                String.valueOf(safeBlocks.size()));
    }

    @Override
    public List<String> tabComplete(final CommandContext context, final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (final Arena arena : ((SquidGame) context.getPlugin()).getArenaManager().getArenas()) {
                completions.add(arena.getName());
            }
        }

        return completions;
    }
}
