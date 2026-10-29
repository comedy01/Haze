package dev.haze.client;

import dev.haze.config.HazeConfig;
import dev.haze.core.Swamp;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class SwampMask {
    private static final int GRID = Swamp.GRID;
    private static final int CELLS = GRID * GRID;
    private static final int SWEEP = 512;
    private static final int[] SAMPLES = {Swamp.CELL / 4, Swamp.CELL * 3 / 4};

    public static final ByteBuffer bytes = BufferUtils.createByteBuffer(CELLS * 4);
    public static final IntBuffer buffer = bytes.asIntBuffer();
    public static int originX;
    public static int originZ;
    public static int version;
    public static int count;

    private static final int[] raw = new int[CELLS];
    private static final int[] scratch = new int[CELLS];
    private static int cursor;
    private static ClientLevel lastLevel;

    private SwampMask() {
    }

    public static void tick(ClientLevel level, Player player, HazeConfig config, float sea) {
        if (!config.swampMist()) {
            if (lastLevel != null || count > 0) {
                reset();
                version++;
            }
            lastLevel = null;
            return;
        }
        int ox = Swamp.origin(player.getX());
        int oz = Swamp.origin(player.getZ());
        boolean changed = false;
        if (level != lastLevel) {
            lastLevel = level;
            reset();
            originX = ox;
            originZ = oz;
            changed = true;
        } else if (ox != originX || oz != originZ) {
            shift((ox - originX) / Swamp.CELL, (oz - originZ) / Swamp.CELL);
            originX = ox;
            originZ = oz;
            changed = true;
        }
        int y = (int) sea;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int n = 0; n < SWEEP; n++) {
            int cell = cursor;
            cursor = (cursor + 1) % CELLS;
            int x0 = originX + (cell % GRID) * Swamp.CELL;
            int z0 = originZ + (cell / GRID) * Swamp.CELL;
            int known = 0;
            int wet = 0;
            for (int sx : SAMPLES) {
                for (int sz : SAMPLES) {
                    int x = x0 + sx;
                    int z = z0 + sz;
                    if (!level.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }
                    known++;
                    pos.set(x, y, z);
                    if (HazeRuntime.humidity(Rainfall.biome(level, pos), Rainfall.key(level, pos)) >= Swamp.HUMIDITY) {
                        wet++;
                    }
                }
            }
            if (known == 0) {
                continue;
            }
            int value = wet * 255 / known;
            if (raw[cell] != value) {
                raw[cell] = value;
                changed = true;
            }
        }
        if (changed) {
            blur();
            version++;
        }
    }

    private static void reset() {
        java.util.Arrays.fill(raw, 0);
        cursor = 0;
        blur();
    }

    private static void shift(int dx, int dz) {
        for (int j = 0; j < GRID; j++) {
            for (int i = 0; i < GRID; i++) {
                int si = i + dx;
                int sj = j + dz;
                boolean inside = si >= 0 && si < GRID && sj >= 0 && sj < GRID;
                scratch[j * GRID + i] = inside ? raw[sj * GRID + si] : 0;
            }
        }
        System.arraycopy(scratch, 0, raw, 0, CELLS);
    }

    private static void blur() {
        count = 0;
        for (int j = 0; j < GRID; j++) {
            for (int i = 0; i < GRID; i++) {
                int sum = 0;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        int si = i + di;
                        int sj = j + dj;
                        if (si >= 0 && si < GRID && sj >= 0 && sj < GRID) {
                            sum += raw[sj * GRID + si] * (di == 0 ? 2 : 1) * (dj == 0 ? 2 : 1);
                        }
                    }
                }
                int cell = j * GRID + i;
                if (raw[cell] != 0) {
                    count++;
                }
                boolean rim = i == 0 || j == 0 || i == GRID - 1 || j == GRID - 1;
                bytes.put(cell * 4, (byte) (rim ? 0 : Math.min(255, sum / 16)));
            }
        }
    }
}
