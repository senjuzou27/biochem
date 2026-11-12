package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.entity.NpcRole;

import static ru.ufamod.world.Plan.*;

/**
 * The main building of the Bashkir State Medical University (ул. Ленина, 3), with a full interior:
 * four storeys, a central hall behind the portico, a rear staircase and a corridor with rooms on both sides.
 */
public class BsmuBuilder extends Build {
    private static final int X1 = BS_X1, X2 = BS_X2, Z1 = BS_Z1, Z2 = BS_Z2;
    /** Corridor walls and the corridor itself. */
    private static final int CW = 301, CE = 305, CX1 = 302, CX2 = 304;
    /** Walls of the central hall. */
    private static final int HN = 378, HS = 395;
    private static final int STOREYS = 4, ROOF = 1 + STOREYS * BS_FLOOR_H;
    /** Stairwell interior: x 285..291, z 384..388. */
    private static final int SX1 = 285, SX2 = 291, SZ1 = 384, SZ2 = 388;

    private static final Block WALL = Blocks.SMOOTH_SANDSTONE, TRIM = Blocks.SMOOTH_QUARTZ, INNER = Blocks.WHITE_TERRACOTTA;

    public BsmuBuilder(Canvas c) {
        super(c, 3L);
    }

    private static int f(int storey) {
        return bsFloor(storey);
    }

    public void build() {
        shell();
        stairwell();
        for (int s = 1; s <= STOREYS; s++) storeyLayout(s);
        facade();
        portico();
        hall();
        canteen();
        lectureHall();
        library();
        anatomy();
        otherRooms();
        surroundings();
    }

    // ------------------------------------------------------------------ structure

    private void shell() {
        fill(X1, 0, Z1, X2, 0, Z2, Blocks.STONE_BRICKS);
        air(X1, 1, Z1, X2, ROOF + 6, Z2);
        walls(X1, 1, Z1, X2, ROOF, Z2, WALL);
        for (int s = 1; s <= STOREYS; s++)
            fill(X1 + 1, f(s), Z1 + 1, X2 - 1, f(s), Z2 - 1, s == 1 ? Blocks.POLISHED_ANDESITE : Blocks.SPRUCE_PLANKS);
        fill(X1, ROOF, Z1, X2, ROOF, Z2, Blocks.SMOOTH_STONE);
        walls(X1, ROOF + 1, Z1, X2, ROOF + 1, Z2, slab(Blocks.SMOOTH_QUARTZ_SLAB));
        fill(X1 + 1, ROOF + 1, Z1 + 1, X2 - 1, ROOF + 1, Z2 - 1, slab(Blocks.DEEPSLATE_TILE_SLAB));
    }

    private void storeyLayout(int s) {
        int y1 = f(s) + 1, y2 = f(s) + 4;
        // corridor walls north and south of the hall
        fill(CW, y1, Z1 + 1, CW, y2, HN, INNER);
        fill(CE, y1, Z1 + 1, CE, y2, HN, INNER);
        fill(CW, y1, HS, CW, y2, Z2 - 1, INNER);
        fill(CE, y1, HS, CE, y2, Z2 - 1, INNER);
        // hall walls with the corridor opening
        fill(X1 + 1, y1, HN, X2 - 1, y2, HN, INNER);
        fill(X1 + 1, y1, HS, X2 - 1, y2, HS, INNER);
        air(CX1, y1, HN, CX2, y1 + 2, HN);
        air(CX1, y1, HS, CX2, y1 + 2, HS);
        // corridor floor runner and lights
        fill(CX1 + 1, f(s), Z1 + 1, CX1 + 1, f(s), Z2 - 1, s == 1 ? Blocks.POLISHED_DIORITE : Blocks.RED_TERRACOTTA);
        for (int z = Z1 + 4; z < Z2; z += 6) hangingLamp(CX1 + 1, y2, z);
        // room partitions
        for (int z : new int[]{350, 362, 406, 418}) {
            fill(X1 + 1, y1, z, CW - 1, y2, z, INNER);
            fill(CE + 1, y1, z, X2 - 1, y2, z, INNER);
        }
    }

    /** Rooms as {z1, z2} interior ranges, north to south. */
    private static final int[][] ROOMS = {{339, 349}, {351, 361}, {363, 377}, {396, 405}, {407, 417}, {419, 433}};

