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

    private static AABB campBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new AABB(CAMP_OX + x1, BASE_Y + y1, CAMP_OZ + z1, CAMP_OX + x2 + 1, BASE_Y + y2 + 1, CAMP_OZ + z2 + 1);
    }
}
