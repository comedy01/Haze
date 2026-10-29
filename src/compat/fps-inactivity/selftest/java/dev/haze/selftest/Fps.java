package dev.haze.selftest;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;

final class Fps {
    private static InactivityFpsLimit saved;

    private Fps() {
    }

    static void unthrottle(Minecraft mc) {
        saved = mc.options.inactivityFpsLimit().get();
        mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
    }

    static void restore(Minecraft mc) {
        if (saved != null) {
            mc.options.inactivityFpsLimit().set(saved);
        }
    }
}
