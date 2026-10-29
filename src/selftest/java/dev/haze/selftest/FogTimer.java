package dev.haze.selftest;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

public final class FogTimer {
    private static final int SLOTS = 8;
    private static int[] queries;
    private static final boolean[] pending = new boolean[SLOTS];
    private static int slot;
    private static long total;
    private static int count;

    private FogTimer() {
    }

    public static void begin() {
        if (queries == null) {
            queries = new int[SLOTS * 2];
            GL15.glGenQueries(queries);
        }
        if (pending[slot]) {
            long start = GL33.glGetQueryObjecti64(queries[slot * 2], GL15.GL_QUERY_RESULT);
            long end = GL33.glGetQueryObjecti64(queries[slot * 2 + 1], GL15.GL_QUERY_RESULT);
            total += end - start;
            count++;
            pending[slot] = false;
        }
        GL33.glQueryCounter(queries[slot * 2], GL33.GL_TIMESTAMP);
    }

    public static void end() {
        if (queries == null) {
            return;
        }
        GL33.glQueryCounter(queries[slot * 2 + 1], GL33.GL_TIMESTAMP);
        pending[slot] = true;
        slot = (slot + 1) % SLOTS;
    }

    static void reset() {
        total = 0;
        count = 0;
    }

    static double averageMs() {
        return count == 0 ? 0 : total / 1e6 / count;
    }

    static int count() {
        return count;
    }
}
