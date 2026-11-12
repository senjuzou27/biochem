package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.entity.NpcRole;

import java.util.ArrayList;
import java.util.List;

import static ru.ufamod.world.Plan.*;

/** Draws the ground (streets, river, greenery) and the ordinary houses of the centre of Ufa. */
public class CityBuilder extends Build {
    private static final int GRASS = 0, SIDEWALK = 1, ROAD = 2, PLAZA = 3, PATH = 4;
    private final byte[][] kind = new byte[SIZE_X][SIZE_Z];
    /** Rectangles {x1, z1, x2, z2} that generic houses and trees must not touch. */
    private final List<int[]> reserved = new ArrayList<>();

    public CityBuilder(Canvas c) {
        super(c, 20261112L);
    }

    public static Canvas buildCity() {
        Canvas c = new Canvas(0, BASE_Y, 0, SIZE_X, SIZE_Z);
        CityBuilder city = new CityBuilder(c);
        city.river();
        city.streets();
        Landmarks lm = new Landmarks(c, city);
        lm.reserveAll();
        city.quarters();
        city.paintGround();
        city.streetFurniture();
        lm.buildAll();
        new DormBuilder(c).build();
        new BsmuBuilder(c).build();
        city.citizens();
        return c;
    }

    // ------------------------------------------------------------------ helpers for other builders

    public void reserve(int x1, int z1, int x2, int z2) {
        reserved.add(new int[]{x1, z1, x2, z2});
    }

    public void mark(int x1, int z1, int x2, int z2, int k) {
        for (int x = Math.max(0, x1); x <= Math.min(SIZE_X - 1, x2); x++)
            for (int z = Math.max(0, z1); z <= Math.min(SIZE_Z - 1, z2); z++)
                kind[x][z] = (byte) k;
    }

    public void plaza(int x1, int z1, int x2, int z2) {
        mark(x1, z1, x2, z2, PLAZA);
    }

    public void path(int x1, int z1, int x2, int z2) {
        mark(x1, z1, x2, z2, PATH);
    }

    private boolean isReserved(int x1, int z1, int x2, int z2) {
        for (int[] r : reserved)
            if (x1 <= r[2] && x2 >= r[0] && z1 <= r[3] && z2 >= r[1]) return true;
        return false;
    }

    // ------------------------------------------------------------------ river

    private boolean inRiver(int x, int z) {
        return z >= RIVER_Z || x <= RIVER_X;
    }

    private void river() {
        BlockState water = st(Blocks.WATER);
        for (int x = 0; x < SIZE_X; x++)
            for (int z = 0; z < SIZE_Z; z++) {
                if (inRiver(x, z)) {
                    set(x, -8, z, rnd.nextInt(4) == 0 ? Blocks.GRAVEL : Blocks.SAND);
                    fill(x, -7, z, x, -5, z, water);
                    air(x, -4, z, x, 0, z);
                } else if (z >= RIVER_Z - 16 && x > RIVER_X + 40) {
                    // The southern bank slopes down to the water in four terraces.
                    int drop = Math.min(4, (z - (RIVER_Z - 16)) / 4 + 1);
                    air(x, 1 - drop, z, x, 0, z);
                    set(x, -drop, z, Blocks.GRASS_BLOCK);
                }
            }
        reserve(0, RIVER_Z - 18, SIZE_X - 1, SIZE_Z - 1);
        reserve(0, 0, RIVER_X + 6, SIZE_Z - 1);
    }

    // ------------------------------------------------------------------ streets

    private void streetNS(int x, int z1, int z2, int width) {
        mark(x, z1, x + width - 1, z2, SIDEWALK);
    }

    private void streetEW(int z, int x1, int x2) {
        mark(x1, z, x2, z + RW - 1, SIDEWALK);
    }

