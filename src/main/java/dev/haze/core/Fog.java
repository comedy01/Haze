package dev.haze.core;

public final class Fog {
    public static boolean squall;

    public static final long DAY = 24000L;
    public static final float ALWAYS = 0.55F;

    private Fog() {
    }

    public record Conditions(long time, long seed, float rain, float wetness, float humidity, boolean cold,
                             float frequency, boolean morning, boolean rainMist, boolean dewy,
                             boolean always) {
    }

    public record Layer(float density, float scale, float warmth, float ground) {
        public static final Layer NONE = new Layer(0F, 1F, 0F, 1F);
    }

    public static float hour(long time) {
        return Math.floorMod(time + 6000L, DAY) / 1000F;
    }

    public static long night(long time) {
        return Math.floorDiv(time + 12000L, DAY);
    }

    public static float chance(float humidity, float wetness, float frequency) {
        return chance(humidity, wetness, frequency, false);
    }

    public static float chance(float humidity, float wetness, float frequency, boolean dewy) {
        float base = 0.12F + 0.38F * humidity + (dewy ? 0.4F : 0F);
        return clamp((base + 0.45F * wetness) * frequency, 0F, 0.95F);
    }

    public static float strength(long seed, long night, float humidity, float wetness, float frequency) {
        return strength(seed, night, humidity, wetness, frequency, false);
    }

    public static float strength(long seed, long night, float humidity, float wetness, float frequency, boolean dewy) {
        double roll = Rng.unit(seed ^ night * 0x5DEECE66DL);
        float p = chance(humidity, wetness, frequency, dewy);
        if (roll >= p) {
            return 0F;
        }
        double depth = Rng.unit(seed + night * 0x2545F4914F6CDD1DL);
        return clamp((float) (0.35 + 0.65 * depth) * (0.75F + 0.25F * humidity), 0F, 1F);
    }

    public static float morningCurve(float hour, float strength) {
        if (strength <= 0F) {
            return 0F;
        }
        float s = (hour - 20F + 24F) % 24F;
        float burnStart = 10.5F + strength;
        float burnEnd = 11F + 3F * strength;
        if (s < 9.5F) {
            return smooth(1F, 9.5F, s);
        }
        if (s < burnStart) {
            return 1F;
        }
        if (s < burnEnd) {
            return 1F - smooth(burnStart, burnEnd, s);
        }
        return 0F;
    }

    public static Layer layer(Conditions c) {
        if (c.humidity() <= 0F && !c.always()) {
            return Layer.NONE;
        }
        float hour = hour(c.time());
        float morning = 0F;
        float strength = 0F;
        if (c.morning()) {
            strength = strength(c.seed(), night(c.time()), c.humidity(), c.wetness(), c.frequency(), c.dewy());
            morning = morningCurve(hour, strength) * strength;
        }
        if (c.always()) {
            strength = Math.max(strength, ALWAYS);
            morning = Math.max(morning, ALWAYS);
        }
        float mist = 0F;
        if (c.rainMist()) {
            mist = (0.35F * (squall ? 0.25F : 1F) * c.rain() + 0.4F * linger(c.wetness(), hour) * (1F - c.rain())) * (0.5F + 0.5F * c.humidity());
        }
        float density = Math.max(morning, mist);
        if (c.cold()) {
            density *= 0.6F;
        }
        if (density <= 0.001F) {
            return Layer.NONE;
        }
        float groundShare = morning / (morning + mist);
        float scale = 0.6F + 0.4F * strength + 2.4F * (1F - groundShare);
        return new Layer(density, scale, morning > 0F ? sunWarmth(hour) : 0F, groundShare);
    }

    public static float linger(float wetness, float hour) {
        float w = clamp(wetness, 0F, 1F);
        return w * w * w * (1F - daylight(hour));
    }

    public static float sunWarmth(float hour) {
        if (hour < 4.5F || hour > 10F) {
            return 0F;
        }
        return smooth(4.5F, 6F, hour) * (1F - smooth(7.5F, 10F, hour));
    }

    public static float daylight(float hour) {
        return smooth(4.5F, 7F, hour) * (1F - smooth(17F, 19.5F, hour));
    }

    public static float[] tint(float hour, float rain) {
        return tint(hour, rain, 0F);
    }

    public static float[] tint(float hour, float rain, float thunder) {
        float day = daylight(hour);
        float dim = 1F - 0.45F * rain;
        float r = (0.16F + (0.80F - 0.16F) * day) * dim;
        float g = (0.18F + (0.82F - 0.18F) * day) * dim;
        float bl = (0.23F + (0.85F - 0.23F) * day) * dim;
        if (squall && rain > 0F) {
            float light = (0.25F + 0.75F * day) * (1F - 0.6F * thunder);
            float t = 0.6F * rain;
            r += (0.52F * light - r) * t;
            g += (0.6F * light - g) * t;
            bl += (0.72F * light - bl) * t;
        }
        float storm = 1F - 0.7F * Math.max(0F, Math.min(1F, thunder));
        return new float[]{r * storm, g * storm, bl * storm};
    }

    public static float nightness(float hour) {
        if (hour >= 19F || hour < 5F) {
            return 1F;
        }
        if (hour < 8F) {
            return 1F - smooth(5F, 8F, hour);
        }
        if (hour > 16F) {
            return smooth(16F, 19F, hour);
        }
        return 0F;
    }

    public static float wetness(float wetness, float rain, boolean rainy) {
        if (rainy && rain > 0.2F) {
            return Math.min(1F, wetness + rain / 2400F);
        }
        return Math.max(0F, wetness - 1F / 36000F);
    }

    public static float approach(float current, float target, float rate) {
        return current + (target - current) * clamp(rate, 0F, 1F);
    }

    static float smooth(float a, float b, float x) {
        float t = clamp((x - a) / (b - a), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
