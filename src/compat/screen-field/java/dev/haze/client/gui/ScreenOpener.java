package dev.haze.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenOpener {
    private ScreenOpener() {
    }

    public static Screen current(Minecraft client) {
        return client.screen;
    }

    static void open(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
}
