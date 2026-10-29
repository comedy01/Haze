package dev.haze.client;

import dev.haze.core.Fog;

public final class FogState {
    public static float density;
    public static float scale = 1F;
    public static float warmth;
    public static float base;
    public static float cave;
    public static float caveDensity;
    public static float water;
    public static float open = 1F;
    public static float waterDensity;
    public static float[] waterTint = {0.1F, 0.3F, 0.4F};
    public static float wetness;
    public static float rain;
    public static float thunder;
    public static float hour;
    public static boolean wisps;
    public static float lod;
    public static float swamp;
    public static float swampBase;

    private static float lastDensity;
    private static float lastScale = 1F;
    private static float lastWarmth;
    private static float lastBase;
    private static float lastCave;
    private static float lastWater;
    private static float lastOpen = 1F;
    private static float lastSwamp;

    private FogState() {
    }

    public record View(float density, float scale, float warmth, float base, float cave, float water, float swamp) {
        public boolean visible() {
            return density >= 0.002F || cave >= 0.002F || water >= 0.002F || swamp >= 0.002F;
        }
    }

    public static View view(float partial) {
        float t = Math.max(0F, Math.min(1F, partial));
        float clear = lerp(lastOpen, open, t);
        return new View(lerp(lastDensity, density, t) * clear, lerp(lastScale, scale, t),
                lerp(lastWarmth, warmth, t), lerp(lastBase, base, t), lerp(lastCave, cave, t) * clear,
                lerp(lastWater, water, t) * clear, lerp(lastSwamp, swamp, t) * clear);
    }

    public static float[] caveTint(double y) {
        float t = (float) Math.max(0.0, Math.min(1.0, (y + 40.0) / 80.0));
        float shade = 0.55F + 0.45F * t;
        return new float[]{0.05F * shade, 0.055F * shade, 0.07F * shade};
    }

    public static float[] medium(View state, double y) {
        float[] cave = caveTint(y);
        float medium = Math.max(state.cave(), state.water());
        float w = medium > 0F ? state.water() / medium : 0F;
        return new float[]{
                lerp(cave[0], waterTint[0], w),
                lerp(cave[1], waterTint[1], w),
                lerp(cave[2], waterTint[2], w),
                lerp(caveDensity, waterDensity, w),
                medium};
    }

    public static float shapeZ(View state) {
        return state.water() > 0.001F ? -state.water() : state.warmth();
    }

    static void remember() {
        lastDensity = density;
        lastScale = scale;
        lastWarmth = warmth;
        lastBase = base;
        lastCave = cave;
        lastWater = water;
        lastOpen = open;
        lastSwamp = swamp;
    }

    static void clear() {
        density = 0F;
        warmth = 0F;
        rain = 0F;
        thunder = 0F;
        cave = 0F;
        water = 0F;
        swamp = 0F;
        remember();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float[] sky(double y) {
        View state = view(1F);
        if (state.density() < 0.002F || state.water() > 0.5F) {
            return new float[5];
        }
        float[] tint = Fog.tint(hour, rain, thunder);
        float k = state.density() * 0.06F;
        float h0 = (float) y - state.base();
        float scale = Math.max(state.scale(), 0.5F);
        float keep = 1F - state.cave();
        float horizon = Math.min(0.97F, 1F - (float) Math.exp(-column(k, h0, scale, 0.02F, 100000F) * keep));
        float zenith = Math.min(0.97F, 1F - (float) Math.exp(-column(k, h0, scale, 0.5F, 100000F) * keep));
        return new float[]{tint[0], tint[1], tint[2], horizon, zenith};
    }

    private static float integral(float k, float h0, float scale, float dy, float len) {
        float lowest = -1.5F * scale;
        float h1 = h0 + dy * len;
        if (Math.abs(dy * len) < 1e-3F * scale) {
            return k * len * (float) Math.exp(-Math.max(0.5F * (h0 + h1), lowest) / scale);
        }
        float cut = Math.max(0F, Math.min(len, (lowest - h0) / dy));
        float low = dy > 0F ? cut : len - cut;
        float high = scale * (float) (Math.exp(-Math.max(h0, lowest) / scale) - Math.exp(-Math.max(h1, lowest) / scale)) / dy;
        return k * ((float) Math.exp(1.5) * low + Math.max(high, 0F));
    }

    private static float column(float k, float h0, float scale, float dy, float len) {
        float top = 3F * scale;
        float ta = 0F;
        float tb = len;
        if (Math.abs(dy) < 1e-6F) {
            tb = h0 < top ? len : 0F;
        } else if (dy > 0F) {
            tb = Math.min(len, (top - h0) / dy);
        } else {
            ta = Math.max(0F, (top - h0) / dy);
        }
        if (tb <= ta) {
            return 0F;
        }
        return Math.max(integral(k, h0 + dy * ta, scale, dy, tb - ta) - k * (float) Math.exp(-3.0) * (tb - ta), 0F);
    }
}
