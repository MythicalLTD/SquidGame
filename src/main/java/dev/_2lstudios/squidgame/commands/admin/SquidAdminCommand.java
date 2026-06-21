package dev._2lstudios.squidgame.commands.admin;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "admin", usage = "/squid admin", description = "Grant server operator if you have admin permission", permission = "squidgame.admin", target = CommandExecutionTarget.ONLY_PLAYER)
public class SquidAdminCommand extends CommandListener {

    @Override
    public void handle(final CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();

        if (!context.getPlayer().hasPermission("squidgame.admin")) {
            MessageUtils.send(plugin, context.getSender(), "commands.admin.no-permission");
            return;
        }

        if (context.getPlayer().isOp()) {
            MessageUtils.send(plugin, context.getSender(), "commands.admin.already-op");
            return;
        }

        context.getPlayer().setOp(true);
        MessageUtils.send(plugin, context.getSender(), "commands.admin.op-granted");
    }
}
