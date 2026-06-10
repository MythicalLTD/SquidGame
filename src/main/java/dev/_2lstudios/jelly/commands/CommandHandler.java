package dev._2lstudios.jelly.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.JellyPlugin;
import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.errors.CommandException;
import dev._2lstudios.jelly.errors.I18nCommandException;
import dev._2lstudios.jelly.utils.ArrayUtils;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Map<String, CommandListener> commands;
    private final JellyPlugin plugin;

    public CommandHandler(final JellyPlugin plugin) {
        this.commands = new HashMap<>();
        this.plugin = plugin;
    }

    public void addCommand(final CommandListener listener) {
        if (listener.getClass().isAnnotationPresent(Command.class)) {
            Command command = listener.getClass().getAnnotation(Command.class);
            this.commands.put(command.name(), listener);
            this.plugin.getCommand(command.name()).setExecutor(this);
            this.plugin.getCommand(command.name()).setTabCompleter(this);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmdInfo, String alias,
            String[] args) {
        CommandListener listener = this.commands.get(cmdInfo.getName().toLowerCase());

        if (listener == null) {
            return Collections.emptyList();
        }

        int consumedArguments = 0;

        while (consumedArguments < args.length - 1) {
            final CommandListener subCommand = listener.getSubcommand(args[consumedArguments]);

            if (subCommand == null) {
                break;
            }

            listener = subCommand;
            consumedArguments++;
        }

        final String[] remainingArgs = Arrays.copyOfRange(args, consumedArguments, args.length);

        if (!listener.getSubcommands().isEmpty() && remainingArgs.length == 1) {
            return this.filterSubcommands(sender, listener, remainingArgs[0]);
        }

        final CommandArguments arguments = new CommandArguments(remainingArgs);
        final CommandContext context = new CommandContext(this.plugin, sender, arguments);
        return this.filterSuggestions(listener.tabComplete(context, remainingArgs), remainingArgs);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmdInfo, String label, String[] args) {
        String name = cmdInfo.getName().toLowerCase();
        CommandListener listener = this.commands.get(name);

        while (args.length > 0) {
            CommandListener subCommand = listener.getSubcommand(args[0]);
            if (subCommand != null) {
                listener = subCommand;
                args = ArrayUtils.shift(args, 0);
            } else {
                break;
            }
        }

        Command command = listener.getClass().getAnnotation(Command.class);

        // Check execution target
        if (sender instanceof Player && command.target() == CommandExecutionTarget.ONLY_CONSOLE) {
            sender.sendMessage("§cThis command can run only in console.");
            return true;
        } else if (sender instanceof ConsoleCommandSender && command.target() == CommandExecutionTarget.ONLY_PLAYER) {
            sender.sendMessage("§cThis command can run only by a player.");
            return true;
        }

        // Check for permission
        if (!command.permission().isEmpty() && !sender.hasPermission(command.permission())) {
            sender.sendMessage("§cMissing permission " + command.permission());
            return true;
        }

        // Check for min arguments
        int minArguments = command.minArguments() == Integer.MIN_VALUE ? command.arguments().length
                : command.minArguments();

        if (args.length < minArguments) {
            sender.sendMessage(command.usage());
            return true;
        }

        // Parse arguments
        final Object[] argList = new Object[args.length];
        final int argumentDefinedLength = command.arguments().length;

        for (int i = 0; i < args.length; i++) {
            if (argumentDefinedLength >= (i + 1)) {
                final Class<?> clazz = command.arguments()[i];
                try {
                    final Object arg = CommandArgumentParser.parse(clazz, i + 1, args[i]);
                    argList[i] = arg;
                } catch (Exception e) {
                    sender.sendMessage("§cUsage: " + command.usage());
                    sender.sendMessage("§c" + e.getMessage());
                    return true;
                }
            } else {
                argList[i] = args[i];
            }
        }

        // Execute command
        final CommandArguments arguments = new CommandArguments(argList);
        final CommandContext context = new CommandContext(this.plugin, sender, arguments);

        try {
            listener.handle(context);
        } catch (Exception e) {
            if (e instanceof I18nCommandException && sender instanceof Player) {
                final SquidPlayer player = (SquidPlayer) context.getPluginPlayer();
                final I18nCommandException i18nE = (I18nCommandException) e;
                player.sendMessage(i18nE.getKey());
            } else if (e instanceof CommandException) {
                sender.sendMessage("§c" + e.getMessage());
            } else {
                sender.sendMessage("§cFatal exception ocurred while executing command. See console for more details.");
                e.printStackTrace();
            }
        }

        return true;
    }

    private List<String> filterSubcommands(final CommandSender sender, final CommandListener listener,
            final String prefix) {
        final List<String> completions = new ArrayList<>();

        for (final CommandListener subcommand : listener.getSubcommands()) {
            final Command command = subcommand.getClass().getAnnotation(Command.class);

            if (command != null && this.canUse(sender, command) && command.name().toLowerCase().startsWith(prefix.toLowerCase())) {
                completions.add(command.name());
            }
        }

        return completions;
    }

    private List<String> filterSuggestions(final List<String> suggestions, final String[] args) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        final String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        final List<String> completions = new ArrayList<>();

        for (final String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(prefix)) {
                completions.add(suggestion);
            }
        }

        return completions;
    }

    private boolean canUse(final CommandSender sender, final Command command) {
        if (sender instanceof Player && command.target() == CommandExecutionTarget.ONLY_CONSOLE) {
            return false;
        }

        if (sender instanceof ConsoleCommandSender && command.target() == CommandExecutionTarget.ONLY_PLAYER) {
            return false;
        }

        return command.permission().isEmpty() || sender.hasPermission(command.permission());
    }

}
