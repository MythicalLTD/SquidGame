package dev._2lstudios.squidgame.economy;

public enum UnlockType {
    COSMETIC("cosmetic"),
    MUSIC("music");

    private final String key;

    UnlockType(final String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
