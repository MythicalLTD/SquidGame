package dev._2lstudios.squidgame.music;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import dev._2lstudios.squidgame.SquidGame;

public class LobbyMusicCosmeticRegistry {

    private final SquidGame plugin;
    private final Map<String, LobbyMusicCosmetic> cosmetics;

    public LobbyMusicCosmeticRegistry(final SquidGame plugin) {
        this.plugin = plugin;
        this.cosmetics = new LinkedHashMap<>();
    }

    public void reload() {
        this.cosmetics.clear();
        this.ensureCosmeticSongFiles();
        this.loadFromDirectory();
    }

    public LobbyMusicCosmetic get(final String id) {
        if (id == null || id.isEmpty() || "none".equalsIgnoreCase(id)) {
            return null;
        }

        return this.cosmetics.get(id.toLowerCase());
    }

    public List<LobbyMusicCosmetic> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(this.cosmetics.values()));
    }

    private void loadFromDirectory() {
        final File directory = new File(this.plugin.getDataFolder(), "songs/cosmetics");

        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));

        if (files == null) {
            return;
        }

        java.util.Arrays.sort(files, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));

        for (final File file : files) {
            final String fileName = file.getName();
            final String id = LobbyMusicCosmetic.toId(fileName);

            try {
                final Song song = NBSDecoder.parse(file);
                final LobbyMusicCosmetic cosmetic = new LobbyMusicCosmetic(id, fileName,
                        LobbyMusicCosmetic.toDisplayName(fileName), song);
                this.cosmetics.put(id, cosmetic);
            } catch (Exception exception) {
                this.plugin.getLogger().warning("Could not load cosmetic song '" + fileName + "': "
                        + exception.getMessage());
            }
        }

        this.plugin.getLogger().info("Loaded " + this.cosmetics.size() + " lobby DJ songs.");
    }

    private void ensureCosmeticSongFiles() {
        final File directory = new File(this.plugin.getDataFolder(), "songs/cosmetics");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            final java.net.URL jarUrl = SquidGame.class.getProtectionDomain().getCodeSource().getLocation();
            final File jarFile = new File(jarUrl.toURI());

            if (!jarFile.isFile()) {
                return;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                for (final JarEntry entry : Collections.list(jar.entries())) {
                    final String name = entry.getName();

                    if (!name.startsWith("songs/cosmetics/") || !name.endsWith(".nbs") || entry.isDirectory()) {
                        continue;
                    }

                    final File output = new File(this.plugin.getDataFolder(), name);

                    if (!output.exists()) {
                        output.getParentFile().mkdirs();
                        this.copyStream(jar.getInputStream(entry), output);
                    }
                }
            }
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Could not extract cosmetic songs: " + exception.getMessage());
        }
    }

    private void copyStream(final InputStream input, final File output) throws IOException {
        try (InputStream in = input; OutputStream out = Files.newOutputStream(output.toPath())) {
            final byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
