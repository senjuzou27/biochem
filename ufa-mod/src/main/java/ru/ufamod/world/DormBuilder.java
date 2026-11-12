package ru.ufamod.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ru.ufamod.entity.NpcRole;

import static ru.ufamod.world.Plan.*;

/** The student dormitory where the day starts. Only the ground storey has an interior. */
public class DormBuilder extends Build {
    private static final int X1 = DORM_X1, X2 = DORM_X2, Z1 = DORM_Z1, Z2 = DORM_Z2;
    private static final int CW = 347, CE = 351;
    private static final int STOREYS = 5;
    /** Interior z ranges of the rooms, north to south; index 3 is the lobby / kitchen. */
    private static final int[][] ROOMS = {{15, 20}, {22, 27}, {29, 34}, {36, 40}, {42, 47}, {49, 54}, {56, 61}};

    public DormBuilder(Canvas c) {
        super(c, 5L);
    }

    public void build() {
        int top = STOREYS * 4;
        fill(X1, 0, Z1, X2, 0, Z2, Blocks.SPRUCE_PLANKS);
        air(X1, 1, Z1, X2, top + 4, Z2);
        walls(X1, 1, Z1, X2, top, Z2, Blocks.BRICKS);
        walls(X1, 1, Z1, X2, 1, Z2, Blocks.STONE_BRICKS);
        for (int s = 0; s < STOREYS; s++) {
            int y0 = 1 + s * 4;
            fill(X1 + 1, y0 + 3, Z1 + 1, X2 - 1, y0 + 3, Z2 - 1, Blocks.SMOOTH_STONE);
            walls(X1, y0 + 3, Z1, X2, y0 + 3, Z2, Blocks.SMOOTH_STONE);
            for (int[] r : ROOMS) {
                int m = (r[0] + r[1]) / 2;
                fill(X1, y0 + 1, m, X1, y0 + 2, m + 1, Blocks.GLASS_PANE);
                fill(X2, y0 + 1, m, X2, y0 + 2, m + 1, Blocks.GLASS_PANE);
                if (s > 0) {
                    set(X1 + 3, y0 + 3, m, Blocks.GLOWSTONE);
                    set(X2 - 3, y0 + 3, m, Blocks.GLOWSTONE);
                }
            }
            fill(CW + 2, y0 + 1, Z1, CW + 2, y0 + 2, Z1, Blocks.GLASS_PANE);
            fill(CW + 2, y0 + 1, Z2, CW + 2, y0 + 2, Z2, Blocks.GLASS_PANE);
        }
        fill(X1, top + 1, Z1, X2, top + 1, Z2, slab(Blocks.SMOOTH_STONE_SLAB));
        walls(X1, top + 1, Z1, X2, top + 1, Z2, Blocks.SMOOTH_STONE);

        // ---- ground storey: corridor with rooms on both sides
        fill(CW, 1, Z1 + 1, CW, 3, Z2 - 1, Blocks.WHITE_TERRACOTTA);
        fill(CE, 1, Z1 + 1, CE, 3, Z2 - 1, Blocks.WHITE_TERRACOTTA);
        fill(CW + 2, 0, Z1 + 1, CW + 2, 0, Z2 - 1, Blocks.GREEN_TERRACOTTA);
        for (int i = 0; i + 1 < ROOMS.length; i++) {
            int z = ROOMS[i][1] + 1;
            fill(X1 + 1, 1, z, CW - 1, 3, z, Blocks.WHITE_TERRACOTTA);
            fill(CE + 1, 1, z, X2 - 1, 3, z, Blocks.WHITE_TERRACOTTA);
        }
        for (int z = Z1 + 3; z < Z2; z += 6) hangingLamp(CW + 2, 3, z);

        for (int i = 0; i < ROOMS.length; i++) {
            if (i == 3) continue;
            int mz = (ROOMS[i][0] + ROOMS[i][1]) / 2;
            for (int side = 0; side < 2; side++) {
                boolean east = side == 1;
                if (i == 1 && east) continue; // the player's room is furnished separately
                int number = 100 + i * 2 + side + 1;
                door(east ? CE : CW, 1, mz, east ? W : E, Blocks.SPRUCE_DOOR, false);
                String label = i == 0 && !east ? "Лестница" : i == 6 && !east ? "Душевая" : i == 6 ? "Прачечная" : "Комната " + number;
                String note = i == 0 && !east ? "закрыто: ремонт" : "";
                sign(east ? CE - 1 : CW + 1, 2, mz - 1, east ? W : E, "", label, note, "");
                if (label.startsWith("Комната")) genericRoom(east ? CE + 1 : X1 + 1, east ? X2 - 1 : CW - 1, ROOMS[i][0], ROOMS[i][1], east);
                else if (label.equals("Душевая")) {
                    for (int z = ROOMS[i][0]; z <= ROOMS[i][1]; z += 2) set(X1 + 1, 1, z, Blocks.CAULDRON);
                    fill(X1 + 1, 0, ROOMS[i][0], CW - 1, 0, ROOMS[i][1], Blocks.DIORITE);
                } else if (label.equals("Прачечная")) {
                    for (int z = ROOMS[i][0]; z <= ROOMS[i][1]; z += 2) set(X2 - 1, 1, z, Blocks.DISPENSER);
                } else {
                    fill(X1 + 1, 1, ROOMS[i][0], CW - 1, 1, ROOMS[i][0], Blocks.IRON_BARS);
                }
            }
        }
        playerRoom();
        lobby();
        kitchen();
        outside();
    }

