package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandExecutionTarget;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "fly", usage = "/fly [player]", description = "Toggle flight", permission = "squidgame.admin", target = CommandExecutionTarget.ONLY_PLAYER, arguments = {
        Player.class }, minArguments = 0)
public class FlyCommand extends CommandListener {
    @Override
    public void handle(final CommandContext context) {
        final Player target = context.getArguments().size() > 0 ? context.getArguments().getPlayer(0)
                : context.getPlayer();
        final boolean enabled = !target.getAllowFlight();

        target.setAllowFlight(enabled);

        if (!enabled) {
            target.setFlying(false);
        }

        MessageUtils.send((SquidGame) context.getPlugin(), context.getPlayer(),
                enabled ? "commands.fly.enabled" : "commands.fly.disabled", "{player}", target.getName());

        if (!target.equals(context.getPlayer())) {
            MessageUtils.send((SquidGame) context.getPlugin(), target,
                    enabled ? "commands.fly.enabled-target" : "commands.fly.disabled-target");
        }
    }

    @Override
    public List<String> tabComplete(final CommandContext context, final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}
