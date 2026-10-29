package dev.haze.core;

public final class Vec {
    private Vec() {
    }

    public static float[] of(float x, float y, float z) {
        return new float[]{x, y, z};
    }

    public static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static float[] cross(float[] a, float[] b) {
        return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }

    public static float[] normalize(float[] a) {
        float length = (float) Math.sqrt(dot(a, a));
        if (length < 1.0e-6F) {
            return new float[]{0.0F, 1.0F, 0.0F};
        }
        return new float[]{a[0] / length, a[1] / length, a[2] / length};
    }

    public static float[] scale(float[] a, float s) {
        return new float[]{a[0] * s, a[1] * s, a[2] * s};
    }

    public static float[] add(float[] a, float[] b) {
        return new float[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    public static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    public static float[] axpy(float[] a, float s, float[] b) {
        return new float[]{a[0] + s * b[0], a[1] + s * b[1], a[2] + s * b[2]};
    }

    public static float[] anyPerpendicular(float[] a) {
        float[] helper = Math.abs(a[1]) < 0.9F ? of(0.0F, 1.0F, 0.0F) : of(1.0F, 0.0F, 0.0F);
        return normalize(cross(a, helper));
    }

    public static float[] tangentAway(float[] position, float[] from) {
        float[] t = sub(scale(position, dot(position, from)), from);
        return dot(t, t) < 1.0e-8F ? anyPerpendicular(position) : normalize(t);
    }
}
