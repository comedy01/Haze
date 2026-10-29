package dev.haze.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

final class BiomeFill {
    private BiomeFill() {
    }

    static void fill(Minecraft mc, String biome) {
        ResourceKey<Biome> key = ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(biome));
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> paint(server.overworld(), key));
        paint(mc.level, key);
    }

    private static void paint(Level level, ResourceKey<Biome> key) {
        Holder<Biome> holder = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(key);
        for (int cx = -6; cx < 6; cx++) {
            for (int cz = -6; cz < 6; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk != null) {
                    chunk.fillBiomesFromNoise((x, y, z, sampler) -> holder, null);
                }
            }
        }
    }
}
