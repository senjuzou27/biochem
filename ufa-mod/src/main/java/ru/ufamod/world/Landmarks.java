package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ru.ufamod.entity.NpcRole;

import static ru.ufamod.world.Plan.*;

/** The recognisable places of the centre of Ufa. */
public class Landmarks extends Build {
    private final CityBuilder city;

    public Landmarks(Canvas c, CityBuilder city) {
        super(c, 77L);
        this.city = city;
    }

    /** Must run before the ordinary houses are placed. */
    public void reserveAll() {
        city.reserve(336, 10, 366, 66);                          // dormitory
        city.reserve(KM + RW, PUSH + RW, LEN - 1, ZV - 1);       // the BSMU quarter
        city.reserve(KM + RW, CHERN + RW, LEN - 1, KOMM - 1);    // Gostiny Dvor and its square
        city.reserve(LEN + RW, CHERN + RW, SOV - 1, KOMM - 1);   // Lenin square and Art-Kvadrat
        city.reserve(234, KOMM + RW, LEN - 1, PUSH - 1);         // opera theatre and its square
        city.reserve(LEN + RW, PUSH + RW, 412, ZV - 1);          // academy of arts, national library
        city.reserve(SOV + RW, PUSH + RW, TSUR - 1, ZV - 1);     // Sovetskaya square
        city.reserve(478, 262, 552, PUSH - 1);                   // ministry of agriculture
        city.reserve(44, ZV + RW, KM - 1, TUK - 1);              // congress hall, TV centre, university
        city.reserve(KM + RW, ZV + RW, SOV - 1, TUK - 1);        // stadium, Kurultai, Lenin park
        city.reserve(30, TUK + RW, 112, RIVER_Z);                // Salavat Yulaev
        city.reserve(296, TUK + RW, 424, RIVER_Z);               // House of the Republic
        city.reserve(468, TUK + RW, 532, RIVER_Z);               // mosque
        city.reserve(572, TUK + RW, 630, RIVER_Z);               // Friendship monument
    }

    public void buildAll() {
        gostinyDvor();
        leninSquareAndArtKvadrat();
        opera();
        gymnasium();
        libraryAndAcademy();
        sovetskayaSquare();
        westBluff();
        stadium();
        kurultaiAndPark();
        salavat();
        houseOfRepublic();
        mosque();
        friendship();
    }

    // ------------------------------------------------------------------ Gostiny Dvor

