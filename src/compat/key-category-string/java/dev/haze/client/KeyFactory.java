package dev.haze.client;

import net.minecraft.client.KeyMapping;

final class KeyFactory {
    private KeyFactory() {
    }

    static KeyMapping create(String name, int code) {
        return new KeyMapping(name, code, "key.categories.haze");
    }
}
