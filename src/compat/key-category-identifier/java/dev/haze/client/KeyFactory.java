package dev.haze.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

final class KeyFactory {
    private static KeyMapping.Category category;

    private KeyFactory() {
    }

    static KeyMapping create(String name, int code) {
        if (category == null) {
            category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(HazeClient.MOD_ID, "main"));
        }
        return new KeyMapping(name, code, category);
    }
}