    private void genericRoom(int xa, int xb, int za, int zb, boolean east) {
        Block[] beds = {Blocks.BLUE_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.YELLOW_BED, Blocks.PURPLE_BED};
        int wallX = east ? xb : xa;       // window side
        int doorX = east ? xa : xb;       // corridor side
        bed(wallX, 1, zb, N, beds[rnd.nextInt(beds.length)]);
        bed(doorX, 1, zb, N, beds[rnd.nextInt(beds.length)]);
        fill(wallX, 1, za, wallX, 2, za, Blocks.BOOKSHELF);
        set(wallX + (east ? -1 : 1), 1, za, Blocks.SPRUCE_PLANKS);
        set(wallX + (east ? -1 : 1), 2, za, Blocks.FLOWER_POT);
        set(wallX + (east ? -2 : 2), 1, za, stair(Blocks.SPRUCE_STAIRS, east ? W : E));
        set(doorX, 1, za, Blocks.BARREL);
        fill(xa + 2, 1, za + 2, xb - 2, 1, zb - 2, Blocks.LIGHT_GRAY_CARPET);
        hangingLamp((xa + xb) / 2, 3, (za + zb) / 2);
    }

    private void playerRoom() {
        int xa = CE + 1, xb = X2 - 1, za = 22, zb = 27;
        door(CE, 1, 24, W, Blocks.SPRUCE_DOOR, false);
        sign(CE - 1, 2, 23, W, "", "Комната 104", "(твоя)", "");
        // beds: yours by the window, your roommate's by the door wall
        bed(BED.getX(), 1, BED.getZ(), N, Blocks.CYAN_BED);
        bed(xa, 1, zb, N, Blocks.BROWN_BED);
        // wardrobe with the coat, the record book and the atlas
        chest(xb, 1, za, S, "wardrobe");
        sign(xb, 2, za + 1, W, "Шкаф:", "халат, зачётка,", "атлас", "");
        fill(xb - 1, 1, za, xb - 1, 2, za, Blocks.BOOKSHELF);
        // desk under a poster
        fill(xa + 1, 1, za, xa + 2, 1, za, slabTop(Blocks.SPRUCE_SLAB));
        set(xa + 2, 2, za, Blocks.POTTED_CACTUS);
        set(xa + 1, 1, za + 1, stair(Blocks.SPRUCE_STAIRS, S));
        sign(xa, 2, za, E, "ЭКЗАМЕН", "по анатомии", "СЕГОДНЯ!", "ауд. 301");
        fill(xa + 1, 1, za + 2, xb - 1, 1, zb - 1, Blocks.RED_CARPET);
        set(xa + 1, 1, za + 1, stair(Blocks.SPRUCE_STAIRS, S));
        hangingLamp(xa + 2, 3, 25);
        npc(xa + 1, 1, zb - 1, yawOf(N), NpcRole.ROOMMATE, "Сосед Азамат", 1, false);
    }

    private void lobby() {
        // open the corridor wall towards the lobby and the entrance
        air(CW, 1, 36, CW, 3, 40);
        fill(X1 + 1, 0, 36, CW, 0, 40, Blocks.POLISHED_ANDESITE);
        air(X1, 1, 38, X1, 2, 38);
        fill(X1, 1, 37, X1, 3, 39, Blocks.BRICKS);
        door(X1, 1, 38, E, Blocks.DARK_OAK_DOOR, false);
        // the watch desk
        fill(X1 + 3, 1, 36, X1 + 5, 1, 36, Blocks.SPRUCE_PLANKS);
        npc(X1 + 4, 1, 37, yawOf(S), NpcRole.WATCHMAN, "Вахтёр тётя Фая", 1, false);
        post(X1 + 2, 1, 40, rot(N), "ВАХТА", "Пропуск", "предъявлять", "в открытом виде");
        set(X1 + 1, 1, 40, Blocks.POTTED_FERN);
        hangingLamp(X1 + 3, 3, 38);
    }

    private void kitchen() {
        air(CE, 1, 37, CE, 2, 39);
        sign(CE - 1, 3, 38, W, "", "Кухня", "", "");
        fill(CE + 1, 0, 36, X2 - 1, 0, 40, Blocks.DIORITE);
        set(X2 - 1, 1, 36, Blocks.SMOKER);
        set(X2 - 2, 1, 36, Blocks.FURNACE);
        set(X2 - 3, 1, 36, Blocks.CAULDRON);
        chest(X2 - 1, 1, 40, N, "fridge");
        sign(X2 - 1, 2, 40, W, "Холодильник", "(общий)", "бери завтрак", "");
        // table and stools
        fill(CE + 2, 1, 38, CE + 3, 1, 38, Blocks.SPRUCE_FENCE);
        fill(CE + 2, 2, 38, CE + 3, 2, 38, Blocks.WHITE_CARPET);
        set(CE + 2, 1, 39, stair(Blocks.SPRUCE_STAIRS, S));
        set(CE + 3, 1, 37, stair(Blocks.SPRUCE_STAIRS, N));
        hangingLamp(CE + 3, 3, 38);
    }

    private void outside() {
        // path from the door to the Lenina sidewalk
        for (int x = LEN + RW; x < X1; x++)
            for (int z = 36; z <= 40; z++)
                set(x, 0, z, Blocks.STONE_BRICKS);
        sign(X1 - 1, 3, 38, W, "Общежитие №1", "БГМУ", "", "");
        sign(X1 - 1, 2, 40, W, "Вход", "по пропускам", "до 23:00", "");
        lamp(X1 - 2, 35);
        lamp(X1 - 2, 41);
        bench(X1 - 2, 43, W);
        for (int z = Z1 + 4; z < Z2; z += 9)
            if (z < 33 || z > 45) tree(X1 - 3, z, Tree.BIRCH);
        for (int z = Z1 + 3; z < Z2; z += 8) tree(X2 + 4, z, Tree.LINDEN);
    }
}