    private void streets() {
        int[] ns = {KM, LEN, SOV, TSUR};
        int[] ew = {CHERN, KOMM, PUSH, ZV, TUK};
        int north = 8, west = 40, east = 606;
        streetNS(KM, north, TUK + RW - 1, RW);
        streetNS(LEN, north, ZV + RW - 1, RW);
        streetNS(SOV, north, TUK + RW - 1, RW);
        streetNS(TSUR, north, TUK + RW - 1, RW);
        streetNS(TEATR, PUSH, ZV + RW - 1, 7);
        for (int z : ew) streetEW(z, z == TUK ? KM : west, east);
        // carriageways
        mark(KM + 3, north, KM + 11, TUK + RW - 4, ROAD);
        mark(LEN + 3, north, LEN + 11, ZV + RW - 4, ROAD);
        mark(SOV + 3, north, SOV + 11, TUK + RW - 4, ROAD);
        mark(TSUR + 3, north, TSUR + 11, TUK + RW - 4, ROAD);
        mark(TEATR + 1, PUSH + 3, TEATR + 5, ZV + RW - 4, ROAD);
        for (int z : ew) mark(z == TUK ? KM + 3 : west, z + 3, east, z + 11, ROAD);
        // Lenina south of Zaki Validi is a pedestrian alley leading into the park.
        mark(LEN + 2, ZV + RW, LEN + 12, TUK - 1, PLAZA);

        // Streets are off-limits for houses.
        for (int x : ns) reserve(x, 0, x + RW - 1, SIZE_Z - 1);
        for (int z : ew) reserve(0, z, SIZE_X - 1, z + RW - 1);
        reserve(TEATR, PUSH, TEATR + 6, ZV);
        reserve(LEN, ZV, LEN + RW - 1, TUK);
    }

    private void paintGround() {
        BlockState road = st(Blocks.GRAY_CONCRETE), walk = st(Blocks.SMOOTH_STONE), plaza = st(Blocks.STONE_BRICKS),
                path = st(Blocks.GRAVEL), white = st(Blocks.WHITE_CONCRETE);
        int[] ns = {KM, LEN, SOV, TSUR};
        int[] ew = {CHERN, KOMM, PUSH, ZV, TUK};
        for (int x = 0; x < SIZE_X; x++)
            for (int z = 0; z < SIZE_Z; z++) {
                if (inRiver(x, z)) continue;
                switch (kind[x][z]) {
                    case SIDEWALK -> set(x, 0, z, walk);
                    case ROAD -> set(x, 0, z, road);
                    case PLAZA -> set(x, 0, z, (x + z) % 7 == 0 ? st(Blocks.POLISHED_ANDESITE) : plaza);
                    case PATH -> set(x, 0, z, path);
                    default -> { }
                }
            }
        // Dashed centre lines.
        for (int x : ns)
            for (int z = 8; z < TUK + RW; z++)
                if (kind[x + 7][z] == ROAD && z % 6 < 3 && !nearCrossing(z, ew)) set(x + 7, 0, z, white);
        for (int z : ew)
            for (int x = 40; x <= 606; x++)
                if (kind[x][z + 7] == ROAD && x % 6 < 3 && !nearCrossing(x, ns)) set(x, 0, z + 7, white);
        // Zebra crossings at every intersection.
        for (int x : ns)
            for (int z : ew) {
                if (kind[x + 7][z + 7] != ROAD) continue;
                for (int i = 3; i <= 11; i += 2) {
                    for (int k = -3; k <= -2; k++) {
                        if (kind[x + i][z + k] == ROAD) set(x + i, 0, z + k, white);
                        if (kind[x + k][z + i] == ROAD) set(x + k, 0, z + i, white);
                    }
                    for (int k = RW + 1; k <= RW + 2; k++) {
                        if (z + k < SIZE_Z && kind[x + i][z + k] == ROAD) set(x + i, 0, z + k, white);
                        if (x + k < SIZE_X && kind[x + k][z + i] == ROAD) set(x + k, 0, z + i, white);
                    }
                }
            }
    }

    private static boolean nearCrossing(int v, int[] streets) {
        for (int s : streets) if (v >= s - 4 && v <= s + RW + 3) return true;
        return false;
    }

