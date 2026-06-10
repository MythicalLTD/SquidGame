package dev._2lstudios.squidgame.commands.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.jelly.errors.CommandException;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.gui.EditArenaFinalDinnerGUI;
import dev._2lstudios.squidgame.gui.EditArenaGame1GUI;
import dev._2lstudios.squidgame.gui.EditArenaGame3GUI;
import dev._2lstudios.squidgame.gui.EditArenaGame4GUI;
import dev._2lstudios.squidgame.gui.EditArenaGame5GUI;
import dev._2lstudios.squidgame.gui.EditArenaGame6GUI;
import dev._2lstudios.squidgame.gui.EditArenaGUI;
import dev._2lstudios.squidgame.gui.EditArenaHideAndSeekGUI;
import dev._2lstudios.squidgame.gui.EditArenaJumpRopeGUI;
import dev._2lstudios.squidgame.gui.EditArenaMingleGUI;
import dev._2lstudios.squidgame.gui.EditArenaSkySquidGUI;
import dev._2lstudios.squidgame.gui.EditArenaWaitingLobbyGUI;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;
import dev._2lstudios.jelly.gui.InventoryGUI;

@Command(name = "editarena", usage = "/squid editarena [arena] [game]", description = "Edit an arena", permission = "squidgame.admin", target = CommandExecutionTarget.ONLY_PLAYER, arguments = {
        String.class, String.class }, minArguments = 1)
public class SquidEditArenaCommand extends CommandListener {
    private static final String[] GAME_KEYS = new String[] { "lobby", "waiting", "intermission", "1", "redlight",
            "redgreenlight", "red-light-green-light", "3", "battle", "4", "marbles", "5", "tug", "tugofwar",
            "tug-of-war", "6", "glass", "glasses", "glassbridge", "glass-bridge", "8", "mingle", "10",
            "hideandseek", "hide-and-seek", "11", "jumprope", "jump-rope", "12", "finaldinner",
            "final-dinner", "13", "skysquid", "sky-squid" };

    @Override
    public List<String> tabComplete(CommandContext context, String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (final Arena arena : ((SquidGame) context.getPlugin()).getArenaManager().getArenas()) {
                completions.add(arena.getName());
            }
        } else if (args.length == 2) {
            for (final String gameKey : GAME_KEYS) {
                completions.add(gameKey);
            }
        }

        return completions;
    }

    @Override
    public void handle(CommandContext context) throws IOException, CommandException {
        final String arenaName = context.getArguments().getString(0);
        final Arena arena = ((SquidGame) context.getPlugin()).getArenaManager().getArena(arenaName);

        if (arena == null) {
            final SquidPlayer player = (SquidPlayer) context.getPluginPlayer();
            player.sendMessage("setup.arena-not-exist");
        } else {
            final EditArenaGUI mainGui = new EditArenaGUI(arena);

            if (context.getArguments().size() < 2) {
                mainGui.open(context.getPlayer());
                return;
            }

            final InventoryGUI gameGui = this.createGameGui(arena, mainGui, context.getArguments().getString(1));

            if (gameGui == null) {
                MessageUtils.send((SquidGame) context.getPlugin(), context.getPlayer(), "setup.unknown-game", "{game}",
                        context.getArguments().getString(1));
                return;
            }

            gameGui.open(context.getPlayer());
        }
    }

    private InventoryGUI createGameGui(final Arena arena, final EditArenaGUI mainGui, final String game) {
        final String normalized = game.toLowerCase().replace("_", "-").replace(" ", "-");

        switch (normalized) {
        case "lobby":
        case "waiting":
        case "intermission":
            return new EditArenaWaitingLobbyGUI(arena, mainGui);
        case "1":
        case "redlight":
        case "redgreenlight":
        case "red-light-green-light":
            return new EditArenaGame1GUI(arena, mainGui);
        case "3":
        case "battle":
            return new EditArenaGame3GUI(arena, mainGui);
        case "4":
        case "marbles":
            return new EditArenaGame4GUI(arena, mainGui);
        case "5":
        case "tug":
        case "tugofwar":
        case "tug-of-war":
            return new EditArenaGame5GUI(arena, mainGui);
        case "6":
        case "glass":
        case "glasses":
        case "glassbridge":
        case "glass-bridge":
            return new EditArenaGame6GUI(arena, mainGui);
        case "8":
        case "mingle":
            return new EditArenaMingleGUI(arena, mainGui);
        case "10":
        case "hideandseek":
        case "hide-and-seek":
            return new EditArenaHideAndSeekGUI(arena, mainGui);
        case "11":
        case "jumprope":
        case "jump-rope":
            return new EditArenaJumpRopeGUI(arena, mainGui);
        case "12":
        case "finaldinner":
        case "final-dinner":
            return new EditArenaFinalDinnerGUI(arena, mainGui);
        case "13":
        case "skysquid":
        case "sky-squid":
            return new EditArenaSkySquidGUI(arena, mainGui);
        default:
            return null;
        }
    }
}
