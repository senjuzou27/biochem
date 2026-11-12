package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import ru.ufamod.entity.NpcRole;

import static ru.ufamod.world.Plan.*;

/** The army training camp of the bad ending. Lives on its own canvas far to the east of the city. */
public class CampBuilder extends Build {
    public CampBuilder(Canvas c) {
        super(c, 11L);
    }

    public static Canvas buildCamp() {
        Canvas c = new Canvas(CAMP_OX, BASE_Y, CAMP_OZ, CAMP_W, CAMP_D);
        new CampBuilder(c).build();
        return c;
    }

    private void build() {
        fence();
        paradeGround();
        barracks(12, 16, "Казарма №1");
        barracks(12, 36, "Казарма №2");
        staff();
        obstacleCourse();
        shootingRange();
        trenches();
        truck();
        tents();
        for (int[] t : new int[][]{{8, 8}, {167, 8}, {8, 151}, {167, 151}}) tower(t[0], t[1]);
        for (int i = 0; i < 40; i++) {
            int x = 8 + rnd.nextInt(160), z = 8 + rnd.nextInt(144);
            if (c.get(x, 0, z) == null && c.isEmpty(x, 1, z) && (x > 156 || z > 140 || z < 14)) tree(x, z, i % 2 == 0 ? Tree.BIRCH : Tree.SPRUCE);
        }
    }

    private void fence() {
        int x1 = 4, x2 = 171, z1 = 4, z2 = 155;
        walls(x1, 1, z1, x2, 3, z2, Blocks.IRON_BARS);
        walls(x1, 4, z1, x2, 4, z2, Blocks.COBWEB);
        for (int x = x1; x <= x2; x += 6) {
            fill(x, 1, z1, x, 4, z1, Blocks.STONE_BRICKS);
            fill(x, 1, z2, x, 4, z2, Blocks.STONE_BRICKS);
        }
        for (int z = z1; z <= z2; z += 6) {
            fill(x1, 1, z, x1, 4, z, Blocks.STONE_BRICKS);
            fill(x2, 1, z, x2, 4, z, Blocks.STONE_BRICKS);
        }
        // the checkpoint with a closed gate on the western side
        fill(x1, 1, 76, x1, 4, 84, Blocks.STONE_BRICKS);
        fill(x1, 1, 78, x1, 3, 82, Blocks.IRON_BLOCK);
        fill(x1 + 1, 0, 70, x1 + 19, 0, 90, Blocks.GRAVEL);
        fill(x1 + 1, 1, 72, x1 + 5, 4, 75, Blocks.GREEN_TERRACOTTA);
        fill(x1 + 2, 2, 75, x1 + 4, 3, 75, Blocks.GLASS_PANE);
        post(x1 + 7, 1, 76, rot(E), "КПП", "Учебный центр", "Выход запрещён", "");
        npc(x1 + 3, 1, 78, yawOf(E), NpcRole.SOLDIER, "Часовой", 0, false);
        npc(x1 + 3, 1, 82, yawOf(E), NpcRole.SOLDIER, "Часовой", 0, false);
    }

    private void paradeGround() {
        fill(24, 0, 64, 58, 0, 96, Blocks.GRAY_CONCRETE);
        walls(26, 0, 66, 56, 0, 94, Blocks.WHITE_CONCRETE);
        flag(41, 66, 12, Blocks.WHITE_WOOL, Blocks.BLUE_WOOL, Blocks.RED_WOOL, true);
        post(40, 1, 68, rot(S), "ПЛАЦ", "Построение", "06:00", "");
        npc(44, 1, 80, yawOf(W), NpcRole.SERGEANT, "Сержант Громов", 0, false);
        String[] names = {"Мобилизованный Ильдар", "Мобилизованный Саня", "Мобилизованный Рамиль", "Мобилизованный Костя", "Мобилизованный Айрат"};
        for (int i = 0; i < names.length; i++) npc(50, 1, 72 + i * 4, yawOf(W), NpcRole.RECRUIT, names[i], 0, false);
        for (int z = 66; z <= 94; z += 14) {
            lamp(23, z);
            lamp(59, z);
        }
    }

