package dev.haze.client.render;

public final class FrameView {
    private static final float[] VIEW = new float[16];
    private static final float[] PROJECTION = new float[16];
    private static double x;
    private static double y;
    private static double z;
    private static boolean fresh;

    private FrameView() {
    }

    public static void set(double camX, double camY, double camZ, float[] view, float[] projection) {
        x = camX;
        y = camY;
        z = camZ;
        System.arraycopy(view, 0, VIEW, 0, 16);
        System.arraycopy(projection, 0, PROJECTION, 0, 16);
        fresh = true;
    }

    public static void runFog() {
        if (!fresh) {
            return;
        }
        fresh = false;
        FogPass.run(x, y, z, VIEW.clone(), PROJECTION.clone());
    }
}
