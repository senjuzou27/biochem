package ru.ufamod.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;
import ru.ufamod.city.Ground;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds one building from its map outline: facades with a window rhythm, roof, entrances, staircases,
 * a corridor-and-rooms interior on every storey and furniture that depends on what the building is used for.
 */
public final class BuildingGenerator {
    // roles of the cells inside the outline
    static final byte OUT = 0, WALL = 1, ROOM = 2, PART = 3, CORR = 4, CORE = 5, STAIR = 6, IDOOR = 7, CWALL = 8, SWALL = 9;

    /** Overrides for the buildings of the story. */
    public static final class Hints {
        public String roomType;
        public boolean coreOpen;
        public int roomWidth;
    }

    /** A way in: the door cell, the direction pointing out of the building and the first free cell behind the door. */
    public record Entrance(int x, int z, Direction out, boolean stairs, BlockPos lobby) {}

    /** A room of the regular grid: enough to address its cells from outside (see {@link #roomCell}). */
    public record RoomRef(int storey, int floorY, Direction out, int index, int width, int depth, int ax, int az) {}

    public final CityData city;
    public final BuildingInfo b;
    public final Footprint fp;
    public final Style st;
    public final Model m;
    public final List<Entrance> entrances = new ArrayList<>();
    public final List<RoomRef> rooms = new ArrayList<>();
    public final List<BlockPos> corridor = new ArrayList<>();
    public final List<BlockPos> core = new ArrayList<>();
    public final int base, top, levels;
    public final int[] floorY;
    private final Hints hints;
    private final Random rnd;
    private final int w, d;
    private final int[] dist, lm;
    private final Direction[] fdir;
    private final byte[] role;
    private final boolean[] hole; // per storey * cell: no floor here (stair wells)
    private int maxDist;
    private final int P;
    private final boolean shopFront;

    private BuildingGenerator(CityData city, BuildingInfo b, Footprint fp, Hints hints) {
        this.city = city;
        this.b = b;
        this.fp = fp;
        this.hints = hints == null ? new Hints() : hints;
        this.rnd = new Random(b.hash());
        this.st = Style.of(b, rnd);
        this.w = fp.w;
        this.d = fp.d;
        this.base = b.baseY;
        this.levels = st.levels;
        this.floorY = new int[levels + 1];
        for (int i = 0; i <= levels; i++) floorY[i] = base + (i == 0 ? 0 : st.groundHeight + (i - 1) * st.storeyHeight);
        this.top = floorY[levels];
        int extra = st.pitched ? 9 : 4;
        if (st.era == Style.Era.RELIGIOUS) extra = 34;
        this.m = new Model(fp.ox, base - 1, fp.oz, w, top - base + 2 + extra, d);
        this.dist = new int[w * d];
        this.lm = new int[w * d];
        this.fdir = new Direction[w * d];
        this.role = new byte[w * d];
        this.hole = new boolean[(levels + 1) * w * d];
        int p = st.period == 2 ? 4 : st.period == 4 ? 8 : 6;
        if (st.use == Style.Use.UNIVERSITY || st.use == Style.Use.SCHOOL) p = st.period == 2 ? 8 : st.period == 4 ? 8 : 9;
        if (this.hints.roomWidth > 0) p = this.hints.roomWidth;
        this.P = p;
        this.shopFront = !b.shops.isEmpty() || st.use == Style.Use.RETAIL || st.use == Style.Use.MALL;
    }

    public static BuildingGenerator generate(CityData city, BuildingInfo b, Hints hints) {
        Footprint fp = new Footprint(city, b);
        if (fp.insideCount < 6) return null;
        BuildingGenerator g = new BuildingGenerator(city, b, fp, hints);
        g.analyse();
        g.placeEntrances();
        g.assignRoles();
        g.shell();
        g.roof();
        g.interior();
        g.stairs();
        g.doorsAndSigns();
        g.extras();
        return g;
    }

    // ------------------------------------------------------------------ small helpers

    private int idx(int lx, int lz) {
        return lz * w + lx;
    }

    private boolean valid(int lx, int lz) {
        return lx >= 0 && lz >= 0 && lx < w && lz < d;
    }

    private int distAt(int lx, int lz) {
        return valid(lx, lz) ? dist[idx(lx, lz)] : 0;
    }

    private byte roleAt(int lx, int lz) {
        return valid(lx, lz) ? role[idx(lx, lz)] : OUT;
    }

    private void set(int lx, int y, int lz, BlockState s) {
        m.set(fp.ox + lx, y, fp.oz + lz, s);
    }

    private void set(int lx, int y, int lz, Block bl) {
        m.set(fp.ox + lx, y, fp.oz + lz, bl.defaultBlockState());
    }

    private static BlockState st(Block bl) {
        return bl.defaultBlockState();
    }

