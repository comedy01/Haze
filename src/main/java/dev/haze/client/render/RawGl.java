package dev.haze.client.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class RawGl {
    private static final Logger LOGGER = LogManager.getLogger("haze");
    private static final int UNITS = 8;
    private static final int PLAIN_STRIDE = 12;
    private static final int TEXTURED_STRIDE = 24;

    private static Boolean usable;
    private static boolean samplerObjects;
    private static final Map<String, Program> PROGRAMS = new HashMap<>();
    private static int vao;
    private static int indexBuffer;
    private static int indexQuads;
    private static int samplerLinear;
    private static int samplerNearest;
    private static int samplerAtlas;
    private static final float[] PROJECTION = new float[16];

    private RawGl() {
    }

    public static final class Buffer {
        final int id;
        final boolean textured;

        Buffer(int id, boolean textured) {
            this.id = id;
            this.textured = textured;
        }
    }

    public static final class Program {
        final int id;
        final Map<String, Integer> uniforms = new HashMap<>();

        Program(int id) {
            this.id = id;
        }

        int uniform(String name) {
            return uniforms.computeIfAbsent(name, n -> GL20.glGetUniformLocation(id, n));
        }
    }

    public static boolean usable() {
        if (usable == null) {
            try {
                var caps = GL.getCapabilities();
                usable = caps.OpenGL32;
                samplerObjects = caps.OpenGL33 || caps.GL_ARB_sampler_objects;
                if (!usable) {
                    LOGGER.warn("Haze needs OpenGL 3.2 for fog; it is off");
                }
            } catch (RuntimeException | LinkageError e) {
                usable = false;
                LOGGER.warn("Haze found no OpenGL context for fog; it is off");
            }
        }
        return usable;
    }

    public static void projection(float[] projection) {
        System.arraycopy(projection, 0, PROJECTION, 0, 16);
    }

    public static Program program(String vertex, String fragment) {
        String key = vertex + "/" + fragment;
        if (PROGRAMS.containsKey(key)) {
            return PROGRAMS.get(key);
        }
        Program program = null;
        try {
            int vs = compile(GL20.GL_VERTEX_SHADER, vertex + ".vsh");
            int fs = compile(GL20.GL_FRAGMENT_SHADER, fragment + ".fsh");
            if (vs != 0 && fs != 0) {
                int id = GL20.glCreateProgram();
                GL20.glAttachShader(id, vs);
                GL20.glAttachShader(id, fs);
                GL20.glBindAttribLocation(id, 0, "Position");
                GL20.glBindAttribLocation(id, 1, "UV0");
                GL20.glBindAttribLocation(id, 2, "Color");
                GL30.glBindFragDataLocation(id, 0, "fragColor");
                GL20.glLinkProgram(id);
                if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                    LOGGER.error("Haze shader {} did not link: {}", key, GL20.glGetProgramInfoLog(id));
                    GL20.glDeleteProgram(id);
                } else {
                    program = new Program(id);
                }
                GL20.glDetachShader(id, vs);
                GL20.glDetachShader(id, fs);
            }
            if (vs != 0) {
                GL20.glDeleteShader(vs);
            }
            if (fs != 0) {
                GL20.glDeleteShader(fs);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Haze shader {} could not be loaded", key, e);
        }
        PROGRAMS.put(key, program);
        return program;
    }

    private static int compile(int type, String file) throws IOException {
        String source;
        try (InputStream in = RawGl.class.getResourceAsStream("/assets/haze/shaders/raw/" + file)) {
            if (in == null) {
                throw new IOException("missing shader " + file);
            }
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Haze shader {} did not compile: {}", file, GL20.glGetShaderInfoLog(id));
            GL20.glDeleteShader(id);
            return 0;
        }
        return id;
    }

    public static GpuMesh mesh(String name, boolean textured, int vertices, Consumer<MeshWriter> fill) {
        GpuMesh mesh = new GpuMesh(name, textured, vertices);
        int stride = textured ? TEXTURED_STRIDE : PLAIN_STRIDE;
        ByteBuffer bytes = MemoryUtil.memAlloc(stride * Math.max(4, vertices));
        try {
            fill.accept(new MeshWriter() {
                @Override
                public void vertex(float x, float y, float z) {
                    bytes.putFloat(x).putFloat(y).putFloat(z);
                }

                @Override
                public void vertex(float x, float y, float z, float u, float v, float red, float green, float blue,
                                   float alpha) {
                    bytes.putFloat(x).putFloat(y).putFloat(z).putFloat(u).putFloat(v)
                            .put(channel(red)).put(channel(green)).put(channel(blue)).put(channel(alpha));
                }
            });
            bytes.flip();
            int previous = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int id = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bytes, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previous);
            mesh.handle = new Buffer(id, textured);
        } finally {
            MemoryUtil.memFree(bytes);
        }
        return mesh;
    }

    private static byte channel(float value) {
        return (byte) (int) (Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
    }

    public static void close(GpuMesh mesh) {
        if (mesh != null && mesh.handle instanceof Buffer buffer) {
            GL15.glDeleteBuffers(buffer.id);
            mesh.handle = null;
        }
    }

    public enum Blend {
        NONE, ADDITIVE, MULTIPLY, TRANSLUCENT, PREMULTIPLIED, LIGHTEN
    }

    public static Blend blend(GpuPipeline.Blend blend) {
        return switch (blend) {
            case ADDITIVE -> Blend.ADDITIVE;
            case MULTIPLY -> Blend.MULTIPLY;
            case TRANSLUCENT -> Blend.TRANSLUCENT;
            case PREMULTIPLIED -> Blend.PREMULTIPLIED;
        };
    }

    private static void applyBlend(Blend blend) {
        if (blend == Blend.NONE) {
            GL11.glDisable(GL11.GL_BLEND);
            return;
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        switch (blend) {
            case ADDITIVE -> GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            case MULTIPLY -> GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
            case TRANSLUCENT -> GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA);
            case PREMULTIPLIED -> GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            case LIGHTEN -> GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE);
            default -> {
            }
        }
    }

    public record Sampler(String name, int texture, Filter filter) {
    }

    public enum Filter {
        LINEAR, NEAREST, ATLAS
    }

    public static void draw(Program program, GpuMesh mesh, int quads, int baseVertex, Blend blend, boolean depthTest,
                            float[] modelView, float[] color, float[] offset, float[] extra, Sampler... samplers) {
        Buffer buffer = (Buffer) mesh.handle;
        GL20.glUseProgram(program.id);
        setMatrix(program, "ModelViewMat", modelView);
        setMatrix(program, "ProjMat", PROJECTION);
        setMatrix(program, "TextureMat", extra);
        int at = program.uniform("ColorModulator");
        if (at >= 0) {
            GL20.glUniform4f(at, color[0], color[1], color[2], color[3]);
        }
        at = program.uniform("ModelOffset");
        if (at >= 0) {
            GL20.glUniform3f(at, offset[0], offset[1], offset[2]);
        }
        for (int unit = 0; unit < samplers.length; unit++) {
            Sampler sampler = samplers[unit];
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sampler.texture);
            if (samplerObjects) {
                GL33.glBindSampler(unit, switch (sampler.filter) {
                    case LINEAR -> samplerLinear;
                    case NEAREST -> samplerNearest;
                    case ATLAS -> samplerAtlas;
                });
            }
            at = program.uniform(sampler.name);
            if (at >= 0) {
                GL20.glUniform1i(at, unit);
            }
        }
        applyBlend(blend);
        if (depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthMask(false);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer.id);
        int stride = buffer.textured ? TEXTURED_STRIDE : PLAIN_STRIDE;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
        if (buffer.textured) {
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 12L);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 20L);
        } else {
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
        }
        ensureIndices(quads);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, quads * 6, GL11.GL_UNSIGNED_INT, 0L, baseVertex);
    }

    private static void setMatrix(Program program, String name, float[] matrix) {
        int at = program.uniform(name);
        if (at >= 0) {
            GL20.glUniformMatrix4fv(at, false, matrix);
        }
    }

    private static void ensureIndices(int quads) {
        if (quads <= indexQuads) {
            return;
        }
        int size = Math.max(quads, Math.max(1024, indexQuads * 2));
        IntBuffer indices = MemoryUtil.memAllocInt(size * 6);
        try {
            for (int q = 0; q < size; q++) {
                int v = q * 4;
                indices.put(v).put(v + 1).put(v + 2).put(v + 2).put(v + 3).put(v);
            }
            indices.flip();
            if (indexBuffer == 0) {
                indexBuffer = GL15.glGenBuffers();
            }
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
            indexQuads = size;
        } finally {
            MemoryUtil.memFree(indices);
        }
    }

    public static final class Target {
        public int width;
        public int height;
        public int color;
        public int depth;
        public int fbo;
        int colorFormat;
        int depthFormat;

        public void ensure(int width, int height, int colorFormat, int depthFormat) {
            if (fbo != 0 && this.width == width && this.height == height && this.colorFormat == colorFormat
                    && this.depthFormat == depthFormat) {
                return;
            }
            delete();
            this.width = width;
            this.height = height;
            this.colorFormat = colorFormat;
            this.depthFormat = depthFormat;
            int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            color = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, color);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, colorFormat, width, height, 0, GL11.GL_RGBA,
                    colorFormat == GL30.GL_RGBA16F ? GL11.GL_FLOAT : GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            plainParameters();
            if (depthFormat != 0) {
                depth = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, depth);
                boolean stencil = hasStencil(depthFormat);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, depthFormat, width, height, 0,
                        stencil ? GL30.GL_DEPTH_STENCIL : GL11.GL_DEPTH_COMPONENT,
                        stencil ? GL30.GL_UNSIGNED_INT_24_8 : GL11.GL_FLOAT, (ByteBuffer) null);
                plainParameters();
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            int previousFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            fbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, color, 0);
            if (depth != 0) {
                GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,
                        hasStencil(depthFormat) ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT,
                        GL11.GL_TEXTURE_2D, depth, 0);
            }
            int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFbo);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                delete();
                throw new IllegalStateException("framebuffer incomplete: 0x" + Integer.toHexString(status));
            }
        }

        public void delete() {
            if (fbo != 0) {
                GL30.glDeleteFramebuffers(fbo);
            }
            if (color != 0) {
                GL11.glDeleteTextures(color);
            }
            if (depth != 0) {
                GL11.glDeleteTextures(depth);
            }
            fbo = 0;
            color = 0;
            depth = 0;
        }
    }

    static boolean hasStencil(int format) {
        return format == GL30.GL_DEPTH24_STENCIL8 || format == GL30.GL_DEPTH32F_STENCIL8;
    }

    private static void plainParameters() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
    }

    public static int formatOf(int texture) {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        int format = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        return format;
    }

    public static int[] sizeOf(int texture) {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        int[] size = {GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH),
                GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT)};
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        return size;
    }

    public static final class Attached {
        int fbo;
        int color = -1;
        int depth = -1;

        public int bind(int colorTexture, int depthTexture) {
            if (fbo == 0 || color != colorTexture || depth != depthTexture) {
                if (fbo == 0) {
                    fbo = GL30.glGenFramebuffers();
                }
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
                GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                        colorTexture, 0);
                int format = formatOf(depthTexture);
                GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT,
                        GL11.GL_TEXTURE_2D, 0, 0);
                GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,
                        hasStencil(format) ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT,
                        GL11.GL_TEXTURE_2D, depthTexture, 0);
                int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
                if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    color = -1;
                    throw new IllegalStateException("main framebuffer incomplete: 0x" + Integer.toHexString(status));
                }
                color = colorTexture;
                depth = depthTexture;
            } else {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
            }
            return fbo;
        }
    }

    public static final class Frame implements AutoCloseable {
        private final int program;
        private final int vertexArray;
        private final int arrayBuffer;
        private final int drawFbo;
        private final int readFbo;
        private final int activeTexture;
        private final int[] textures = new int[UNITS];
        private final int[] samplers = new int[UNITS];
        private final int[] viewport = new int[4];
        private final boolean blend;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final int blendEquationRgb;
        private final int blendEquationAlpha;
        private final boolean depthTest;
        private final int depthFunc;
        private final boolean depthMask;
        private final boolean cull;
        private final boolean scissor;
        private final boolean stencil;
        private final boolean polygonOffset;
        private final boolean[] colorMask = new boolean[4];
        private final int unpackAlignment;
        private final int unpackRowLength;
        private final int unpackSkipRows;
        private final int unpackSkipPixels;
        private final int unpackBuffer;

        public Frame() {
            program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            for (int unit = 0; unit < UNITS; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                textures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                samplers[unit] = samplerObjects ? GL11.glGetInteger(GL33.GL_SAMPLER_BINDING) : 0;
            }
            GL13.glActiveTexture(activeTexture);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer v = stack.mallocInt(4);
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, v);
                v.get(viewport);
                ByteBuffer mask = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
                for (int i = 0; i < 4; i++) {
                    colorMask[i] = mask.get(i) != 0;
                }
            }
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
            blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
            depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
            polygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
            unpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
            unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
            unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
            unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
            unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);

            if (vao == 0) {
                vao = GL30.glGenVertexArrays();
            }
            if (samplerObjects && samplerLinear == 0) {
                samplerLinear = sampler(GL11.GL_LINEAR, GL11.GL_LINEAR);
                samplerNearest = sampler(GL11.GL_NEAREST, GL11.GL_NEAREST);
                samplerAtlas = sampler(GL11.GL_NEAREST_MIPMAP_NEAREST, GL11.GL_NEAREST);
            }
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glColorMask(true, true, true, true);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        }

        @Override
        public void close() {
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
            for (int unit = 0; unit < UNITS; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[unit]);
                if (samplerObjects) {
                    GL33.glBindSampler(unit, samplers[unit]);
                }
            }
            GL13.glActiveTexture(activeTexture);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            set(GL11.GL_BLEND, blend);
            GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            set(GL11.GL_DEPTH_TEST, depthTest);
            GL11.glDepthFunc(depthFunc);
            GL11.glDepthMask(depthMask);
            set(GL11.GL_CULL_FACE, cull);
            set(GL11.GL_SCISSOR_TEST, scissor);
            set(GL11.GL_STENCIL_TEST, stencil);
            set(GL11.GL_POLYGON_OFFSET_FILL, polygonOffset);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        }

        private static void set(int capability, boolean on) {
            if (on) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }

    private static int sampler(int min, int mag) {
        int id = GL33.glGenSamplers();
        GL33.glSamplerParameteri(id, GL11.GL_TEXTURE_MIN_FILTER, min);
        GL33.glSamplerParameteri(id, GL11.GL_TEXTURE_MAG_FILTER, mag);
        GL33.glSamplerParameteri(id, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL33.glSamplerParameteri(id, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL33.glSamplerParameteri(id, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        return id;
    }

    public static void target(int fbo, int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, width, height);
    }

    public static void clear(float r, float g, float b, float a) {
        GL11.glClearColor(r, g, b, a);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public static void copyDepth(int from, int to, int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
    }

    public static int texture(int texture, int width, int height) {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (texture == 0) {
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            plainParameters();
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        return texture;
    }

    public static void upload(int texture, int y, int width, int rows, IntBuffer pixels) {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, y, width, rows, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
    }

    public static void deleteTexture(int texture) {
        if (texture != 0) {
            GL11.glDeleteTextures(texture);
        }
    }
}
