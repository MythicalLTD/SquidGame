package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.games.ArenaGameBase;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "skipgame", usage = "/squid skipgame [arena]", description = "Skip the current game", permission = "squidgame.admin", arguments = {
        String.class }, minArguments = 0)
public class SquidSkipGameCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final Arena arena = this.getTargetArena(plugin, context);

        if (arena == null) {
            MessageUtils.send(plugin, context.getSender(), "setup.arena-not-exist");
            return;
        }

        if (arena.getCurrentGame() == null || !arena.hasNextGame()) {
            MessageUtils.send(plugin, context.getSender(), "commands.skipgame.no-next");
            return;
        }

        final ArenaGameBase nextGame = arena.getNextGame();

        arena.nextGame();
        MessageUtils.send(plugin, context.getSender(), "commands.skipgame.done", "{arena}", arena.getName(), "{game}",
                nextGame.getName());
    }

    @Override
    public List<String> tabComplete(CommandContext context, String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (final Arena arena : ((SquidGame) context.getPlugin()).getArenaManager().getArenas()) {
                completions.add(arena.getName());
            }
        }

        return completions;
    }

    private Arena getTargetArena(final SquidGame plugin, final CommandContext context) {
        if (context.getArguments().size() > 0) {
            return plugin.getArenaManager().getArena(context.getArguments().getString(0));
        }

        if (!plugin.getMainConfig().getBoolean("game-settings.proxy-mode.enabled", false)) {
            return null;
        }

        try {
            return plugin.getArenaManager().getProxyArena();
        } catch (Exception exception) {
            return null;
        }
    }
}
