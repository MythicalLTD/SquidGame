package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "settime", usage = "/squid settime [arena] [seconds]", description = "Set an arena timer", permission = "squidgame.admin", arguments = {
        String.class, Integer.class })
public class SquidSetTimeCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final String arenaName = context.getArguments().getString(0);
        final int seconds = context.getArguments().getInt(1);
        final Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            MessageUtils.send(plugin, context.getSender(), "setup.arena-not-exist");
            return;
        }

        arena.setInternalTime(seconds);
        MessageUtils.send(plugin, context.getSender(), "commands.settime.done", "{arena}", arena.getName(), "{time}",
                String.valueOf(seconds));
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
