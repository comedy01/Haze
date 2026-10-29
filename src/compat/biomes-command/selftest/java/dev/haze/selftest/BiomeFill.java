package dev.haze.selftest;

import net.minecraft.client.Minecraft;

final class BiomeFill {
    private BiomeFill() {
    }

    static void fill(Minecraft mc, String biome) {
        for (int x = -96; x < 96; x += 16) {
            for (int z = -96; z < 96; z += 16) {
                HazeSelfTest.run(mc, "fillbiome " + x + " -64 " + z + " " + (x + 15) + " 0 " + (z + 15) + " " + biome);
            }
        }
    }
}
