package dev.haze.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.lang.reflect.Field;
import java.util.Arrays;

final class BiomeFill {
    private BiomeFill() {
    }

    static void fill(Minecraft mc, String biome) {
        ResourceLocation id = new ResourceLocation(biome);
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> paint(server.overworld(), id));
        paint(mc.level, id);
    }

    private static void paint(Level level, ResourceLocation id) {
        Biome biome = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(id);
        for (int cx = -6; cx < 6; cx++) {
            for (int cz = -6; cz < 6; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk != null && chunk.getBiomes() != null) {
                    Arrays.fill(array(chunk.getBiomes()), biome);
                }
            }
        }
    }

    private static Biome[] array(ChunkBiomeContainer container) {
        for (Field field : ChunkBiomeContainer.class.getDeclaredFields()) {
            if (field.getType() == Biome[].class) {
                try {
                    field.setAccessible(true);
                    return (Biome[]) field.get(container);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("no biome array");
    }
}