    private void barracks(int x, int z, String name) {
        int x2 = x + 40, z2 = z + 12;
        fill(x, 0, z, x2, 0, z2, Blocks.SPRUCE_PLANKS);
        air(x, 1, z, x2, 9, z2);
        walls(x, 1, z, x2, 4, z2, Blocks.GREEN_TERRACOTTA);
        for (int wx = x + 3; wx < x2 - 2; wx += 4) {
            fill(wx, 2, z, wx + 1, 3, z, Blocks.GLASS_PANE);
            fill(wx, 2, z2, wx + 1, 3, z2, Blocks.GLASS_PANE);
        }
        gableRoof(x, z, x2, z2, 5, Blocks.DARK_OAK_STAIRS, Blocks.GREEN_TERRACOTTA);
        fill(x + 1, 5, z + 1, x2 - 1, 5, z2 - 1, AIR);
        int mz = (z + z2) / 2;
        door(x2, 1, mz, W, Blocks.SPRUCE_DOOR, false);
        sign(x2 + 1, 3, mz, E, "", name, "", "");
        for (int bx = x + 2; bx < x2 - 3; bx += 3) {
            bed(bx, 1, z + 2, N, Blocks.GREEN_BED);
            bed(bx, 1, z2 - 1, N, Blocks.GREEN_BED);
            set(bx + 1, 1, z + 1, Blocks.BARREL);
        }
        for (int bx = x + 3; bx < x2 - 3; bx += 6) set(bx, 2, z + 1, Blocks.LANTERN);
    }

    private void staff() {
        int x = 12, z = 100, x2 = 40, z2 = 114;
        fill(x, 0, z, x2, 0, z2, Blocks.SPRUCE_PLANKS);
        air(x, 1, z, x2, 8, z2);
        walls(x, 1, z, x2, 4, z2, Blocks.BRICKS);
        for (int wx = x + 3; wx < x2 - 2; wx += 4) fill(wx, 2, z, wx + 1, 3, z, Blocks.GLASS_PANE);
        fill(x, 5, z, x2, 5, z2, slab(Blocks.SMOOTH_STONE_SLAB));
        door(x2, 1, z + 7, W, Blocks.SPRUCE_DOOR, false);
        sign(x2 + 1, 3, z + 7, E, "", "Штаб", "Столовая", "");
        for (int tx = x + 4; tx < x2 - 3; tx += 5) {
            set(tx, 1, z + 5, Blocks.SPRUCE_FENCE);
            set(tx, 2, z + 5, Blocks.WHITE_CARPET);
            set(tx, 1, z + 4, stair(Blocks.SPRUCE_STAIRS, N));
            set(tx, 1, z + 6, stair(Blocks.SPRUCE_STAIRS, S));
        }
        set(x + 1, 1, z + 12, Blocks.SMOKER);
        set(x + 2, 1, z + 12, Blocks.CAULDRON);
        set(x + 10, 4, z + 7, Blocks.GLOWSTONE);
        set(x + 20, 4, z + 7, Blocks.GLOWSTONE);
        fill(x + 1, 4, z + 1, x2 - 1, 4, z2 - 1, AIR);
        set(x + 10, 5, z + 7, Blocks.GLOWSTONE);
        set(x + 20, 5, z + 7, Blocks.GLOWSTONE);
    }

