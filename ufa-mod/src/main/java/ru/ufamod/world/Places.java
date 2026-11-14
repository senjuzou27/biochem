package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import ru.ufamod.building.BuildingCache;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;

/** Where the story happens. The positions come from the generated story buildings. */
public final class Places {
    private static volatile Places instance;

    public static Places get() {
        Places p = instance;
        if (p == null) {
            synchronized (Places.class) {
                p = instance;
                if (p == null) instance = p = new Places();
            }
        }
        return p;
    }

    public final BlockPos wake, bed, wardrobe, fridge, dormOut, busFrom, busTo, bsmuDoor, guard, lecture, exam, examOut, library, canteen,
            salavat, friendship, gostiny, opera, park, artKvadrat, mainDoor;
    /** Camera positions for the developer self-test. */
    public final BlockPos dormView, dormLobby, bsmuView, bsmuHall, examHall, canteenHall, libraryHall, mainView, lectureRoom;
    public final int dormIndex, bsmuIndex, anatomyIndex;
    private final CityData city;

    private Places() {
        city = CityData.get();
        for (String role : new String[]{"dorm", "bsmu_main", "bsmu_k7", "bsmu_anatomy", "bsmu_canteen", "library"}) {
            BuildingInfo b = city.role(role);
            if (b != null) BuildingCache.get(b);
        }
        dormIndex = index("dorm");
        bsmuIndex = index("bsmu_k7");
        anatomyIndex = index("bsmu_anatomy");
        wake = mark("wake", "dorm");
        bed = mark("bed", "dorm");
        wardrobe = mark("wardrobe", "dorm");
        fridge = mark("fridge", "dorm");
        dormOut = mark("dorm_out", "dorm");
        busFrom = mark("bus_from", "dorm");
        busTo = mark("bus_to", "bsmu_k7");
        bsmuDoor = mark("bsmu_door", "bsmu_k7");
        guard = mark("guard", "bsmu_k7");
        lecture = mark("lecture", "bsmu_k7");
        exam = mark("exam", "bsmu_anatomy");
        examOut = mark("exam_out", "bsmu_anatomy");
        library = mark("library", "library");
        canteen = mark("canteen", "bsmu_canteen");
        dormView = mark("dorm_view", "dorm");
        dormLobby = mark("dorm_lobby", "dorm");
        bsmuView = mark("bsmu_view", "bsmu_k7");
        bsmuHall = mark("bsmu_hall", "bsmu_k7");
        examHall = mark("exam_hall", "bsmu_anatomy");
        canteenHall = mark("canteen_hall", "bsmu_canteen");
        libraryHall = mark("library_hall", "library");
        mainDoor = mark("main_door", "bsmu_main");
        mainView = mark("main_view", "bsmu_main");
        lectureRoom = mark("lecture_room", "bsmu_k7");
        salavat = spot("salavat", 0, -12);
        friendship = spot("friendship", 0, -11);
        gostiny = spot("gostiny_sq", 0, 0);
        opera = near("opera");
        park = spot("lenin_park", 0, 0);
        artKvadrat = spot("art_kvadrat", 0, 0);
    }

    private int index(String role) {
        BuildingInfo b = city.role(role);
        return b == null ? -1 : b.index;
    }

    private BlockPos mark(String name, String role) {
        BlockPos p = Story.MARKS.get(name);
        if (p != null) return p;
        return near(role);
    }

    /** A free spot of ground beside a building. */
    private BlockPos near(String role) {
        BuildingInfo b = city.role(role);
        if (b == null) return new BlockPos(0, city.height(0, 0) + 2, 0);
        int x = (b.minX + b.maxX) / 2, z = b.maxZ + 3;
        for (int k = 0; k < 20 && city.building(x, z) != 0; k++) z++;
        return new BlockPos(x, city.height(x, z) + 2, z);
    }

    private BlockPos spot(String name, int dx, int dz) {
        int[] s = city.spots.get(name);
        if (s == null) return new BlockPos(0, city.height(0, 0) + 2, 0);
        int x = s[0] + dx, z = s[1] + dz;
        for (int k = 0; k < 20 && city.building(x, z) != 0; k++) z++;
        return new BlockPos(x, city.height(x, z) + 2, z);
    }

    public boolean insideBuilding(double x, double z, int index) {
        return index > 0 && city.building((int) Math.floor(x), (int) Math.floor(z)) == index;
    }

    public boolean outdoors(double x, double z) {
        return city.building((int) Math.floor(x), (int) Math.floor(z)) == 0;
    }
}
