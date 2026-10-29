package dev.haze.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class Rainfall {
    private Rainfall() {
    }

    public static Biome biome(ClientLevel level, BlockPos pos) {
        return level.getBiome(pos);
    }

    public static String key(ClientLevel level, BlockPos pos) {
        ResourceLocation id = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(level.getBiome(pos));
        return id == null ? "" : id.toString();
    }

    public static boolean falls(Biome biome) {
        return biome.getPrecipitation() != Biome.Precipitation.NONE;
    }
}
