package dev.haze.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import dev.haze.client.render.FrameView;
import java.nio.FloatBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FogPassMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void haze$camera(PoseStack pose, float partialTick, long finishNanoTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        FrameView.set(pos.x, pos.y, pos.z, floats(pose.last().pose()), floats(projection));
    }

    private static float[] floats(Matrix4f matrix) {
        float[] result = new float[16];
        matrix.store(FloatBuffer.wrap(result));
        return result;
    }
}
