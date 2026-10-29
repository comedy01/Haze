package dev.haze.mixin;

import dev.haze.client.HazeRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void haze$keybinds(CallbackInfo ci) {
        HazeRuntime.beforeKeybinds();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void haze$tick(CallbackInfo ci) {
        HazeRuntime.tick();
    }
}
