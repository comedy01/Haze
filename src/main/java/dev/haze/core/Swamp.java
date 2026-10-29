package dev.haze.core;

public final class Swamp {
    public static final int GRID = 128;
    public static final int CELL = 16;
    public static final int SNAP = 512;
    public static final float HUMIDITY = 0.95F;

    private Swamp() {
    }

    public static float density(float hour, boolean cold) {
        return (0.22F + 0.12F * Fog.nightness(hour)) * (cold ? 0.6F : 1F);
    }

    public static int origin(double coord) {
        return Math.floorDiv((int) Math.floor(coord), SNAP) * SNAP - (GRID * CELL - SNAP) / 2;
    }

    public static int center(int origin, int cell) {
        return origin + cell * CELL + CELL / 2;
    }
}
