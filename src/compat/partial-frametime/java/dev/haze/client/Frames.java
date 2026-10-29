package dev.haze.client;

import net.minecraft.client.Minecraft;

public final class Frames {
    private Frames() {
    }

    public static float partialTick(Minecraft mc) {
        return mc.getFrameTime();
    }
}
