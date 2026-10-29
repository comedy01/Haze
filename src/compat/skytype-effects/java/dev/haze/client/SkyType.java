package dev.haze.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

public final class SkyType {
    private SkyType() {
    }

    public static boolean overworld(ClientLevel level) {
        return BuiltinDimensionTypes.OVERWORLD_EFFECTS.equals(level.dimensionType().effectsLocation());
    }
}
