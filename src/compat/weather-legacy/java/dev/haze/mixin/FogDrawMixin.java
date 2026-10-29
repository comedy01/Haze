package dev.haze.mixin;

import dev.haze.client.render.FrameView;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class FogDrawMixin {
    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private void haze$fog(CallbackInfo ci) {
        FrameView.runFog();
    }
}
