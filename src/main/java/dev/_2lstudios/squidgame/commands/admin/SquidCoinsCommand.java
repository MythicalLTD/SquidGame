package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.economy.UnlockType;
import dev._2lstudios.squidgame.player.PlayerProfile;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "coins", usage = "/squid coins <give|set|take|unlock> ...", description = "Manage player coins and unlocks", permission = "squidgame.admin", arguments = {
        String.class, String.class, String.class, String.class })
public class SquidCoinsCommand extends CommandListener {

    private static final List<String> ACTIONS = Arrays.asList("give", "set", "take", "unlock");

    @Override
    public void handle(final CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();

        if (!plugin.getPlayerDataManager().isEnabled()) {
            MessageUtils.send(plugin, context.getSender(), "economy.disabled");
            return;
        }

        final String action = context.getArguments().getString(0, "").toLowerCase();

        if (action.isEmpty()) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.usage");
            return;
        }

        if ("unlock".equals(action)) {
            this.handleUnlock(plugin, context);
            return;
        }

        if (!ACTIONS.contains(action)) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.usage");
            return;
        }

        final String targetName = context.getArguments().getString(1);
        final int amount = context.getArguments().getInt(2, -1);

        if (targetName == null || targetName.isEmpty() || amount < 0) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.usage");
            return;
        }

        final Player online = Bukkit.getPlayer(targetName);

        if (online == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.not-online", "{player}", targetName);
            return;
        }

        final SquidPlayer squidPlayer = (SquidPlayer) plugin.getPlayerManager().getPlayer(online);
        final PlayerProfile profile = plugin.getPlayerDataManager().getProfile(online.getUniqueId());

        if (profile == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.profile-loading", "{player}", targetName);
            return;
        }

        switch (action) {
        case "give":
            profile.addCoins(amount);
            plugin.getPlayerDataManager().saveProfile(profile);
            squidPlayer.refreshScoreboard();
            MessageUtils.send(plugin, context.getSender(), "commands.coins.give-sender", "{player}", targetName,
                    "{amount}", String.valueOf(amount), "{balance}", String.valueOf(profile.getCoins()));
            MessageUtils.send(plugin, online, "commands.coins.give-target", "{amount}", String.valueOf(amount),
                    "{balance}", String.valueOf(profile.getCoins()));
            break;
        case "set":
            profile.setCoins(amount);
            plugin.getPlayerDataManager().saveProfile(profile);
            squidPlayer.refreshScoreboard();
            MessageUtils.send(plugin, context.getSender(), "commands.coins.set-sender", "{player}", targetName,
                    "{amount}", String.valueOf(amount));
            MessageUtils.send(plugin, online, "commands.coins.set-target", "{amount}", String.valueOf(amount));
            break;
        case "take":
            if (!profile.removeCoins(amount)) {
                MessageUtils.send(plugin, context.getSender(), "commands.coins.not-enough", "{player}", targetName,
                        "{balance}", String.valueOf(profile.getCoins()), "{amount}", String.valueOf(amount));
                return;
            }

            plugin.getPlayerDataManager().saveProfile(profile);
            squidPlayer.refreshScoreboard();
            MessageUtils.send(plugin, context.getSender(), "commands.coins.take-sender", "{player}", targetName,
                    "{amount}", String.valueOf(amount), "{balance}", String.valueOf(profile.getCoins()));
            MessageUtils.send(plugin, online, "commands.coins.take-target", "{amount}", String.valueOf(amount),
                    "{balance}", String.valueOf(profile.getCoins()));
            break;
        default:
            MessageUtils.send(plugin, context.getSender(), "commands.coins.usage");
            break;
        }
    }

    private void handleUnlock(final SquidGame plugin, final CommandContext context) {
        final String type = context.getArguments().getString(1, "").toLowerCase();
        final String targetName = context.getArguments().getString(2);
        final String unlockId = context.getArguments().getString(3, "").toLowerCase();

        if (!"cosmetic".equals(type) && !"music".equals(type)) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.usage");
            return;
        }

        if (targetName == null || targetName.isEmpty() || unlockId.isEmpty()) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.usage");
            return;
        }

        final Player online = Bukkit.getPlayer(targetName);

        if (online == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.not-online", "{player}", targetName);
            return;
        }

        final PlayerProfile profile = plugin.getPlayerDataManager().getProfile(online.getUniqueId());

        if (profile == null) {
            MessageUtils.send(plugin, context.getSender(), "commands.coins.profile-loading", "{player}", targetName);
            return;
        }

        final UnlockType unlockType = "music".equals(type) ? UnlockType.MUSIC : UnlockType.COSMETIC;
        profile.unlock(unlockType, unlockId);
        plugin.getPlayerDataManager().saveProfile(profile);
        MessageUtils.send(plugin, context.getSender(), "commands.coins.unlock-sender", "{player}", targetName,
                "{type}", type, "{item}", unlockId);
        MessageUtils.send(plugin, online, "commands.coins.unlock-target", "{type}", type, "{item}", unlockId);
    }

    @Override
    public List<String> tabComplete(final CommandContext context, final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (final String action : ACTIONS) {
                if (action.startsWith(args[0].toLowerCase())) {
                    completions.add(action);
                }
            }

            return completions;
        }

        if (args.length == 2 && "unlock".equalsIgnoreCase(args[0])) {
            if ("cosmetic".startsWith(args[1].toLowerCase())) {
                completions.add("cosmetic");
            }

            if ("music".startsWith(args[1].toLowerCase())) {
                completions.add("music");
            }

            return completions;
        }

        if ((args.length == 2 && !"unlock".equalsIgnoreCase(args[0]))
                || (args.length == 3 && "unlock".equalsIgnoreCase(args[0]))) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }

            return completions;
        }

        return completions;
    }
}
