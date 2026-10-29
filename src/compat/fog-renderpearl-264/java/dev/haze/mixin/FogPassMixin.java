package dev.haze.mixin;

import dev.haze.client.render.FogPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FogPassMixin {
    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;executeOutline(Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V"))
    private void haze$fog(CallbackInfo ci) {
        CameraRenderState camera = levelRenderState.cameraRenderState;
        FogPass.run(targets.main.get(), camera.pos, camera.viewRotationMatrix);
    }
}
