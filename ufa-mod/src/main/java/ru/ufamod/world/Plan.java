package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.ufamod.city.CityData;

/** Coordinates of the army training camp. It stands on the flat land east of the mapped city. */
public final class Plan {
    private Plan() {}

    /** Ground level of the flat land outside the map. */
    public static final int BASE_Y = CityData.get().defaultY;

    public static final int CAMP_OX = 4000, CAMP_OZ = 0, CAMP_W = 176, CAMP_D = 160;
    public static final Vec3 CAMP_ARRIVE = new Vec3(CAMP_OX + 30.5, BASE_Y + 1, CAMP_OZ + 80.5);
    public static final BlockPos CAMP_SERGEANT = new BlockPos(CAMP_OX + 44, BASE_Y + 1, CAMP_OZ + 80);
    public static final AABB CAMP_COURSE_FINISH = campBox(140, 0, 20, 150, 8, 34);
    public static final BlockPos CAMP_COURSE_END = new BlockPos(CAMP_OX + 146, BASE_Y + 1, CAMP_OZ + 27);
    public static final BlockPos CAMP_RANGE = new BlockPos(CAMP_OX + 70, BASE_Y + 1, CAMP_OZ + 64);
    public static final BlockPos CAMP_TRENCH = new BlockPos(CAMP_OX + 100, BASE_Y + 1, CAMP_OZ + 112);
    public static final AABB CAMP_TRENCH_BOX = campBox(84, -4, 100, 118, 3, 124);
    public static final BlockPos CAMP_TRUCK = new BlockPos(CAMP_OX + 11, BASE_Y + 1, CAMP_OZ + 124);
    public static final AABB CAMP_TRUCK_BOX = campBox(14, 3, 122, 19, 6, 126);

    // ---- the front line of the bad ending: an invented place, far away from the camp
    public static final int FRONT_OX = 8000, FRONT_OZ = 0, FRONT_W = 176, FRONT_D = 160;
    /** Inside the dugout; its floor is three blocks below the ground. */
    public static final Vec3 FRONT_ARRIVE = new Vec3(FRONT_OX + 46.5, BASE_Y - 2, FRONT_OZ + 80.5);
    public static final BlockPos FRONT_COMMANDER = new BlockPos(FRONT_OX + 41, BASE_Y - 2, FRONT_OZ + 76);
    public static final BlockPos FRONT_MEDIC = new BlockPos(FRONT_OX + 41, BASE_Y - 2, FRONT_OZ + 85);
    public static final BlockPos FRONT_TRENCH = new BlockPos(FRONT_OX + 59, BASE_Y - 1, FRONT_OZ + 80);
    public static final BlockPos FRONT_OP = new BlockPos(FRONT_OX + 86, BASE_Y, FRONT_OZ + 40);
    public static final AABB FRONT_BOX = new AABB(FRONT_OX, BASE_Y - 8, FRONT_OZ, FRONT_OX + FRONT_W, BASE_Y + 40, FRONT_OZ + FRONT_D);
    /** Where the enemy comes out of the tree line, and the craters it advances to (local x, z). */
    public static final int FRONT_ENEMY_X = 150;
    public static final int[][] FRONT_COVER = {{118, 34}, {122, 50}, {116, 62}, {124, 74}, {118, 88}, {122, 100}, {116, 112}, {124, 124}, {120, 44}, {120, 118}};
    public static final int[][] FRONT_COVER_MID = {{98, 32}, {100, 46}, {96, 58}, {102, 70}, {97, 82}, {101, 94}, {96, 106}, {100, 118}, {98, 128}};
    /** Just beyond the second row of wire. */
    public static final int[][] FRONT_COVER_NEAR = {{76, 30}, {77, 44}, {75, 56}, {78, 66}, {76, 78}, {77, 90}, {75, 102}, {78, 114}, {76, 126}};
    /** Local x of the low parapet in front of the firing step: the trench zigzags, so it is one of two. */
    public static int frontLoopholeX(int localZ) {
        int jog = ((localZ - 24) / 16) % 2 == 0 ? 0 : 1;
        return 58 + jog + 3;
    }

    private static AABB campBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new AABB(CAMP_OX + x1, BASE_Y + y1, CAMP_OZ + z1, CAMP_OX + x2 + 1, BASE_Y + y2 + 1, CAMP_OZ + z2 + 1);
    }
}
