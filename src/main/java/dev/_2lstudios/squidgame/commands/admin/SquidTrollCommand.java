package dev._2lstudios.squidgame.commands.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import dev._2lstudios.jelly.annotations.Command;
import dev._2lstudios.jelly.commands.CommandContext;
import dev._2lstudios.jelly.commands.CommandListener;
import dev._2lstudios.jelly.player.PluginPlayer;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.MessageUtils;

@Command(name = "troll", usage = "/squid troll [player] [lightning|launch|blind|fakekill]", description = "Run a harmless troll effect", permission = "squidgame.admin", arguments = {
        Player.class, String.class })
public class SquidTrollCommand extends CommandListener {
    private final List<String> actions = Arrays.asList("lightning", "launch", "blind", "fakekill");

    @Override
    public void handle(CommandContext context) {
        final SquidGame plugin = (SquidGame) context.getPlugin();
        final Player target = context.getArguments().getPlayer(0);
        final String action = context.getArguments().getString(1).toLowerCase();

        if (action.equals("lightning")) {
            target.getWorld().strikeLightningEffect(target.getLocation());
        } else if (action.equals("launch")) {
            target.setVelocity(new Vector(0, 1.5, 0));
        } else if (action.equals("blind")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
        } else if (action.equals("fakekill")) {
            new PluginPlayer(target).sendTitle(MessageUtils.format(plugin, "commands.troll.fakekill.title"),
                    MessageUtils.format(plugin, "commands.troll.fakekill.subtitle"), 3);
        } else {
            MessageUtils.send(plugin, context.getSender(), "commands.troll.unknown");
            return;
        }

        MessageUtils.send(plugin, context.getSender(), "commands.troll.done", "{player}", target.getName(), "{action}",
                action);
    }

    @Override
    public List<String> tabComplete(CommandContext context, String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            completions.addAll(this.actions);
        }

        return completions;
    }
}
