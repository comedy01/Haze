package dev.haze.client.render;

public final class GpuPipeline {
    public enum Blend {
        ADDITIVE, MULTIPLY, TRANSLUCENT, PREMULTIPLIED
    }

    public final String name;
    public final String vertex;
    public final String fragment;
    public final boolean textured;
    public final Blend blend;
    public final boolean depth;
    public Object handle;
    public boolean failed;

    public GpuPipeline(String name, String vertex, String fragment, boolean textured, Blend blend) {
        this(name, vertex, fragment, textured, blend, false);
    }

    public GpuPipeline(String name, String vertex, String fragment, boolean textured, Blend blend, boolean depth) {
        this.name = name;
        this.vertex = vertex;
        this.fragment = fragment;
        this.textured = textured;
        this.blend = blend;
        this.depth = depth;
    }
}