    private void stairwell() {
        Block st = Blocks.STONE_BRICKS;
        fill(SX1 - 1, 0, SZ1 - 1, X1 - 1, 0, SZ2 + 1, Blocks.STONE_BRICKS);
        air(SX1 - 1, 1, SZ1 - 1, X1 - 1, ROOF + 2, SZ2 + 1);
        fill(SX1 - 1, 1, SZ1 - 1, X1 - 1, ROOF, SZ1 - 1, WALL);
        fill(SX1 - 1, 1, SZ2 + 1, X1 - 1, ROOF, SZ2 + 1, WALL);
        fill(SX1 - 1, 1, SZ1 - 1, SX1 - 1, ROOF, SZ2 + 1, WALL);
        fill(SX1 - 1, ROOF, SZ1 - 1, X1 - 1, ROOF, SZ2 + 1, Blocks.SMOOTH_STONE);
        fill(SX1, 1, SZ1, SX2, 1, SZ2, Blocks.SMOOTH_STONE);
        for (int s = 1; s <= STOREYS; s++) {
            int F = f(s);
            // opening from the hall
            air(X1, F + 1, SZ1, X1, F + 3, SZ2);
            // arrival strip (two blocks next to the hall)
            fill(SX2 - 1, F, SZ1, SX2, F, SZ2, Blocks.SMOOTH_STONE);
            // tall windows in the rear wall
            fill(SX1 - 1, F + 2, SZ1 + 1, SX1 - 1, F + 4, SZ2 - 1, Blocks.GLASS_PANE);
            hangingLamp(SX2, F + 4, SZ1 + 2);
            if (s == STOREYS) {
                // railing around the open well on the top storey
                fill(SX2 - 2, F + 1, SZ1, SX2 - 2, F + 1, SZ1 + 2, Blocks.IRON_BARS);
                fill(SX1 + 2, F + 1, SZ1 + 2, SX1 + 4, F + 1, SZ1 + 2, Blocks.IRON_BARS);
                continue;
            }
            // flight A: west-bound, three steps along the northern side
            for (int i = 0; i < 3; i++) {
                int x = SX2 - 2 - i;
                fill(x, F + 1 + i, SZ1, x, F + 1 + i, SZ1 + 1, stair(Blocks.STONE_BRICK_STAIRS, W));
                if (i > 0) fill(x, F + 1, SZ1, x, F + i, SZ1 + 1, st);
            }
            // half landing at the rear wall
            fill(SX1, F + 3, SZ1, SX1 + 1, F + 3, SZ2, Blocks.SMOOTH_STONE);
            // flight B: east-bound, two steps along the southern side, then the upper floor
            fill(SX1 + 2, F + 4, SZ2 - 1, SX1 + 2, F + 4, SZ2, stair(Blocks.STONE_BRICK_STAIRS, E));
            fill(SX1 + 3, F + 5, SZ2 - 1, SX1 + 3, F + 5, SZ2, stair(Blocks.STONE_BRICK_STAIRS, E));
            fill(SX1 + 4, F + 5, SZ2 - 1, SX1 + 4, F + 5, SZ2, Blocks.SMOOTH_STONE);
            // spine wall between the flights
            fill(SX1 + 2, F + 1, SZ1 + 2, SX1 + 4, F + 5, SZ1 + 2, st);
        }
        sign(SX2, f(1) + 3, SZ1, S, "Лестница", "2 эт. — лекции", "3 эт. — анатомия", "4 эт. — биохимия");
    }

    private void facade() {
        // plinth, string courses and cornice
        walls(X1, 1, Z1, X2, 1, Z2, Blocks.POLISHED_ANDESITE);
        walls(X1, f(2), Z1, X2, f(2), Z2, TRIM);
        walls(X1, ROOF, Z1, X2, ROOF, Z2, TRIM);
        for (int s = 1; s <= STOREYS; s++) {
            int F = f(s);
            for (int z = Z1 + 2; z <= Z2 - 2; z++) {
                int m = (z - Z1) % 4;
                boolean partition = z == HN || z == HS || z == 350 || z == 362 || z == 406 || z == 418;
                if ((m == 2 || m == 3) && !partition) {
                    fill(X2, F + 2, z, X2, F + 4, z, Blocks.GLASS_PANE);
                    if (z < SZ1 - 1 || z > SZ2 + 1) fill(X1, F + 2, z, X1, F + 4, z, Blocks.GLASS_PANE);
                } else if (m == 0 && s >= 2) {
                    // flat pilasters between the window pairs of the upper storeys
                    fill(X2, F + 1, z, X2, F + 4, z, TRIM);
                }
            }
            for (int x = X1 + 3; x <= X2 - 3; x++)
                if ((x - X1) % 4 >= 2) {
                    fill(x, F + 2, Z1, x, F + 4, Z1, Blocks.GLASS_PANE);
                    fill(x, F + 2, Z2, x, F + 4, Z2, Blocks.GLASS_PANE);
                }
        }
        // rusticated ground storey
        for (int z = Z1; z <= Z2; z += 4) fill(X2, 2, z, X2, f(2) - 1, z, Blocks.CUT_SANDSTONE);
    }

