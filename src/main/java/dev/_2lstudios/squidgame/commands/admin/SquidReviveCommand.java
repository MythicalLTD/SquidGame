package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "revive", usage = "/squid revive [player]", description = "Revive a dead player", permission = "squidgame.admin", arguments = {
        Player.class })
public class SquidReviveCommand extends CommandListener {
    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final Player target = context.getArguments().getPlayer(0);
        final SquidPlayer squidPlayer = (SquidPlayer) plugin.getPlayerManager().getPlayer(target);

        if (squidPlayer == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.revive.not-loaded");
            return;
        }

        final Arena arena = squidPlayer.getArena();

        if (arena == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.revive.not-in-game");
            return;
        }

        if (!squidPlayer.isSpectator()) {
            MessageUtils.send(plugin, context.getSender(), "commands.revive.not-dead");
            return;
        }

        if (arena.getState() == ArenaState.FINISHING_ARENA) {
            MessageUtils.send(plugin, context.getSender(), "commands.revive.arena-finishing");
            return;
        }

        if (arena.revivePlayer(squidPlayer)) {
            MessageUtils.send(plugin, target, "commands.revive.target");
            MessageUtils.send(plugin, context.getSender(), "commands.revive.sender", "{player}", target.getName());
        } else {
            MessageUtils.send(plugin, context.getSender(), "commands.revive.failed");
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
