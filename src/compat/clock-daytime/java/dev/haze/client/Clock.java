package dev.haze.client;

import net.minecraft.world.level.Level;

public final class Clock {
    private Clock() {
    }

    public static long time(Level level) {
        return level.getDayTime();
    }
}
