package dev.haze.client.render;

public interface MeshWriter {
    void vertex(float x, float y, float z);

    void vertex(float x, float y, float z, float u, float v, float red, float green, float blue, float alpha);
}