    private void gostinyDvor() {
        int x1 = 204, x2 = 292, z1 = 100, z2 = 168;
        city.plaza(KM + RW + 2, CHERN + RW + 2, LEN - 3, KOMM - 3);
        Block wall = Blocks.YELLOW_TERRACOTTA, white = Blocks.SMOOTH_QUARTZ;
        fill(x1, 0, z1, x2, 0, z2, Blocks.POLISHED_ANDESITE);
        air(x1, 1, z1, x2, 16, z2);
        // outer arcade: white piers with arches, a gallery two blocks deep behind them
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                int ring = Math.min(Math.min(x - x1, x2 - x), Math.min(z - z1, z2 - z));
                boolean corner = (x - x1 < 3 || x2 - x < 3) && (z - z1 < 3 || z2 - z < 3);
                int along = (x == x1 || x == x2) ? z - z1 : x - x1;
                if (ring == 0) {
                    if (along % 4 == 0 || corner) fill(x, 1, z, x, 4, z, white);
                    else {
                        set(x, 4, z, white);
                        if (along % 4 != 2) set(x, 3, z, stairTop(Blocks.QUARTZ_STAIRS, archSide(x, z, x1, x2, z1, z2, along % 4 == 1)));
                    }
                    fill(x, 5, z, x, 9, z, wall);
                    set(x, 5, z, white);
                    if (along % 4 == 2 && !corner) fill(x, 7, z, x, 8, z, Blocks.GLASS_PANE);
                    set(x, 10, z, white);
                } else if (ring <= 2) {
                    set(x, 5, z, Blocks.SMOOTH_STONE);
                    set(x, 10, z, Blocks.SMOOTH_STONE);
                } else if (ring == 3) {
                    fill(x, 1, z, x, 9, z, wall);
                    boolean gate = Math.abs(x - (x1 + x2) / 2) <= 2 || Math.abs(z - (z1 + z2) / 2) <= 2;
                    if (gate) air(x, 1, z, x, 3, z);
                    else if (along % 4 == 2) fill(x, 2, z, x, 3, z, Blocks.GLASS);
                    else if (along % 8 == 4) set(x, 2, z, Blocks.LANTERN);
                    set(x, 10, z, Blocks.SMOOTH_STONE);
                } else {
                    // the glazed atrium
                    set(x, 0, z, (x + z) % 2 == 0 ? Blocks.POLISHED_DIORITE : Blocks.POLISHED_ANDESITE);
                    if (ring <= 6) {
                        set(x, 5, z, Blocks.SMOOTH_STONE);
                        if (ring == 6) set(x, 6, z, Blocks.IRON_BARS);
                        set(x, 10, z, Blocks.SMOOTH_STONE);
                        if (ring == 5 && (x + z) % 6 == 0) hangingLamp(x, 4, z);
                    } else {
                        int rise = Math.min(3, ring - 6);
                        set(x, 10 + rise, z, (x - x1) % 6 == 0 || (z - z1) % 6 == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_BLUE_STAINED_GLASS);
                    }
                }
            }
        // kiosks, benches and a small fountain inside
        int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
        fill(mx - 2, 1, mz - 2, mx + 2, 1, mz + 2, Blocks.SMOOTH_QUARTZ);
        fill(mx - 1, 1, mz - 1, mx + 1, 1, mz + 1, Blocks.WATER);
        set(mx, 1, mz, Blocks.SMOOTH_QUARTZ);
        set(mx, 2, mz, Blocks.SEA_LANTERN);
        String[] shops = {"Башкирский мёд", "Сувениры", "Чак-чак", "Кумыс", "Книги", "Цветы"};
        for (int i = 0; i < 6; i++) {
            int kx = x1 + 14 + i * 12;
            for (int kz : new int[]{z1 + 12, z2 - 14}) {
                fill(kx, 1, kz, kx + 3, 1, kz + 2, Blocks.SPRUCE_PLANKS);
                fill(kx, 3, kz, kx + 3, 3, kz + 2, slab(Blocks.RED_NETHER_BRICK_SLAB));
                set(kx, 2, kz, Blocks.SPRUCE_FENCE);
                set(kx + 3, 2, kz, Blocks.SPRUCE_FENCE);
                set(kx, 2, kz + 2, Blocks.SPRUCE_FENCE);
                set(kx + 3, 2, kz + 2, Blocks.SPRUCE_FENCE);
                post(kx + 1, 2, kz + 1, rot(kz < mz ? S : N), "", shops[i], "", "");
            }
        }
        npc(mx + 6, 1, mz, 90, NpcRole.CITIZEN, "Продавец мёда", 2, false);
        npc(mx - 8, 1, mz + 3, 0, NpcRole.CITIZEN, "Покупательница", 1, true);
        // signs at the gates
        post((x1 + x2) / 2 + 4, 1, z2 + 2, rot(S), "Гостиный двор", "Верхнеторговая", "площадь, 1", "с 1825 года");
        post(x2 + 2, 1, mz + 4, rot(E), "Гостиный двор", "торговые ряды", "XIX века", "");
        // the square: lamps, benches, trees in tubs
        for (int x = KM + RW + 8; x < LEN - 6; x += 12) {
            lamp(x, z2 + 8);
            lamp(x, z1 - 6);
            bench(x + 3, z2 + 10, S);
        }
        for (int z = z1; z <= z2; z += 10) {
            tree(x2 + 12, z, Tree.LINDEN);
            tree(x1 - 12, z, Tree.LINDEN);
        }
    }

    private static Direction archSide(int x, int z, int x1, int x2, int z1, int z2, boolean first) {
        if (z == z1 || z == z2) return first ? Direction.WEST : Direction.EAST;
        return first ? Direction.NORTH : Direction.SOUTH;
    }

    // ------------------------------------------------------------------ Lenin square, Art-Kvadrat

    private void leninSquareAndArtKvadrat() {
        // Lenin square at the corner of Lenina and Kommunisticheskaya
        int sx1 = LEN + RW + 2, sx2 = 384, sz1 = 146, sz2 = KOMM - 3;
        city.plaza(sx1 + 14, sz1 + 2, sx1 + 28, sz2 - 2);
        city.path(sx1, (sz1 + sz2) / 2, sx2, (sz1 + sz2) / 2 + 1);
        city.path(sx1 + 20, sz1, sx1 + 21, sz2);
        int lx = sx1 + 21, lz = (sz1 + sz2) / 2;
        fill(lx - 2, 1, lz - 2, lx + 2, 1, lz + 2, Blocks.POLISHED_GRANITE);
        fill(lx - 1, 2, lz - 1, lx + 1, 4, lz + 1, Blocks.POLISHED_GRANITE);
        statue(lx, 5, lz, Blocks.POLISHED_DEEPSLATE, true);
        post(lx - 3, 1, lz, rot(W), "Сквер имени", "Ленина", "Памятник", "В. И. Ленину");
        for (int i = 0; i < 14; i++) {
            int x = sx1 + 2 + rnd.nextInt(sx2 - sx1 - 4), z = sz1 + 2 + rnd.nextInt(sz2 - sz1 - 4);
            if (Math.abs(x - lx) < 9 && Math.abs(z - lz) < 9) continue;
            tree(x, z, i % 3 == 0 ? Tree.SPRUCE : Tree.LINDEN);
        }
        bench(lx - 6, lz - 6, S);
        bench(lx + 5, lz + 6, N);
        lamp(lx - 8, lz + 4);
        lamp(lx + 8, lz - 4);

        // Art-Kvadrat: old brick yards turned into a public space
        int ax1 = 390, ax2 = 446, az1 = 92, az2 = 184;
        city.plaza(ax1, az1, ax2, az2);
        int[][] blocks = {{ax1, az1, ax1 + 22, az1 + 12}, {ax1 + 30, az1, ax2, az1 + 12}, {ax1, az1 + 24, ax1 + 12, az1 + 46},
                {ax2 - 12, az1 + 24, ax2, az1 + 50}, {ax1, az2 - 30, ax1 + 12, az2 - 6}, {ax1 + 22, az2 - 12, ax2, az2}};
        Direction[] fronts = {S, S, E, W, E, N};
        for (int i = 0; i < blocks.length; i++)
            city.house(blocks[i][0], blocks[i][1], blocks[i][2], blocks[i][3], fronts[i], true);
        int cx = (ax1 + ax2) / 2, cz = (az1 + az2) / 2;
        for (int z = az1 + 18; z <= az2 - 18; z += 6) {
            fill(cx - 8, 5, z, cx + 8, 5, z, Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.X));
            fill(cx - 8, 1, z, cx - 8, 5, z, Blocks.SPRUCE_FENCE);
            fill(cx + 8, 1, z, cx + 8, 5, z, Blocks.SPRUCE_FENCE);
            for (int x = cx - 6; x <= cx + 6; x += 4) hangingLamp(x, 4, z);
        }
        bench(cx - 3, cz, E);
        bench(cx + 3, cz + 4, W);
        set(cx, 1, cz + 10, Blocks.CAMPFIRE);
        post(ax1 + 14, 1, az2 + 1, rot(S), "Арт-квадрат", "кафе, музыка,", "выставки", "");
        post(cx, 1, az1 + 15, rot(S), "Арт-квадрат", "", "Кофе и эчпочмак", "→");
        npc(cx + 2, 1, cz - 6, 180, NpcRole.CITIZEN, "Уличный музыкант", 3, false);
        npc(cx - 4, 1, cz + 8, 0, NpcRole.CITIZEN, "Бариста", 0, false);
    }

    /** A simple standing figure, 5 blocks tall; optionally with an outstretched arm. */
    private void statue(int x, int y, int z, Block b, boolean arm) {
        fill(x, y, z, x, y + 2, z, b);
        set(x - 1, y + 2, z, b);
        set(x + 1, y + 2, z, b);
        set(x, y + 3, z, b);
        if (arm) set(x - 2, y + 2, z, b);
    }

    // ------------------------------------------------------------------ opera and ballet theatre

    private void opera() {
        // Aksakov People's House (1909-1920): brick, a classical front to Lenina, an oriental side to Pushkina
        int x1 = 262, x2 = 314, z1 = 256, z2 = 304, h = 15;
        Block brick = Blocks.BRICKS, white = Blocks.SMOOTH_QUARTZ;
        city.plaza(236, KOMM + RW + 2, LEN - 3, PUSH - 3);
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        air(x1, 1, z1, x2, 30, z2);
        walls(x1, 1, z1, x2, h, z2, brick);
        walls(x1, 1, z1, x2, 1, z2, Blocks.STONE_BRICKS);
        walls(x1, 6, z1, x2, 6, z2, white);
        walls(x1, h, z1, x2, h, z2, white);
        fill(x1, h + 1, z1, x2, h + 1, z2, Blocks.SMOOTH_STONE);
        for (int y : new int[]{5, 10}) {
            fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, Blocks.SPRUCE_PLANKS);
            for (int x = x1 + 4; x < x2; x += 6)
                for (int z = z1 + 4; z < z2; z += 6)
                    set(x, y, z, Blocks.GLOWSTONE);
        }
        // east front: windows
        for (int z = z1 + 3; z <= z2 - 3; z += 4) {
            fill(x2, 2, z, x2, 4, z + 1, Blocks.GLASS_PANE);
            fill(x2, 8, z, x2, 12, z + 1, Blocks.GLASS_PANE);
        }
        // north side: plain windows; south side (Pushkina): tall narrow "oriental" slits with pointed tops
        for (int x = x1 + 3; x <= x2 - 3; x += 4) {
            fill(x, 2, z1, x + 1, 4, z1, Blocks.GLASS_PANE);
            fill(x, 8, z1, x + 1, 11, z1, Blocks.GLASS_PANE);
            fill(x, 2, z2, x, 4, z2, Blocks.GLASS_PANE);
            fill(x, 7, z2, x, 12, z2, Blocks.GLASS_PANE);
            set(x, 13, z2, white);
            fill(x + 2, 7, z2, x + 2, 13, z2, Blocks.TERRACOTTA);
        }
        for (int z = z1 + 3; z <= z2 - 3; z += 4) fill(x1, 8, z, x1, 11, z + 1, Blocks.GLASS_PANE);
        // stage tower at the back
        walls(x1, h + 1, z1 + 8, x1 + 20, h + 7, z2 - 8, brick);
        fill(x1, h + 8, z1 + 8, x1 + 20, h + 8, z2 - 8, Blocks.SMOOTH_STONE);
        walls(x1, h + 8, z1 + 8, x1 + 20, h + 8, z2 - 8, white);
        // entrance: a deep balcony canopy on four square brick piers, a four-column portico above it
        int mz = (z1 + z2) / 2;
        fill(x2 + 1, 0, mz - 9, x2 + 6, 0, mz + 9, Blocks.STONE_BRICKS);
        fill(x2 + 1, 1, mz - 8, x2 + 5, 1, mz + 8, Blocks.POLISHED_ANDESITE);
        fill(x2 + 6, 1, mz - 8, x2 + 6, 1, mz + 8, stair(Blocks.POLISHED_ANDESITE_STAIRS, W));
        for (int z : new int[]{mz - 7, mz - 2, mz + 3, mz + 8 - 1}) {
            fill(x2 + 4, 2, z, x2 + 5, 5, z, brick);
        }
        fill(x2 + 1, 6, mz - 8, x2 + 5, 6, mz + 8, white);
        fill(x2 + 5, 7, mz - 8, x2 + 5, 7, mz + 8, Blocks.IRON_BARS);
        for (int z : new int[]{mz - 6, mz - 2, mz + 2, mz + 6}) fill(x2 + 3, 7, z, x2 + 3, 12, z, Blocks.QUARTZ_PILLAR);
        fill(x2 + 1, 13, mz - 8, x2 + 4, 14, mz + 8, white);
        for (int i = 0; i < 4; i++) fill(x2 + 1, 15 + i, mz - 7 + i * 2, x2 + 4, 15 + i, mz + 7 - i * 2, white);
        // dome with a spire over the front part
        int dx = x2 - 9;
        for (int i = 0; i < 5; i++) {
            int r = 6 - i - (i > 2 ? 1 : 0);
            for (int ax = -r; ax <= r; ax++)
                for (int az = -r; az <= r; az++)
                    if (ax * ax + az * az <= r * r + 1) set(dx + ax, h + 2 + i, mz + az, Blocks.OXIDIZED_COPPER);
        }
        fill(dx, h + 7, mz, dx, h + 12, mz, Blocks.LIGHTNING_ROD);
        // doors
        for (int z : new int[]{mz - 4, mz, mz + 4}) {
            fill(x2, 2, z - 1, x2, 4, z + 2, white);
            doubleDoor(x2, 2, z + 1, W, Blocks.DARK_OAK_DOOR);
        }
        // the copper muses Euterpe and Terpsichore on the front
        statue(x2 + 2, 15, mz - 10, Blocks.EXPOSED_COPPER, false);
        statue(x2 + 2, 15, mz + 10, Blocks.EXPOSED_COPPER, false);
        fill(x2 + 1, 13, mz - 11, x2 + 3, 14, mz - 9, white);
        fill(x2 + 1, 13, mz + 9, x2 + 3, 14, mz + 11, white);
        post(x2 + 7, 1, mz - 10, rot(E), "Башкирский гос.", "театр оперы", "и балета", "ул. Ленина, 5/1");
        post(x2 + 7, 1, mz + 10, rot(E), "Аксаковский", "народный дом", "1909—1920", "арх. Рудавский");
        sign(x2 + 1, 3, mz + 6, E, "Здесь впервые", "вышел на сцену", "Рудольф", "Нуреев");
        // Pushkin alley along the southern side
        for (int x = 240; x <= x2; x += 7) {
            tree(x, z2 + 3, Tree.LINDEN);
            if (x % 2 == 0) bench(x + 3, z2 + 3, S);
        }
        // Theatre square to the north with the "Seven Girls" fountain
        int fx = 286, fz = 228;
        for (int ax = -7; ax <= 7; ax++)
            for (int az = -7; az <= 7; az++) {
                int d = ax * ax + az * az;
                if (d <= 50 && d > 30) set(fx + ax, 1, fz + az, Blocks.POLISHED_GRANITE);
                else if (d <= 30) set(fx + ax, 0, fz + az, Blocks.WATER);
            }
        fill(fx - 7, -1, fz - 7, fx + 7, -1, fz + 7, Blocks.PRISMARINE);
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2 * i / 7;
            int gx = fx + (int) Math.round(Math.cos(a) * 3), gz = fz + (int) Math.round(Math.sin(a) * 3);
            fill(gx, 0, gz, gx, 2, gz, Blocks.EXPOSED_COPPER);
            set(gx, 3, gz, Blocks.CUT_COPPER);
        }
        set(fx, 0, fz, Blocks.SEA_LANTERN);
        post(fx + 9, 1, fz, rot(E), "Фонтан", "«Семь девушек»", "Театральный", "сквер");
        for (int x = 244; x <= 310; x += 11) {
            if (Math.abs(x - fx) < 12) continue;
            tree(x, 216, Tree.OAK);
            tree(x, 240, Tree.BIRCH);
        }
        bench(fx - 10, fz - 2, E);
        bench(fx + 10, fz + 4, W);
        lamp(fx - 9, fz + 9);
        lamp(fx + 9, fz - 9);
    }

    // ------------------------------------------------------------------ BSMU building no. 2 (men's gymnasium, 1847)

    private void gymnasium() {
        // classicism: rusticated ground storey, flat pilasters, paired windows on the third storey
        int x1 = 236, x2 = 272, z1 = 404, z2 = 432, h = 15;
        Block wall = Blocks.YELLOW_TERRACOTTA, white = Blocks.SMOOTH_QUARTZ;
        body(x1, z1, x2, z2, h, wall, white, true);
        for (int x = x1; x <= x2; x += 4) fill(x, 7, z2, x, h - 1, z2, white);
        // two-storey wings on both sides
        body(224, 412, 232, z2, 10, wall, white, false);
        body(276, 412, 284, z2, 10, wall, white, false);
        hipRoof(x1, z1, x2, z2, h + 1, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        hipRoof(224, 412, 232, z2, 11, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        hipRoof(276, 412, 284, z2, 11, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        int mx = (x1 + x2) / 2;
        fill(mx - 1, 1, z2, mx + 2, 4, z2, white);
        doubleDoor(mx + 1, 2, z2, N, Blocks.DARK_OAK_DOOR);
        fill(mx - 1, 1, z2 + 1, mx + 2, 1, z2 + 1, stair(Blocks.POLISHED_ANDESITE_STAIRS, N));
        sign(mx - 2, 3, z2 + 1, S, "БГМУ", "Учебный корпус №2", "ул. Заки Валиди,", "47");
        sign(mx + 3, 3, z2 + 1, S, "Здание губернской", "мужской гимназии", "1847 г.", "классицизм");
        // forecourt up to the street and the yard of the quarter
        for (int x = 222; x <= 286; x++)
            for (int z = z2 + 1; z <= ZV - 1; z++)
                if (c.get(x, 0, z) == null) set(x, 0, z, Blocks.STONE_BRICKS);
        for (int x = 226; x <= 284; x += 8) if (Math.abs(x - mx) > 5) tree(x, ZV - 3, Tree.LINDEN);
        // a plainer teaching building on the Pushkina side of the quarter
        city.house(226, 328, 258, 340, N, false);
        post(242, 1, 326, rot(N), "БГМУ", "учебный корпус", "ул. Пушкина", "");
        for (int i = 0; i < 16; i++) {
            int x = 224 + rnd.nextInt(60), z = 346 + rnd.nextInt(52);
            tree(x, z, i % 2 == 0 ? Tree.BIRCH : Tree.OAK);
        }
        bench(270, 380, E);
        bench(270, 392, E);
    }

    /** A plain masonry block with regular windows, used for the classical public buildings. */
    private void body(int x1, int z1, int x2, int z2, int h, Block wall, Block trim, boolean rustic) {
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        air(x1, 1, z1, x2, h + 8, z2);
        walls(x1, 1, z1, x2, h, z2, wall);
        if (rustic) walls(x1, 1, z1, x2, 5, z2, Blocks.CUT_SANDSTONE);
        walls(x1, 1, z1, x2, 1, z2, Blocks.POLISHED_ANDESITE);
        walls(x1, 6, z1, x2, 6, z2, trim);
        walls(x1, h, z1, x2, h, z2, trim);
        fill(x1, h, z1, x2, h, z2, Blocks.SMOOTH_STONE);
        walls(x1, h, z1, x2, h, z2, trim);
        for (int y = 5; y < h; y += 5) {
            fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, Blocks.SPRUCE_PLANKS);
            for (int x = x1 + 3; x < x2; x += 6)
                for (int z = z1 + 3; z < z2; z += 6)
                    set(x, y, z, Blocks.GLOWSTONE);
        }
        for (int y0 = 1; y0 + 4 <= h; y0 += 5) {
            for (int x = x1 + 2; x <= x2 - 2; x++)
                if ((x - x1) % 4 == 2 || ((x - x1) % 4 == 3 && y0 > 10)) {
                    fill(x, y0 + 1, z1, x, y0 + 3, z1, Blocks.GLASS_PANE);
                    fill(x, y0 + 1, z2, x, y0 + 3, z2, Blocks.GLASS_PANE);
                }
            for (int z = z1 + 2; z <= z2 - 2; z++)
                if ((z - z1) % 4 == 2 || ((z - z1) % 4 == 3 && y0 > 10)) {
                    fill(x1, y0 + 1, z, x1, y0 + 3, z, Blocks.GLASS_PANE);
                    fill(x2, y0 + 1, z, x2, y0 + 3, z, Blocks.GLASS_PANE);
                }
        }
    }

    // ------------------------------------------------------------------ national library, academy of arts

    private void libraryAndAcademy() {
        // Academy of Arts: the former Assembly of the Nobility (1856)
        int ax1 = 340, ax2 = 384, az1 = 328, az2 = 344;
        body(ax1, az1, ax2, az2, 10, Blocks.WHITE_TERRACOTTA, Blocks.SMOOTH_QUARTZ, false);
        for (int z = az1; z <= az2; z += 4) fill(ax1, 2, z, ax1, 9, z, Blocks.SMOOTH_QUARTZ);
        hipRoof(ax1, az1, ax2, az2, 11, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        int amz = (az1 + az2) / 2;
        fill(ax1, 1, amz - 1, ax1, 4, amz + 2, Blocks.SMOOTH_QUARTZ);
        doubleDoor(ax1, 2, amz, E, Blocks.DARK_OAK_DOOR);
        fill(ax1 - 1, 1, amz - 1, ax1 - 1, 1, amz + 2, stair(Blocks.POLISHED_ANDESITE_STAIRS, E));
        post(ax1 - 2, 1, amz - 3, rot(W), "Академия искусств", "им. З. Исмагилова", "Дворянское", "собрание, 1856");

        // National library named after Akhmet-Zaki Validi (Lenina, 4), opposite the university
        int x1 = 342, x2 = 398, z1 = 356, z2 = 422, h = 20;
        Block wall = Blocks.LIGHT_GRAY_CONCRETE, white = Blocks.SMOOTH_QUARTZ;
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        air(x1, 1, z1, x2, h + 4, z2);
        walls(x1, 1, z1, x2, h, z2, wall);
        fill(x1, h, z1, x2, h, z2, Blocks.SMOOTH_STONE);
        walls(x1, h, z1, x2, h + 1, z2, white);
        for (int y = 5; y < h; y += 5) {
            fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, Blocks.SPRUCE_PLANKS);
            walls(x1, y, z1, x2, y, z2, white);
            for (int x = x1 + 4; x < x2; x += 6)
                for (int z = z1 + 4; z < z2; z += 6)
                    set(x, y, z, Blocks.GLOWSTONE);
        }
        for (int z = z1 + 2; z <= z2 - 2; z++) {
            boolean rib = (z - z1) % 3 == 0;
            for (int y0 = 1; y0 < h; y0 += 5) {
                if (rib) {
                    fill(x1, y0, z, x1, y0 + 3, z, white);
                    fill(x2, y0, z, x2, y0 + 3, z, white);
                } else {
                    fill(x1, y0 + 1, z, x1, y0 + 3, z, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
                    fill(x2, y0 + 1, z, x2, y0 + 3, z, Blocks.GLASS_PANE);
                }
            }
        }
        for (int x = x1 + 2; x <= x2 - 2; x++)
            if ((x - x1) % 3 != 0)
                for (int y0 = 1; y0 < h; y0 += 5) {
                    fill(x, y0 + 1, z1, x, y0 + 3, z1, Blocks.GLASS_PANE);
                    fill(x, y0 + 1, z2, x, y0 + 3, z2, Blocks.GLASS_PANE);
                }
        int mz = (z1 + z2) / 2;
        fill(x1 - 3, 1, mz - 6, x1 - 1, 1, mz + 6, Blocks.POLISHED_ANDESITE);
        fill(x1 - 4, 1, mz - 6, x1 - 4, 1, mz + 6, stair(Blocks.POLISHED_ANDESITE_STAIRS, E));
        for (int z = mz - 6; z <= mz + 6; z += 4) fill(x1 - 2, 2, z, x1 - 2, 6, z, Blocks.QUARTZ_PILLAR);
        fill(x1 - 3, 7, mz - 7, x1 - 1, 7, mz + 7, white);
        fill(x1, 1, mz - 2, x1, 4, mz + 3, white);
        doubleDoor(x1, 2, mz, E, Blocks.DARK_OAK_DOOR);
        doubleDoor(x1, 2, mz + 2, E, Blocks.DARK_OAK_DOOR);
        sign(x1 - 1, 3, mz - 3, W, "Национальная", "библиотека им.", "А.-З. Валиди", "ул. Ленина, 4");
        for (int x = LEN + RW; x < x1 - 4; x++)
            for (int z = mz - 8; z <= mz + 8; z++)
                set(x, 0, z, Blocks.STONE_BRICKS);
        for (int z = z1 + 4; z <= z2 - 4; z += 9) if (Math.abs(z - mz) > 10) tree(x1 - 4, z, Tree.SPRUCE);
        for (int i = 0; i < 10; i++) tree(402 + rnd.nextInt(8), 330 + rnd.nextInt(104), Tree.OAK);
    }

    // ------------------------------------------------------------------ Sovetskaya square

    private void sovetskayaSquare() {
        int x1 = SOV + RW + 2, x2 = TSUR - 3, z1 = PUSH + RW + 2, z2 = ZV - 3;
        city.plaza(x1, z1, x2 - 26, z2 - 30);
        // House of Industry (Stalinist empire) on the eastern side
        body(x2 - 22, z1, x2, z2 - 34, 20, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_QUARTZ, true);
        for (int z = z1; z <= z2 - 34; z += 4) fill(x2 - 22, 7, z, x2 - 22, 19, z, Blocks.SMOOTH_QUARTZ);
        int mz = (z1 + z2 - 34) / 2;
        fill(x2 - 22, 1, mz - 1, x2 - 22, 4, mz + 2, Blocks.SMOOTH_QUARTZ);
        doubleDoor(x2 - 22, 2, mz, E, Blocks.DARK_OAK_DOOR);
        post(x2 - 24, 1, mz - 3, rot(W), "Дом", "промышленности", "ул. Пушкина, 95", "");
        // equestrian monument to General Shaimuratov with a colonnade behind it
        int px = x1 + 30, pz = z1 + 34;
        fill(px - 2, 1, pz - 4, px + 2, 4, pz + 4, Blocks.POLISHED_GRANITE);
        horseman(px, 5, pz - 3, Blocks.WAXED_EXPOSED_COPPER, false, false);
        for (int i = -5; i <= 5; i++) {
            int cx = px + i * 3, cz = pz + 14 - Math.abs(i);
            fill(cx, 1, cz, cx, 7, cz, Blocks.QUARTZ_PILLAR);
            fill(cx - 1, 8, cz, cx + 1, 8, cz, Blocks.SMOOTH_QUARTZ);
        }
        post(px + 4, 1, pz - 5, rot(N), "Памятник генералу", "М. М. Шаймуратову", "Советская", "площадь");
        // flat fountain: jets straight from the paving
        for (int x = px - 8; x <= px + 8; x += 2)
            for (int z = z1 + 8; z <= z1 + 16; z += 2) {
                set(x, 0, z, Blocks.PRISMARINE);
                if ((x + z) % 4 == 0) set(x, 1, z, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
            }
        post(px + 10, 1, z1 + 12, rot(E), "Советская", "площадь", "плоскостной", "фонтан");
        for (int x = x1 + 4; x < x2 - 28; x += 12) {
            lamp(x, z1 + 2);
            bench(x + 3, z1 + 4, S);
        }
        // south part of the quarter: trees and an ordinary house
        city.house(x1 + 6, z2 - 22, x1 + 36, z2 - 10, S, false);
        for (int i = 0; i < 12; i++) tree(x1 + 40 + rnd.nextInt(45), z2 - 26 + rnd.nextInt(24), i % 2 == 0 ? Tree.LINDEN : Tree.SPRUCE);

        // Ministry of Agriculture (former Council of People's Commissars) across Pushkina
        body(482, 268, 548, 300, 15, Blocks.LIGHT_GRAY_CONCRETE, Blocks.SMOOTH_QUARTZ, false);
        body(506, 262, 524, 300, 25, Blocks.LIGHT_GRAY_CONCRETE, Blocks.SMOOTH_QUARTZ, false);
        fill(511, 1, 300, 518, 4, 300, Blocks.SMOOTH_QUARTZ);
        doubleDoor(516, 2, 300, N, Blocks.DARK_OAK_DOOR);
        sign(512, 3, 301, S, "Министерство", "сельского", "хозяйства РБ", "ул. Пушкина, 106");
        flag(510, 303, 9, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL, Blocks.GREEN_WOOL, true);
        tree(486, 304, Tree.SPRUCE);
        tree(544, 304, Tree.SPRUCE);
    }

    /**
     * An equestrian statue about 13 blocks tall standing on (x, y, z..z+11); the horse looks towards +z.
     * With {@code rearing} the front legs are lifted, with {@code whip} the rider raises his right hand.
     */
    private void horseman(int x, int y, int z, Block b, boolean rearing, boolean whip) {
        // hind legs
        for (int sx : new int[]{-1, 1}) {
            fill(x + sx, y, z + 1, x + sx, y + 3, z + 1, b);
            set(x + sx, y, z + 2, b);
        }
        // front legs
        if (rearing) {
            fill(x - 1, y + 2, z + 8, x - 1, y + 4, z + 8, b);
            set(x - 1, y + 2, z + 9, b);
            fill(x + 1, y + 4, z + 8, x + 1, y + 4, z + 10, b);
            set(x + 1, y + 3, z + 10, b);
        } else {
            fill(x - 1, y, z + 7, x - 1, y + 3, z + 7, b);
            fill(x + 1, y + 2, z + 7, x + 1, y + 3, z + 7, b);
            set(x + 1, y + 2, z + 8, b);
        }
        // body, tilted upwards at the front when rearing
        fill(x - 1, y + 4, z + 1, x + 1, y + 6, z + 4, b);
        int lift = rearing ? 1 : 0;
        fill(x - 1, y + 4 + lift, z + 5, x + 1, y + 6 + lift, z + 8, b);
        // tail
        fill(x, y + 3, z, x, y + 6, z, b);
        fill(x, y + 2, z - 1, x, y + 4, z - 1, b);
        // neck and head
        fill(x, y + 7 + lift, z + 8, x, y + 9 + lift, z + 8, b);
        fill(x, y + 8 + lift, z + 9, x, y + 10 + lift, z + 9, b);
        fill(x, y + 9 + lift, z + 10, x, y + 10 + lift, z + 11, b);
        set(x, y + 11 + lift, z + 9, b);
        // rider: legs, torso, shoulders, head, fur hat
        for (int sx : new int[]{-2, 2}) fill(x + sx, y + 4, z + 4, x + sx, y + 6, z + 5, b);
        fill(x, y + 7, z + 4, x, y + 10, z + 5, b);
        fill(x - 1, y + 9, z + 4, x + 1, y + 10, z + 5, b);
        set(x, y + 11, z + 5, b);
        fill(x - 1, y + 12, z + 4, x + 1, y + 12, z + 5, b);
        if (whip) {
            set(x + 2, y + 10, z + 4, b);
            set(x + 2, y + 11, z + 3, b);
            set(x + 2, y + 12, z + 3, b);
            fill(x + 2, y + 13, z + 2, x + 2, y + 13, z + 3, Blocks.IRON_BARS);
        } else {
            set(x + 2, y + 9, z + 5, b);
        }
    }

    // ------------------------------------------------------------------ the western bluff: congress hall, TV tower, university

    private void westBluff() {
        // Congress Hall "Toratau": glass and white ribs
        int x1 = 56, x2 = 120, z1 = ZV + RW + 6, z2 = z1 + 36, h = 18;
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        air(x1, 1, z1, x2, h + 4, z2);
        walls(x1, 1, z1, x2, h, z2, Blocks.LIGHT_BLUE_STAINED_GLASS);
        for (int x = x1; x <= x2; x += 4) {
            fill(x, 1, z1, x, h, z1, Blocks.WHITE_CONCRETE);
            fill(x, 1, z2, x, h, z2, Blocks.WHITE_CONCRETE);
        }
        for (int z = z1; z <= z2; z += 4) {
            fill(x1, 1, z, x1, h, z, Blocks.WHITE_CONCRETE);
            fill(x2, 1, z, x2, h, z, Blocks.WHITE_CONCRETE);
        }
        for (int y = 6; y <= h; y += 6) {
            fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, Blocks.SMOOTH_STONE);
            walls(x1, y, z1, x2, y, z2, Blocks.WHITE_CONCRETE);
            for (int x = x1 + 4; x < x2; x += 6)
                for (int z = z1 + 4; z < z2; z += 6)
                    set(x, y, z, Blocks.SEA_LANTERN);
        }
        // the wavy canopy over the entrance
        for (int x = x1 + 10; x <= x2 - 10; x++) {
            int wave = (int) Math.round(Math.sin((x - x1) / 5.0) * 1.5);
            fill(x, 6 + wave, z1 - 4, x, 6 + wave, z1 - 1, Blocks.WHITE_CONCRETE);
        }
        doubleDoor((x1 + x2) / 2, 1, z1, S, Blocks.IRON_DOOR);
        post((x1 + x2) / 2 + 3, 1, z1 - 2, rot(N), "Конгресс-холл", "«Торатау»", "ул. Заки Валиди,", "2");
        // TV tower of the Ufa television centre
        int tx = 138, tz = z1 + 14;
        for (int y = 1; y <= 64; y++) {
            int r = y < 20 ? 3 : y < 40 ? 2 : y < 54 ? 1 : 0;
            Block b = (y / 6) % 2 == 0 ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE;
            if (r == 0) set(tx, y, tz, b);
            else {
                for (int[] corner : new int[][]{{-r, -r}, {r, -r}, {-r, r}, {r, r}}) set(tx + corner[0], y, tz + corner[1], b);
                if (y % 6 == 0) walls(tx - r, y, tz - r, tx + r, y, tz + r, Blocks.IRON_BARS);
            }
        }
        fill(tx - 4, 1, tz - 4, tx + 4, 3, tz + 4, Blocks.LIGHT_GRAY_CONCRETE);
        post(tx, 1, tz - 6, rot(N), "Уфимский", "телецентр", "", "");
        // the university (former BashGU) and the Gafuri drama theatre further along the street
        body(60, z2 + 14, 122, z2 + 34, 20, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_QUARTZ, true);
        for (int x = 84; x <= 98; x += 3) fill(x, 2, z2 + 12, x, 14, z2 + 12, Blocks.QUARTZ_PILLAR);
        fill(83, 15, z2 + 11, 99, 16, z2 + 13, Blocks.SMOOTH_QUARTZ);
        fill(83, 1, z2 + 11, 99, 1, z2 + 13, Blocks.POLISHED_ANDESITE);
        doubleDoor(91, 2, z2 + 14, S, Blocks.DARK_OAK_DOOR);
        post(101, 1, z2 + 10, rot(N), "Уфимский ун-т", "науки и техн.", "(бывш. БашГУ)", "Заки Валиди, 32");
        for (int i = 0; i < 14; i++) tree(128 + rnd.nextInt(26), z1 + 30 + rnd.nextInt(50), i % 2 == 0 ? Tree.BIRCH : Tree.SPRUCE);
        for (int x = 50; x < 126; x += 10) tree(x, z1 - 3, Tree.SPRUCE);
    }

    // ------------------------------------------------------------------ Dynamo stadium

    private void stadium() {
        int x1 = 180, x2 = 248, z1 = ZV + RW + 8, z2 = TUK - 8;
        fill(x1, 0, z1, x2, 0, z2, Blocks.RED_TERRACOTTA);
        for (int x = x1 + 5; x <= x2 - 5; x++)
            for (int z = z1 + 5; z <= z2 - 5; z++)
                set(x, 0, z, ((z - z1) / 6) % 2 == 0 ? Blocks.GREEN_CONCRETE : Blocks.LIME_TERRACOTTA);
        walls(x1 + 8, 0, z1 + 8, x2 - 8, 0, z2 - 8, Blocks.WHITE_CONCRETE);
        int mz = (z1 + z2) / 2, mx = (x1 + x2) / 2;
        fill(x1 + 8, 0, mz, x2 - 8, 0, mz, Blocks.WHITE_CONCRETE);
        for (int a = 0; a < 40; a++) {
            double t = Math.PI * 2 * a / 40;
            set(mx + (int) Math.round(Math.cos(t) * 6), 0, mz + (int) Math.round(Math.sin(t) * 6), Blocks.WHITE_CONCRETE);
        }
        // goals
        for (int gz : new int[]{z1 + 8, z2 - 8}) {
            fill(mx - 3, 1, gz, mx - 3, 2, gz, Blocks.IRON_BARS);
            fill(mx + 3, 1, gz, mx + 3, 2, gz, Blocks.IRON_BARS);
            fill(mx - 3, 3, gz, mx + 3, 3, gz, Blocks.IRON_BARS);
        }
        // stands on the long sides
        for (int i = 0; i < 5; i++) {
            fill(x1 - 1 - i, 1, z1 + 4, x1 - 1 - i, 1 + i, z2 - 4, Blocks.SMOOTH_STONE);
            fill(x1 - 1 - i, 1 + i, z1 + 4, x1 - 1 - i, 1 + i, z2 - 4, stair(Blocks.STONE_BRICK_STAIRS, W));
            fill(x2 + 1 + i, 1, z1 + 4, x2 + 1 + i, 1 + i, z2 - 4, Blocks.SMOOTH_STONE);
            fill(x2 + 1 + i, 1 + i, z1 + 4, x2 + 1 + i, 1 + i, z2 - 4, stair(Blocks.STONE_BRICK_STAIRS, E));
        }
        for (int z : new int[]{z1, z2}) {
            fill(x1 - 3, 1, z, x1 - 3, 14, z, Blocks.IRON_BARS);
            fill(x1 - 4, 14, z, x1 - 2, 14, z, Blocks.SEA_LANTERN);
            fill(x2 + 3, 1, z, x2 + 3, 14, z, Blocks.IRON_BARS);
            fill(x2 + 2, 14, z, x2 + 4, 14, z, Blocks.SEA_LANTERN);
        }
        post(mx, 1, z1 - 3, rot(N), "Стадион", "«Динамо»", "ул. К. Маркса, 2", "с 1934 года");
    }

    // ------------------------------------------------------------------ Kurultai and Lenin park

    private void kurultaiAndPark() {
        // State Assembly (Kurultai) of Bashkortostan
        int x1 = 258, x2 = 312, z1 = ZV + RW + 8, z2 = z1 + 26;
        body(x1, z1, x2, z2, 20, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ, false);
        int mx = (x1 + x2) / 2;
        for (int x = mx - 9; x <= mx + 9; x += 3) fill(x, 1, z1 - 2, x, 12, z1 - 2, Blocks.QUARTZ_PILLAR);
        fill(mx - 10, 13, z1 - 3, mx + 10, 14, z1 - 1, Blocks.SMOOTH_QUARTZ);
        fill(mx - 10, 0, z1 - 4, mx + 10, 0, z1 - 1, Blocks.POLISHED_ANDESITE);
        fill(mx - 1, 1, z1, mx + 2, 3, z1, Blocks.SMOOTH_QUARTZ);
        doubleDoor(mx, 1, z1, S, Blocks.DARK_OAK_DOOR);
        post(mx + 12, 1, z1 - 3, rot(N), "Государственное", "Собрание —", "Курултай РБ", "Заки Валиди, 40");
        flag(mx - 13, z1 - 3, 11, Blocks.WHITE_WOOL, Blocks.BLUE_WOOL, Blocks.RED_WOOL, true);
        flag(mx + 15, z1 - 3, 11, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL, Blocks.GREEN_WOOL, true);

        // Lenin park (the old Ushakov park, laid out in 1867-1872)
        int px1 = 256, px2 = SOV - 3, pz1 = ZV + RW + 2, pz2 = TUK - 3;
        int cx = 384, cz = 506;
        city.path(LEN + RW, cz - 1, px2, cz + 1);
        city.path(cx - 1, pz1, cx + 1, pz2);
        city.path(px1 + 4, 530, LEN + 1, 531);
        for (int i = 0; i < 60; i++) {
            city.path(LEN + RW + i, pz1 + 6 + i * 4 / 5, LEN + RW + i + 1, pz1 + 6 + i * 4 / 5);
            city.path(px2 - i - 1, pz1 + 6 + i * 4 / 5, px2 - i, pz1 + 6 + i * 4 / 5);
        }
        // the fountain on the central crossing
        for (int ax = -6; ax <= 6; ax++)
            for (int az = -6; az <= 6; az++) {
                int d = ax * ax + az * az;
                if (d <= 38 && d > 22) set(cx + ax, 1, cz + az, Blocks.SMOOTH_QUARTZ);
                else if (d <= 22) set(cx + ax, 1, cz + az, Blocks.WATER);
                if (d <= 60) set(cx + ax, 0, cz + az, Blocks.STONE_BRICKS);
            }
        fill(cx, 1, cz, cx, 3, cz, Blocks.SMOOTH_QUARTZ);
        set(cx, 4, cz, Blocks.SEA_LANTERN);
        // memorial to those who fell in the Revolution and the Civil War
        int ox = 420, oz = 480;
        fill(ox - 2, 1, oz - 2, ox + 2, 1, oz + 2, Blocks.POLISHED_GRANITE);
        fill(ox, 2, oz, ox, 9, oz, Blocks.POLISHED_GRANITE);
        set(ox, 10, oz, Blocks.REDSTONE_BLOCK);
        post(ox, 1, oz - 3, rot(N), "Мемориал", "павшим в годы", "Гражданской", "войны");
        // gates on Zaki Validi
        fill(cx - 4, 1, pz1, cx - 3, 6, pz1, Blocks.SMOOTH_QUARTZ);
        fill(cx + 3, 1, pz1, cx + 4, 6, pz1, Blocks.SMOOTH_QUARTZ);
        fill(cx - 4, 7, pz1, cx + 4, 7, pz1, Blocks.SMOOTH_QUARTZ);
        sign(cx - 1, 6, pz1 - 1, N, "Парк имени", "В. И. Ленина", "(Ушаковский)", "1867");
        set(cx - 1, 6, pz1, Blocks.SMOOTH_QUARTZ);
        post(LEN + 7, 1, ZV + RW + 3, rot(N), "ул. Ленина:", "пешеходная", "зона, аллея", "ArtTerria");
        // trees everywhere except on the paths
        for (int i = 0; i < 230; i++) {
            int x = px1 + 2 + rnd.nextInt(px2 - px1 - 4), z = pz1 + 2 + rnd.nextInt(pz2 - pz1 - 4);
            if (x >= x1 - 4 && x <= x2 + 4 && z <= z2 + 4) continue;
            if (Math.abs(x - cx) < 10 && Math.abs(z - cz) < 10) continue;
            if (x >= LEN - 1 && x <= LEN + RW) continue;
            if (Math.abs(x - cx) < 3 || Math.abs(z - cz) < 3 || Math.abs(x - ox) < 4 && Math.abs(z - oz) < 4) continue;
            if (!c.isEmpty(x, 1, z)) continue;
            tree(x, z, i % 5 == 0 ? Tree.BIRCH : i % 3 == 0 ? Tree.LINDEN : Tree.OAK);
        }
        for (int z = pz1 + 10; z < pz2; z += 14) {
            lamp(cx - 3, z);
            bench(cx + 3, z + 4, W);
            lamp(LEN + 1, z);
            lamp(LEN + 13, z + 7);
            bench(LEN + 2, z + 4, E);
        }
        // modern sculptures of the ArtTerria alley
        Block[] art = {Blocks.RED_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE};
        for (int i = 0; i < 4; i++) {
            int z = ZV + RW + 16 + i * 22;
            fill(LEN + 12, 1, z, LEN + 12, 2 + i % 2, z, art[i]);
            set(LEN + 12 + (i % 2 == 0 ? -1 : 0), 3 + i % 2, z, art[(i + 1) % 4]);
        }
        npc(cx + 5, 1, cz + 9, 200, NpcRole.CITIZEN, "Бабушка с семечками", 1, false);
        npc(LEN + 6, 1, 500, 0, NpcRole.CITIZEN, "Художник", 3, true);
    }

    // ------------------------------------------------------------------ Salavat Yulaev

    private void salavat() {
        int cx = 70, cz = 616;
        // observation platform above the Belaya
        for (int x = 36; x <= 104; x++)
            for (int z = TUK + RW + 4; z <= RIVER_Z - 1; z++) {
                int d = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                if (d <= 26 * 26 && c.get(x, 0, z) == null)
                    set(x, 0, z, d < 10 * 10 ? Blocks.POLISHED_GRANITE : ((x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.POLISHED_ANDESITE));
            }
        // path from Tukaeva / Karla Marksa
        for (int x = 70; x <= KM + 2; x++) fill(x, 0, TUK + RW, x, 0, TUK + RW + 3, Blocks.STONE_BRICKS);
        fill(cx - 2, 0, TUK + RW, cx + 2, 0, cz - 20, Blocks.STONE_BRICKS);
        // granite pedestal and the 9.8 m horseman facing the river
        fill(cx - 3, 1, cz - 6, cx + 3, 1, cz + 7, Blocks.POLISHED_GRANITE);
        fill(cx - 2, 2, cz - 5, cx + 2, 5, cz + 6, Blocks.POLISHED_GRANITE);
        horseman(cx, 6, cz - 5, Blocks.POLISHED_BLACKSTONE, true, true);
        post(cx + 5, 1, cz - 8, rot(N), "Памятник", "Салавату Юлаеву", "1967 г.", "ск. С. Тавасиев");
        post(cx - 5, 1, cz - 8, rot(N), "Высота 9,8 м,", "масса 40 тонн,", "всего три", "точки опоры");
        // railing at the cliff edge, lamps, benches
        for (int x = 36; x <= 104; x++)
            if (c.isEmpty(x, 1, RIVER_Z - 1) && c.get(x, 0, RIVER_Z - 1) != null) set(x, 1, RIVER_Z - 1, Blocks.IRON_BARS);
        for (int a = 0; a < 8; a++) {
            double t = Math.PI * 2 * a / 8 + 0.4;
            int lx = cx + (int) Math.round(Math.cos(t) * 20), lz = cz + (int) Math.round(Math.sin(t) * 20);
            if (lz < RIVER_Z - 2) lamp(lx, lz);
        }
        bench(cx - 12, cz + 18, S);
        bench(cx + 11, cz + 18, S);
        for (int i = 0; i < 16; i++) {
            int x = 34 + rnd.nextInt(74), z = TUK + RW + 4 + rnd.nextInt(60);
            int d = (x - cx) * (x - cx) + (z - cz) * (z - cz);
            if (d > 28 * 28 && Math.abs(x - cx) > 4) tree(x, z, Tree.BIRCH);
        }
        npc(cx - 9, 1, cz + 10, 180, NpcRole.CITIZEN, "Турист", 2, true);
    }

    // ------------------------------------------------------------------ House of the Republic

    private void houseOfRepublic() {
        int x1 = 314, x2 = 406, z1 = 596, z2 = 630, h = 30;
        Block white = Blocks.WHITE_CONCRETE;
        city.plaza(x1 - 8, TUK + RW + 2, x2 + 8, z1 - 1);
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        air(x1, 1, z1, x2, h + 4, z2);
        walls(x1, 1, z1, x2, h, z2, white);
        fill(x1, h, z1, x2, h, z2, Blocks.SMOOTH_STONE);
        walls(x1, h, z1, x2, h + 1, z2, Blocks.SMOOTH_QUARTZ);
        for (int y = 5; y < h; y += 5) {
            fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, Blocks.SMOOTH_STONE);
            for (int x = x1 + 4; x < x2; x += 7)
                for (int z = z1 + 4; z < z2; z += 7)
                    set(x, y, z, Blocks.SEA_LANTERN);
        }
        // vertical ribs with dark glazing between them
        for (int x = x1 + 1; x < x2; x++)
            if ((x - x1) % 3 != 0) {
                fill(x, 6, z1, x, h - 2, z1, Blocks.GRAY_STAINED_GLASS);
                fill(x, 6, z2, x, h - 2, z2, Blocks.GRAY_STAINED_GLASS);
                fill(x, 2, z1, x, 4, z1, Blocks.GLASS_PANE);
            }
        for (int z = z1 + 1; z < z2; z++)
            if ((z - z1) % 3 != 0) {
                fill(x1, 6, z, x1, h - 2, z, Blocks.GRAY_STAINED_GLASS);
                fill(x2, 6, z, x2, h - 2, z, Blocks.GRAY_STAINED_GLASS);
            }
        int mx = (x1 + x2) / 2;
        fill(mx - 8, 1, z1 - 3, mx + 8, 1, z1 - 1, Blocks.POLISHED_GRANITE);
        fill(mx - 8, 5, z1 - 3, mx + 8, 5, z1 - 1, white);
        for (int x = mx - 8; x <= mx + 8; x += 4) fill(x, 2, z1 - 3, x, 4, z1 - 3, white);
        fill(mx - 2, 1, z1, mx + 3, 4, z1, Blocks.SMOOTH_QUARTZ);
        doubleDoor(mx, 2, z1, S, Blocks.IRON_DOOR);
        doubleDoor(mx + 2, 2, z1, S, Blocks.IRON_DOOR);
        post(mx + 10, 1, z1 - 4, rot(N), "Дом Республики", "ул. Тукаева, 46", "", "");
        flag(mx - 14, z1 - 6, 13, Blocks.WHITE_WOOL, Blocks.BLUE_WOOL, Blocks.RED_WOOL, true);
        flag(mx + 12, z1 - 6, 13, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL, Blocks.GREEN_WOOL, true);
        for (int x = x1 - 4; x <= x2 + 4; x += 8) if (Math.abs(x - mx) > 16) tree(x, z1 - 5, Tree.SPRUCE);
        for (int x = x1; x <= x2; x += 9) tree(x, z2 + 6, Tree.BIRCH);
    }

    // ------------------------------------------------------------------ the First Cathedral Mosque (1830)

    private void mosque() {
        int x1 = 486, x2 = 506, z1 = 598, z2 = 622, h = 8;
        Block wall = Blocks.SMOOTH_QUARTZ, green = Blocks.DARK_PRISMARINE;
        fill(x1, 0, z1, x2, 0, z2, Blocks.STONE_BRICKS);
        air(x1, 1, z1, x2, 30, z2);
        walls(x1, 1, z1, x2, h, z2, wall);
        fill(x1 + 1, 0, z1 + 1, x2 - 1, 0, z2 - 1, Blocks.GREEN_TERRACOTTA);
        for (int x = x1 + 3; x <= x2 - 3; x += 4) {
            fill(x, 3, z1, x, 6, z1, Blocks.GLASS_PANE);
            fill(x, 3, z2, x, 6, z2, Blocks.GLASS_PANE);
        }
        for (int z = z1 + 3; z <= z2 - 3; z += 4) {
            fill(x1, 3, z, x1, 6, z, Blocks.GLASS_PANE);
            fill(x2, 3, z, x2, 6, z, Blocks.GLASS_PANE);
        }
        hipRoof(x1, z1, x2, z2, h + 1, Blocks.DARK_PRISMARINE_STAIRS, green);
        // dome over the prayer hall
        int mx = (x1 + x2) / 2, mz = z1 + 14;
        for (int i = 0; i < 5; i++) {
            int r = 5 - i;
            for (int ax = -r; ax <= r; ax++)
                for (int az = -r; az <= r; az++)
                    if (ax * ax + az * az <= r * r + 1) set(mx + ax, h + 4 + i, mz + az, green);
        }
        set(mx, h + 9, mz, Blocks.GOLD_BLOCK);
        // minaret at the northern entrance
        fill(mx - 1, 1, z1 - 3, mx + 1, 22, z1 - 1, wall);
        air(mx, 1, z1 - 3, mx, 2, z1 - 1);
        fill(mx - 2, 17, z1 - 4, mx + 2, 17, z1, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        walls(mx - 2, 18, z1 - 4, mx + 2, 18, z1, Blocks.IRON_BARS);
        for (int i = 0; i < 4; i++) fill(mx - (i < 2 ? 1 : 0), 23 + i, z1 - 2 - (i < 2 ? 1 : 0), mx + (i < 2 ? 1 : 0), 23 + i, z1 - 2 + (i < 2 ? 1 : 0), green);
        set(mx, 27, z1 - 2, Blocks.GOLD_BLOCK);
        air(mx, 1, z1, mx, 2, z1);
        hangingLamp(mx, 7, mz);
        set(mx, 8, mz, wall);
        post(mx + 3, 1, z1 - 4, rot(N), "Первая соборная", "мечеть", "ул. Тукаева, 52", "1830 год");
        for (int i = 0; i < 8; i++) tree(472 + rnd.nextInt(10) + (i % 2) * 44, 584 + rnd.nextInt(50), Tree.LINDEN);
        for (int z = TUK + RW; z < z1 - 3; z++) fill(mx - 1, 0, z, mx + 1, 0, z, Blocks.STONE_BRICKS);
    }

    // ------------------------------------------------------------------ Friendship monument

    private void friendship() {
        int cx = 598, cz = 612;
        // Trinity hill: a round terrace
        for (int ax = -18; ax <= 18; ax++)
            for (int az = -18; az <= 18; az++) {
                int d = ax * ax + az * az;
                if (d > 18 * 18) continue;
                int x = cx + ax, z = cz + az;
                if (z >= RIVER_Z - 16) fill(x, -4, z, x, 0, z, Blocks.STONE_BRICKS);
                set(x, 0, z, d <= 7 * 7 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                if (d > 16 * 16 && z > cz + 4 && Math.abs(ax) > 3) set(x, 1, z, Blocks.IRON_BARS);
            }
        // round base of blue granite with bronze reliefs
        for (int ax = -6; ax <= 6; ax++)
            for (int az = -6; az <= 6; az++)
                if (ax * ax + az * az <= 38) {
                    fill(cx + ax, 1, cz + az, cx + ax, 3, cz + az, Blocks.POLISHED_ANDESITE);
                    if (ax * ax + az * az > 26) set(cx + ax, 2, cz + az, Blocks.WAXED_EXPOSED_CUT_COPPER);
                }
        // two 30-metre steles of pink granite: a sword driven into the ground
        for (int sx : new int[]{-1, 1}) {
            fill(cx + sx, 4, cz - 2, cx + sx, 22, cz + 2, Blocks.POLISHED_GRANITE);
            fill(cx + sx, 23, cz - 1, cx + sx, 30, cz + 1, Blocks.POLISHED_GRANITE);
            fill(cx + sx, 31, cz, cx + sx, 34, cz, Blocks.POLISHED_GRANITE);
        }
        // the two seated bronze women: Russia and Bashkiria
        for (int sx : new int[]{-5, 5}) {
            fill(cx + sx, 4, cz - 1, cx + sx, 4, cz + 1, Blocks.POLISHED_GRANITE);
            fill(cx + sx, 5, cz, cx + sx, 7, cz, Blocks.WAXED_EXPOSED_COPPER);
            set(cx + sx, 5, cz + 1, Blocks.WAXED_EXPOSED_COPPER);
            set(cx + sx + (sx < 0 ? 1 : -1), 6, cz, Blocks.WAXED_EXPOSED_COPPER);
        }
        post(cx, 1, cz - 9, rot(N), "Монумент Дружбы", "1557 — 1957", "400 лет вместе", "открыт в 1965 г.");
        post(cx + 4, 1, cz - 9, rot(N), "Слава великой", "братской дружбе", "русского и башк.", "народов");
        // the granite staircase down to the Belaya
        for (int i = 0; i < 4; i++)
            fill(cx - 3, -i, cz + 19 + i * 3, cx + 3, -i, cz + 21 + i * 3, i == 0 ? Blocks.POLISHED_GRANITE.defaultBlockState() : stair(Blocks.POLISHED_GRANITE_STAIRS, N));
        for (int z = TUK + RW; z < cz - 18; z++) fill(cx - 2, 0, z, cx + 2, 0, z, Blocks.STONE_BRICKS);
        lamp(cx - 12, cz - 10);
        lamp(cx + 12, cz - 10);
        bench(cx - 10, cz + 12, S);
        bench(cx + 9, cz + 12, S);
    }
}
