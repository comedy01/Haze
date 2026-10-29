package dev.haze.client;

import dev.haze.client.gui.HazeSettingsScreen;
import dev.haze.config.HazeConfig;
import dev.haze.core.Fog;
import dev.haze.core.Swamp;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class HazeRuntime {
    private static final long SEED = 0x48415A45L;
    private static final float EASE = 0.01F;

    private static ClientLevel lastLevel;
    private static boolean voxy;

    private HazeRuntime() {
    }

    public static void beforeKeybinds() {
        Minecraft mc = Minecraft.getInstance();
        HazeConfig config = HazeClient.config();

        KeyMapping toggle = HazeClient.toggleKey();
        while (toggle != null && toggle.consumeClick()) {
            config.setEnabled(!config.enabled());
            HazeClient.saveConfig();
            if (mc.player != null) {
                Messages.overlay(mc.player, Texts.translatable(
                        config.enabled() ? "haze.message.on" : "haze.message.off"));
            }
        }

        KeyMapping settings = HazeClient.settingsKey();
        boolean open = false;
        while (settings != null && settings.consumeClick()) {
            open = true;
        }
        if (open) {
            HazeSettingsScreen.open(mc);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        HazeConfig config = HazeClient.config();
        if (level == null || mc.player == null || !config.enabled() || !SkyType.overworld(level)) {
            lastLevel = level;
            FogState.clear();
            return;
        }
        boolean fresh = level != lastLevel;
        if (fresh) {
            Fog.squall = Compat.modLoaded("squall");
            voxy = Compat.modLoaded("voxy");
        }
        lastLevel = level;
        FogState.remember();

        BlockPos feet = mc.player.blockPosition();
        Biome biome = Rainfall.biome(level, feet);
        float humidity = humidity(biome, Rainfall.key(level, feet));
        boolean cold = biome.getBaseTemperature() < 0.15F;
        float rain = level.getRainLevel(1F);
        long time = Clock.time(level);
        if (fresh) {
            FogState.wetness = rain > 0.5F ? 0.6F : 0F;
        }
        FogState.wetness = Fog.wetness(FogState.wetness, rain, humidity > 0F);
        FogState.rain = rain;
        FogState.thunder = Fog.approach(FogState.thunder, level.getThunderLevel(1F), fresh ? 1F : 0.05F);
        FogState.hour = Fog.hour(time);
        FogState.wisps = config.wisps();
        FogState.lod = voxy ? Sight.viewDistance(mc) : 0F;

        Fog.Layer layer = Fog.layer(new Fog.Conditions(time, SEED, rain, FogState.wetness, humidity, cold,
                config.frequency() / 100F, config.morningFog(), config.rainMist(),
                Fog.squall && Compat.dewyNight(time), config.alwaysFog()));
        float density = layer.density() * config.intensity() / 100F;
        float sea = level.getSeaLevel();
        SwampMask.tick(level, mc.player, config, sea);
        float swamp = config.swampMist() && SwampMask.count > 0 ? Swamp.density(FogState.hour, false) * config.intensity() / 100F : 0F;
        FogState.swamp = Fog.approach(FogState.swamp, swamp, fresh ? 1F : 0.05F);
        FogState.swampBase = sea - 1F;
        float terrain = terrain(level, mc.player.getX(), mc.player.getZ(), sea);
        float base = sea - 2F + (1F - layer.ground()) * Math.max(0F, terrain - sea - 2F);
        float rate = fresh ? 1F : EASE;
        FogState.density = Fog.approach(FogState.density, density, rate);
        FogState.scale = Fog.approach(FogState.scale, layer.scale() * config.depth(), rate);
        FogState.warmth = Fog.approach(FogState.warmth, config.sunGlow() ? layer.warmth() : 0F, rate);
        FogState.base = Fog.approach(FogState.base, base, fresh ? 1F : 0.02F);
        Vec3 eye = Sight.eye(mc.player);
        boolean blocked = blocked(level, mc.player, eye);
        boolean wet = !blocked && mc.player.isEyeInFluid(FluidTags.WATER);
        float cave = config.caveFog() && !wet ? underground(level, eye) : 0F;
        FogState.open = Fog.approach(FogState.open, blocked ? 0F : 1F, fresh ? 1F : 0.3F);
        FogState.cave = Fog.approach(FogState.cave, cave, fresh ? 1F : 0.05F);
        FogState.caveDensity = 0.012F * config.intensity() / 100F;
        float water = wet && config.underwater() ? 1F : 0F;
        if (water > 0F) {
            water(level, eye, config.intensity() / 100F, fresh);
        }
        FogState.water = Fog.approach(FogState.water, water, fresh ? 1F : 0.25F);
        if (fresh) {
            FogState.remember();
        }
    }

    static boolean blocked(ClientLevel level, Player player, Vec3 eye) {
        return player.isEyeInFluid(FluidTags.LAVA)
                || Sight.stuck(level.getBlockState(new BlockPos(Mth.floor(eye.x), Mth.floor(eye.y), Mth.floor(eye.z))))
                || Sight.blind(player);
    }

    static void water(ClientLevel level, Vec3 eye, float intensity, boolean fresh) {
        BlockPos at = new BlockPos(Mth.floor(eye.x), Mth.floor(eye.y), Mth.floor(eye.z));
        int above = 0;
        BlockPos.MutableBlockPos scan = at.mutable();
        for (int i = 0; i < 64; i++) {
            scan.move(0, 1, 0);
            if (!level.getFluidState(scan).is(FluidTags.WATER)) {
                break;
            }
            above++;
        }
        float deep = smooth(0F, 40F, above);
        float sky = level.getBrightness(LightLayer.SKY, at) / 15F;
        float light = 0.2F + 0.8F * sky * Math.max(Fog.daylight(FogState.hour), 0.15F);
        int color = BiomeColors.getAverageWaterColor(level, at);
        float[] wc = {(color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F};
        float[] teal = {0.25F, 0.75F, 0.8F};
        float[] navy = {0.015F, 0.035F, 0.09F};
        for (int i = 0; i < 3; i++) {
            float shallow = (wc[i] + (teal[i] - wc[i]) * 0.35F) * 0.6F;
            float target = (shallow + (navy[i] - shallow) * deep) * light;
            FogState.waterTint[i] = fresh ? target : Fog.approach(FogState.waterTint[i], target, 0.1F);
        }
        FogState.waterDensity = (0.010F + 0.025F * deep) * intensity;
    }

    static float terrain(ClientLevel level, double x, double z, float sea) {
        float sum = 0F;
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int sx = (int) Math.floor(x + dx * 16.0);
                int sz = (int) Math.floor(z + dz * 16.0);
                if (level.hasChunk(sx >> 4, sz >> 4)) {
                    sum += level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                    count++;
                }
            }
        }
        return count == 0 ? sea : sum / count;
    }

    static float underground(ClientLevel level, Vec3 eye) {
        BlockPos at = new BlockPos(Mth.floor(eye.x), Mth.floor(eye.y), Mth.floor(eye.z));
        float sky = level.getBrightness(LightLayer.SKY, at);
        float cover = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - (float) eye.y;
        return (1F - smooth(0F, 10F, sky)) * smooth(3F, 12F, cover);
    }

    private static float smooth(float a, float b, float x) {
        float t = Math.max(0F, Math.min(1F, (x - a) / (b - a)));
        return t * t * (3F - 2F * t);
    }

    static float humidity(Biome biome, String path) {
        if (!Rainfall.falls(biome)) {
            return 0F;
        }
        if (path.contains("swamp") || path.contains("mangrove")) {
            return 1F;
        }
        if (path.contains("jungle")) {
            return 0.9F;
        }
        if (path.contains("river") || path.contains("beach") || path.contains("ocean") || path.contains("shore")) {
            return 0.8F;
        }
        if (path.contains("forest") || path.contains("taiga") || path.contains("grove")) {
            return 0.7F;
        }
        return 0.5F;
    }
}
