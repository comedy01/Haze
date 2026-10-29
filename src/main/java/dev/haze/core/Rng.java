package dev.haze.core;

public final class Rng {
    private long state;

    public Rng(long seed) {
        state = seed;
    }

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static double unit(long key) {
        return (mix(key * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        return mix(state);
    }

    public float nextFloat() {
        return (nextLong() >>> 40) * 0x1.0p-24f;
    }

    public float range(float min, float max) {
        return min + (max - min) * nextFloat();
    }

    public double gaussian() {
        double u = Math.max(1.0e-9, nextFloat());
        double v = nextFloat();
        return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
    }

    public float[] unitVector() {
        float z = range(-1.0F, 1.0F);
        float a = range(0.0F, (float) (Math.PI * 2.0));
        float r = (float) Math.sqrt(1.0F - z * z);
        return new float[]{r * (float) Math.cos(a), r * (float) Math.sin(a), z};
    }
}