    private void portico() {
        int pz1 = HN, pz2 = HS;
        // platform and steps down to the Lenina sidewalk
        fill(X2 + 1, 1, pz1, X2 + 4, 1, pz2, Blocks.POLISHED_ANDESITE);
        fill(X2 + 5, 1, pz1, X2 + 5, 1, pz2, stair(Blocks.POLISHED_ANDESITE_STAIRS, W));
        fill(X2 + 1, 0, pz1, X2 + 5, 0, pz2, Blocks.STONE_BRICKS);
        // wall behind the columns is plain and white
        fill(X2, 2, pz1, X2, ROOF - 1, pz2, TRIM);
        for (int s = 2; s <= STOREYS; s++)
            for (int z = pz1 + 2; z <= pz2 - 2; z += 3)
                fill(X2, f(s) + 2, z, X2, f(s) + 4, z + 1, Blocks.GLASS_PANE);
        // main entrance: a double door in the middle
        fill(X2, 2, BS_MID_Z - 1, X2, 4, BS_MID_Z + 2, Blocks.SMOOTH_QUARTZ);
        air(X2, 2, BS_MID_Z, X2, 3, BS_MID_Z + 1);
        doubleDoor(X2, 2, BS_MID_Z + 1, W, Blocks.DARK_OAK_DOOR);
        fill(X2, 4, BS_MID_Z, X2, 4, BS_MID_Z + 1, Blocks.GLASS);
        // six columns through three storeys
        int top = f(4) + 1;
        for (int z : new int[]{pz1, pz1 + 3, pz1 + 6, pz2 - 6, pz2 - 3, pz2}) {
            fill(X2 + 3, 2, z, X2 + 3, top - 1, z, Blocks.QUARTZ_PILLAR);
            set(X2 + 3, 2, z, Blocks.CHISELED_QUARTZ_BLOCK);
            set(X2 + 3, top - 1, z, Blocks.CHISELED_QUARTZ_BLOCK);
        }
        // entablature and pediment
        fill(X2 + 1, top, pz1 - 1, X2 + 4, top + 1, pz2 + 1, TRIM);
        fill(X2 + 4, top, pz1 - 1, X2 + 4, top, pz2 + 1, Blocks.QUARTZ_BRICKS);
        for (int i = 0; i < 5; i++) {
            int za = pz1 + i * 2, zb = pz2 - i * 2;
            if (za > zb) break;
            fill(X2 + 1, top + 2 + i, za, X2 + 4, top + 2 + i, zb, TRIM);
            set(X2 + 4, top + 2 + i, za, stair(Blocks.QUARTZ_STAIRS, S));
            set(X2 + 4, top + 2 + i, zb, stair(Blocks.QUARTZ_STAIRS, N));
        }
        set(X2 + 4, top + 3, BS_MID_Z, Blocks.GOLD_BLOCK);
        set(X2 + 4, top + 3, BS_MID_Z + 1, Blocks.GOLD_BLOCK);
        // name plates beside the door
        sign(X2 + 1, 3, BS_MID_Z - 2, E, "Башкирский", "государственный", "медицинский", "университет");
        sign(X2 + 1, 3, BS_MID_Z + 3, E, "БГМУ", "Главный корпус", "ул. Ленина, 3", "осн. в 1932 г.");
        hangingLamp(X2 + 2, top - 1, BS_MID_Z);
        hangingLamp(X2 + 2, top - 1, BS_MID_Z + 1);
    }

    // ------------------------------------------------------------------ rooms

