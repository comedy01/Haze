package dev.haze.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.haze.client.render.FrameView;
import dev.haze.client.render.LevelProjection;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FogPassMixin {
    @Inject(method = {"render", "renderLevel"}, at = @At("HEAD"))
    private void haze$camera(CallbackInfo ci, @Local(argsOnly = true) CameraRenderState camera) {
        FrameView.set(camera.pos.x, camera.pos.y, camera.pos.z, camera.viewRotationMatrix.get(new float[16]),
                LevelProjection.take(camera.projectionMatrix));
    }
}
