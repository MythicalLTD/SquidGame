package dev._2lstudios.squidgame.commands.admin;

import java.util.List;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "listarena", usage = "/squid listarena", description = "List loaded arenas", permission = "squidgame.admin")
public class SquidListArenaCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final List<Arena> arenas = plugin.getArenaManager().getArenas();

        if (arenas.isEmpty()) {
            MessageUtils.send(plugin, context.getSender(), "commands.listarena.empty");
            return;
        }

        MessageUtils.send(plugin, context.getSender(), "commands.listarena.header", "{count}",
                String.valueOf(arenas.size()));

        for (final Arena arena : arenas) {
            MessageUtils.send(plugin, context.getSender(), "commands.listarena.entry", "{arena}", arena.getName(),
                    "{state}", arena.getState().toString(), "{players}", String.valueOf(arena.getPlayers().size()),
                    "{max}", String.valueOf(arena.getMaxPlayers()));
        }
    }
}