    private void hall() {
        int F = f(1);
        // chequered floor
        for (int x = X1 + 1; x < X2; x++)
            for (int z = HN + 1; z < HS; z++)
                set(x, F, z, (x + z) % 2 == 0 ? Blocks.POLISHED_DIORITE : Blocks.POLISHED_ANDESITE);
        // turnstiles between the entrance and the rest of the hall
        fill(308, F + 1, HN + 1, 308, F + 1, HS - 1, Blocks.IRON_BARS);
        air(308, F + 1, BS_MID_Z - 1, 308, F + 1, BS_MID_Z + 2);
        // security desk
        fill(309, F + 1, 380, 311, F + 1, 380, Blocks.SPRUCE_PLANKS);
        fill(309, F + 1, 381, 309, F + 1, 382, Blocks.SPRUCE_PLANKS);
        npc(310, F + 1, 382, yawOf(S), NpcRole.WATCHMAN, "Охранник Ринат", 0, false);
        post(307, F + 1, 383, rot(W), "Пропускной", "режим!", "Вход только", "в халате");
        // cloakroom in the south-west corner
        fill(X1 + 1, F + 1, 390, 299, F + 1, 390, slabTop(Blocks.SPRUCE_SLAB));
        Block[] coats = {Blocks.BLACK_WOOL, Blocks.GRAY_WOOL, Blocks.BROWN_WOOL, Blocks.BLUE_WOOL, Blocks.RED_WOOL, Blocks.GREEN_WOOL};
        for (int x = X1 + 1; x <= 299; x += 2)
            for (int z = 392; z <= 393; z++) {
                set(x, F + 1, z, Blocks.SPRUCE_FENCE);
                set(x, F + 2, z, coats[rnd.nextInt(coats.length)]);
            }
        post(300, F + 1, 391, rot(E), "", "Гардероб", "", "");
        // notice boards
        sign(X1 + 1, F + 2, 381, E, "ЭКЗАМЕН", "Анатомия", "3 этаж, ауд. 301", "сегодня");
        sign(X1 + 1, F + 2, 382, E, "Лекция", "Биохимия", "2 этаж, ауд. №1", "(налево)");
        sign(CX1 - 1, F + 2, HN + 1, S, "← Столовая", "", "Приёмная", "комиссия →");
        sign(CX2 + 1, F + 2, HS - 1, N, "Гардероб →", "", "Медпункт", "←");
        for (int x = X1 + 4; x < X2; x += 5)
            for (int z = HN + 3; z < HS; z += 5)
                hangingLamp(x, F + 4, z);
        // upper halls: benches and plants
        for (int s = 2; s <= STOREYS; s++) {
            int G = f(s);
            for (int z = HN + 2; z < HS - 2; z += 4) {
                set(X2 - 1, G + 1, z, stair(Blocks.SPRUCE_STAIRS, E));
                set(X2 - 1, G + 1, z + 1, stair(Blocks.SPRUCE_STAIRS, E));
            }
            set(X1 + 1, G + 1, HN + 1, Blocks.POTTED_FERN);
            set(X1 + 1, G + 1, HS - 1, Blocks.POTTED_FERN);
            for (int x = X1 + 4; x < X2; x += 5) hangingLamp(x, G + 4, BS_MID_Z);
        }
        sign(X1 + 1, f(2) + 2, 381, E, "2 этаж", "← Лекц. ауд. №1", "Библиотека →", "Деканат стомат.");
        sign(X1 + 1, f(3) + 2, 381, E, "3 этаж", "Кафедра", "анатомии человека", "ауд. 301 →");
        sign(X1 + 1, f(4) + 2, 381, E, "4 этаж", "Кафедра", "биологической", "химии");
    }

