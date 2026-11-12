package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The street grid of the (compressed, axis-aligned) centre of Ufa and all coordinates the story needs.
 * x grows to the east, z grows to the south. The city canvas origin is world (0, 63, 0),
 * so plan x/z are world x/z and world y = BASE_Y + local y.
 */
public final class Plan {
    private Plan() {}

    public static final int BASE_Y = 63;
    public static final int SIZE_X = 640, SIZE_Z = 720;

    /** Width of a normal street: 3 sidewalk + 9 road + 3 sidewalk. */
    public static final int RW = 15;

    // North-south streets (x of the western edge).
    public static final int KM = 160;      // ул. Карла Маркса
    public static final int TEATR = 215;   // ул. Театральная (7 wide)
    public static final int LEN = 320;     // ул. Ленина
    public static final int SOV = 450;     // ул. Советская
    public static final int TSUR = 560;    // ул. Цюрупы

    // East-west streets (z of the northern edge).
    public static final int CHERN = 70;    // ул. Чернышевского
    public static final int KOMM = 190;    // ул. Коммунистическая
    public static final int PUSH = 310;    // ул. Пушкина
    public static final int ZV = 440;      // ул. Заки Валиди
    public static final int TUK = 560;     // ул. Тукаева

    /** The Belaya river: everything with z >= RIVER_Z or x <= RIVER_X. */
    public static final int RIVER_Z = 664, RIVER_X = 24;

    // ------------------------------------------------------------------ dormitory (east side of Lenina, north end)
    public static final int DORM_X1 = 340, DORM_X2 = 358, DORM_Z1 = 14, DORM_Z2 = 62;
    /** Foot of the player's bed; the head is one block north. */
    public static final BlockPos BED = w(357, 1, 26);
    public static final Vec3 WAKE_POS = new Vec3(355.5, BASE_Y + 1, 25.5);
    public static final AABB DORM_BOX = box(DORM_X1, 0, DORM_Z1, DORM_X2, 24, DORM_Z2);

    // ------------------------------------------------------------------ BSMU main building (Lenina, 3)
    public static final int BS_X1 = 292, BS_X2 = 314, BS_Z1 = 338, BS_Z2 = 434;
    public static final int BS_FLOOR_H = 5;
    /** Local y of the floor slab of storey n (1-based). */
    public static int bsFloor(int storey) {
        return 1 + (storey - 1) * BS_FLOOR_H;
    }
    public static final int BS_MID_Z = 386;
    public static final BlockPos BSMU_DOOR = w(318, 1, 386);
    /** In front of the entrance, on the Lenina sidewalk. */
    public static final AABB BSMU_FRONT = box(315, 0, 372, 336, 8, 400);
    public static final AABB BSMU_INSIDE = box(284, 0, BS_Z1, BS_X2, 30, BS_Z2);
    public static final BlockPos LECTURE_HALL = w(303, bsFloor(2) + 1, 352);
    public static final BlockPos EXAM_ROOM = w(303, bsFloor(3) + 1, 424);
    public static final BlockPos CANTEEN = w(309, bsFloor(1) + 1, 352);
    public static final BlockPos LIBRARY = w(303, bsFloor(2) + 1, 424);

    // ------------------------------------------------------------------ landmarks
    public static final BlockPos SALAVAT = w(70, 1, 606);
    public static final AABB SALAVAT_BOX = box(52, -2, 592, 88, 30, 640);
    public static final BlockPos FRIENDSHIP = w(598, 1, 608);
    public static final BlockPos GOSTINY = w(250, 1, 140);
    public static final BlockPos OPERA = w(300, 1, 280);
    public static final BlockPos PARK = w(380, 1, 500);

    // ------------------------------------------------------------------ training camp (its own canvas, far east)
    public static final int CAMP_OX = 3000, CAMP_OZ = 0, CAMP_W = 176, CAMP_D = 160;
    public static final Vec3 CAMP_ARRIVE = new Vec3(CAMP_OX + 30.5, BASE_Y + 1, CAMP_OZ + 80.5);
    public static final BlockPos CAMP_SERGEANT = new BlockPos(CAMP_OX + 44, BASE_Y + 1, CAMP_OZ + 80);
    public static final AABB CAMP_COURSE_START = campBox(60, 0, 20, 66, 6, 34);
    public static final AABB CAMP_COURSE_FINISH = campBox(140, 0, 20, 150, 8, 34);
    public static final BlockPos CAMP_COURSE = new BlockPos(CAMP_OX + 62, BASE_Y + 1, CAMP_OZ + 27);
    public static final BlockPos CAMP_RANGE = new BlockPos(CAMP_OX + 70, BASE_Y + 1, CAMP_OZ + 64);
    public static final BlockPos CAMP_TRENCH = new BlockPos(CAMP_OX + 100, BASE_Y + 1, CAMP_OZ + 112);
    public static final AABB CAMP_TRENCH_BOX = campBox(84, -4, 100, 118, 3, 124);
    public static final BlockPos CAMP_TRUCK = new BlockPos(CAMP_OX + 11, BASE_Y + 1, CAMP_OZ + 124);
    public static final AABB CAMP_TRUCK_BOX = campBox(14, 3, 122, 19, 6, 126);

    public static BlockPos w(int x, int localY, int z) {
        return new BlockPos(x, BASE_Y + localY, z);
    }

    public static AABB box(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new AABB(x1, BASE_Y + y1, z1, x2 + 1, BASE_Y + y2 + 1, z2 + 1);
    }

    private static AABB campBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new AABB(CAMP_OX + x1, BASE_Y + y1, CAMP_OZ + z1, CAMP_OX + x2 + 1, BASE_Y + y2 + 1, CAMP_OZ + z2 + 1);
    }
}
