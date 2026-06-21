package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;

public class G12FinalDinnerGame extends ArenaGameBase {

    private boolean playing;

    public G12FinalDinnerGame(final Arena arena, final int durationTime) {
        super("§6Final Dinner", "final-dinner", durationTime, arena);
    }

    @Override
    public void onStart() {
        this.playing = true;
        this.getArena().setPvPAllowed(true);

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.giveDinnerKnife(player.getBukkitPlayer());
        }
    }

    @Override
    public void onStop() {
        this.playing = false;
        this.getArena().setPvPAllowed(false);

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removeDinnerKnife(player.getBukkitPlayer());
        }
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public void onTimeUp() {
        if (!this.playing) {
            return;
        }

        this.playing = false;
        this.getArena().setPvPAllowed(false);
        this.getArena().broadcastTitle("games.final-dinner.finish.title", "games.final-dinner.finish.subtitle");

        if (this.getArena().getPlayers().size() > 1) {
            this.getArena().broadcastTitle("games.final-dinner.failed.title", "games.final-dinner.failed.subtitle");
            for (final SquidPlayer player : new ArrayList<>(this.getArena().getPlayers())) {
                this.removeDinnerKnife(player.getBukkitPlayer());
                this.getArena().killPlayer(player);
            }
            return;
        }

        for (final SquidPlayer player : new ArrayList<>(this.getArena().getPlayers())) {
            this.removeDinnerKnife(player.getBukkitPlayer());
            player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
            player.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
        }

        if (this.getArena().getState() == ArenaState.IN_GAME) {
            this.getArena().setInternalTime(1);
        }
    }

    private void giveDinnerKnife(final Player player) {
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.updateInventory();
    }

    private void removeDinnerKnife(final Player player) {
        player.getInventory().remove(Material.IRON_SWORD);
        player.updateInventory();
    }
}