    private void streetFurniture() {
        int[] ns = {KM, LEN, SOV, TSUR};
        int[] ew = {CHERN, KOMM, PUSH, ZV, TUK};
        for (int x : ns)
            for (int z = 16; z < TUK; z += 14) {
                if (nearCrossing(z, ew)) continue;
                if (kind[x][z] == SIDEWALK && c.isEmpty(x, 1, z)) lamp(x, z);
                if (kind[x + RW - 1][z + 7] == SIDEWALK && c.isEmpty(x + RW - 1, 1, z + 7)) lamp(x + RW - 1, z + 7);
            }
        for (int z : ew)
            for (int x = 48; x < 600; x += 14) {
                if (nearCrossing(x, ns) || kind[x][z] != SIDEWALK) continue;
                if (c.isEmpty(x, 1, z)) lamp(x, z);
                if (kind[x + 7][z + RW - 1] == SIDEWALK && c.isEmpty(x + 7, 1, z + RW - 1)) lamp(x + 7, z + RW - 1);
            }
        // Parked cars along the kerbs.
        for (int x : ns)
            for (int z = 24; z < TUK - 6; z += 9) {
                if (nearCrossing(z, ew) || nearCrossing(z + 4, ew) || rnd.nextInt(3) != 0) continue;
                if (kind[x + 3][z] == ROAD && kind[x + 3][z + 3] == ROAD)
                    car(rnd.nextBoolean() ? x + 3 : x + 10, z, false, rnd.nextInt(8));
            }
        for (int z : ew)
            for (int x = 50; x < 596; x += 9) {
                if (nearCrossing(x, ns) || nearCrossing(x + 4, ns) || rnd.nextInt(3) != 0) continue;
                if (kind[x][z + 3] == ROAD && kind[x + 3][z + 3] == ROAD)
                    car(x, rnd.nextBoolean() ? z + 3 : z + 10, true, rnd.nextInt(8));
            }
        // Street name plates at the corners of Lenina.
        String[] names = {"ул. Чернышевского", "ул. Коммунистич.", "ул. Пушкина", "ул. Заки Валиди"};
        for (int i = 0; i < names.length; i++) {
            int z = ew[i];
            post(LEN - 1, 1, z - 1, rot(S), "ул. Ленина", "↕", names[i], "↔");
            post(LEN + RW, 1, z + RW, rot(N), "ул. Ленина", "↕", names[i], "↔");
        }
        String[] across = {"ул. К. Маркса", "ул. Советская", "ул. Цюрупы"};
        int[] ax = {KM, SOV, TSUR};
        for (int i = 0; i < ax.length; i++)
            for (int j = 0; j < 4; j++)
                post(ax[i] - 1, 1, ew[j] - 1, rot(S), across[i], "↕", names[j], "↔");
        // Bus stops on Lenina.
        busStop(LEN + RW - 1, 120, "Гостиный двор");
        busStop(LEN + RW - 1, 262, "Театр оперы и", "балета");
        busStop(LEN + RW - 1, 362, "Медицинский", "университет");
    }

    private void busStop(int x, int z, String... name) {
        fill(x, 1, z, x, 3, z, Blocks.IRON_BARS);
        fill(x, 1, z + 5, x, 3, z + 5, Blocks.IRON_BARS);
        fill(x, 4, z, x - 1, 4, z + 5, slab(Blocks.SMOOTH_STONE_SLAB));
        fill(x, 1, z + 1, x, 3, z + 4, Blocks.GLASS_PANE);
        bench(x - 1, z + 2, Direction.WEST);
        String[] lines = new String[4];
        lines[0] = "Остановка";
        for (int i = 0; i < 3; i++) lines[i + 1] = i < name.length ? name[i] : "";
        post(x - 1, 1, z - 1, rot(W), lines);
    }

    // ------------------------------------------------------------------ generic quarters

    private void quarters() {
        int[] xs = {44, KM, KM + RW, LEN, LEN + RW, SOV, SOV + RW, TSUR, TSUR + RW, 606};
        int[] zs = {12, CHERN, CHERN + RW, KOMM, KOMM + RW, PUSH, PUSH + RW, ZV, ZV + RW, TUK, TUK + RW, RIVER_Z - 18};
        for (int i = 0; i + 1 < xs.length; i += 2)
            for (int j = 0; j + 1 < zs.length; j += 2) {
                int x1 = xs[i] + 2, x2 = xs[i + 1] - 3, z1 = zs[j] + 2, z2 = zs[j + 1] - 3;
                if (x2 - x1 < 14 || z2 - z1 < 14) continue;
                // The old centre keeps low merchant and wooden houses; the edges get taller Soviet blocks.
                boolean old = j >= 4 && j <= 8;
                fillQuarter(x1, z1, x2, z2, old);
            }
    }

