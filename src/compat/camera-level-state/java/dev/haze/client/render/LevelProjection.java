package dev.haze.client.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class LevelProjection {
    private static final Matrix4f MATRIX = new Matrix4f();
    private static boolean captured;

    private LevelProjection() {
    }

    public static void capture(Matrix4fc projection) {
        MATRIX.set(projection);
        captured = true;
    }

    public static float[] take(Matrix4fc fallback) {
        float[] result = (captured ? MATRIX : fallback).get(new float[16]);
        captured = false;
        return result;
    }
}
