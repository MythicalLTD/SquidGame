package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;
import dev._2lstudios.squidgame.utils.MinigameLocationUtils;

@Command(name = "tpminigame", usage = "/squid tpminigame [arena] [game]", description = "Teleport to a minigame area", permission = "squidgame.admin", target = CommandExecutionTarget.ONLY_PLAYER, arguments = {
        String.class, String.class }, minArguments = 2)
public class SquidTpMinigameCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final SquidPlayer player = (SquidPlayer) context.getPluginPlayer();

        if (player.getArena() != null) {
            player.sendMessage("arena.already-in-game");
            return;
        }

        final String arenaName = context.getArguments().getString(0);
        final String gameInput = context.getArguments().getString(1);
        final Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            MessageUtils.send(plugin, context.getSender(), "setup.arena-not-exist");
            return;
        }

        final String gameKey = MinigameLocationUtils.resolveGameKey(gameInput);

        if (gameKey == null) {
            MessageUtils.send(plugin, context.getSender(), "setup.unknown-game", "{game}", gameInput);
            return;
        }

        final Location destination = MinigameLocationUtils.resolveLocation(arena, gameKey);

        if (destination == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.tpminigame.not-configured", "{arena}",
                    arena.getName(), "{game}", gameInput);
            return;
        }

        player.teleport(destination);
        MessageUtils.send(plugin, context.getSender(), "commands.tpminigame.done", "{arena}", arena.getName(), "{game}",
                gameInput);
    }

    @Override
    public List<String> tabComplete(CommandContext context, String[] args) {
        final List<String> completions = new ArrayList<>();
        final SquidGame plugin = (SquidGame) context.getPlugin();

        if (args.length == 1) {
            for (final Arena arena : plugin.getArenaManager().getArenas()) {
                completions.add(arena.getName());
            }
        } else if (args.length == 2) {
            for (final String gameKey : MinigameLocationUtils.GAME_ALIASES) {
                completions.add(gameKey);
            }
        }

        return completions;
    }
}