    private static BlockState stair(Block bl, Direction facing) {
        return bl.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState slabTop(Block bl) {
        return bl.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private BlockState wallState(int y) {
        if (st.logWalls) return st.wall.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        return st.wall.defaultBlockState();
    }

    private int storeyHeight(int storey) {
        return floorY[storey + 1] - floorY[storey];
    }

    private int coordOf(Direction facadeNormal, int lx, int lz) {
        return facadeNormal.getAxis() == Direction.Axis.Z ? fp.ox + lx : fp.oz + lz;
    }

    private static Direction uPos(Direction facadeNormal) {
        return facadeNormal.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
    }

    private static int hash(int a, int b2, int c) {
        int h = a * 73856093 ^ b2 * 19349663 ^ c * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    // ------------------------------------------------------------------ analysis of the outline

    private void analyse() {
        // Chebyshev distance from the outside; the wall ring is 1
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++)
                if (fp.wall(lx, lz)) {
                    dist[idx(lx, lz)] = 1;
                    Direction n = fp.normalAt(lx, lz);
                    if (n == null)
                        for (Direction dir : Direction.Plane.HORIZONTAL)
                            if (!fp.inside(lx + dir.getStepX(), lz + dir.getStepZ())) {
                                n = dir;
                                break;
                            }
                    fdir[idx(lx, lz)] = n == null ? Direction.NORTH : n;
                    queue.add(new int[]{lx, lz});
                }
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int dd = dist[idx(c[0], c[1])];
            maxDist = Math.max(maxDist, dd);
            // straight neighbours first so that the facade direction is inherited from the nearest wall
            for (int pass = 0; pass < 2; pass++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((dx == 0 && dz == 0) || ((dx == 0 || dz == 0) != (pass == 0))) continue;
                        int nx = c[0] + dx, nz = c[1] + dz;
                        if (!fp.interior(nx, nz) || dist[idx(nx, nz)] != 0) continue;
                        dist[idx(nx, nz)] = dd + 1;
                        fdir[idx(nx, nz)] = fdir[idx(c[0], c[1])];
                        queue.add(new int[]{nx, nz});
                    }
        }
        // local half-width: the distance value of the medial axis this cell hangs on
        List<int[]> order = new ArrayList<>();
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++)
                if (dist[idx(lx, lz)] > 0) order.add(new int[]{lx, lz});
        order.sort((a, c) -> Integer.compare(dist[idx(c[0], c[1])], dist[idx(a[0], a[1])]));
        for (int[] c : order) {
            int dd = dist[idx(c[0], c[1])];
            int best = dd;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = c[0] + dx, nz = c[1] + dz;
                    if (valid(nx, nz) && dist[idx(nx, nz)] > dd) best = Math.max(best, lm[idx(nx, nz)]);
                }
            lm[idx(c[0], c[1])] = best;
        }
    }

    private int corridorWallDist(int local) {
        if (local <= 4) return -1;
        return local <= 7 ? local - 1 : 6;
    }

    private int roomDepth(int local) {
        if (local <= 4) return Math.max(1, local - 1);
        return corridorWallDist(local) - 2;
    }

    private void assignRoles() {
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                int i = idx(lx, lz);
                int dd = dist[i];
                if (dd == 0) continue;
                if (role[i] == STAIR || role[i] == CORR || role[i] == PART || role[i] == SWALL) continue;
                if (dd == 1) {
                    role[i] = WALL;
                    continue;
                }
                int local = lm[i];
                Direction n = fdir[i];
                int coord = coordOf(n, lx, lz);
                byte r;
                if (local <= 4) {
                    r = ROOM;
                    if (Math.floorMod(coord, P) == 0 && fp.insideCount > 60) r = dd == Math.min(3, local) ? IDOOR : PART;
                } else {
                    int cw = corridorWallDist(local);
                    if (dd < cw) r = Math.floorMod(coord, P) == 0 ? PART : ROOM;
                    else if (dd == cw) r = Math.floorMod(coord, P) == P / 2 ? IDOOR : CWALL;
                    else if (local >= 11 && dd == 9) r = (Math.floorMod(fp.ox + lx, 10) == 5 || Math.floorMod(fp.oz + lz, 10) == 5) ? IDOOR : CWALL;
                    else if (local >= 11 && dd >= 10) {
                        r = CORE;
                        if (!hints.coreOpen && (Math.floorMod(fp.ox + lx, 10) == 0 || Math.floorMod(fp.oz + lz, 10) == 0)) r = PART;
                    } else r = CORR;
                }
                role[i] = r;
            }
        // a passage from every door to the corridor
        for (Entrance e : new ArrayList<>(entrances)) {
            Direction in = e.out().getOpposite();
            int lx = e.lobby().getX() - fp.ox, lz = e.lobby().getZ() - fp.oz;
            BlockPos hall = e.lobby();
            for (int k = 0; k <= 12; k++) {
                int cx = lx + in.getStepX() * k, cz = lz + in.getStepZ() * k;
                byte r = roleAt(cx, cz);
                if (r == OUT || r == WALL) break;
                hall = new BlockPos(fp.ox + cx, base + 1, fp.oz + cz);
                if (r == CORR || r == CORE) break;
                if (r == STAIR) continue;
                role[idx(cx, cz)] = CORR;
            }
            halls.add(hall);
        }
        connectRooms();
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                byte r = role[idx(lx, lz)];
                if (r == CORR) corridor.add(new BlockPos(fp.ox + lx, 0, fp.oz + lz));
                else if (r == CORE) core.add(new BlockPos(fp.ox + lx, 0, fp.oz + lz));
            }
    }

    private boolean walkable(byte r) {
        return r == ROOM || r == CORR || r == CORE || r == IDOOR || r == STAIR;
    }

    /**
     * Makes sure every room can be reached: finds the groups of cells that are cut off from the entrance
     * and opens a door in a partition between such a group and the part already connected.
     */
    private void connectRooms() {
        int[] comp = new int[w * d];
        int count = 0;
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                if (comp[idx(lx, lz)] != 0 || !walkable(role[idx(lx, lz)])) continue;
                comp[idx(lx, lz)] = ++count;
                queue.add(new int[]{lx, lz});
                while (!queue.isEmpty()) {
                    int[] c = queue.poll();
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int nx = c[0] + dir.getStepX(), nz = c[1] + dir.getStepZ();
                        if (!valid(nx, nz) || comp[idx(nx, nz)] != 0 || !walkable(role[idx(nx, nz)])) continue;
                        comp[idx(nx, nz)] = count;
                        queue.add(new int[]{nx, nz});
                    }
                }
            }
        if (count <= 1) return;
        int[] parent = new int[count + 1];
        for (int i = 0; i <= count; i++) parent[i] = i;
        // start from the entrance if there is one, otherwise from the largest group
        int main = 0;
        if (!entrances.isEmpty()) {
            BlockPos l = entrances.get(0).lobby();
            if (valid(l.getX() - fp.ox, l.getZ() - fp.oz)) main = comp[idx(l.getX() - fp.ox, l.getZ() - fp.oz)];
        }
        if (main == 0) {
            int[] size = new int[count + 1];
            for (int c : comp) size[c]++;
            for (int i = 1; i <= count; i++) if (size[i] > size[main] || main == 0) main = i;
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int lz = 1; lz < d - 1; lz++)
                for (int lx = 1; lx < w - 1; lx++) {
                    byte r = role[idx(lx, lz)];
                    if (r != PART && r != CWALL) continue;
                    for (int axis = 0; axis < 2; axis++) {
                        int a = comp[axis == 0 ? idx(lx - 1, lz) : idx(lx, lz - 1)], b2 = comp[axis == 0 ? idx(lx + 1, lz) : idx(lx, lz + 1)];
                        if (a == 0 || b2 == 0) continue;
                        int ra = find(parent, a), rb = find(parent, b2), rm = find(parent, main);
                        if (ra == rb || (ra != rm && rb != rm)) continue;
                        // not next to another door: two doors side by side look wrong
                        boolean nearDoor = false;
                        for (Direction dir : Direction.Plane.HORIZONTAL) if (roleAt(lx + dir.getStepX(), lz + dir.getStepZ()) == IDOOR) nearDoor = true;
                        if (nearDoor) continue;
                        role[idx(lx, lz)] = IDOOR;
                        comp[idx(lx, lz)] = main;
                        parent[ra == rm ? rb : ra] = rm;
                        changed = true;
                        break;
                    }
                }
        }
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) i = parent[i] = parent[parent[i]];
        return i;
    }

    // ------------------------------------------------------------------ entrances and staircases

    private boolean facesStreet(int lx, int lz, Direction n) {
        for (int k = 1; k <= 24; k++) {
            int x = fp.ox + lx + n.getStepX() * k, z = fp.oz + lz + n.getStepZ() * k;
            int bi = city.building(x, z);
            if (bi != 0 && bi != b.index) return false;
            int g = city.ground(x, z);
            if (g == Ground.ROAD || g == Ground.SIDEWALK || g == Ground.PLAZA) return true;
        }
        return false;
    }

    private boolean doorSpot(int lx, int lz) {
        Direction n = fp.normalAt(lx, lz);
        if (n == null) return false;
        Footprint.Run run = fp.runAt(lx, lz);
        if (run == null || run.length() < 3) return false;
        int k = fp.runPos[idx(lx, lz)];
        if (k == 0 || k == run.length() - 1) return false;
        // something to walk into
        return fp.interior(lx - n.getStepX(), lz - n.getStepZ()) && city.building(fp.ox + lx + n.getStepX(), fp.oz + lz + n.getStepZ()) == 0;
    }

    private int[] nearestDoorSpot(int wx, int wz, int maxDist2) {
        int best = maxDist2 + 1;
        int[] out = null;
        for (Footprint.Run run : fp.runs)
            for (int[] c : run.cells()) {
                int dx = c[0] + fp.ox - wx, dz = c[1] + fp.oz - wz;
                int dd = dx * dx + dz * dz;
                if (dd < best && doorSpot(c[0], c[1])) {
                    best = dd;
                    out = c;
                }
            }
        return out;
    }

    private final List<int[]> doorCells = new ArrayList<>();
    /** For every entrance: the first cell of the corridor behind it. */
    public final List<BlockPos> halls = new ArrayList<>();

    /**
     * A free corridor cell next to a wall near {@code p}: a place for somebody to stand without blocking the way.
     * {@code skip} selects the n-th such place.
     */
    public BlockPos spotNear(BlockPos p, int skip) {
        int lx = p.getX() - fp.ox, lz = p.getZ() - fp.oz;
        for (int rad = 1; rad <= 6; rad++)
            for (int dz = -rad; dz <= rad; dz++)
                for (int dx = -rad; dx <= rad; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
                    int cx = lx + dx, cz = lz + dz;
                    byte r = roleAt(cx, cz);
                    if (r != CORR && r != CORE) continue;
                    if (!m.isAir(fp.ox + cx, base + 1, fp.oz + cz) || !m.isAir(fp.ox + cx, base + 2, fp.oz + cz)) continue;
                    int open = 0;
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        byte nr = roleAt(cx + dir.getStepX(), cz + dir.getStepZ());
                        if (nr == CORR || nr == CORE) open++;
                    }
                    if (open < 2 || open > 3) continue;
                    if (skip-- > 0) continue;
                    return new BlockPos(fp.ox + cx, base + 1, fp.oz + cz);
                }
        return p;
    }

    /** Developer aid: the role map and the blocks at two heights of a storey as text. */
    public String debugMap(int storey) {
        StringBuilder sb = new StringBuilder();
        String chars = " #.|cOSDWX";
        int F = floorY[storey];
        for (int lz = 0; lz < d; lz++) {
            for (int lx = 0; lx < w; lx++) sb.append(chars.charAt(role[idx(lx, lz)]));
            sb.append("   ");
            for (int lx = 0; lx < w; lx++) {
                BlockState a1 = m.get(fp.ox + lx, F + 1, fp.oz + lz), a2 = m.get(fp.ox + lx, F + 2, fp.oz + lz), fl = m.get(fp.ox + lx, F, fp.oz + lz);
                char c;
                if (a1 == null) c = ' ';
                else if (a1.getBlock() instanceof DoorBlock) c = 'D';
                else if (a1.getBlock() instanceof StairBlock) c = '/';
                else if (a1.blocksMotion()) c = a2 != null && a2.blocksMotion() ? '#' : 'o';
                else if (fl == null || !fl.blocksMotion()) c = fl != null && fl.getBlock() instanceof StairBlock ? '\\' : '_';
                else c = '.';
                sb.append(c);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public boolean isCorridor(int wx, int wz) {
        byte r = roleAt(wx - fp.ox, wz - fp.oz);
        return r == CORR || r == CORE;
    }
    private final List<int[]> stairWells = new ArrayList<>(); // lx, lz of index 0 lane A, dir ordinal, side ordinal

    private boolean farFromDoors(int lx, int lz, int minDist) {
        for (int[] c : doorCells) if (Math.abs(c[0] - lx) + Math.abs(c[1] - lz) < minDist) return false;
        return true;
    }

    private void placeEntrances() {
        List<int[]> spots = new ArrayList<>();
        for (BuildingInfo.Entrance e : b.entrances) {
            if (e.kind().equals("emergency") || e.kind().equals("garage")) continue;
            int[] c = nearestDoorSpot(e.x(), e.z(), 16);
            if (c != null && farFromDoors(c[0], c[1], 4)) {
                spots.add(c);
                doorCells.add(c);
            }
        }
        if (spots.isEmpty()) {
            Footprint.Run bestRun = null;
            int bestScore = -1;
            for (Footprint.Run run : fp.runs) {
                if (run.length() < 3) continue;
                int[] mid = run.cells().get(run.length() / 2);
                int score = run.length() + (facesStreet(mid[0], mid[1], run.normal()) ? (st.residential() ? -8 : 60) : 0);
                if (score > bestScore) {
                    bestScore = score;
                    bestRun = run;
                }
            }
            if (bestRun != null) {
                int count = st.use == Style.Use.RESIDENTIAL || st.use == Style.Use.DORM ? Math.max(1, Math.round(bestRun.length() / 22f)) : 1;
                for (int j = 0; j < count; j++) {
                    int k = (int) ((j + 0.5) * bestRun.length() / count);
                    for (int off = 0; off < 4; off++) {
                        int kk = Math.min(bestRun.length() - 1, Math.max(0, k + (off % 2 == 0 ? off / 2 : -(off + 1) / 2)));
                        int[] c = bestRun.cells().get(kk);
                        if (doorSpot(c[0], c[1]) && farFromDoors(c[0], c[1], 4)) {
                            spots.add(c);
                            doorCells.add(c);
                            break;
                        }
                    }
                }
            }
        }
        int wells = 0;
        int maxWells = st.residential() || st.use == Style.Use.DORM ? 12 : 3;
        for (int[] c : spots) {
            Direction out = fp.normalAt(c[0], c[1]);
            Direction in = out.getOpposite();
            boolean hasStairs = false;
            int lobbyX = c[0] + in.getStepX(), lobbyZ = c[1] + in.getStepZ();
            if (levels > 1 && wells < maxWells) {
                // three lanes (a through passage and two flights) where there is room, otherwise two
                outer:
                for (int lanes = levels > 2 ? 3 : 2; lanes >= 2; lanes--)
                    for (Direction side : new Direction[]{out.getClockWise(), out.getCounterClockWise()}) {
                        if (tryWell(lobbyX, lobbyZ, in, side, lanes)) {
                            hasStairs = true;
                            wells++;
                            if (lanes == 2) {
                                // the lane without the first flight is the way in on the ground floor
                                lobbyX += side.getStepX();
                                lobbyZ += side.getStepZ();
                            }
                            break outer;
                        }
                    }
            }
            entrances.add(new Entrance(fp.ox + c[0], fp.oz + c[1], out, hasStairs, new BlockPos(fp.ox + lobbyX, base + 1, fp.oz + lobbyZ)));
        }
        if (levels > 1 && wells == 0) {
            // no staircase fits behind a door: look for any free strip
            outer:
            for (int lanes = levels > 2 ? 3 : 2; lanes >= 2; lanes--)
                for (int lz = 1; lz < d - 1; lz++)
                    for (int lx = 1; lx < w - 1; lx++)
                        for (Direction dir : Direction.Plane.HORIZONTAL)
                            if (fp.interior(lx, lz) && tryWell(lx, lz, dir, dir.getClockWise(), lanes)) break outer;
        }
    }

    private int wellLength() {
        return Math.max(st.groundHeight, st.storeyHeight);
    }

    /**
     * Reserves a staircase {@code wellLength + 2} cells long, starting at (lx, lz) and running along dir.
     * With three lanes the first one is a level passage through the well and the other two carry the flights;
     * with two lanes there is no passage.
     */
    private boolean tryWell(int lx, int lz, Direction dir, Direction side, int lanes) {
        int len = wellLength() + 2;
        for (int k = 0; k < len; k++)
            for (int lane = 0; lane < lanes; lane++) {
                int cx = lx + dir.getStepX() * k + side.getStepX() * lane, cz = lz + dir.getStepZ() * k + side.getStepZ() * lane;
                if (!fp.interior(cx, cz) || role[idx(cx, cz)] != 0) return false;
            }
        for (int k = 0; k < len; k++)
            for (int lane = 0; lane < lanes; lane++)
                role[idx(lx + dir.getStepX() * k + side.getStepX() * lane, lz + dir.getStepZ() * k + side.getStepZ() * lane)] = STAIR;
        // walls along the well: doors may be cut into the passage side, never into the side of the flights
        for (int k = 0; k < len; k++) {
            int ax = lx + dir.getStepX() * k - side.getStepX(), az = lz + dir.getStepZ() * k - side.getStepZ();
            if (fp.interior(ax, az) && role[idx(ax, az)] == 0 && k >= 1 && k < len - 1) role[idx(ax, az)] = lanes == 3 ? PART : SWALL;
            int bx = lx + dir.getStepX() * k + side.getStepX() * lanes, bz = lz + dir.getStepZ() * k + side.getStepZ() * lanes;
            if (fp.interior(bx, bz) && role[idx(bx, bz)] == 0) role[idx(bx, bz)] = SWALL;
        }
        stairWells.add(new int[]{lx, lz, dir.ordinal(), side.ordinal(), lanes});
        return true;
    }

    private void stairs() {
        Block stairBlock = st.era == Style.Era.WOOD ? Blocks.SPRUCE_STAIRS : Blocks.STONE_BRICK_STAIRS;
        Block landing = st.era == Style.Era.WOOD ? Blocks.SPRUCE_PLANKS : Blocks.SMOOTH_STONE;
        int len = wellLength();
        for (int[] well : stairWells) {
            Direction dir = Direction.values()[well[2]], side = Direction.values()[well[3]];
            int lanes = well[4];
            int laneA = lanes == 3 ? 1 : 0, laneB = laneA + 1;
            for (int s = 0; s < levels; s++) {
                int F = floorY[s], h = storeyHeight(s);
                // floor of this storey inside the well
                for (int k = 0; k <= len + 1; k++)
                    for (int lane = 0; lane < lanes; lane++) {
                        int cx = well[0] + dir.getStepX() * k + side.getStepX() * lane, cz = well[1] + dir.getStepZ() * k + side.getStepZ() * lane;
                        if (!hole[(s * w * d) + idx(cx, cz)]) set(cx, F, cz, landing);
                        else if (s > 0 && isFloorBlock(cx, F, cz)) set(cx, F, cz, Blocks.AIR);
                        for (int y = F + 1; y < F + h; y++) set(cx, y, cz, Blocks.AIR);
                    }
                // a lamp in the wall beside the flights
                int lx = well[0] + dir.getStepX() * 2 + side.getStepX() * lanes, lz = well[1] + dir.getStepZ() * 2 + side.getStepZ() * lanes;
                if (roleAt(lx, lz) == SWALL) set(lx, F + 2, lz, Blocks.OCHRE_FROGLIGHT);
                else set(well[0] + dir.getStepX() * (len + 1), F + h, well[1] + dir.getStepZ() * (len + 1), Blocks.GLOWSTONE);
                if (s == levels - 1) break;
                boolean even = s % 2 == 0;
                int lane = even ? laneA : laneB;
                for (int j = 1; j <= h; j++) {
                    int k = even ? j : len + 1 - j;
                    int cx = well[0] + dir.getStepX() * k + side.getStepX() * lane, cz = well[1] + dir.getStepZ() * k + side.getStepZ() * lane;
                    set(cx, F + j, cz, stair(stairBlock, even ? dir : dir.getOpposite()));
                    for (int y = F + 1; y < F + j; y++) set(cx, y, cz, landing);
                    // headroom: no floor above the flight (the last step itself is level with the upper floor)
                    hole[((s + 1) * w * d) + idx(cx, cz)] = true;
                    for (int y = F + j + 1; y <= F + h + 2 && y < floorY[Math.min(levels, s + 2)]; y++) set(cx, y, cz, Blocks.AIR);
                }
                // and no floor above the spot where the flight is entered, or one could not get off the first step
                int k0 = even ? 0 : len + 1;
                int hx = well[0] + dir.getStepX() * k0 + side.getStepX() * lane, hz = well[1] + dir.getStepZ() * k0 + side.getStepZ() * lane;
                hole[((s + 1) * w * d) + idx(hx, hz)] = true;
                set(hx, F + h, hz, Blocks.AIR);
            }
            if (st.flat && levels >= 4) {
                // the roof exit
                for (int k = 0; k <= 2; k++)
                    for (int lane = 0; lane < 2; lane++) {
                        int cx = well[0] + dir.getStepX() * k + side.getStepX() * lane, cz = well[1] + dir.getStepZ() * k + side.getStepZ() * lane;
                        set(cx, top + 1, cz, Blocks.BRICKS);
                        set(cx, top + 2, cz, Blocks.BRICKS);
                        set(cx, top + 3, cz, Blocks.SMOOTH_STONE_SLAB);
                    }
            }
        }
    }

    private boolean isFloorBlock(int lx, int y, int lz) {
        BlockState b2 = m.get(fp.ox + lx, y, fp.oz + lz);
        return b2 != null && !(b2.getBlock() instanceof StairBlock) && !b2.isAir();
    }

    // ------------------------------------------------------------------ shell: walls, windows, floors

    private boolean windowHere(int lx, int lz, Direction n) {
        Footprint.Run run = fp.runAt(lx, lz);
        if (run == null || run.length() < 3) return false;
        int k = fp.runPos[idx(lx, lz)];
        if (k == 0 || k == run.length() - 1) return false;
        return st.windowAt[Math.floorMod(coordOf(n, lx, lz), st.period)];
    }

    private void shell() {
        BlockState glass = st(st.glass);
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                int i = idx(lx, lz);
                if (dist[i] == 0) continue;
                boolean isWall = dist[i] == 1;
                // floors and the ceiling of the last storey
                for (int s = 0; s <= levels; s++) {
                    if (isWall) continue;
                    Block f = s == 0 ? (st.use == Style.Use.GARAGE || st.era == Style.Era.INDUSTRIAL ? Blocks.SMOOTH_STONE : st.floor) : s == levels ? Blocks.SMOOTH_STONE : st.floor;
                    if (s == 0 && (st.corridorPlan() || shopFront) && st.era != Style.Era.WOOD) f = role[i] == CORR ? Blocks.POLISHED_ANDESITE : st.floor;
                    set(lx, floorY[s], lz, f);
                    if (s < levels) for (int y = floorY[s] + 1; y < floorY[s + 1]; y++) set(lx, y, lz, Blocks.AIR);
                }
                if (!isWall) continue;
                Direction n = fp.normalAt(lx, lz);
                boolean corner = n == null;
                boolean win = !corner && windowHere(lx, lz, n);
                set(lx, base, lz, st.plinth);
                for (int s = 0; s < levels; s++) {
                    int F = floorY[s], h = storeyHeight(s);
                    for (int y = F + 1; y <= F + h; y++) {
                        BlockState ws = wallState(y);
                        if (corner && (st.quoins || st.era == Style.Era.STALIN)) ws = st(st.trim);
                        else if (s == 0 && y == F + 1 && st.era != Style.Era.WOOD && st.era != Style.Era.PANEL) ws = st(st.plinth);
                        else if (y == F + h && s < levels - 1 && (st.era == Style.Era.MERCHANT || st.era == Style.Era.STALIN) && s == 0) ws = st(st.trim);
                        else if (st.era == Style.Era.PANEL && y == F + 1 && !corner) ws = st(Math.floorMod(coordOf(n, lx, lz), P) >= P / 2 ? st.accent : st.wall);
                        set(lx, y, lz, ws);
                    }
                    if (!win) continue;
                    boolean shop = s == 0 && shopFront && facesStreet(lx, lz, n);
                    int y1 = shop ? F + 1 : F + 2, y2 = h >= 4 ? F + h - 1 : F + 2;
                    if (shop && h >= 4) y2 = F + 3;
                    for (int y = y1; y <= y2; y++) set(lx, y, lz, glass);
                    if (st.era == Style.Era.WOOD) {
                        // carved window surrounds
                        Direction sd = n.getClockWise();
                        for (int k = -1; k <= 1; k += 2) {
                            int ax = lx + sd.getStepX() * k, az = lz + sd.getStepZ() * k;
                            if (fp.wall(ax, az) && fp.normalAt(ax, az) == n && !windowHere(ax, az, n)) set(ax, F + 2, az, st.trim);
                        }
                    } else if (st.era == Style.Era.MERCHANT && h >= 4 && s > 0) {
                        set(lx, F + h, lz, st.trim);
                    }
                }
                set(lx, top, lz, st.cornice || st.era == Style.Era.STALIN ? st(st.trim) : wallState(top));
            }
        if (st.cornice) {
            for (Footprint.Run run : fp.runs)
                for (int[] c : run.cells()) {
                    int ox = c[0] + run.normal().getStepX(), oz = c[1] + run.normal().getStepZ();
                    if (valid(ox, oz) && city.building(fp.ox + ox, fp.oz + oz) == 0) set(ox, top, oz, slabTop(slabOf(st.trim)));
                }
        }
    }

    static Block slabOf(Block full) {
        if (full == Blocks.SMOOTH_QUARTZ) return Blocks.SMOOTH_QUARTZ_SLAB;
        if (full == Blocks.SMOOTH_SANDSTONE) return Blocks.SMOOTH_SANDSTONE_SLAB;
        if (full == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_SLAB;
        if (full == Blocks.BRICKS) return Blocks.BRICK_SLAB;
        if (full == Blocks.STONE_BRICKS) return Blocks.STONE_BRICK_SLAB;
        if (full == Blocks.POLISHED_ANDESITE) return Blocks.POLISHED_ANDESITE_SLAB;
        return Blocks.SMOOTH_STONE_SLAB;
    }

    // ------------------------------------------------------------------ roof

    private void roof() {
        if (st.flat) {
            for (int lz = 0; lz < d; lz++)
                for (int lx = 0; lx < w; lx++) {
                    int dd = dist[idx(lx, lz)];
                    if (dd == 0) continue;
                    if (dd == 1) set(lx, top + 1, lz, st.era == Style.Era.MODERN ? st(st.trim) : st(Blocks.SMOOTH_STONE_SLAB));
                    else {
                        set(lx, top, lz, Blocks.POLISHED_DEEPSLATE);
                        int h = hash(fp.ox + lx, fp.oz + lz, 31);
                        if (dd >= 3 && h % 97 == 0) {
                            set(lx, top + 1, lz, Blocks.SMOOTH_STONE);
                            set(lx, top + 2, lz, Blocks.IRON_TRAPDOOR);
                        } else if (dd >= 3 && h % 211 == 0) {
                            set(lx, top + 1, lz, Blocks.IRON_BARS);
                            set(lx, top + 2, lz, Blocks.IRON_BARS);
                            set(lx, top + 3, lz, Blocks.LIGHTNING_ROD);
                        }
                    }
                }
            return;
        }
        boolean steep = st.era == Style.Era.WOOD || st.era == Style.Era.MERCHANT || st.era == Style.Era.RELIGIOUS;
        int cap = steep ? (st.era == Style.Era.WOOD ? 4 : 5) : 8;
        for (int lz = 0; lz < d; lz++)
            for (int lx = 0; lx < w; lx++) {
                int dd = dist[idx(lx, lz)];
                if (dd == 0) continue;
                if (steep) {
                    if (dd > cap) {
                        set(lx, top + cap, lz, st.roofBlock);
                        continue;
                    }
                    Direction up = null;
                    for (Direction dir : Direction.Plane.HORIZONTAL)
                        if (distAt(lx + dir.getStepX(), lz + dir.getStepZ()) > dd) {
                            up = dir;
                            break;
                        }
                    if (up != null) set(lx, top + dd, lz, stair(st.roofStairs, up));
                    else set(lx, top + dd, lz, st(st.roofSlab));
                } else {
                    int k = Math.min(dd, cap);
                    int y = top + (k + 1) / 2;
                    set(lx, y, lz, k % 2 == 1 ? st(st.roofSlab) : st(st.roofBlock));
                }
            }
        if (steep && st.use != Style.Use.CHURCH && st.use != Style.Use.MOSQUE && fp.insideCount > 30) {
            // a chimney
            for (int tries = 0; tries < 20; tries++) {
                int lx = rnd.nextInt(w), lz = rnd.nextInt(d);
                if (distAt(lx, lz) >= 2 && roleAt(lx, lz) != STAIR) {
                    int h = Math.min(distAt(lx, lz), cap);
                    for (int y = top + 1; y <= top + h + 2; y++) set(lx, y, lz, Blocks.BRICKS);
                    set(lx, top + h + 3, lz, Blocks.BRICK_WALL);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ interior

    private String roomType(int storey, Direction n, int roomIndex) {
        if (hints.roomType != null) return hints.roomType;
        int h = hash(roomIndex, storey * 7 + n.ordinal(), (int) b.osm);
        return switch (st.use) {
            case RESIDENTIAL, HOUSE -> new String[]{"bedroom", "living", "kitchen", "bedroom", "living"}[h % 5];
            case DORM, HOTEL -> "dorm";
            case UNIVERSITY, SCHOOL, KINDERGARTEN -> h % 5 == 0 ? "office" : "class";
            case HOSPITAL -> h % 3 == 0 ? "office" : "ward";
            case RETAIL, MALL -> "shop";
            case LIBRARY -> "library";
            case MUSEUM -> "museum";
            case INDUSTRIAL, GARAGE -> "storage";
            case CHURCH, MOSQUE, THEATRE -> "hall";
            default -> storey == 0 && shopFront ? "shop" : "office";
        };
    }

    private void interior() {
        for (int s = 0; s < levels; s++) {
            int F = floorY[s], h = storeyHeight(s);
            for (int lz = 0; lz < d; lz++)
                for (int lx = 0; lx < w; lx++) {
                    int i = idx(lx, lz);
                    byte r = role[i];
                    if (r == OUT || r == WALL || r == STAIR) continue;
                    int wx = fp.ox + lx, wz = fp.oz + lz;
                    Direction n = fdir[i];
                    switch (r) {
                        case PART, CWALL, SWALL -> {
                            for (int y = F + 1; y < F + h; y++) set(lx, y, lz, st.partition);
                        }
                        case IDOOR -> {
                            for (int y = F + 3; y < F + h; y++) set(lx, y, lz, st.partition);
                            // the door swings across the wall it sits in
                            boolean passX = walkable(roleAt(lx - 1, lz)) && walkable(roleAt(lx + 1, lz));
                            boolean passZ = walkable(roleAt(lx, lz - 1)) && walkable(roleAt(lx, lz + 1));
                            // a door only where the opening is a real doorway in a wall line
                            if (passX != passZ) door(lx, F + 1, lz, passX ? Direction.EAST : Direction.NORTH, st.interiorDoor);
                        }
                        case CORR -> {
                            if (hash(wx, wz, 5 + s) % 9 == 0 && nearWallCount(lx, lz) == 0 && h >= 3) ceilingLight(lx, lz, s);
                            else if (st.corridorPlan() && nearWallCount(lx, lz) == 1 && hash(wx, wz, 17) % 23 == 0) set(lx, F + 1, lz, Blocks.POTTED_FERN);
                        }
                        case CORE -> furnishCore(lx, lz, s);
                        case ROOM -> furnishRoom(lx, lz, s, n);
                        default -> { }
                    }
                }
        }
        // remember the rooms of the regular grid
        for (int s = 0; s < levels; s++)
            for (Footprint.Run run : fp.runs) {
                if (run.length() < P) continue;
                Direction n = run.normal();
                int lastIndex = Integer.MIN_VALUE;
                int[] first = run.cells().get(0), last = run.cells().get(run.length() - 1);
                for (int[] c : run.cells()) {
                    int coord = coordOf(n, c[0], c[1]);
                    int ri = Math.floorDiv(coord, P);
                    if (ri == lastIndex || Math.floorMod(coord, P) != 1) continue;
                    int ix = c[0] - n.getStepX(), iz = c[1] - n.getStepZ();
                    if (roleAt(ix, iz) != ROOM) continue;
                    // the whole room, with the partitions on both sides, has to lie on this run
                    if (coordOf(n, first[0], first[1]) > coord - 1 || coordOf(n, last[0], last[1]) < coord + P - 1) continue;
                    RoomRef ref = new RoomRef(s, floorY[s], n, ri, P, roomDepth(lm[idx(ix, iz)]), fp.ox + ix, fp.oz + iz);
                    if (!regular(ref)) continue;
                    lastIndex = ri;
                    rooms.add(ref);
                }
            }
    }

    /** A room is regular when every cell of its grid rectangle really is room floor. */
    private boolean regular(RoomRef r) {
        for (int u = 1; u < r.width(); u++)
            for (int v = 0; v < r.depth(); v++) {
                BlockPos c = roomCell(r, u, v);
                if (roleAt(c.getX() - fp.ox, c.getZ() - fp.oz) != ROOM) return false;
            }
        return true;
    }

    private int nearWallCount(int lx, int lz) {
        int c = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            byte r = roleAt(lx + dir.getStepX(), lz + dir.getStepZ());
            if (r == CWALL || r == PART || r == WALL || r == IDOOR || r == SWALL) c++;
        }
        return c;
    }

    private void ceilingLight(int lx, int lz, int storey) {
        int y = floorY[storey + 1];
        set(lx, y, lz, Blocks.GLOWSTONE);
        if (storey + 1 < levels && m.isAir(fp.ox + lx, y + 1, fp.oz + lz)) set(lx, y + 1, lz, Blocks.LIGHT_GRAY_CARPET);
    }

    /** World position of the cell (u, v) of a room; u runs along the facade (1..width-1), v from the window inwards. */
    public static BlockPos roomCell(RoomRef r, int u, int v) {
        Direction up = uPos(r.out());
        Direction in = r.out().getOpposite();
        return new BlockPos(r.ax() + up.getStepX() * (u - 1) + in.getStepX() * v, r.floorY() + 1, r.az() + up.getStepZ() * (u - 1) + in.getStepZ() * v);
    }

    private void furnishRoom(int lx, int lz, int s, Direction n) {
        int i = idx(lx, lz);
        int local = lm[i];
        int depth = roomDepth(local);
        int coord = coordOf(n, lx, lz);
        int u = Math.floorMod(coord, P), v = dist[i] - 2;
        if (u == 0) return;
        if (keepFree(lx, lz)) return;
        int F = floorY[s], y = F + 1;
        Direction in = n.getOpposite(), up = uPos(n), un = up.getOpposite();
        String type = roomType(s, n, Math.floorDiv(coord, P));
        int last = P - 1, mid = P / 2;
        boolean doorRow = v == depth - 1;
        if (u == mid && doorRow && storeyHeight(s) >= 3) {
            ceilingLight(lx, lz, s);
            return;
        }
        if (u == mid) {
            // keep the way from the door to the window free, except for a desk under the window
            if (v == 0 && depth >= 3 && (type.equals("dorm") || type.equals("bedroom") || type.equals("office"))) {
                set(lx, y, lz, slabTop(Blocks.SPRUCE_SLAB));
                if (hash(coord, s, 3) % 2 == 0) set(lx, y + 1, lz, hash(coord, s, 4) % 2 == 0 ? Blocks.POTTED_FERN : Blocks.POTTED_CACTUS);
            } else if (v == 1 && depth >= 4 && (type.equals("dorm") || type.equals("bedroom") || type.equals("office"))) {
                set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, in));
            }
            return;
        }
        int h = hash(Math.floorDiv(coord, P), s, n.ordinal() + 11);
        switch (type) {
            case "dorm", "bedroom", "ward" -> {
                Block[] beds = type.equals("ward") ? new Block[]{Blocks.WHITE_BED} : new Block[]{Blocks.BLUE_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.YELLOW_BED, Blocks.CYAN_BED, Blocks.BROWN_BED};
                boolean second = !type.equals("bedroom");
                if ((u == 1 || (u == last && second)) && v == 1 && depth >= 2) bed(lx, y, lz, n, beds[(h + u) % beds.length]);
                else if (u == last && !second && v <= 1) {
                    set(lx, y, lz, Blocks.BOOKSHELF);
                    set(lx, y + 1, lz, Blocks.BOOKSHELF);
                } else if ((u == 1 || u == last) && doorRow && depth >= 3) set(lx, y, lz, u == 1 ? Blocks.BARREL : Blocks.BOOKSHELF);
                else if (u > 1 && u < last && v >= 1 && !doorRow) set(lx, y, lz, type.equals("ward") ? Blocks.WHITE_CARPET : carpet(h));
            }
            case "living" -> {
                if (u == 1 && v >= 1 && v <= 2 && !doorRow) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, un));
                else if (u == last && v == 0) {
                    set(lx, y, lz, Blocks.BOOKSHELF);
                    set(lx, y + 1, lz, Blocks.BOOKSHELF);
                } else if (u == last && v == 1 && !doorRow) {
                    set(lx, y, lz, Blocks.SPRUCE_PLANKS);
                    set(lx, y + 1, lz, Blocks.BLACK_CONCRETE);
                } else if (u == 1 && v == 0) set(lx, y, lz, Blocks.POTTED_FERN);
                else if (u > 1 && u < last && v >= 1 && !doorRow) set(lx, y, lz, carpet(h));
            }
            case "kitchen" -> {
                if (v == 0) {
                    Block[] row = {Blocks.SMOKER, Blocks.CAULDRON, Blocks.CRAFTING_TABLE, Blocks.BARREL, Blocks.FURNACE};
                    set(lx, y, lz, row[(u - 1) % row.length]);
                } else if (u == last && v == 1 && depth >= 3) {
                    set(lx, y, lz, Blocks.IRON_BLOCK);
                    set(lx, y + 1, lz, Blocks.IRON_BLOCK);
                } else if (u == 2 && v == depth - 2 && depth >= 4) {
                    set(lx, y, lz, Blocks.SPRUCE_FENCE);
                    set(lx, y + 1, lz, Blocks.WHITE_CARPET);
                } else if (u == 1 && v == depth - 2 && depth >= 4) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, un));
            }
            case "class" -> {
                if (u == 1 && v == depth / 2) {
                    set(lx, y, lz, Blocks.DARK_OAK_PLANKS);
                } else if (u >= 3 && u < last && !doorRow) {
                    if (u % 2 == 1) set(lx, y, lz, slabTop(Blocks.SPRUCE_SLAB));
                    else set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, up));
                } else if (u == last && !doorRow) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, up));
            }
            case "office" -> {
                if ((u == 2 || u == last - 1) && v == 0) set(lx, y, lz, slabTop(Blocks.SPRUCE_SLAB));
                else if ((u == 2 || u == last - 1) && v == 1 && depth >= 3) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, in));
                else if ((u == 1 || u == last) && doorRow && depth >= 2) {
                    set(lx, y, lz, Blocks.BOOKSHELF);
                    set(lx, y + 1, lz, Blocks.BOOKSHELF);
                } else if (u == 1 && v == 0) set(lx, y, lz, Blocks.POTTED_FERN);
            }
            case "shop" -> {
                if ((u == 1 || u == last) && v >= 1) {
                    Block[] goods = {Blocks.BOOKSHELF, Blocks.BARREL, Blocks.CHISELED_BOOKSHELF, Blocks.BARREL};
                    set(lx, y, lz, goods[(h + v) % goods.length]);
                    set(lx, y + 1, lz, goods[(h + v + 1) % goods.length]);
                } else if (v == depth - 2 && depth >= 3 && u > 1 && u < last && u != mid) set(lx, y, lz, Blocks.SMOOTH_QUARTZ);
                else if (v == 0 && u % 2 == 0) set(lx, y, lz, hash(coord, s, 8) % 2 == 0 ? Blocks.FLOWER_POT : Blocks.BARREL);
            }
            case "library" -> {
                if (u % 2 == 1 && v >= 1 && !doorRow) {
                    set(lx, y, lz, Blocks.BOOKSHELF);
                    set(lx, y + 1, lz, Blocks.BOOKSHELF);
                } else if (v == 0 && u % 3 == 1) set(lx, y, lz, slabTop(Blocks.SPRUCE_SLAB));
            }
            case "museum" -> {
                if (u % 2 == 0 && v == 1 && depth >= 3) {
                    set(lx, y, lz, Blocks.SMOOTH_QUARTZ);
                    Block[] ex = {Blocks.FLOWER_POT, Blocks.DECORATED_POT, Blocks.SKELETON_SKULL, Blocks.AMETHYST_CLUSTER};
                    set(lx, y + 1, lz, ex[(h + u) % 4]);
                }
            }
            case "canteen" -> {
                if (u % 3 == 2 && v % 2 == 0 && !doorRow) {
                    set(lx, y, lz, Blocks.SPRUCE_FENCE);
                    set(lx, y + 1, lz, Blocks.WHITE_CARPET);
                } else if (u % 3 == 1 && v % 2 == 0 && !doorRow && u != 1) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, un));
                else if (u % 3 == 0 && v % 2 == 0 && !doorRow && u != last) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, up));
            }
            case "anatomy" -> {
                if ((u == 1 || u == last) && v % 2 == 0) {
                    set(lx, y, lz, hash(coord, v, 2) % 2 == 0 ? Blocks.BONE_BLOCK : Blocks.GLASS);
                    set(lx, y + 1, lz, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, u == 1 ? rotationOf(up) : rotationOf(un)));
                } else if (u >= 3 && u < last && v == 0) set(lx, y, lz, u % 2 == 1 ? slabTop(Blocks.SPRUCE_SLAB) : stair(Blocks.SPRUCE_STAIRS, in));
            }
            case "storage" -> {
                if ((u == 1 || u == last) && hash(coord, v, 6) % 3 == 0) {
                    set(lx, y, lz, Blocks.BARREL);
                    if (hash(coord, v, 7) % 2 == 0 && storeyHeight(s) >= 4) set(lx, y + 1, lz, Blocks.BARREL);
                }
            }
            default -> { }
        }
    }

    /** Cells in front of doors, door openings, passages and staircases stay empty. */
    private boolean keepFree(int lx, int lz) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int nx = lx + dir.getStepX(), nz = lz + dir.getStepZ();
            byte r = roleAt(nx, nz);
            if (r == IDOOR || r == CORR || r == STAIR) return true;
            if (r == WALL) for (int[] c : doorCells) if (c[0] == nx && c[1] == nz) return true;
        }
        return false;
    }

    private static Block carpet(int h) {
        Block[] c = {Blocks.RED_CARPET, Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.BLUE_CARPET, Blocks.ORANGE_CARPET, Blocks.LIGHT_GRAY_CARPET};
        return c[h % c.length];
    }

    static int rotationOf(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            default -> 12;
        };
    }

    private void furnishCore(int lx, int lz, int s) {
        int wx = fp.ox + lx, wz = fp.oz + lz;
        int F = floorY[s], y = F + 1;
        if (Math.floorMod(wx, 5) == 2 && Math.floorMod(wz, 5) == 2) {
            ceilingLight(lx, lz, s);
            return;
        }
        if (hints.coreOpen) return;
        int u = Math.floorMod(wx, 10), v = Math.floorMod(wz, 10);
        if (u == 5 || v == 5 || u == 0 || v == 0) return;
        String type = hints.roomType != null ? hints.roomType : switch (st.use) {
            case UNIVERSITY, SCHOOL -> "class";
            case RETAIL, MALL -> "shop";
            case LIBRARY -> "library";
            case MUSEUM -> "museum";
            default -> "office";
        };
        switch (type) {
            case "class" -> {
                if (v >= 3 && v <= 8 && u != 1 && u != 9) {
                    if (v % 2 == 1) set(lx, y, lz, slabTop(Blocks.SPRUCE_SLAB));
                    else set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
                } else if (v == 1 && u >= 3 && u <= 4) set(lx, y, lz, Blocks.DARK_OAK_PLANKS);
            }
            case "shop" -> {
                if ((u == 2 || u == 8) && v >= 2 && v <= 8) {
                    set(lx, y, lz, Blocks.BOOKSHELF);
                    set(lx, y + 1, lz, hash(wx, wz, 3) % 2 == 0 ? Blocks.BARREL : Blocks.CHISELED_BOOKSHELF);
                } else if (v == 2 && u >= 3 && u <= 4) set(lx, y, lz, Blocks.SMOOTH_QUARTZ);
            }
            case "library" -> {
                if (u % 2 == 0 && v >= 2 && v <= 8) {
                    set(lx, y, lz, Blocks.BOOKSHELF);
                    set(lx, y + 1, lz, Blocks.BOOKSHELF);
                }
            }
            case "museum" -> {
                if (u % 3 == 1 && v % 3 == 1) {
                    set(lx, y, lz, Blocks.SMOOTH_QUARTZ);
                    set(lx, y + 1, lz, hash(wx, wz, 9) % 2 == 0 ? Blocks.DECORATED_POT : Blocks.FLOWER_POT);
                }
            }
            default -> {
                if ((u == 2 || u == 7) && v % 3 == 2) set(lx, y, lz, slabTop(Blocks.SPRUCE_SLAB));
                else if ((u == 3 || u == 8) && v % 3 == 2) set(lx, y, lz, stair(Blocks.SPRUCE_STAIRS, Direction.EAST));
            }
        }
    }

    private void bed(int lx, int y, int lz, Direction facing, Block bed) {
        int hx = lx + facing.getStepX(), hz = lz + facing.getStepZ();
        if (roleAt(hx, hz) != ROOM || keepFree(hx, hz) || !m.isAir(fp.ox + hx, y, fp.oz + hz) || !m.isAir(fp.ox + lx, y, fp.oz + lz)) return;
        BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, facing);
        set(lx, y, lz, s.setValue(BedBlock.PART, BedPart.FOOT));
        set(hx, y, hz, s.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private void door(int lx, int y, int lz, Direction facing, Block door) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(lx, y, lz, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(lx, y + 1, lz, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    // ------------------------------------------------------------------ doors, signs, shop fronts

    private void wallSign(int lx, int y, int lz, Direction facing, String... lines) {
        if (!valid(lx, lz) || city.building(fp.ox + lx, fp.oz + lz) != 0) return;
        set(lx, y, lz, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        m.signText(fp.ox + lx, y, fp.oz + lz, lines);
    }

    public static List<String> wrap(String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word.length() > width ? word.substring(0, width) : word);
        }
        if (line.length() > 0) out.add(line.toString());
        while (out.size() > maxLines) out.remove(out.size() - 1);
        while (out.size() < maxLines) out.add("");
        return out;
    }

    private static String shopKind(String kind) {
        return switch (kind) {
            case "supermarket", "convenience", "food", "greengrocer", "butcher", "deli" -> "Продукты";
            case "pharmacy", "chemist" -> "Аптека";
            case "cafe" -> "Кафе";
            case "restaurant" -> "Ресторан";
            case "fast_food" -> "Быстрое питание";
            case "bar", "pub" -> "Бар";
            case "bank", "atm" -> "Банк";
            case "bakery", "pastry", "confectionery" -> "Пекарня";
            case "florist" -> "Цветы";
            case "clothes", "shoes", "fashion", "boutique" -> "Одежда";
            case "hairdresser", "beauty" -> "Салон красоты";
            case "books", "stationery" -> "Книги";
            case "alcohol", "beverages" -> "Напитки";
            case "dentist" -> "Стоматология";
            case "doctors", "clinic" -> "Клиника";
            case "post_office" -> "Почта";
            case "optician" -> "Оптика";
            case "electronics", "mobile_phone", "computer" -> "Электроника";
            case "jewelry" -> "Ювелирный";
            case "library" -> "Библиотека";
            case "cinema" -> "Кинотеатр";
            case "travel_agency" -> "Турагентство";
            case "gift", "souvenir" -> "Сувениры";
            default -> "Магазин";
        };
    }

    private void outerDoor(int lx, int lz, Direction out, boolean main) {
        int y = base + 1;
        Block frame = st.era == Style.Era.WOOD ? st.trim : (st.era == Style.Era.KHRUSHCHEV || st.era == Style.Era.PANEL ? st.wall : st.trim);
        Direction side = out.getClockWise();
        for (int yy = y; yy <= y + 2 && yy < floorY[1]; yy++) set(lx, yy, lz, frame);
        door(lx, y, lz, out.getOpposite(), main && st.era != Style.Era.WOOD ? st.door : st.door);
        // a canopy and a lamp
        int ox = lx + out.getStepX(), oz = lz + out.getStepZ();
        if (valid(ox, oz) && city.building(fp.ox + ox, fp.oz + oz) == 0 && floorY[1] - base >= 3) {
            for (int k = -1; k <= 1; k++) {
                int cx = ox + side.getStepX() * k, cz = oz + side.getStepZ() * k;
                if (valid(cx, cz) && city.building(fp.ox + cx, fp.oz + cz) == 0) set(cx, y + 2, cz, slabTop(Blocks.SMOOTH_STONE_SLAB));
            }
        }
        // steps between the floor and the street where the ground outside is lower or higher
        int level = base;
        for (int k = 1; k <= Footprint.MARGIN - 1; k++) {
            int cx = lx + out.getStepX() * k, cz = lz + out.getStepZ() * k;
            if (!valid(cx, cz) || city.building(fp.ox + cx, fp.oz + cz) != 0) break;
            int gy = city.height(fp.ox + cx, fp.oz + cz);
            if (gy == level) break;
            if (gy < level) {
                for (int yy = gy + 1; yy < level; yy++) set(cx, yy, cz, Blocks.STONE_BRICKS);
                set(cx, level, cz, stair(Blocks.STONE_BRICK_STAIRS, out.getOpposite()));
                for (int yy = level + 1; yy <= level + 3; yy++) set(cx, yy, cz, Blocks.AIR);
                level--;
            } else {
                for (int yy = level + 2; yy <= gy + 3; yy++) set(cx, yy, cz, Blocks.AIR);
                set(cx, level + 1, cz, stair(Blocks.STONE_BRICK_STAIRS, out));
                level++;
            }
        }
    }

    private void doorsAndSigns() {
        boolean first = true;
        for (Entrance e : entrances) {
            int lx = e.x() - fp.ox, lz = e.z() - fp.oz;
            outerDoor(lx, lz, e.out(), first);
            Direction side = e.out().getClockWise();
            int sy = base + 2;
            if (first) {
                String addr = b.address();
                String title = b.name;
                if (title.isEmpty() && !b.pois.isEmpty()) title = b.pois.get(0).name();
                int ax = lx + side.getStepX() + e.out().getStepX(), az = lz + side.getStepZ() + e.out().getStepZ();
                solidBehind(lx + side.getStepX(), sy, lz + side.getStepZ());
                if (!addr.isEmpty()) {
                    List<String> l = wrap(addr, 15, 2);
                    wallSign(ax, sy, az, e.out(), "", l.get(0), l.get(1), "");
                }
                if (!title.isEmpty()) {
                    int bx = lx - side.getStepX() + e.out().getStepX(), bz = lz - side.getStepZ() + e.out().getStepZ();
                    solidBehind(lx - side.getStepX(), sy, lz - side.getStepZ());
                    List<String> l = wrap(title, 15, 4);
                    wallSign(bx, sy, bz, e.out(), l.get(0), l.get(1), l.get(2), l.get(3));
                }
            } else if (st.residential()) {
                int ax = lx + side.getStepX() + e.out().getStepX(), az = lz + side.getStepZ() + e.out().getStepZ();
                solidBehind(lx + side.getStepX(), sy, lz + side.getStepZ());
                wallSign(ax, sy, az, e.out(), "", "Подъезд " + (entrances.indexOf(e) + 1), "", "");
            }
            first = false;
        }
        // shops get their own doors and signboards
        int count = 0;
        for (BuildingInfo.Shop shop : b.shops) {
            if (count++ >= 14) break;
            int[] c = nearestDoorSpot(shop.x(), shop.z(), 400);
            if (c == null || !farFromDoors(c[0], c[1], 3)) continue;
            Direction out = fp.normalAt(c[0], c[1]);
            doorCells.add(c);
            outerDoor(c[0], c[1], out, false);
            Direction side = out.getClockWise();
            int ax = c[0] + side.getStepX() + out.getStepX(), az = c[1] + side.getStepZ() + out.getStepZ();
            solidBehind(c[0] + side.getStepX(), base + 2, c[1] + side.getStepZ());
            List<String> l = wrap(shop.name(), 15, 3);
            wallSign(ax, base + 2, az, out, shopKind(shop.kind()), l.get(0), l.get(1), l.get(2));
            // an opening through the rooms behind it
            Direction in = out.getOpposite();
            for (int k = 1; k <= 3; k++) {
                int cx = c[0] + in.getStepX() * k, cz = c[1] + in.getStepZ() * k;
                if (roleAt(cx, cz) == ROOM || roleAt(cx, cz) == PART) {
                    set(cx, base + 1, cz, Blocks.AIR);
                    set(cx, base + 2, cz, Blocks.AIR);
                }
            }
        }
        for (BuildingInfo.Shop pl : b.plaques) {
            int[] c = fp.nearestFacade(pl.x(), pl.z());
            if (c == null || pl.name().isEmpty()) continue;
            Direction out = fp.normalAt(c[0], c[1]);
            solidBehind(c[0], base + 2, c[1]);
            List<String> l = wrap(pl.name(), 15, 3);
            wallSign(c[0] + out.getStepX(), base + 2, c[1] + out.getStepZ(), out, "Мемор. доска", l.get(0), l.get(1), l.get(2));
        }
    }

    /** Signs need something solid to hang on: turn a window behind them back into wall. */
    private void solidBehind(int lx, int y, int lz) {
        if (fp.wall(lx, lz)) set(lx, y, lz, st.logWalls ? wallState(y) : st(st.trim == Blocks.BIRCH_PLANKS ? st.wall : st.trim));
    }

    // ------------------------------------------------------------------ balconies, domes, minarets

    private void extras() {
        if ((st.balconies || st.loggias) && levels >= 2) {
            for (Footprint.Run run : fp.runs) {
                if (run.length() < 6) continue;
                Direction n = run.normal();
                for (int[] c : run.cells()) {
                    int coord = coordOf(n, c[0], c[1]);
                    int u = Math.floorMod(coord, P);
                    if (u != P - 2 && u != P - 1) continue;
                    if (!windowHere(c[0], c[1], n)) continue;
                    int ox = c[0] + n.getStepX(), oz = c[1] + n.getStepZ();
                    if (!valid(ox, oz) || city.building(fp.ox + ox, fp.oz + oz) != 0) continue;
                    boolean isDoorColumn = false;
                    for (int[] dc : doorCells) if (Math.abs(dc[0] - c[0]) + Math.abs(dc[1] - c[1]) <= 2) isDoorColumn = true;
                    if (isDoorColumn) continue;
                    for (int s = 1; s < levels; s++) {
                        if (st.balconies && hash(Math.floorDiv(coord, P), s, 77) % 4 == 0) continue;
                        int F = floorY[s];
                        set(ox, F, oz, slabTop(Blocks.SMOOTH_STONE_SLAB));
                        set(ox, F + 1, oz, st.loggias ? st(st.accent) : st(Blocks.IRON_BARS));
                        set(c[0], F + 1, c[1], st.glass);
                    }
                }
            }
        }
        if (st.use == Style.Use.CHURCH || st.use == Style.Use.MOSQUE) {
            int bx = -1, bz = -1, bd = 0;
            for (int lz = 0; lz < d; lz++)
                for (int lx = 0; lx < w; lx++)
                    if (dist[idx(lx, lz)] > bd) {
                        bd = dist[idx(lx, lz)];
                        bx = lx;
                        bz = lz;
                    }
            if (bd >= 3) {
                int r = Math.min(5, bd - 1);
                int y0 = top + Math.min(bd, 5);
                boolean mosque = st.use == Style.Use.MOSQUE;
                Block drum = st.wall, dome = mosque ? Blocks.DARK_PRISMARINE : Blocks.GOLD_BLOCK;
                for (int y = y0; y < y0 + 4; y++) disc(bx, y, bz, r, drum, true);
                int[] profile = mosque ? new int[]{r, r, r - 1, r - 2, 1} : new int[]{r, r + 1, r, r - 1, Math.max(1, r - 3), 1};
                for (int k = 0; k < profile.length; k++) disc(bx, y0 + 4 + k, bz, Math.max(0, profile[k]), dome, false);
                int ty = y0 + 4 + profile.length;
                set(bx, ty, bz, mosque ? Blocks.GOLD_BLOCK : Blocks.IRON_BARS);
                set(bx, ty + 1, bz, mosque ? Blocks.LIGHTNING_ROD : Blocks.GOLD_BLOCK);
                if (!mosque) {
                    set(bx, ty + 2, bz, Blocks.IRON_BARS);
                    set(bx - 1, ty + 1, bz, Blocks.IRON_BARS);
                    set(bx + 1, ty + 1, bz, Blocks.IRON_BARS);
                }
                if (mosque && !fp.runs.isEmpty()) {
                    // a minaret at the first corner of the longest wall
                    int[] c = fp.runs.get(0).cells().get(0);
                    int h = top - base + 14;
                    for (int y = base + 1; y <= base + h; y++) {
                        set(c[0], y, c[1], st.wall);
                        if (y > top) for (Direction dir : Direction.Plane.HORIZONTAL)
                            if (valid(c[0] + dir.getStepX(), c[1] + dir.getStepZ()) && y < base + h - 3) set(c[0] + dir.getStepX(), y, c[1] + dir.getStepZ(), st.wall);
                    }
                    for (Direction dir : Direction.Plane.HORIZONTAL) set(c[0] + dir.getStepX() * 2, base + h - 4, c[1] + dir.getStepZ() * 2, Blocks.IRON_BARS);
                    set(c[0], base + h + 1, c[1], Blocks.DARK_PRISMARINE);
                    set(c[0], base + h + 2, c[1], Blocks.PRISMARINE_WALL);
                    set(c[0], base + h + 3, c[1], Blocks.GOLD_BLOCK);
                }
            }
        }
    }

    private void disc(int cx, int y, int cz, int r, Block block, boolean hollow) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int dd = dx * dx + dz * dz;
                if (dd > r * r + 1) continue;
                if (hollow && dd < (r - 1) * (r - 1)) continue;
                if (valid(cx + dx, cz + dz)) set(cx + dx, y, cz + dz, block);
            }
    }
}