    private void obstacleCourse() {
        int z1 = 21, z2 = 33;
        fill(60, 0, z1, 152, 0, z2, Blocks.COARSE_DIRT);
        fill(60, 1, z1 - 1, 152, 1, z1 - 1, Blocks.SPRUCE_FENCE);
        fill(60, 1, z2 + 1, 152, 1, z2 + 1, Blocks.SPRUCE_FENCE);
        fill(60, 0, z1, 61, 0, z2, Blocks.WHITE_CONCRETE);
        post(59, 1, z1 + 6, rot(W), "ПОЛОСА", "ПРЕПЯТСТВИЙ", "СТАРТ →", "добеги до финиша");
        // 1: a low wall to jump over
        fill(70, 1, z1, 70, 1, z2, Blocks.STONE_BRICKS);
        // 2: stepping stones across a water pit
        fill(76, 0, z1, 84, 0, z2, Blocks.WATER);
        fill(76, -1, z1, 84, -1, z2, Blocks.CLAY);
        for (int i = 0; i < 5; i++) {
            int sz = z1 + 2 + (i % 2) * 4;
            fill(76 + i * 2, 0, sz, 76 + i * 2, 0, sz + 2, Blocks.SPRUCE_LOG);
            fill(76 + i * 2, 0, sz + 6, 76 + i * 2, 0, Math.min(z2, sz + 8), Blocks.SPRUCE_LOG);
        }
        // 3: a low passage under barbed wire
        for (int x = 90; x <= 96; x++) {
            fill(x, 3, z1, x, 3, z2, Blocks.IRON_BARS);
            if (x % 3 == 0) {
                fill(x, 1, z1, x, 2, z1, Blocks.SPRUCE_FENCE);
                fill(x, 1, z2, x, 2, z2, Blocks.SPRUCE_FENCE);
            }
        }
        // 4: a wall with ladders
        fill(103, 1, z1, 104, 4, z2, Blocks.SPRUCE_PLANKS);
        for (int z = z1 + 1; z < z2; z += 2) {
            fill(102, 1, z, 102, 4, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
            fill(105, 1, z, 105, 4, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
        // 5: balance beams over mud
        fill(112, 0, z1, 122, 0, z2, Blocks.WATER);
        fill(112, -1, z1, 122, -1, z2, Blocks.MUD);
        for (int z = z1 + 2; z <= z2 - 2; z += 4) fill(112, 1, z, 122, 1, z, Blocks.SPRUCE_LOG.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
        fill(111, 1, z1, 111, 1, z2, slab(Blocks.SPRUCE_SLAB));
        fill(123, 1, z1, 123, 1, z2, slab(Blocks.SPRUCE_SLAB));
        // 6: a slalom between fences
        for (int i = 0; i < 4; i++) {
            int x = 128 + i * 3;
            if (i % 2 == 0) fill(x, 1, z1, x, 2, z2 - 3, Blocks.SPRUCE_FENCE);
            else fill(x, 1, z1 + 3, x, 2, z2, Blocks.SPRUCE_FENCE);
        }
        // finish
        for (int x = 142; x <= 150; x++)
            for (int z = z1; z <= z2; z++)
                set(x, 0, z, (x + z) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE);
        post(151, 1, z1 + 6, rot(W), "ФИНИШ", "", "Доложи", "сержанту");
    }

    private void shootingRange() {
        int z1 = 56, z2 = 72;
        fill(62, 0, z1, 120, 0, z2, Blocks.SAND);
        fill(62, -1, z1, 120, -1, z2, Blocks.SANDSTONE);
        // firing line
        fill(72, 1, z1, 72, 1, z2, Blocks.SANDSTONE);
        for (int z = z1 + 2; z <= z2; z += 3) set(72, 1, z, AIR);
        // targets on posts and a backstop behind them
        for (int i = 0; i < 5; i++) {
            int z = z1 + 2 + i * 3;
            int x = 98 + (i % 2) * 8;
            set(x, 1, z, Blocks.SPRUCE_FENCE);
            set(x, 2, z, Blocks.TARGET);
        }
        fill(116, 1, z1, 118, 6, z2, Blocks.SANDSTONE);
        fill(62, 1, z1 - 1, 118, 1, z1 - 1, Blocks.SPRUCE_FENCE);
        fill(74, 1, z2 + 1, 118, 1, z2 + 1, Blocks.SPRUCE_FENCE);
        set(66, 1, z1 - 1, AIR);
        post(66, 1, z1 - 1, rot(N), "СТРЕЛЬБИЩЕ", "5 попаданий", "по мишеням", "из лука");
    }

    private void trenches() {
        fill(84, 0, 100, 118, 0, 124, Blocks.DIRT);
        fill(84, -3, 100, 118, -1, 124, Blocks.DIRT);
        // a sample trench with sandbags
        air(86, -1, 104, 100, 0, 105);
        fill(86, 1, 103, 100, 1, 103, Blocks.SANDSTONE);
        for (int x = 86; x <= 100; x += 3) set(x, 1, 103, slab(Blocks.SANDSTONE_SLAB));
        post(83, 1, 112, rot(W), "ОКОПЫ", "Выкопай", "12 блоков земли", "лопаткой");
        fill(84, 1, 99, 118, 1, 99, Blocks.SPRUCE_FENCE);
        fill(84, 1, 125, 118, 1, 125, Blocks.SPRUCE_FENCE);
    }

    private void truck() {
        int x = 14, z = 122;
        fill(8, 0, 118, 30, 0, 130, Blocks.GRAVEL);
        // wheels
        for (int wx : new int[]{x + 1, x + 4, x + 7})
            for (int wz : new int[]{z, z + 4})
                set(wx, 1, wz, Blocks.BLACK_CONCRETE);
        // chassis and the cargo bed with benches
        fill(x, 2, z, x + 8, 2, z + 4, Blocks.GREEN_TERRACOTTA);
        fill(x, 3, z, x + 5, 3, z, Blocks.SPRUCE_FENCE);
        fill(x, 3, z + 4, x + 5, 3, z + 4, Blocks.SPRUCE_FENCE);
        fill(x + 1, 3, z + 1, x + 4, 3, z + 1, stair(Blocks.SPRUCE_STAIRS, N));
        fill(x + 1, 3, z + 3, x + 4, 3, z + 3, stair(Blocks.SPRUCE_STAIRS, S));
        // tarpaulin
        fill(x, 5, z, x + 5, 5, z + 4, Blocks.GREEN_WOOL);
        fill(x, 4, z, x + 5, 4, z, Blocks.GREEN_WOOL);
        fill(x, 4, z + 4, x + 5, 4, z + 4, Blocks.GREEN_WOOL);
        fill(x, 3, z, x + 5, 3, z, Blocks.GREEN_WOOL);
        fill(x, 3, z + 4, x + 5, 3, z + 4, Blocks.GREEN_WOOL);
        // cab
        fill(x + 6, 3, z, x + 8, 4, z + 4, Blocks.GREEN_TERRACOTTA);
        fill(x + 8, 4, z + 1, x + 8, 4, z + 3, Blocks.GLASS);
        fill(x + 7, 4, z, x + 7, 4, z, Blocks.GLASS);
        fill(x + 7, 4, z + 4, x + 7, 4, z + 4, Blocks.GLASS);
        fill(x + 6, 5, z, x + 7, 5, z + 4, slab(Blocks.DARK_PRISMARINE_SLAB));
        set(x + 8, 3, z + 1, Blocks.SEA_LANTERN);
        set(x + 8, 3, z + 3, Blocks.SEA_LANTERN);
        // steps up into the bed from the back
        fill(x - 2, 1, z + 1, x - 2, 1, z + 3, stair(Blocks.SPRUCE_STAIRS, E));
        fill(x - 1, 1, z + 1, x - 1, 1, z + 3, Blocks.SPRUCE_PLANKS);
        fill(x - 1, 2, z + 1, x - 1, 2, z + 3, stair(Blocks.SPRUCE_STAIRS, E));
        post(x - 3, 1, z - 1, rot(W), "ПОГРУЗКА", "«Урал»", "Садись в кузов", "");
        npc(x + 10, 1, z + 6, yawOf(W), NpcRole.SOLDIER, "Водитель", 0, false);
    }

    private void tents() {
        for (int i = 0; i < 5; i++) {
            int x = 62 + i * 14, z = 134;
            fill(x, 0, z, x + 8, 0, z + 10, Blocks.COARSE_DIRT);
            for (int k = 0; k < 4; k++) {
                fill(x + k, 1 + k, z, x + k, 1 + k, z + 10, Blocks.GREEN_WOOL);
                fill(x + 8 - k, 1 + k, z, x + 8 - k, 1 + k, z + 10, Blocks.GREEN_WOOL);
            }
            fill(x + 4, 5, z, x + 4, 5, z + 10, Blocks.GREEN_WOOL);
            for (int k = 1; k < 4; k++) fill(x + k + 1, k, z + 10, x + 7 - k, k, z + 10, Blocks.GREEN_WOOL);
            bed(x + 3, 1, z + 4, S, Blocks.GREEN_BED);
            bed(x + 5, 1, z + 4, S, Blocks.GREEN_BED);
            set(x + 4, 4, z + 5, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        post(60, 1, 139, rot(W), "Палаточный", "городок", "", "");
    }

    private void tower(int x, int z) {
        for (int[] p : new int[][]{{0, 0}, {3, 0}, {0, 3}, {3, 3}}) fill(x + p[0], 1, z + p[1], x + p[0], 8, z + p[1], Blocks.SPRUCE_LOG);
        fill(x - 1, 9, z - 1, x + 4, 9, z + 4, Blocks.SPRUCE_PLANKS);
        walls(x - 1, 10, z - 1, x + 4, 10, z + 4, Blocks.SPRUCE_FENCE);
        fill(x - 1, 13, z - 1, x + 4, 13, z + 4, slab(Blocks.DARK_OAK_SLAB));
        for (int[] p : new int[][]{{-1, -1}, {4, -1}, {-1, 4}, {4, 4}}) fill(x + p[0], 10, z + p[1], x + p[0], 12, z + p[1], Blocks.SPRUCE_FENCE);
        set(x + 1, 12, z + 1, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }
}
