package dev._2lstudios.squidgame.arena.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev._2lstudios.jelly.gui.InventoryGUI;
import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.arena.Arena;
import dev._2lstudios.squidgame.arena.ArenaState;
import dev._2lstudios.squidgame.player.SquidPlayer;
import dev._2lstudios.squidgame.utils.MessageUtils;

public class G4MarblesGame extends ArenaGameBase {

    private final Map<UUID, UUID> partners;
    private final Map<UUID, Integer> marbles;
    private final Set<UUID> completed;
    private final Random random;

    public G4MarblesGame(final Arena arena, final int durationTime) {
        super("§bMarbles", "fourth", durationTime, arena);

        this.partners = new HashMap<>();
        this.marbles = new HashMap<>();
        this.completed = new HashSet<>();
        this.random = new Random();
    }

    @Override
    public void onExplainStart() {
        super.onExplainStart();
        this.getArena().setPvPAllowed(false);

        this.partners.clear();
        this.marbles.clear();
        this.completed.clear();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.givePartnerSelector(player.getBukkitPlayer());
            MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.fourth.select-partner");
        }
    }

    @Override
    public void onStart() {
        this.getArena().setPvPAllowed(false);
        this.pairRemainingPlayers();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removePartnerSelector(player.getBukkitPlayer());
            this.marbles.put(player.getBukkitPlayer().getUniqueId(), 10);
        }

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (this.getPartner(player) == null) {
                this.completed.add(player.getBukkitPlayer().getUniqueId());
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
            } else {
                this.giveMarblePouch(player.getBukkitPlayer());
                this.openChallenge(player);
            }
        }
    }

    @Override
    public void onStop() {
        for (final SquidPlayer player : this.getArena().getPlayers()) {
            this.removePartnerSelector(player.getBukkitPlayer());
            this.removeMarblePouch(player.getBukkitPlayer());
        }
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public void onTimeUp() {
        this.getArena().broadcastTitle("events.game-timeout.title", "events.game-timeout.subtitle");

        final List<SquidPlayer> alive = new ArrayList<>();
        final List<SquidPlayer> death = new ArrayList<>();

        for (final SquidPlayer squidPlayer : this.getArena().getPlayers()) {
            this.removeMarblePouch(squidPlayer.getBukkitPlayer());

            if (this.isCompleted(squidPlayer)) {
                alive.add(squidPlayer);
            } else {
                death.add(squidPlayer);
            }
        }

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer player : death) {
                player.sendTitle("events.game-timeout-died.title", "events.game-timeout-died.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
            }

            for (final SquidPlayer player : alive) {
                player.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
                player.playSound(
                        this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(SquidGame.getInstance(), () -> {
            for (final SquidPlayer squidPlayer : death) {
                this.getArena().killPlayer(squidPlayer);
            }
        }, 80L);
    }

    public void selectPartner(final SquidPlayer player, final SquidPlayer target) {
        if (player == target || this.getArena().getState() != ArenaState.EXPLAIN_GAME) {
            return;
        }

        if (!this.getArena().getPlayers().contains(player) || !this.getArena().getPlayers().contains(target)) {
            return;
        }

        if (this.getPartner(player) != null) {
            MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.fourth.already-partnered");
            return;
        }

        if (this.getPartner(target) != null) {
            MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.fourth.target-partnered");
            return;
        }

        this.pair(player, target);
    }

    public void openChallenge(final SquidPlayer player) {
        if (!this.isCompleted(player) && this.getArena().getPlayers().contains(player) && this.getPartner(player) != null) {
            new MarblesGUI(this, player).open(player.getBukkitPlayer());
        }
    }

    private void playRound(final SquidPlayer player, final boolean oddGuess, final int requestedWager) {
        final SquidPlayer partner = this.getPartner(player);

        if (partner == null || this.isCompleted(player) || this.isCompleted(partner)) {
            return;
        }

        final int playerMarbles = this.getMarbles(player);
        final int partnerMarbles = this.getMarbles(partner);
        final int wager = Math.max(1, Math.min(requestedWager, Math.min(playerMarbles, partnerMarbles)));
        final int hiddenMarbles = this.random.nextInt(10) + 1;
        final boolean hiddenOdd = hiddenMarbles % 2 != 0;
        final boolean playerWon = hiddenOdd == oddGuess;

        if (playerWon) {
            this.setMarbles(player, playerMarbles + wager);
            this.setMarbles(partner, partnerMarbles - wager);
        } else {
            this.setMarbles(player, playerMarbles - wager);
            this.setMarbles(partner, partnerMarbles + wager);
        }

        final String result = hiddenOdd ? "odd" : "even";
        MessageUtils.send(SquidGame.getInstance(), player.getBukkitPlayer(), "games.fourth.round-result", "{amount}",
                String.valueOf(hiddenMarbles), "{result}", result);
        MessageUtils.send(SquidGame.getInstance(), partner.getBukkitPlayer(), "games.fourth.partner-guess",
                "{player}", player.getBukkitPlayer().getName(), "{guess}", oddGuess ? "odd" : "even", "{wager}",
                String.valueOf(wager));

        this.checkPairFinished(player, partner);

        if (!this.isCompleted(player) && this.getArena().getPlayers().contains(player)) {
            this.openChallenge(player);
        }

        if (!this.isCompleted(partner) && this.getArena().getPlayers().contains(partner)) {
            this.openChallenge(partner);
        }
    }

    private void checkPairFinished(final SquidPlayer first, final SquidPlayer second) {
        if (this.getMarbles(first) <= 0) {
            this.finishPair(second, first);
        } else if (this.getMarbles(second) <= 0) {
            this.finishPair(first, second);
        }
    }

    private void finishPair(final SquidPlayer winner, final SquidPlayer loser) {
        this.completed.add(winner.getBukkitPlayer().getUniqueId());
        this.removeMarblePouch(winner.getBukkitPlayer());
        this.removeMarblePouch(loser.getBukkitPlayer());

        winner.getBukkitPlayer().closeInventory();
        loser.getBukkitPlayer().closeInventory();
        winner.sendTitle("events.game-pass.title", "events.game-pass.subtitle", 3);
        winner.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-pass-game", "LEVEL_UP"));
        loser.sendTitle("games.fourth.lost.title", "games.fourth.lost.subtitle", 3);
        loser.playSound(this.getArena().getMainConfig().getSound("game-settings.sounds.player-loss-game", "CAT_HIT"));
        this.getArena().killPlayer(loser);
    }

    private void pairRemainingPlayers() {
        final List<SquidPlayer> unpaired = new ArrayList<>();

        for (final SquidPlayer player : this.getArena().getPlayers()) {
            if (this.getPartner(player) == null) {
                unpaired.add(player);
            }
        }

        Collections.shuffle(unpaired);

        for (int i = 0; i + 1 < unpaired.size(); i += 2) {
            this.pair(unpaired.get(i), unpaired.get(i + 1));
        }
    }

    private void pair(final SquidPlayer first, final SquidPlayer second) {
        this.partners.put(first.getBukkitPlayer().getUniqueId(), second.getBukkitPlayer().getUniqueId());
        this.partners.put(second.getBukkitPlayer().getUniqueId(), first.getBukkitPlayer().getUniqueId());
        MessageUtils.send(SquidGame.getInstance(), first.getBukkitPlayer(), "games.fourth.partnered", "{player}",
                second.getBukkitPlayer().getName());
        MessageUtils.send(SquidGame.getInstance(), second.getBukkitPlayer(), "games.fourth.partnered", "{player}",
                first.getBukkitPlayer().getName());
    }

    private SquidPlayer getPartner(final SquidPlayer player) {
        final UUID partnerId = this.partners.get(player.getBukkitPlayer().getUniqueId());

        if (partnerId == null) {
            return null;
        }

        final Player partner = Bukkit.getPlayer(partnerId);
        return partner == null ? null : (SquidPlayer) SquidGame.getInstance().getPlayerManager().getPlayer(partner);
    }

    private int getMarbles(final SquidPlayer player) {
        return this.marbles.get(player.getBukkitPlayer().getUniqueId());
    }

    private void setMarbles(final SquidPlayer player, final int amount) {
        this.marbles.put(player.getBukkitPlayer().getUniqueId(), amount);
    }

    private boolean isCompleted(final SquidPlayer player) {
        return this.completed.contains(player.getBukkitPlayer().getUniqueId());
    }

    private void givePartnerSelector(final Player player) {
        final ItemStack selector = new ItemStack(Material.NAME_TAG);
        final ItemMeta meta = selector.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.partner-selector"));
        selector.setItemMeta(meta);
        player.getInventory().addItem(selector);
    }

    private void removePartnerSelector(final Player player) {
        player.getInventory().remove(Material.NAME_TAG);
        player.updateInventory();
    }

    private void giveMarblePouch(final Player player) {
        final ItemStack pouch = new ItemStack(Material.CLAY_BALL, 10);
        final ItemMeta meta = pouch.getItemMeta();
        meta.setDisplayName(MessageUtils.format(SquidGame.getInstance(), "items.marble-pouch"));
        pouch.setItemMeta(meta);
        player.getInventory().addItem(pouch);
    }

    private void removeMarblePouch(final Player player) {
        player.getInventory().remove(Material.CLAY_BALL);
        player.updateInventory();
    }

    private static class MarblesGUI extends InventoryGUI {
        private final G4MarblesGame game;
        private final SquidPlayer player;

        public MarblesGUI(final G4MarblesGame game, final SquidPlayer player) {
            super("§b§lMarbles", 27);
            this.game = game;
            this.player = player;
        }

        @Override
        public void init() {
            final SquidPlayer partner = this.game.getPartner(this.player);
            final int ownMarbles = this.game.getMarbles(this.player);
            final int partnerMarbles = partner == null ? 0 : this.game.getMarbles(partner);

            this.addItem(0, this.createItem("§bYour marbles: §f" + ownMarbles, Material.CLAY_BALL,
                    "§r\n§7Partner: §f" + (partner == null ? "None" : partner.getBukkitPlayer().getName())
                            + "\n§7Partner marbles: §f" + partnerMarbles + "\n§r"),
                    5, 2);
            this.addItem(11, this.createItem("§aOdd §7- §f1 marble", Material.EMERALD), 2, 3);
            this.addItem(12, this.createItem("§aOdd §7- §f3 marbles", Material.EMERALD, "", 3), 3, 3);
            this.addItem(13, this.createItem("§aOdd §7- §fAll marbles", Material.EMERALD_BLOCK), 4, 3);
            this.addItem(21, this.createItem("§eEven §7- §f1 marble", Material.GOLD_INGOT), 6, 3);
            this.addItem(22, this.createItem("§eEven §7- §f3 marbles", Material.GOLD_INGOT, "", 3), 7, 3);
            this.addItem(23, this.createItem("§eEven §7- §fAll marbles", Material.GOLD_BLOCK), 8, 3);
        }

        @Override
        public void handle(final int id, final Player player) {
            if (id == 11) {
                this.game.playRound(this.player, true, 1);
            } else if (id == 12) {
                this.game.playRound(this.player, true, 3);
            } else if (id == 13) {
                this.game.playRound(this.player, true, this.game.getMarbles(this.player));
            } else if (id == 21) {
                this.game.playRound(this.player, false, 1);
            } else if (id == 22) {
                this.game.playRound(this.player, false, 3);
            } else if (id == 23) {
                this.game.playRound(this.player, false, this.game.getMarbles(this.player));
            }
        }
    }
}
