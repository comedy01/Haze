package dev.haze.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HazeConfig {
    public static final String FILE_NAME = "haze.json";

    private static final Logger LOGGER = LogManager.getLogger("haze");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("enabled")
    private boolean enabled = true;

    @SerializedName("intensity")
    private int intensity = 100;

    @SerializedName("frequency")
    private int frequency = 100;

    @SerializedName("depth")
    private int depth = 24;

    @SerializedName("morningFog")
    private boolean morningFog = true;

    @SerializedName("alwaysFog")
    private boolean alwaysFog = false;

    @SerializedName("rainMist")
    private boolean rainMist = true;

    @SerializedName("swampMist")
    private boolean swampMist = true;

    @SerializedName("sunGlow")
    private boolean sunGlow = true;

    @SerializedName("wisps")
    private boolean wisps = true;

    @SerializedName("cave_fog")
    private boolean caveFog = true;

    @SerializedName("underwater")
    private boolean underwater = true;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public int intensity() {
        return intensity;
    }

    public void setIntensity(int value) {
        intensity = clamp(value, 10, 200);
    }

    public int frequency() {
        return frequency;
    }

    public void setFrequency(int value) {
        frequency = clamp(value, 0, 200);
    }

    public int depth() {
        return depth;
    }

    public void setDepth(int value) {
        depth = clamp(value, 8, 64);
    }

    public boolean morningFog() {
        return morningFog;
    }

    public void setMorningFog(boolean value) {
        morningFog = value;
    }

    public boolean alwaysFog() {
        return alwaysFog;
    }

    public void setAlwaysFog(boolean value) {
        alwaysFog = value;
    }

    public boolean rainMist() {
        return rainMist;
    }

    public void setRainMist(boolean value) {
        rainMist = value;
    }

    public boolean swampMist() {
        return swampMist;
    }

    public void setSwampMist(boolean value) {
        swampMist = value;
    }

    public boolean sunGlow() {
        return sunGlow;
    }

    public void setSunGlow(boolean value) {
        sunGlow = value;
    }

    public boolean wisps() {
        return wisps;
    }

    public void setWisps(boolean value) {
        wisps = value;
    }

    public boolean caveFog() {
        return caveFog;
    }

    public void setCaveFog(boolean value) {
        caveFog = value;
    }

    public boolean underwater() {
        return underwater;
    }

    public void setUnderwater(boolean value) {
        underwater = value;
    }

    public void resetToDefaults() {
        HazeConfig fresh = new HazeConfig();
        enabled = fresh.enabled;
        intensity = fresh.intensity;
        frequency = fresh.frequency;
        depth = fresh.depth;
        morningFog = fresh.morningFog;
        alwaysFog = fresh.alwaysFog;
        rainMist = fresh.rainMist;
        swampMist = fresh.swampMist;
        sunGlow = fresh.sunGlow;
        wisps = fresh.wisps;
        caveFog = fresh.caveFog;
        underwater = fresh.underwater;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sanitize() {
        setIntensity(intensity);
        setFrequency(frequency);
        setDepth(depth);
    }

    public static HazeConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            HazeConfig fresh = new HazeConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            HazeConfig loaded = GSON.fromJson(reader, HazeConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            HazeConfig fresh = new HazeConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
