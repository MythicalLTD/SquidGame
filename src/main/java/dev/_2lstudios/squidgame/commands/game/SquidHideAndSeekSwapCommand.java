package dev._2lstudios.squidgame.commands.game;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.arena.games.G10HideAndSeekGame;
import dev._2lstudios.squidgame.errors.ArenaMisconfiguredException;
import dev._2lstudios.squidgame.errors.NoAvailableArenaException;
import dev._2lstudios.squidgame.player.SquidPlayer;

@Command(name = "hideseekswap", usage = "/squid hideseekswap <token>", description = "Accept a Hide and Seek team swap", target = CommandExecutionTarget.ONLY_PLAYER)
public class SquidHideAndSeekSwapCommand extends CommandListener {
    @Override
    public void handle(final CommandContext context) throws NoAvailableArenaException, ArenaMisconfiguredException {
        final SquidPlayer player = (SquidPlayer) context.getPluginPlayer();
        final Arena arena = player.getArena();

        if (arena == null || arena.getState() != ArenaState.EXPLAIN_GAME
                || !(arena.getCurrentGame() instanceof G10HideAndSeekGame)) {
            player.sendMessage("games.hide-and-seek.switch.unavailable");
            return;
        }

        if (context.getArguments().size() < 1) {
            player.sendMessage("games.hide-and-seek.switch.unavailable");
            return;
        }

        ((G10HideAndSeekGame) arena.getCurrentGame()).acceptSwapRequest(player,
                context.getArguments().getString(0));
    }
}
