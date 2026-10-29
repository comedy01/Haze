package dev.haze.client;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class KeyRegistrar {
    private KeyRegistrar() {
    }

    public static KeyMapping register(KeyMapping key) {
        return KeyMappingHelper.registerKeyMapping(key);
    }
}
