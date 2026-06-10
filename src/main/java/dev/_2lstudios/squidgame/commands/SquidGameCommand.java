package dev._2lstudios.squidgame.commands;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.commands.admin.SquidCreateArenaCommand;
import dev._2lstudios.squidgame.commands.admin.SquidDeleteArenaCommand;
import dev._2lstudios.squidgame.commands.admin.SquidListArenaCommand;
import dev._2lstudios.squidgame.commands.admin.SquidReloadCommand;
import dev._2lstudios.squidgame.commands.admin.SquidRevealGlassCommand;
import dev._2lstudios.squidgame.commands.admin.SquidReviveCommand;
import dev._2lstudios.squidgame.commands.admin.SquidSetLobbyCommand;
import dev._2lstudios.squidgame.commands.admin.SquidEditArenaCommand;
import dev._2lstudios.squidgame.commands.admin.SquidSetTimeCommand;
import dev._2lstudios.squidgame.commands.admin.SquidSkipGameCommand;
import dev._2lstudios.squidgame.commands.admin.SquidStopArenaCommand;
import dev._2lstudios.squidgame.commands.admin.SquidTrollCommand;
import dev._2lstudios.squidgame.commands.admin.SquidWandCommand;
import dev._2lstudios.squidgame.commands.game.SquidJoinCommand;
import dev._2lstudios.squidgame.commands.game.SquidLeaveCommand;
import dev._2lstudios.squidgame.commands.game.SquidStartCommand;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "squidgame")
public class SquidGameCommand extends CommandListener {
    public SquidGameCommand() {
        // Admin commands
        this.addSubcommand(new SquidCreateArenaCommand());
        this.addSubcommand(new SquidDeleteArenaCommand());
        this.addSubcommand(new SquidListArenaCommand());
        this.addSubcommand(new SquidSetLobbyCommand());
        this.addSubcommand(new SquidEditArenaCommand());
        this.addSubcommand(new SquidWandCommand());
        this.addSubcommand(new SquidReviveCommand());
        this.addSubcommand(new SquidReloadCommand());
        this.addSubcommand(new SquidStopArenaCommand());
        this.addSubcommand(new SquidSkipGameCommand());
        this.addSubcommand(new SquidSetTimeCommand());
        this.addSubcommand(new SquidTrollCommand());
        this.addSubcommand(new SquidRevealGlassCommand());

        // Game commands
        this.addSubcommand(new SquidLeaveCommand());
        this.addSubcommand(new SquidJoinCommand());
        this.addSubcommand(new SquidStartCommand());
    }

    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();

        MessageUtils.send(plugin, context.getSender(), "commands.help.header");

        for (final CommandListener subcommand : this.getSubcommands()) {
            final Command command = subcommand.getClass().getAnnotation(Command.class);
            final String description = MessageUtils.format(plugin, "commands.descriptions." + command.name());
            MessageUtils.send(plugin, context.getSender(), "commands.help.entry", "{name}", command.name(), "{usage}",
                    command.usage(), "{description}", description);
        }
    }
}
