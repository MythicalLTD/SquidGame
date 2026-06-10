package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "stoparena", usage = "/squid stoparena [arena]", description = "Stop and reset an arena", permission = "squidgame.admin", arguments = {
        String.class })
public class SquidStopArenaCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final String arenaName = context.getArguments().getString(0);
        final Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            MessageUtils.send(plugin, context.getSender(), "setup.arena-not-exist");
            return;
        }

        arena.resetArena();
        MessageUtils.send(plugin, context.getSender(), "commands.stoparena.done", "{arena}", arena.getName());
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
}
