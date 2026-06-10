package dev._2lstudios.squidgame.commands.admin;

import org.bukkit.GameMode;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "gms", usage = "/gms", description = "Switch to survival mode", permission = "squidgame.admin", target = CommandExecutionTarget.ONLY_PLAYER)
public class GamemodeSurvivalCommand extends CommandListener {
    @Override
    public void handle(final CommandContext context) {
        context.getPlayer().setGameMode(GameMode.SURVIVAL);
        context.getPlayer().setFlying(false);
        context.getPlayer().setAllowFlight(false);
        MessageUtils.send((SquidGame) context.getPlugin(), context.getPlayer(), "commands.gamemode.survival");
    }
}
