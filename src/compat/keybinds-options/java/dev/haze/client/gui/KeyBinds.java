package dev.haze.client.gui;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

final class KeyBinds {
    private KeyBinds() {
    }

    static Screen screen(Screen parent, Options options) {
        return new KeyBindsScreen(parent, options);
    }
}
