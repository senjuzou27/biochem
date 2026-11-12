package ru.ufamod.building;

import net.minecraft.core.Direction;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The outline of a building on a local grid: which cells are inside, which of them form the outer wall,
 * which way each wall faces and how the wall cells line up into straight facade runs.
 */
public final class Footprint {
    public static final int MARGIN = 6;
    public static final byte OUT = 0, WALL = 1, IN = 2;

    public final int ox, oz, w, d;
    public final byte[] kind;
    /** For wall cells: the single outward direction, or null for corners. */
    public final Direction[] normal;
    /** For wall cells: index of the facade run, position in it and its length. */
    public final int[] run, runPos;
    public final List<Run> runs = new ArrayList<>();
    public int insideCount;

    public record Run(int id, Direction normal, List<int[]> cells) {
        public int length() {
            return cells.size();
        }
    }

    public Footprint(CityData city, BuildingInfo b) {
        ox = b.minX - MARGIN;
        oz = b.minZ - MARGIN;
        w = b.maxX - b.minX + 1 + 2 * MARGIN;
        d = b.maxZ - b.minZ + 1 + 2 * MARGIN;
        kind = new byte[w * d];
        normal = new Direction[w * d];
        run = new int[w * d];
        runPos = new int[w * d];
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++)
                if (city.building(ox + lx, oz + lz) == b.index) {
                    kind[lz * w + lx] = IN;
                    insideCount++;
                }
        classify();
    }

    public boolean valid(int lx, int lz) {
        return lx >= 0 && lz >= 0 && lx < w && lz < d;
    }

    public int at(int lx, int lz) {
        return valid(lx, lz) ? kind[lz * w + lx] : OUT;
    }

    public boolean inside(int lx, int lz) {
        return at(lx, lz) != OUT;
    }

    public boolean wall(int lx, int lz) {
        return at(lx, lz) == WALL;
    }

    public boolean interior(int lx, int lz) {
        return at(lx, lz) == IN;
    }

    public Direction normalAt(int lx, int lz) {
        return valid(lx, lz) ? normal[lz * w + lx] : null;
    }

    private void classify() {
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                if (kind[lz * w + lx] == OUT) continue;
                boolean edge = false;
                for (int dz = -1; dz <= 1 && !edge; dz++)
                    for (int dx = -1; dx <= 1; dx++)
                        if (at(lx + dx, lz + dz) == OUT && !(dx == 0 && dz == 0)) {
                            edge = true;
                            break;
                        }
                if (edge) kind[lz * w + lx] = WALL;
            }
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                if (kind[lz * w + lx] != WALL) continue;
                Direction only = null;
                int count = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL)
                    if (at(lx + dir.getStepX(), lz + dir.getStepZ()) == OUT) {
                        only = dir;
                        count++;
                    }
                // a facade cell has exactly one open side and the wall continues on both of its flanks
                if (count == 1) {
                    Direction side = only.getClockWise();
                    if (at(lx + side.getStepX(), lz + side.getStepZ()) != WALL && at(lx - side.getStepX(), lz - side.getStepZ()) != WALL) only = null;
                }
                normal[lz * w + lx] = count == 1 ? only : null;
            }
        // group facade cells into straight runs
        Map<Long, List<int[]>> lines = new HashMap<>();
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                Direction n = normal[lz * w + lx];
                if (n == null) continue;
                long key = n.getAxis() == Direction.Axis.Z ? ((long) n.ordinal() << 32) | lz : ((long) n.ordinal() << 32) | (1L << 31) | lx;
                lines.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{lx, lz});
            }
        java.util.Arrays.fill(run, -1);
        for (Map.Entry<Long, List<int[]>> e : lines.entrySet()) {
            List<int[]> cells = e.getValue();
            Direction n = normal[cells.get(0)[1] * w + cells.get(0)[0]];
            boolean alongX = n.getAxis() == Direction.Axis.Z;
            cells.sort((a, b) -> alongX ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
            List<int[]> cur = new ArrayList<>();
            int last = Integer.MIN_VALUE;
            for (int[] c : cells) {
                int v = alongX ? c[0] : c[1];
                if (v != last + 1 && !cur.isEmpty()) {
                    addRun(n, cur);
                    cur = new ArrayList<>();
                }
                cur.add(c);
                last = v;
            }
            if (!cur.isEmpty()) addRun(n, cur);
        }
        runs.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            runs.set(i, new Run(i, r.normal(), r.cells()));
            for (int k = 0; k < r.cells().size(); k++) {
                int[] c = r.cells().get(k);
                run[c[1] * w + c[0]] = i;
                runPos[c[1] * w + c[0]] = k;
            }
        }
    }

    private void addRun(Direction n, List<int[]> cells) {
        runs.add(new Run(runs.size(), n, cells));
    }

    public Run runAt(int lx, int lz) {
        int r = valid(lx, lz) ? run[lz * w + lx] : -1;
        return r < 0 ? null : runs.get(r);
    }

    /** The facade cell closest to a world position, or null. */
    public int[] nearestFacade(int wx, int wz) {
        int best = Integer.MAX_VALUE;
        int[] out = null;
        for (Run r : runs)
            for (int[] c : r.cells()) {
                int dx = c[0] + ox - wx, dz = c[1] + oz - wz;
                int dist = dx * dx + dz * dz;
                if (dist < best) {
                    best = dist;
                    out = c;
                }
            }
        return out;
    }

    public int minInX() {
        for (int lx = 0; lx < w; lx++) for (int lz = 0; lz < d; lz++) if (inside(lx, lz)) return lx;
        return 0;
    }
}
