package dev.haze.client.render;

import dev.haze.client.Compat;
import dev.haze.client.FogState;
import dev.haze.client.Frames;
import dev.haze.client.SwampMask;
import dev.haze.core.Fog;
import dev.haze.core.Swamp;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class FogPass {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("haze");
    private static final long START = System.nanoTime();
    private static final RawGl.Attached MAIN = new RawGl.Attached();
    private static final RawGl.Target COPY = new RawGl.Target();

    private static RawGl.Program program;
    private static GpuMesh screen;
    private static boolean broken;
    private static int frames;
    private static int maskTex;
    private static int maskVersion = -1;

    private FogPass() {
    }

    public static int frames() {
        return frames;
    }

    public static boolean broken() {
        return broken;
    }

    public static void run(double x, double y, double z, float[] view, float[] projection) {
        FogState.View state = FogState.view(Frames.partialTick(Minecraft.getInstance()));
        if (broken || !state.visible() || Compat.shaderPackInUse() || !RawGl.usable()) {
            return;
        }
        try (RawGl.Frame frame = new RawGl.Frame()) {
            RawGl.projection(projection);
            if (program == null) {
                program = RawGl.program("screen", "fog");
                if (program == null) {
                    fail("its shader did not compile", null);
                    return;
                }
            }
            if (screen == null) {
                screen = RawGl.mesh("Haze screen", false, 4, w -> {
                    w.vertex(-1.0F, -1.0F, 0.0F);
                    w.vertex(1.0F, -1.0F, 0.0F);
                    w.vertex(1.0F, 1.0F, 0.0F);
                    w.vertex(-1.0F, 1.0F, 0.0F);
                });
            }
            int color = GlTextures.mainColor();
            int depth = GlTextures.mainDepth();
            int[] size = RawGl.sizeOf(color);
            int main = MAIN.bind(color, depth);
            COPY.ensure(size[0], size[1], GL11.GL_RGBA8, RawGl.formatOf(depth));
            RawGl.copyDepth(main, COPY.fbo, size[0], size[1]);
            RawGl.target(main, size[0], size[1]);
            float[] tint = Fog.tint(FogState.hour, FogState.rain, FogState.thunder);
            float[] cave = FogState.medium(state, y);
            float sun = (FogState.hour - 6F) / 12F * (float) Math.PI;
            float seconds = (System.nanoTime() - START) / 1.0E9F % 3600F;
            float[] fog = {tint[0], tint[1], tint[2], state.density()};
            float[] shape = {(float) y - state.base(), state.scale(), FogState.shapeZ(state)};
            float[] extra = {
                    (float) Math.cos(sun), (float) Math.sin(sun), (float) (x - SwampMask.originX), (float) (z - SwampMask.originZ),
                    (float) y - FogState.swampBase, seconds, (float) y, FogState.wisps ? state.swamp() + 1F : -(state.swamp() + 1F),
                    (float) (x % 16384.0), (float) (z % 16384.0), cave[4], FogState.lod,
                    cave[0], cave[1], cave[2], cave[3]};
            if (SwampMask.version != maskVersion) {
                maskTex = RawGl.texture(maskTex, Swamp.GRID, Swamp.GRID);
                RawGl.upload(maskTex, 0, Swamp.GRID, Swamp.GRID, SwampMask.buffer);
                maskVersion = SwampMask.version;
            }
            RawGl.draw(program, screen, 1, 0, RawGl.Blend.PREMULTIPLIED, false, view, fog, shape, extra,
                    new RawGl.Sampler("SceneDepth", COPY.depth, RawGl.Filter.NEAREST),
                    new RawGl.Sampler("SwampMask", maskTex, RawGl.Filter.LINEAR));
            frames++;
        } catch (RuntimeException e) {
            fail("drawing failed", e);
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
}
