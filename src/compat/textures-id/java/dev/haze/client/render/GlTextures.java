package dev.haze.client.render;

public final class GlTextures {
    private GlTextures() {
    }

    public static int mainColor() {
        return MainTarget.get().getColorTextureId();
    }

    public static int mainDepth() {
        return MainTarget.get().getDepthTextureId();
    }
}