    private void canteen() {
        int F = f(1);
        // merge the two north-eastern rooms
        air(CE + 1, F + 1, 350, X2 - 1, F + 4, 350);
        door(CE, F + 1, 356, W, Blocks.SPRUCE_DOOR, false);
        sign(CE - 1, F + 3, 356, W, "", "Столовая", "", "");
        // serving counter and kitchen at the north end
        fill(CE + 1, F + 1, 343, X2 - 1, F + 1, 343, Blocks.SMOOTH_QUARTZ);
        set(X2 - 1, F + 1, 343, AIR);
        set(CE + 2, F + 1, 339, Blocks.SMOKER);
        set(CE + 3, F + 1, 339, Blocks.FURNACE);
        set(CE + 4, F + 1, 339, Blocks.CAULDRON);
        set(CE + 5, F + 1, 339, Blocks.BARREL);
        chest(CE + 2, F + 2, 343, S, "canteen");
        npc(309, F + 1, 341, yawOf(S), NpcRole.COOK, "Повар Зульфия-апа", 0, false);
        post(CE + 5, F + 2, 343, rot(S), "Меню", "Эчпочмак", "Чак-чак", "Кумыс");
        // tables
        for (int z = 347; z <= 359; z += 4) {
            for (int x : new int[]{CE + 2, CE + 6}) {
                set(x, F + 1, z, Blocks.SPRUCE_FENCE);
                set(x, F + 2, z, Blocks.WHITE_CARPET);
                set(x - 1, F + 1, z, stair(Blocks.SPRUCE_STAIRS, W));
                set(x + 1, F + 1, z, stair(Blocks.SPRUCE_STAIRS, E));
            }
        }
        hangingLamp(CE + 4, F + 4, 346);
        hangingLamp(CE + 4, F + 4, 354);
        npc(CE + 5, F + 1, 349, 90, NpcRole.STUDENT, "Студент Айдар", 1, false);
    }

