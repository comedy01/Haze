package dev.haze.client.render;

public final class GpuMesh {
    public final String name;
    public final boolean textured;
    public final int vertices;
    public Object handle;

    public GpuMesh(String name, boolean textured, int vertices) {
        this.name = name;
        this.textured = textured;
        this.vertices = vertices;
    }
}
