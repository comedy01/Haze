package dev.haze.fabric;

import dev.haze.client.KeyRegistrar;
import dev.haze.client.HazeClient;
import dev.haze.client.render.ShaderSetup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class HazeFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HazeClient.init(FabricLoader.getInstance().getConfigDir());
        ShaderSetup.init();
        HazeClient.setKeys(
                KeyRegistrar.register(HazeClient.createToggleKey()),
                KeyRegistrar.register(HazeClient.createSettingsKey()));
    }
}
