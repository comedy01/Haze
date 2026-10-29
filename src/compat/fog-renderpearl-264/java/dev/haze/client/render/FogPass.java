package dev.haze.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.haze.client.Compat;
import dev.haze.client.FogState;
import dev.haze.client.Frames;
import dev.haze.client.HazeClient;
import dev.haze.client.SwampMask;
import dev.haze.core.Fog;
import dev.haze.core.Swamp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;

public final class FogPass {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("haze");
    private static final BindGroupLayout DEPTH = BindGroupLayout.builder()
            .withUniform("SceneDepth", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("SwampMask", UniformType.COMBINED_IMAGE_SAMPLER)
            .build();
    private static final long START = System.nanoTime();

    private static RenderPipeline pipeline;
    private static TextureTarget copy;
    private static GpuMesh screen;
    private static boolean broken;
    private static int frames;
    private static GpuTexture maskTex;
    private static GpuTextureView maskView;
    private static int maskVersion = -1;

    private FogPass() {
    }

    public static int frames() {
        return frames;
    }

    public static boolean broken() {
        return broken;
    }

    public static void run(RenderTarget main, Vec3 camera, Matrix4f view) {
        FogState.View state = FogState.view(Frames.partialTick(Minecraft.getInstance()));
        if (broken || !state.visible() || Compat.shaderPackInUse()) {
            return;
        }
        try {
            if (pipeline == null) {
                pipeline = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                        .withBindGroupLayout(DEPTH)
                        .withLocation(id("pipeline/fog"))
                        .withVertexShader(id("core/screen"))
                        .withFragmentShader(id("core/fog"))
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withCull(false)
                        .build();
            }
            CompiledRenderPipeline compiled = RenderSystem.getCompiledPipelineNullable(pipeline);
            if (compiled == null) {
                fail("its shader did not compile", null);
                return;
            }
            copyDepth(main);
            ensureScreen();
            float[] tint = Fog.tint(FogState.hour, FogState.rain, FogState.thunder);
            float[] cave = FogState.medium(state, camera.y);
            float sun = (FogState.hour - 6F) / 12F * (float) Math.PI;
            float seconds = (System.nanoTime() - START) / 1.0E9F % 3600F;
            Vector4f color = new Vector4f(tint[0], tint[1], tint[2], state.density());
            Vector3f shape = new Vector3f((float) camera.y - state.base(), state.scale(), FogState.shapeZ(state));
            Matrix4f extra = new Matrix4f()
                    .m00((float) Math.cos(sun)).m01((float) Math.sin(sun))
                    .m02((float) (camera.x - SwampMask.originX)).m03((float) (camera.z - SwampMask.originZ))
                    .m10((float) camera.y - FogState.swampBase).m11(seconds).m12((float) camera.y)
                    .m13(FogState.wisps ? state.swamp() + 1F : -(state.swamp() + 1F))
                    .m20((float) (camera.x % 16384.0)).m21((float) (camera.z % 16384.0))
                    .m22(cave[4]).m23(FogState.lod).m30(cave[0]).m31(cave[1]).m32(cave[2]).m33(cave[3]);
            ensureMask();
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(view, color, shape, extra);
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Haze fog",
                    main.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(compiled);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform("SceneDepth", copy.getDepthTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("SwampMask", maskView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setVertexBuffer(0, ((GpuBuffer) screen.handle).slice());
                pass.setIndexBuffer(indices.getBuffer(6), indices.type());
                pass.drawIndexed(6, 1, 0, 0, 0);
            }
            frames++;
        } catch (RuntimeException e) {
            fail("drawing failed", e);
        }
    }

    private static void copyDepth(RenderTarget main) {
        if (copy == null || copy.width != main.width || copy.height != main.height) {
            if (copy != null) {
                copy.destroyBuffers();
            }
            copy = new TextureTarget("Haze depth copy", main.width, main.height,
                    main.getColorTexture().getFormat(), main.getDepthTexture().getFormat());
        }
        copy.copyDepthFrom(main);
    }

    private static void ensureMask() {
        if (maskTex == null) {
            maskTex = RenderSystem.getDevice().createTexture(() -> "haze swamp mask", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    GpuFormat.RGBA8_UNORM, Swamp.GRID, Swamp.GRID, 1, 1);
            maskView = RenderSystem.getDevice().createTextureView(maskTex);
            maskVersion = -1;
        }
        if (maskVersion != SwampMask.version) {
            maskVersion = SwampMask.version;
            SwampMask.bytes.rewind();
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(maskTex, SwampMask.bytes, 0, 0, 0, 0, Swamp.GRID, Swamp.GRID);
        }
    }

    private static void ensureScreen() {
        if (screen == null) {
            screen = Gpu.mesh("Haze screen", false, 4, w -> {
                w.vertex(-1.0F, -1.0F, 0.0F);
                w.vertex(1.0F, -1.0F, 0.0F);
                w.vertex(1.0F, 1.0F, 0.0F);
                w.vertex(-1.0F, 1.0F, 0.0F);
            });
        }
    }

    private static void fail(String reason, RuntimeException e) {
        broken = true;
        if (e == null) {
            LOGGER.error("Haze turned off fog: {}", reason);
        } else {
            LOGGER.error("Haze turned off fog: {}", reason, e);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HazeClient.MOD_ID, path);
    }
}