    private void lectureHall() {
        int F = f(2), G = f(3);
        int za = Z1 + 1, zb = 361;
        // one double-height room across the whole width
        air(X1 + 1, F + 1, za, X2 - 1, G + 4, zb);
        fill(X1 + 1, F + 1, 362, X2 - 1, G + 4, 362, INNER);
        air(CX1, F + 1, 362, CX1 + 1, F + 2, 362);
        doubleDoor(CX1, F + 1, 362, N, Blocks.SPRUCE_DOOR);
        sign(CX2, F + 2, 363, S, "Лекционная", "аудитория №1", "", "Биохимия");
        // on the third storey the corridor simply ends here
        // tiers rising towards the north
        for (int z = za; z <= 357; z++) {
            int t = Math.min(4, (357 - z) / 4);
            if (t > 0) fill(X1 + 1, F + 1, z, X2 - 1, F + t, z, Blocks.SPRUCE_PLANKS);
            boolean stepRow = t > 0 && (357 - z) % 4 == 0;
            if (stepRow) fill(CX1, F + t, z, CX2, F + t, z, stair(Blocks.SPRUCE_STAIRS, N));
            int local = (357 - z) % 4;
            for (int x = X1 + 2; x <= X2 - 2; x++) {
                if (x >= CX1 && x <= CX2) continue;
                if (local == 3 || local == 1) set(x, F + t + 1, z, stair(Blocks.DARK_OAK_STAIRS, N));
                else if (z > za) set(x, F + t + 1, z, slabTop(Blocks.DARK_OAK_SLAB));
            }
        }
        // blackboards on the south wall, both sides of the door
        for (int y = F + 2; y <= F + 4; y++) {
            for (int x = X1 + 2; x <= CX1 - 2; x++) set(x, y, zb, trapdoor(Blocks.DARK_OAK_TRAPDOOR, N, false, true));
            for (int x = CX2 + 2; x <= X2 - 2; x++) set(x, y, zb, trapdoor(Blocks.DARK_OAK_TRAPDOOR, N, false, true));
        }
        // lecturer's desk
        fill(297, F + 1, 359, 299, F + 1, 359, Blocks.DARK_OAK_PLANKS);
        set(296, F + 1, 359, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, S));
        npc(298, F + 1, 358, yawOf(N), NpcRole.LECTURER, "Доцент Хабибуллина", 0, false);
        seated(296, 354, "Студентка Лейсан", 0);
        seated(310, 350, "Студент Данис", 1);
        seated(299, 346, "Студентка Камила", 0);
        for (int x = X1 + 4; x < X2; x += 5)
            for (int z = za + 3; z < zb; z += 6)
                hangingLamp(x, G + 4, z);
    }

    /** A student standing in the place of a seat of the lecture hall. */
    private void seated(int x, int z, String name, int variant) {
        int y = f(2) + Math.min(4, (357 - z) / 4) + 1;
        set(x, y, z, AIR);
        npc(x, y, z, yawOf(S), NpcRole.STUDENT, name, variant, false);
    }

    private void library() {
        int F = f(2);
        int za = 407, zb = Z2 - 1;
        air(X1 + 1, F + 1, za, X2 - 1, F + 4, zb);
        fill(X1 + 1, F + 1, 406, X2 - 1, F + 4, 406, INNER);
        air(CX1, F + 1, 406, CX1 + 1, F + 2, 406);
        doubleDoor(CX1 + 1, F + 1, 406, S, Blocks.SPRUCE_DOOR);
        sign(CX2, F + 2, 405, N, "Библиотека", "Читальный зал", "", "Тишина!");
        // shelves along the walls and in rows at the far end
        fill(X1 + 1, F + 1, za + 2, X1 + 1, F + 3, zb - 1, Blocks.BOOKSHELF);
        for (int z = 423; z <= 431; z += 4) {
            fill(X1 + 3, F + 1, z, CX1 - 1, F + 3, z, Blocks.BOOKSHELF);
            fill(CX2 + 1, F + 1, z, X2 - 3, F + 3, z, Blocks.BOOKSHELF);
        }
        // reading tables
        for (int z = 410; z <= 418; z += 4)
            for (int x : new int[]{297, 309}) {
                fill(x, F + 1, z, x + 1, F + 1, z, slabTop(Blocks.SPRUCE_SLAB));
                set(x, F + 1, z + 1, stair(Blocks.SPRUCE_STAIRS, S));
                set(x + 1, F + 1, z + 1, stair(Blocks.SPRUCE_STAIRS, S));
            }
        // the anatomy atlas lies open on a lectern
        BlockState lectern = Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, N).setValue(LecternBlock.HAS_BOOK, true);
        set(CX2 + 2, F + 1, 409, lectern);
        c.lecterns.add(new int[]{CX2 + 2, F + 1, 409});
        sign(CX2 + 2, F + 2, 407, S, "Атлас анатомии", "(читать перед", "экзаменом!)", "↓");
        npc(X2 - 2, F + 1, 421, yawOf(W), NpcRole.LIBRARIAN, "Библиотекарь Роза Маратовна", 0, false);
        for (int x = X1 + 4; x < X2; x += 5)
            for (int z = za + 2; z < zb; z += 6)
                hangingLamp(x, F + 4, z);
    }

    private void anatomy() {
        int F = f(3);
        // --- room 301: the exam
        int za = 419, zb = Z2 - 1;
        air(X1 + 1, F + 1, za, X2 - 1, F + 4, zb);
        fill(X1 + 1, F + 1, 418, X2 - 1, F + 4, 418, INNER);
        air(CX1 + 1, F + 1, 418, CX1 + 1, F + 2, 418);
        door(CX1 + 1, F + 1, 418, S, Blocks.DARK_OAK_DOOR, false);
        sign(CX1, F + 2, 417, N, "Ауд. 301", "Кафедра анатомии", "ЭКЗАМЕН", "Тихо!");
        // examiner's table at the far wall
        fill(300, F + 1, 430, 306, F + 1, 430, Blocks.DARK_OAK_PLANKS);
        fill(300, F + 2, 430, 306, F + 2, 430, Blocks.GREEN_CARPET);
        set(301, F + 2, 430, Blocks.POTTED_RED_TULIP);
        npc(303, F + 1, 432, yawOf(N), NpcRole.PROFESSOR, "Профессор Латыпов", 0, false);
        // exam tickets and student desks
        for (int z = 421; z <= 427; z += 3)
            for (int x : new int[]{295, 298, 308, 311}) {
                set(x, F + 1, z, slabTop(Blocks.SPRUCE_SLAB));
                set(x, F + 1, z - 1, stair(Blocks.SPRUCE_STAIRS, N));
            }
        skeleton(X1 + 2, F + 1, 432);
        skeleton(X2 - 2, F + 1, 432);
        for (int x = X1 + 3; x <= X2 - 3; x++)
            if (x < 300 || x > 306) set(x, F + 3, zb, trapdoor(Blocks.DARK_OAK_TRAPDOOR, N, false, true));
        for (int x = X1 + 4; x < X2; x += 5) {
            hangingLamp(x, F + 4, 422);
            hangingLamp(x, F + 4, 428);
        }
        // nervous students in the corridor
        npc(CX1, F + 1, 414, yawOf(S), NpcRole.STUDENT, "Одногруппник Ильнур", 1, false);
        npc(CX2, F + 1, 412, yawOf(S), NpcRole.STUDENT, "Одногруппница Алина", 0, false);
        set(CX1, F + 1, 410, stair(Blocks.SPRUCE_STAIRS, W));
        set(CX1, F + 1, 409, stair(Blocks.SPRUCE_STAIRS, W));

        // --- anatomy museum (east room 407..417)
        room(3, 4, true, "Анатомический", "музей");
        for (int z = 408; z <= 416; z += 2) {
            set(X2 - 2, F + 1, z, Blocks.GLASS);
            set(X2 - 2, F + 2, z, z % 4 == 0 ? Blocks.BONE_BLOCK : Blocks.GLASS);
            set(X2 - 2, F + 3, z, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 4));
        }
        skeleton(CE + 2, F + 1, 416);
        set(CE + 2, F + 1, 408, Blocks.BONE_BLOCK);
        set(CE + 2, F + 2, 408, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 0));
        // --- dissection room (west room 407..417)
        room(3, 4, false, "Препараторская", "");
        fill(296, F + 1, 411, 298, F + 1, 413, Blocks.SMOOTH_QUARTZ);
        fill(296, F + 2, 411, 298, F + 2, 413, Blocks.WHITE_CARPET);
        set(X1 + 1, F + 1, 416, Blocks.CAULDRON);
    }

    /** A simple standing skeleton made of bone blocks. */
    private void skeleton(int x, int y, int z) {
        fill(x, y, z, x, y + 1, z, Blocks.BONE_BLOCK);
        set(x, y + 2, z, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 8));
    }

    private void otherRooms() {
        String[][][] names = {
                // storey 1: west rooms then east rooms, north to south
                {{"Архив", ""}, {"Кафедра", "латинского языка"}, {"Приёмная", "комиссия"}, {"Медпункт", ""}, {"Учебная часть", ""}, {"Актовый зал", "(ремонт)"}},
                {{"", ""}, {"", ""}, {"Отдел кадров", ""}, {"Бухгалтерия", ""}, {"Профком", "студентов"}, {"Музей истории", "БГМУ"}},
                // storey 2
                {{"", ""}, {"", ""}, {"Учебная", "комната 207"}, {"Кафедра", "гистологии"}, {"", ""}, {"", ""}},
                {{"", ""}, {"", ""}, {"Деканат", "стомат. ф-та"}, {"Кафедра", "пропедевтики"}, {"", ""}, {"", ""}},
                // storey 3
                {{"", ""}, {"", ""}, {"Учебная", "комната 305"}, {"Учебная", "комната 303"}, {"", ""}, {"", ""}},
                {{"", ""}, {"", ""}, {"Зав. кафедрой", "анатомии"}, {"Лаборантская", ""}, {"", ""}, {"", ""}},
                // storey 4
                {{"Учебная", "комната 401"}, {"Учебная", "комната 403"}, {"Кафедра", "норм. физиологии"}, {"Кафедра биохимии", "лаборатория"}, {"Учебная", "комната 407"}, {"Кафедра", "микробиологии"}},
                {{"Учебная", "комната 402"}, {"Учебная", "комната 404"}, {"Кафедра", "фармакологии"}, {"Кафедра биохимии", "практикум"}, {"Учебная", "комната 408"}, {"Кафедра", "патанатомии"}},
        };
        for (int s = 1; s <= STOREYS; s++)
            for (int side = 0; side < 2; side++)
                for (int r = 0; r < ROOMS.length; r++) {
                    String[] n = names[(s - 1) * 2 + side][r];
                    if (n[0].isEmpty()) continue;
                    boolean east = side == 1;
                    room(s, r, east, n[0], n[1]);
                    furnish(s, r, east, n[0]);
                }
    }

    /** Cuts a door from the corridor into a room and hangs its name next to it. */
    private void room(int storey, int r, boolean east, String l1, String l2) {
        int F = f(storey);
        int mz = (ROOMS[r][0] + ROOMS[r][1]) / 2;
        int wx = east ? CE : CW;
        door(wx, F + 1, mz, east ? W : E, Blocks.SPRUCE_DOOR, false);
        sign(east ? CE - 1 : CW + 1, F + 2, mz - 1, east ? W : E, "", l1, l2, "");
        int xa = east ? CE + 1 : X1 + 1, xb = east ? X2 - 1 : CW - 1;
        hangingLamp((xa + xb) / 2, F + 4, mz - 2);
        hangingLamp((xa + xb) / 2, F + 4, mz + 3);
    }

    private void furnish(int storey, int r, boolean east, String name) {
        int F = f(storey);
        int za = ROOMS[r][0], zb = ROOMS[r][1];
        int xa = east ? CE + 1 : X1 + 1, xb = east ? X2 - 1 : CW - 1;
        // the side along the corridor wall stays free as an aisle
        int ia = east ? xa + 2 : xa, ib = east ? xb : xb - 2;
        if (name.startsWith("Учебная") || name.startsWith("Кафедра") && !name.contains("биохимии")) {
            for (int x = ia + 1; x <= ib - 1; x++)
                for (int y = F + 2; y <= F + 3; y++) set(x, y, za, trapdoor(Blocks.DARK_OAK_TRAPDOOR, S, false, true));
            fill(ia + 1, F + 1, za + 2, ia + 2, F + 1, za + 2, Blocks.DARK_OAK_PLANKS);
            for (int z = za + 5; z <= zb - 1; z += 2)
                for (int x = ia; x <= ib; x++) {
                    if ((x - ia) % 3 == 2) continue;
                    if ((z - za) % 4 == 1) set(x, F + 1, z, slabTop(Blocks.SPRUCE_SLAB));
                    else set(x, F + 1, z, stair(Blocks.SPRUCE_STAIRS, S));
                }
        } else if (name.contains("биохимии")) {
            // laboratory benches with glassware
            for (int z = za + 2; z <= zb - 2; z += 3) {
                fill(ia, F + 1, z, ib, F + 1, z, Blocks.SMOOTH_QUARTZ);
                for (int x = ia; x <= ib; x += 2)
                    set(x, F + 2, z, x % 4 == 0 ? Blocks.BREWING_STAND : Blocks.FLOWER_POT);
            }
            set(ia, F + 1, zb, Blocks.CAULDRON);
            set(ia + 1, F + 1, zb, Blocks.BARREL);
        } else {
            // office: desks, shelves, a cupboard
            fill(ia, F + 1, za + 1, ia, F + 3, za + 1, Blocks.BOOKSHELF);
            fill(ib, F + 1, za + 1, ib, F + 3, za + 1, Blocks.BOOKSHELF);
            for (int z = za + 3; z <= zb - 2; z += 4) {
                fill(ia + 1, F + 1, z, ia + 2, F + 1, z, Blocks.SPRUCE_PLANKS);
                set(ia + 1, F + 1, z + 1, stair(Blocks.SPRUCE_STAIRS, S));
                set(ia + 1, F + 2, z, Blocks.FLOWER_POT);
            }
            set(ib, F + 1, zb, Blocks.BARREL);
            if (name.startsWith("Медпункт")) {
                bed(ib, F + 1, zb - 3, N, Blocks.WHITE_BED);
                set(ia, F + 1, zb, Blocks.BREWING_STAND);
            }
        }
    }

    // ------------------------------------------------------------------ outside

    private void surroundings() {
        // forecourt between the building and the Lenina sidewalk
        for (int z = Z1; z <= Z2; z++)
            for (int x = X2 + 1; x <= LEN - 1; x++)
                if (c.get(x, 0, z) == null) set(x, 0, z, (z < HN - 2 || z > HS + 2) ? Blocks.GRASS_BLOCK : Blocks.STONE_BRICKS);
        for (int z = Z1 + 6; z <= Z2 - 6; z += 8) {
            if (z > HN - 6 && z < HS + 6) continue;
            tree(X2 + 3, z, Tree.SPRUCE);
        }
        flag(X2 + 4, HN - 3, 9, Blocks.WHITE_WOOL, Blocks.BLUE_WOOL, Blocks.RED_WOOL, false);
        flag(X2 + 4, HS + 3, 9, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL, Blocks.GREEN_WOOL, false);
        bench(X2 + 2, HN - 6, E);
        bench(X2 + 2, HS + 6, E);
        lamp(X2 + 5, HN - 1);
        lamp(X2 + 5, HS + 1);
        // rear yard
        for (int z = Z1 + 4; z <= Z2 - 4; z += 9)
            if (z < SZ1 - 6 || z > SZ2 + 6) tree(X1 - 4, z, Tree.BIRCH);
    }
}
