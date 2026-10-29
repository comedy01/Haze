package dev.haze.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.dimension.DimensionType;

public final class SkyType {
    private SkyType() {
    }

    public static boolean overworld(ClientLevel level) {
        return DimensionType.OVERWORLD_EFFECTS.equals(level.dimensionType().effectsLocation());
    }
}
