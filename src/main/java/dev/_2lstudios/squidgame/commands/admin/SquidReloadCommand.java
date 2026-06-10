package dev._2lstudios.squidgame.commands.admin;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "reload", usage = "/squid reload", description = "Reload plugin configuration", permission = "squidgame.admin")
public class SquidReloadCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();

        plugin.reloadPluginConfigs();
        plugin.getMainConfig();
        plugin.getMessagesConfig();
        plugin.getScoreboardConfig();

        MessageUtils.send(plugin, context.getSender(), "commands.reload.done");
    }
}
