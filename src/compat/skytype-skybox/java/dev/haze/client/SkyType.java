package dev.haze.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.dimension.DimensionType;

public final class SkyType {
    private SkyType() {
    }

    public static boolean overworld(ClientLevel level) {
        return level.dimensionType().skybox() == DimensionType.Skybox.OVERWORLD;
    }
}
