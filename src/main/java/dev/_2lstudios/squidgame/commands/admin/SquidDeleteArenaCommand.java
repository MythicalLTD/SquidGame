package dev._2lstudios.squidgame.commands.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.jelly.errors.CommandException;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "deletearena", usage = "/squid deletearena [arena]", description = "Delete an arena", permission = "squidgame.admin", arguments = {
        String.class })
public class SquidDeleteArenaCommand extends CommandListener {
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

    @Override
    public void handle(CommandContext context) throws IOException, CommandException {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final String arenaName = context.getArguments().getString(0);

        if (plugin.getArenaManager().deleteArena(arenaName)) {
            MessageUtils.send(plugin, context.getSender(), "commands.deletearena.deleted", "{arena}", arenaName);
        } else {
            MessageUtils.send(plugin, context.getSender(), "setup.arena-not-exist");
        }
    }
}
