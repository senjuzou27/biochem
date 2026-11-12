package ru.ufamod.world;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.entity.NpcRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory voxel buffer the city is "drawn" into before being pasted into the world.
 * Local y = 0 is the ground surface block, y = 1 is the first air block above the ground.
 */
public final class Canvas {
    public static final int MIN_Y = -8;
    public static final int SY = 88;

    /** World coordinates of local (0, 0, 0). */
    public final int ox, baseY, oz;
    public final int sx, sz;

    private final short[] data;
    private final List<BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Short> ids = new HashMap<>();

    public final List<SignSpec> signs = new ArrayList<>();
    public final List<NpcSpec> npcs = new ArrayList<>();
    public final List<ChestSpec> chests = new ArrayList<>();
    public final List<int[]> lecterns = new ArrayList<>();

    public record SignSpec(int x, int y, int z, String[] lines) {}

    public record NpcSpec(double x, int y, double z, float yaw, NpcRole role, String name, int variant, boolean wander) {}

    public record ChestSpec(int x, int y, int z, String loot) {}

    public Canvas(int ox, int baseY, int oz, int sx, int sz) {
        this.ox = ox;
        this.baseY = baseY;
        this.oz = oz;
        this.sx = sx;
        this.sz = sz;
        this.data = new short[sx * sz * SY];
        palette.add(null);
    }

    private int index(int x, int y, int z) {
        return ((y - MIN_Y) * sz + z) * sx + x;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && x < sx && z >= 0 && z < sz && y >= MIN_Y && y < MIN_Y + SY;
    }

    public void set(int x, int y, int z, BlockState state) {
        if (!inside(x, y, z)) return;
        Short id = ids.get(state);
        if (id == null) {
            id = (short) palette.size();
            palette.add(state);
            ids.put(state, id);
        }
        data[index(x, y, z)] = id;
    }

    /** Returns null where nothing has been drawn. */
    public BlockState get(int x, int y, int z) {
        if (!inside(x, y, z)) return null;
        return palette.get(data[index(x, y, z)]);
    }

    public boolean isEmpty(int x, int y, int z) {
        BlockState s = get(x, y, z);
        return s == null || s.isAir();
    }

    public void clear(int x, int y, int z) {
        set(x, y, z, Blocks.AIR.defaultBlockState());
    }

    public int worldX(int x) { return ox + x; }

    public int worldY(int y) { return baseY + y; }

    public int worldZ(int z) { return oz + z; }
}
