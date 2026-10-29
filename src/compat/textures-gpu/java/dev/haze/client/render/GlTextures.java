package dev.haze.client.render;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;

public final class GlTextures {
    private GlTextures() {
    }

    private static int name(GpuTexture texture) {
        for (int depth = 0; depth < 4 && !(texture instanceof GlTexture) && texture != null; depth++) {
            try {
                texture = (GpuTexture) texture.getClass().getMethod("getRealTexture").invoke(texture);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("not an OpenGL texture: " + texture.getClass().getName(), e);
            }
        }
        return ((GlTexture) texture).glId();
    }

    public static int mainColor() {
        return name(MainTarget.get().getColorTexture());
    }

    public static int mainDepth() {
        return name(MainTarget.get().getDepthTexture());
    }
}
