package dev._2lstudios.squidgame.cosmetics;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev._2lstudios.squidgame.SquidGame;
import dev._2lstudios.squidgame.utils.CompatibilityUtils;
import dev._2lstudios.squidgame.utils.MessageUtils;

public enum CosmeticType {

    NONE("none", false, "BARRIER") {
        @Override
        public void apply(final Player player) {
        }
    },
    HEARTS("hearts", false, "RED_DYE", "INK_SACK") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnHeartParticles(player.getWorld(), trail(player), 1);
        }
    },
    FLAMES("flames", false, "BLAZE_POWDER") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnFlameParticles(player.getWorld(), trail(player), 2);
        }
    },
    INK_CLOUD("ink-cloud", false, "INK_SAC", "INK_SACK") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnLargeSmokeParticles(player.getWorld(), trail(player), 1);
        }
    },
    GREEN_LIGHT("green-light", false, "LIME_DYE", "INK_SACK", "EMERALD") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnHappyVillagerParticles(player.getWorld(), trail(player), 2);
        }
    },
    RED_ALERT("red-alert", false, "REDSTONE") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.RED);
        }
    },
    STARS("stars", false, "NETHER_STAR") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnEnchantmentParticles(player.getWorld(), trail(player), 2);
        }
    },
    RAINBOW("rainbow", false, "MAGENTA_DYE", "INK_SACK", "WOOL") {
        @Override
        public void apply(final Player player) {
            final long time = System.currentTimeMillis();
            dust(player, Color.fromRGB(
                    (int) (127 + 127 * Math.sin(time / 220.0)),
                    (int) (127 + 127 * Math.sin(time / 220.0 + 2)),
                    (int) (127 + 127 * Math.sin(time / 220.0 + 4))));
        }
    },
    NOTES("notes", false, "NOTE_BLOCK") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnNoteParticles(player.getWorld(), trail(player), 1);
        }
    },
    CROWN("crown", true, "GOLDEN_HELMET", "GOLD_HELMET") {
        @Override
        public void apply(final Player player) {
        }
    },
    TOP_HAT("top-hat", true, "IRON_HELMET", "CHAINMAIL_HELMET") {
        @Override
        public void apply(final Player player) {
        }
    },
    DEVIL("devil", true, "LEATHER_HELMET") {
        @Override
        public void apply(final Player player) {
        }
    },
    PUMPKIN("pumpkin", true, "JACK_O_LANTERN", "PUMPKIN") {
        @Override
        public void apply(final Player player) {
        }
    },
    SNOW("snow", false, "SNOW_BALL", "SNOWBALL", "SNOW") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnSnowParticles(player.getWorld(), trail(player), 2);
        }
    },
    ENDER("ender", false, "ENDER_PEARL", "ENDER_STONE") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnPortalParticles(player.getWorld(), trail(player), 2);
        }
    },
    CRIT("crit", false, "DIAMOND_SWORD", "IRON_SWORD") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnCritParticles(player.getWorld(), trail(player), 2);
        }
    },
    MAGIC("magic", false, "POTION", "GLASS_BOTTLE") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnMagicParticles(player.getWorld(), trail(player), 2);
        }
    },
    GOLDEN_SPARK("golden-spark", false, "GOLD_NUGGET", "GOLD_INGOT") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(255, 215, 0));
        }
    },
    SQUID_PINK("squid-pink", false, "WOOL") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(255, 105, 180));
        }
    },
    BLOOD("blood", false, "REDSTONE", "RED_WOOL", "WOOL") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(140, 0, 0));
        }
    },
    ELECTRIC("electric", false, "GLOWSTONE_DUST", "GLOWSTONE") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.YELLOW);
        }
    },
    BUBBLES("bubbles", false, "WATER_BUCKET", "BUCKET") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.AQUA);
        }
    },
    LAVA_GLOW("lava-glow", false, "LAVA_BUCKET", "BUCKET") {
        @Override
        public void apply(final Player player) {
            final Location location = trail(player);
            CompatibilityUtils.spawnFlameParticles(player.getWorld(), location, 1);
            CompatibilityUtils.spawnColoredDustParticles(player.getWorld(), location, 1, Color.ORANGE);
        }
    },
    FEATHER("feather", false, "FEATHER") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnSnowParticles(player.getWorld(), trail(player), 2);
        }
    },
    SMOKE("smoke", false, "COAL", "CHARCOAL") {
        @Override
        public void apply(final Player player) {
            CompatibilityUtils.spawnLargeSmokeParticles(player.getWorld(), trail(player), 2);
        }
    },
    TOXIC("toxic", false, "SLIME_BALL", "SLIME_BLOCK") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(85, 255, 85));
        }
    },
    GALAXY("galaxy", false, "LAPIS_BLOCK", "LAPIS_LAZULI") {
        @Override
        public void apply(final Player player) {
            final Location location = trail(player);
            CompatibilityUtils.spawnColoredDustParticles(player.getWorld(), location, 2, Color.fromRGB(90, 40, 255));
            CompatibilityUtils.spawnPortalParticles(player.getWorld(), location, 1);
        }
    },
    GHOST("ghost", false, "GHAST_TEAR", "SOUL_SAND") {
        @Override
        public void apply(final Player player) {
            final Location location = trail(player);
            CompatibilityUtils.spawnLargeSmokeParticles(player.getWorld(), location, 1);
            CompatibilityUtils.spawnSnowParticles(player.getWorld(), location, 1);
        }
    },
    NEON_BLUE("neon-blue", false, "DIAMOND", "DIAMOND_BLOCK") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(0, 200, 255));
        }
    },
    NEON_GREEN("neon-green", false, "EMERALD", "EMERALD_BLOCK") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(57, 255, 20));
        }
    },
    SHADOW("shadow", false, "BLACK_WOOL", "WOOL", "COAL_BLOCK") {
        @Override
        public void apply(final Player player) {
            final Location location = trail(player);
            CompatibilityUtils.spawnLargeSmokeParticles(player.getWorld(), location, 1);
            CompatibilityUtils.spawnColoredDustParticles(player.getWorld(), location, 1, Color.fromRGB(30, 30, 30));
        }
    },
    ICE_TRAIL("ice-trail", false, "PACKED_ICE", "ICE") {
        @Override
        public void apply(final Player player) {
            final Location location = trail(player);
            CompatibilityUtils.spawnSnowParticles(player.getWorld(), location, 2);
            CompatibilityUtils.spawnColoredDustParticles(player.getWorld(), location, 1, Color.fromRGB(180, 240, 255));
        }
    },
    LOVE_STORM("love-storm", false, "ROSE_BUSH", "RED_ROSE", "YELLOW_FLOWER") {
        @Override
        public void apply(final Player player) {
            final Location location = trail(player);
            CompatibilityUtils.spawnHeartParticles(player.getWorld(), location, 1);
            CompatibilityUtils.spawnColoredDustParticles(player.getWorld(), location, 1, Color.fromRGB(255, 50, 100));
        }
    },
    COIN_TRAIL("coin-trail", false, "GOLD_BLOCK", "GOLD_INGOT") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(255, 200, 0));
        }
    },
    CHERRY("cherry", false, "REDSTONE", "APPLE") {
        @Override
        public void apply(final Player player) {
            dust(player, Color.fromRGB(255, 120, 150));
        }
    },
    SAMURAI("samurai", true, "IRON_HELMET") {
        @Override
        public void apply(final Player player) {
        }
    },
    BUNNY("bunny", true, "GOLD_HELMET", "GOLDEN_HELMET") {
        @Override
        public void apply(final Player player) {
        }
    },
    ASTRONAUT("astronaut", true, "CHAINMAIL_HELMET") {
        @Override
        public void apply(final Player player) {
        }
    };

    private static Location trail(final Player player) {
        return CompatibilityUtils.cosmeticTrailLocation(player);
    }

    private static void dust(final Player player, final Color color) {
        CompatibilityUtils.spawnColoredDustParticles(player.getWorld(), trail(player), 2, color);
    }

    private final String id;
    private final Material icon;
    private final boolean hat;

    CosmeticType(final String id, final boolean hat, final String... materialNames) {
        this.id = id;
        this.icon = CompatibilityUtils.material(materialNames);
        this.hat = hat;
    }

    public String getId() {
        return this.id;
    }

    public Material getIcon() {
        return this.icon;
    }

    public boolean isHat() {
        return this.hat;
    }

    public abstract void apply(final Player player);

    public String getDisplayName() {
        return MessageUtils.format(SquidGame.getInstance(), "cosmetics.types." + this.id + ".name");
    }

    public String getDescription() {
        return MessageUtils.format(SquidGame.getInstance(), "cosmetics.types." + this.id + ".description");
    }

    public ItemStack createMenuItem(final boolean selected) {
        return this.createMenuItem(selected, true, 0, 0);
    }

    public ItemStack createMenuItem(final boolean selected, final boolean unlocked, final int price,
            final int playerCoins) {
        final ItemStack item = new ItemStack(this.icon);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(this.getDisplayName());

        final StringBuilder lore = new StringBuilder(this.getDescription());

        if (!unlocked) {
            lore.append("\n§c§lLOCKED");
            lore.append("\n§ePrice: §f").append(price).append(" coins");

            if (playerCoins < price) {
                lore.append("\n§cYou need §f").append(price - playerCoins).append(" §cmore coins");
            }

            lore.append("\n§7Click to purchase");
        } else if (selected) {
            lore.append("\n§a§lSELECTED");
        } else {
            lore.append("\n§7Click to equip");
        }

        meta.setLore(java.util.Arrays.asList(lore.toString().split("\n")));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createHatItem() {
        if (!this.hat) {
            return null;
        }

        final ItemStack helmet = new ItemStack(this.icon);
        final ItemMeta meta = helmet.getItemMeta();
        meta.setDisplayName(this.getDisplayName());
        CompatibilityUtils.setUnbreakable(meta, true);
        helmet.setItemMeta(meta);
        return helmet;
    }

    public static CosmeticType fromId(final String id) {
        if (id == null || id.isEmpty()) {
            return NONE;
        }

        for (final CosmeticType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }

        return NONE;
    }

    public static CosmeticType[] selectable() {
        return values();
    }
}