    private void fillQuarter(int x1, int z1, int x2, int z2, boolean old) {
        // Houses along the northern and southern edges face their streets.
        for (int side = 0; side < 2; side++) {
            int x = x1;
            while (x + 12 <= x2) {
                int w = 13 + rnd.nextInt(12);
                if (x + w > x2) w = x2 - x;
                if (w < 11) break;
                int d = 10 + rnd.nextInt(4);
                int za = side == 0 ? z1 : z2 - d, zb = side == 0 ? z1 + d : z2;
                if (!isReserved(x - 1, za - 1, x + w + 1, zb + 1)) {
                    house(x, za, x + w, zb, side == 0 ? N : S, old);
                    reserve(x, za, x + w, zb);
                }
                x += w + 3 + rnd.nextInt(3);
            }
        }
        // Then the western and eastern edges.
        for (int side = 0; side < 2; side++) {
            int z = z1 + 16;
            while (z + 12 <= z2 - 16) {
                int w = 13 + rnd.nextInt(10);
                if (z + w > z2 - 16) w = z2 - 16 - z;
                if (w < 11) break;
                int d = 10 + rnd.nextInt(3);
                int xa = side == 0 ? x1 : x2 - d, xb = side == 0 ? x1 + d : x2;
                if (!isReserved(xa - 1, z - 1, xb + 1, z + w + 1)) {
                    house(xa, z, xb, z + w, side == 0 ? W : E, old);
                    reserve(xa, z, xb, z + w);
                }
                z += w + 3 + rnd.nextInt(3);
            }
        }
        // Courtyard greenery.
        int trees = (x2 - x1) * (z2 - z1) / 260;
        for (int i = 0; i < trees; i++) {
            int x = x1 + 3 + rnd.nextInt(Math.max(1, x2 - x1 - 6)), z = z1 + 3 + rnd.nextInt(Math.max(1, z2 - z1 - 6));
            if (isReserved(x - 3, z - 3, x + 3, z + 3) || kind[x][z] != GRASS) continue;
            tree(x, z, rnd.nextInt(3) == 0 ? Tree.BIRCH : (rnd.nextInt(3) == 0 ? Tree.LINDEN : Tree.OAK));
        }
    }

    private static final String[] SHOPS = {"Продукты", "Аптека", "Башкирский мёд", "Пекарня", "Цветы", "Книги",
            "Парикмахерская", "Кафе «Агидель»", "Чак-чак и чай", "Кумыс", "Ремонт обуви", "Фотоателье", "Столовая №1", "Сувениры"};

