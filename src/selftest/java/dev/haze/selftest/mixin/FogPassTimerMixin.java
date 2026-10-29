package dev.haze.selftest.mixin;

import dev.haze.selftest.FogTimer;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.haze.client.render.FogPass")
abstract class FogPassTimerMixin {
    @Inject(method = "run", at = @At("HEAD"), require = 0, remap = false)
    private static void hazeSelftest$begin(CallbackInfo ci) {
        if (Boolean.getBoolean("haze.gputimer")) {
            FogTimer.begin();
        }
    }

    @Inject(method = "run", at = @At("RETURN"), require = 0, remap = false)
    private static void hazeSelftest$end(CallbackInfo ci) {
        if (Boolean.getBoolean("haze.gputimer")) {
            FogTimer.end();
        }
    }
}
