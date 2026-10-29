package dev.haze.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.haze.client.render.FrameView;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FogPassMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void haze$camera(CallbackInfo ci, @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true, ordinal = 0) Matrix4f view, @Local(argsOnly = true, ordinal = 1) Matrix4f projection) {
        Vec3 pos = camera.getPosition();
        FrameView.set(pos.x, pos.y, pos.z, view.get(new float[16]), projection.get(new float[16]));
    }
}
