package dev.haze.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.haze.config.HazeConfig;
import net.minecraft.client.KeyMapping;

import java.nio.file.Path;

public final class HazeClient {
    public static final String MOD_ID = "haze";

    private static HazeConfig config = new HazeConfig();
    private static Path configPath;
    private static KeyMapping toggleKey;
    private static KeyMapping settingsKey;

    private HazeClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(HazeConfig.FILE_NAME);
        config = HazeConfig.load(configPath);
    }

    public static KeyMapping createToggleKey() {
        return KeyFactory.create("key.haze.toggle", InputConstants.UNKNOWN.getValue());
    }

    public static KeyMapping createSettingsKey() {
        return KeyFactory.create("key.haze.settings", InputConstants.UNKNOWN.getValue());
    }

    public static void setKeys(KeyMapping toggle, KeyMapping settings) {
        toggleKey = toggle;
        settingsKey = settings;
    }

    public static KeyMapping toggleKey() {
        return toggleKey;
    }

    public static KeyMapping settingsKey() {
        return settingsKey;
    }

    public static HazeConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
