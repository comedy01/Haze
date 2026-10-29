package dev.haze.mixin;

import dev.haze.client.render.LevelProjection;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
abstract class ProjectionCaptureMixin {
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private Matrix4f haze$projection(Matrix4f projection) {
        LevelProjection.capture(projection);
        return projection;
    }
}
