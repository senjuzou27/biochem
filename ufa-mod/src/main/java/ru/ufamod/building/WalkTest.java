package ru.ufamod.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
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

    static boolean passable(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        if (s == null || s.isAir()) return true;
        return s.getBlock() instanceof DoorBlock || s.getBlock() instanceof net.minecraft.world.level.block.SignBlock || !s.blocksMotion();
    }

    static boolean solid(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        return s != null && s.blocksMotion() && !(s.getBlock() instanceof DoorBlock) && !(s.getBlock() instanceof net.minecraft.world.level.block.SignBlock);
    }

    static boolean ladder(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        return s != null && s.getBlock() instanceof LadderBlock;
    }

    /** A place where a player can stand: room for the body, and a floor or a ladder to hold on to. */
    static boolean standable(Model m, int x, int y, int z) {
        return passable(m, x, y, z) && passable(m, x, y + 1, z) && (solid(m, x, y - 1, z) || ladder(m, x, y, z) || ladder(m, x, y - 1, z));
    }

    static boolean ramp(Model m, int x, int y, int z) {
        BlockState s = m.get(x, y, z);
        // a stair, or a slab in the lower half of the block: a top slab (a desk) is as tall as a full block
        return s != null && (s.getBlock() instanceof StairBlock
                || s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
    }

    /**
     * @param jumping whether stepping up a full block without stairs is allowed
     * @return true if the outside of the building can be reached from {@code start} (feet position)
     */
    /**
     * Everything a player can walk to from the street through the doors of the building, without jumping:
     * up and down only by stairs, slabs and ladders. Moves are symmetric, so whatever is reached can also be left.
     * Positions are feet positions as {@link BlockPos#asLong()}; new starts are added to the given set.
     */
    static void reach(CityData city, BuildingGenerator g, Iterable<BlockPos> starts, Set<Long> seen) {
        Model m = g.m;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos s : starts) {
            seen.add(s.asLong());
            queue.add(s);
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int x = p.getX(), y = p.getY(), z = p.getZ();
            if (ladder(m, x, y, z) && passable(m, x, y + 2, z) && passable(m, x, y + 1, z)) {
                BlockPos up = p.above();
                if (seen.add(up.asLong())) queue.add(up);
            }
            if (ladder(m, x, y - 1, z) && passable(m, x, y - 1, z)) {
                BlockPos down = p.below();
                if (seen.add(down.asLong())) queue.add(down);
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int nx = x + dir.getStepX(), nz = z + dir.getStepZ();
                if (city.building(nx, nz) != g.b.index) continue;
                for (int dy = 1; dy >= -1; dy--) {
                    int ny = y + dy;
                    if (!standable(m, nx, ny, nz)) continue;
                    if (dy == 1 && (!ramp(m, nx, ny - 1, nz) || !passable(m, x, y + 2, z))) continue;
                    if (dy == -1 && (!ramp(m, x, y - 1, z) || !passable(m, nx, ny + 2, nz))) continue;
                    BlockPos n = new BlockPos(nx, ny, nz);
                    if (seen.add(n.asLong())) queue.add(n);
                    break;
                }
            }
        }
    }

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
            // up and down a ladder
            if (ladder(m, p.getX(), p.getY(), p.getZ()) && passable(m, p.getX(), p.getY() + 2, p.getZ())) {
                BlockPos up = p.above();
                if (seen.add(up.asLong())) queue.add(up);
            }
            if (ladder(m, p.getX(), p.getY() - 1, p.getZ())) {
                BlockPos down = p.below();
                if (seen.add(down.asLong())) queue.add(down);
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int nx = p.getX() + dir.getStepX(), nz = p.getZ() + dir.getStepZ();
                if (city.building(nx, nz) != g.b.index) {
                    // the doorway leads outside
                    if (passable(m, nx, p.getY(), nz) || passable(m, nx, p.getY() + 1, nz)) return true;
                    continue;
                }
                for (int dy = 1; dy >= -3; dy--) {
                    int ny = p.getY() + dy;
                    if (!standable(m, nx, ny, nz)) continue;
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
