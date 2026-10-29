package dev.haze.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public final class Rainfall {
    private Rainfall() {
    }

    public static Biome biome(ClientLevel level, BlockPos pos) {
        return level.getBiome(pos).value();
    }

    public static String key(ClientLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(Object::toString).orElse("");
    }

    public static boolean falls(Biome biome) {
        return biome.getPrecipitation() != Biome.Precipitation.NONE;
    }
}
