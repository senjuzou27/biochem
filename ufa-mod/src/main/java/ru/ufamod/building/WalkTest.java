package ru.ufamod.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.city.CityData;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Checks on the block model of a building that a player can walk from a spot inside it out to the street. */
public final class WalkTest {
    private WalkTest() {}

    private static boolean passable(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        if (s == null || s.isAir()) return true;
        return s.getBlock() instanceof DoorBlock || s.getBlock() instanceof net.minecraft.world.level.block.SignBlock || !s.blocksMotion();
    }

    private static boolean solid(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        return s != null && s.blocksMotion() && !(s.getBlock() instanceof DoorBlock) && !(s.getBlock() instanceof net.minecraft.world.level.block.SignBlock);
    }

    private static boolean ramp(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        return s != null && (s.getBlock() instanceof StairBlock || s.getBlock() instanceof SlabBlock);
    }

    /**
     * @param jumping whether stepping up a full block without stairs is allowed
     * @return true if the outside of the building can be reached from {@code start} (feet position)
     */
    public static boolean canLeave(CityData city, BuildingGenerator g, BlockPos start, boolean jumping) {
        return canLeave(city, g, start, jumping, new HashSet<>());
    }

    public static boolean canLeave(CityData city, BuildingGenerator g, BlockPos start, boolean jumping, Set<Long> seen) {
        Model m = g.m;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start.asLong());
        while (!queue.isEmpty() && seen.size() < 400_000) {
            BlockPos p = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int nx = p.getX() + dir.getStepX(), nz = p.getZ() + dir.getStepZ();
                if (city.building(nx, nz) != g.b.index) {
                    // the doorway leads outside
                    if (passable(m, nx, p.getY(), nz) || passable(m, nx, p.getY() + 1, nz)) return true;
                    continue;
                }
                for (int dy = 1; dy >= -3; dy--) {
                    int ny = p.getY() + dy;
                    if (!passable(m, nx, ny, nz) || !passable(m, nx, ny + 1, nz) || !solid(m, nx, ny - 1, nz)) continue;
                    if (dy == 1) {
                        if (!passable(m, p.getX(), p.getY() + 2, p.getZ())) continue;
                        if (!jumping && !ramp(m, nx, ny - 1, nz)) continue;
                    }
                    if (dy < 0) {
                        boolean clear = true;
                        for (int y = ny + 2; y <= p.getY() + 1; y++) if (!passable(m, nx, y, nz)) clear = false;
                        if (!clear) continue;
                    }
                    BlockPos n = new BlockPos(nx, ny, nz);
                    if (seen.add(n.asLong())) queue.add(n);
                    break;
                }
            }
        }
        return false;
    }
}
