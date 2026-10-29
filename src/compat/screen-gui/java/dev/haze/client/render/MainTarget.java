package dev.haze.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

public final class MainTarget {
    private MainTarget() {
    }

    public static RenderTarget get() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }
}