    /** An ordinary house. {@code front} is the side with the entrance. */
    public void house(int x1, int z1, int x2, int z2, Direction front, boolean old) {
        int style = old ? (rnd.nextInt(5) < 2 ? 4 : 3) : rnd.nextInt(3);
        if (old && rnd.nextInt(6) == 0) style = 1;
        Block wall, trim, roofStairs = Blocks.DEEPSLATE_TILE_STAIRS, roofFull = Blocks.DEEPSLATE_TILES;
        int floors;
        boolean pitched = false;
        switch (style) {
            case 0 -> { wall = Blocks.BRICKS; trim = Blocks.SMOOTH_STONE; floors = 5; }                 // brick "khrushchevka"
            case 1 -> { wall = rnd.nextBoolean() ? Blocks.SMOOTH_SANDSTONE : Blocks.YELLOW_TERRACOTTA;   // "stalinka"
                trim = Blocks.SMOOTH_QUARTZ; floors = 4 + rnd.nextInt(2); pitched = true; }
            case 2 -> { wall = rnd.nextBoolean() ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE;   // panel block
                trim = Blocks.CYAN_TERRACOTTA; floors = 6 + rnd.nextInt(4); }
            case 3 -> { Block[] w = {Blocks.BRICKS, Blocks.WHITE_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.SMOOTH_SANDSTONE};
                wall = w[rnd.nextInt(w.length)]; trim = Blocks.SMOOTH_QUARTZ; floors = 2; pitched = true;   // merchant house
                roofStairs = Blocks.DARK_PRISMARINE_STAIRS; roofFull = Blocks.DARK_PRISMARINE; }
            default -> { wall = rnd.nextBoolean() ? Blocks.SPRUCE_PLANKS : Blocks.STRIPPED_SPRUCE_LOG;  // wooden house
                trim = Blocks.BIRCH_PLANKS; floors = 1 + rnd.nextInt(2); pitched = true;
                roofStairs = Blocks.DARK_OAK_STAIRS; roofFull = Blocks.DARK_OAK_PLANKS; }
        }
        int top = floors * 4;
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        walls(x1, 1, z1, x2, top, z2, wall);
        if (style == 3 || style == 1) walls(x1, 1, z1, x2, 1, z2, Blocks.STONE_BRICKS);
        air(x1 + 1, 1, z1 + 1, x2 - 1, top, z2 - 1);
        for (int f = 0; f < floors; f++) {
            int y0 = 1 + f * 4;
            fill(x1 + 1, y0 + 3, z1 + 1, x2 - 1, y0 + 3, z2 - 1, Blocks.SPRUCE_PLANKS);
            for (int x = x1 + 3; x < x2 - 2; x += 5)
                for (int z = z1 + 3; z < z2 - 2; z += 5)
                    set(x, y0 + 3, z, Blocks.GLOWSTONE);
            if (style != 4) walls(x1, y0 + 3, z1, x2, y0 + 3, z2, style == 2 ? wall : trim);
            for (int x = x1 + 2; x <= x2 - 2; x++)
                if ((x - x1) % 3 != 0) {
                    fill(x, y0 + 1, z1, x, y0 + 2, z1, Blocks.GLASS_PANE);
                    fill(x, y0 + 1, z2, x, y0 + 2, z2, Blocks.GLASS_PANE);
                }
            for (int z = z1 + 2; z <= z2 - 2; z++)
                if ((z - z1) % 3 != 0) {
                    fill(x1, y0 + 1, z, x1, y0 + 2, z, Blocks.GLASS_PANE);
                    fill(x2, y0 + 1, z, x2, y0 + 2, z, Blocks.GLASS_PANE);
                }
        }
        if (style == 2) {
            // Coloured stripes between the windows of a panel block.
            for (int f = 0; f < floors; f++) {
                fill(x1 + 1, 1 + f * 4, z1, x2 - 1, 1 + f * 4, z1, trim);
                fill(x1 + 1, 1 + f * 4, z2, x2 - 1, 1 + f * 4, z2, trim);
            }
        }
        if (pitched) {
            if (style == 1) hipRoof(x1, z1, x2, z2, top + 1, roofStairs, roofFull);
            else gableRoof(x1, z1, x2, z2, top + 1, roofStairs, style == 4 ? wall : roofFull);
        } else {
            fill(x1, top + 1, z1, x2, top + 1, z2, slab(Blocks.SMOOTH_STONE_SLAB));
            walls(x1, top + 1, z1, x2, top + 1, z2, Blocks.SMOOTH_STONE);
        }
        // Entrance.
        int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
        int dx = front == W ? x1 : front == E ? x2 : mx, dz = front == N ? z1 : front == S ? z2 : mz;
        fill(dx, 1, dz, dx, 3, dz, wall);
        door(dx, 1, dz, front.getOpposite(), style == 4 ? Blocks.SPRUCE_DOOR : Blocks.DARK_OAK_DOOR, false);
        int ox = dx + front.getStepX(), oz = dz + front.getStepZ();
        if (kind[clamp(ox, SIZE_X)][clamp(oz, SIZE_Z)] == GRASS) mark(ox, oz, ox + front.getStepX(), oz + front.getStepZ(), PATH);
        // A shop or a house number next to the door.
        Direction side = front.getClockWise();
        int sx = dx + side.getStepX() * 2 + front.getStepX(), sz = dz + side.getStepZ() * 2 + front.getStepZ();
        set(sx - front.getStepX(), 2, sz - front.getStepZ(), wall);
        set(sx - front.getStepX(), 3, sz - front.getStepZ(), wall);
        if (style != 4 && rnd.nextInt(3) != 0) sign(sx, 3, sz, front, "", SHOPS[rnd.nextInt(SHOPS.length)], "", "");
        else sign(sx, 2, sz, front, "", "дом " + (1 + rnd.nextInt(120)), "", "");
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max - 1, v));
    }

    // ------------------------------------------------------------------ people on the streets

    private void citizens() {
        String[] names = {"Айгуль", "Рустам", "Гульнара", "Ильдар", "Алсу", "Азат", "Лилия", "Тимур", "Зульфия", "Радик",
                "Ольга", "Сергей", "Наталья", "Булат", "Эльвира", "Марат", "Диана", "Артур", "Регина", "Вадим"};
        int[][] spots = {{LEN + 1, 100}, {LEN + 13, 150}, {LEN + 1, 230}, {LEN + 13, 270}, {LEN + 1, 300}, {LEN + 13, 350},
                {LEN + 1, 410}, {LEN + 6, 470}, {LEN + 7, 520}, {250, KOMM + 1}, {290, KOMM + 13}, {380, KOMM + 1},
                {250, PUSH + 13}, {400, PUSH + 1}, {250, ZV + 1}, {400, ZV + 13}, {500, ZV + 1}, {KM + 1, 260},
                {SOV + 13, 380}, {SOV + 1, 250}, {500, PUSH + 13}, {LEN + 13, 50}, {LEN + 1, 30}, {230, CHERN + 1}};
        for (int i = 0; i < spots.length; i++)
            npc(spots[i][0], 1, spots[i][1], rnd.nextInt(360), NpcRole.CITIZEN, names[i % names.length], i, true);
    }
}
