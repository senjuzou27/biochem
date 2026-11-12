package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import ru.ufamod.building.BuildingGenerator;
import ru.ufamod.building.BuildingGenerator.Entrance;
import ru.ufamod.building.BuildingGenerator.RoomRef;
import ru.ufamod.building.Model;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;
import ru.ufamod.city.Ground;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.quest.Loot;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Turns a few real buildings into the places of the story and remembers where things are. */
public final class Story {
    private Story() {}

    /** Named positions, filled in while the story buildings are generated. */
    public static final Map<String, BlockPos> MARKS = new ConcurrentHashMap<>();

    @Nullable
    private static String roleOf(CityData city, BuildingInfo b) {
        for (Map.Entry<String, Integer> e : city.roles.entrySet()) if (e.getValue() == b.index) return e.getKey();
        return null;
    }

    @Nullable
    public static Model build(CityData city, BuildingInfo b) {
        if (b == null) return null;
        String role = roleOf(city, b);
        BuildingGenerator.Hints hints = new BuildingGenerator.Hints();
        if (role != null) {
            switch (role) {
                case "bsmu_main" -> hints.coreOpen = true;
                case "bsmu_anatomy" -> hints.roomType = "anatomy";
                case "bsmu_canteen" -> hints.roomType = "canteen";
                case "library" -> hints.roomType = "library";
                default -> { }
            }
        }
        BuildingGenerator g = BuildingGenerator.generate(city, b, hints);
        if (g == null) return null;
        if (role != null && !g.entrances.isEmpty()) {
            switch (role) {
                case "dorm" -> dorm(city, g);
                case "bsmu_main" -> bsmuMain(city, g);
                case "bsmu_anatomy" -> anatomy(city, g);
                case "bsmu_canteen" -> canteen(g);
                case "library" -> library(g);
                default -> { }
            }
        }
        return g.m;
    }

    private static BlockPos outside(CityData city, Entrance e, int forward, int sideways) {
        Direction side = e.out().getClockWise();
        int x = e.x() + e.out().getStepX() * forward + side.getStepX() * sideways;
        int z = e.z() + e.out().getStepZ() * forward + side.getStepZ() * sideways;
        // never inside another building
        for (int k = 0; k < 6 && city.building(x, z) != 0; k++) {
            x -= e.out().getStepX();
            z -= e.out().getStepZ();
        }
        int y = city.height(x, z) + (city.ground(x, z) == Ground.SIDEWALK ? 2 : 1);
        return new BlockPos(x, y, z);
    }

    private static void npc(Model m, BlockPos p, float yaw, NpcRole role, String name, int variant) {
        m.npc(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, role, name, variant, false);
    }

    private static float yawOf(Direction d) {
        return switch (d) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            default -> -90f;
        };
    }

    // ------------------------------------------------------------------ the dormitory

    private static void dorm(CityData city, BuildingGenerator g) {
        Model m = g.m;
        Entrance e = g.entrances.get(0);
        for (Entrance c : g.entrances)
            if (c.stairs()) {
                e = c;
                break;
            }
        int storey = Math.min(1, g.levels - 1);
        RoomRef best = null;
        int bestDist = Integer.MAX_VALUE;
        for (RoomRef r : g.rooms) {
            if (r.storey() != storey || r.depth() < 3) continue;
            int dd = Math.abs(r.ax() - e.x()) + Math.abs(r.az() - e.z());
            if (dd < bestDist) {
                bestDist = dd;
                best = r;
            }
        }
        if (best != null) {
            int last = best.width() - 1, mid = best.width() / 2, back = best.depth() - 1;
            Direction up = best.out().getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
            BlockPos wardrobe = BuildingGenerator.roomCell(best, last, back);
            m.set(wardrobe.getX(), wardrobe.getY(), wardrobe.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, up.getOpposite()));
            m.chestItems(wardrobe.getX(), wardrobe.getY(), wardrobe.getZ(), Loot.of("wardrobe"));
            m.set(wardrobe.getX(), wardrobe.getY() + 1, wardrobe.getZ(), Blocks.AIR.defaultBlockState());
            BlockPos fridge = BuildingGenerator.roomCell(best, 1, back);
            m.set(fridge.getX(), fridge.getY(), fridge.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, up));
            m.chestItems(fridge.getX(), fridge.getY(), fridge.getZ(), Loot.of("fridge"));
            m.set(fridge.getX(), fridge.getY() + 1, fridge.getZ(), Blocks.AIR.defaultBlockState());
            MARKS.put("wake", BuildingGenerator.roomCell(best, mid, back));
            MARKS.put("bed", BuildingGenerator.roomCell(best, 1, 1));
            MARKS.put("wardrobe", wardrobe);
            MARKS.put("fridge", fridge);
            BlockPos mate = BuildingGenerator.roomCell(best, Math.max(2, mid - 1), 1);
            m.set(mate.getX(), mate.getY(), mate.getZ(), Blocks.AIR.defaultBlockState());
            npc(m, mate, yawOf(best.out().getOpposite()), NpcRole.ROOMMATE, "Сосед Азамат", 1);
        } else {
            MARKS.put("wake", e.lobby());
        }
        MARKS.put("dorm_out", outside(city, e, 3, 0));
        BlockPos hall = g.halls.get(g.entrances.indexOf(e));
        npc(m, g.spotNear(hall, 0), yawOf(e.out()), NpcRole.WATCHMAN, "Вахтёр тётя Фая", 1);
        BlockPos bus = outside(city, e, 6, 3);
        MARKS.put("bus_from", bus);
        npc(m, bus, yawOf(e.out().getOpposite()), NpcRole.DRIVER, "Водитель маршрутки", 2);
    }

    // ------------------------------------------------------------------ BSMU, building 1 (ul. Lenina, 3)

    private static void bsmuMain(CityData city, BuildingGenerator g) {
        Model m = g.m;
        Entrance e = g.entrances.get(0);
        BlockPos hall = g.halls.get(0);
        MARKS.put("bsmu_door", outside(city, e, 3, 0));
        BlockPos guard = g.spotNear(hall, 0);
        MARKS.put("guard", guard);
        npc(m, guard, yawOf(e.out()), NpcRole.WATCHMAN, "Охранник Ринат", 0);
        npc(m, g.spotNear(hall, 3), 0f, NpcRole.STUDENT, "Студент Айдар", 1);
        npc(m, g.spotNear(hall, 6), 90f, NpcRole.STUDENT, "Студентка Алина", 0);
        BlockPos stop = outside(city, e, 9, 4);
        MARKS.put("bus_to", stop);
        npc(m, stop, yawOf(e.out().getOpposite()), NpcRole.DRIVER, "Водитель маршрутки", 3);

        // the lecture hall: the open middle of the building
        BlockPos desk = guard;
        if (!g.core.isEmpty()) {
            long sx = 0, sz = 0;
            for (BlockPos p : g.core) {
                sx += p.getX();
                sz += p.getZ();
            }
            int cx = (int) (sx / g.core.size()), cz = (int) (sz / g.core.size());
            int best = Integer.MAX_VALUE;
            for (BlockPos p : g.core) {
                int dd = Math.abs(p.getX() - cx) + Math.abs(p.getZ() - cz);
                if (dd < best) {
                    best = dd;
                    desk = new BlockPos(p.getX(), g.base + 1, p.getZ());
                }
            }
            int y = g.base + 1;
            for (BlockPos p : g.core) {
                int dx = p.getX() - desk.getX(), dz = p.getZ() - desk.getZ();
                if (dx < 3 || dx > 11 || Math.abs(dz) > 7 || dz == 0) continue;
                if (!m.isAir(p.getX(), y, p.getZ())) continue;
                if (dx % 2 == 1) m.set(p.getX(), y, p.getZ(), Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                else m.set(p.getX(), y, p.getZ(), Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            }
            for (int dz = -1; dz <= 1; dz++)
                if (g.isCorridor(desk.getX() + 1, desk.getZ() + dz)) m.set(desk.getX() + 1, y, desk.getZ() + dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            if (g.isCorridor(desk.getX() + 1, desk.getZ() + 2))
                m.set(desk.getX() + 1, y, desk.getZ() + 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
            int[][] seats = {{4, -3, 0}, {6, 4, 1}, {8, -5, 0}, {6, -2, 1}};
            String[] names = {"Студентка Лейсан", "Студент Данис", "Студентка Камила", "Студент Ильнур"};
            for (int i = 0; i < seats.length; i++) {
                int x = desk.getX() + seats[i][0], z = desk.getZ() + seats[i][1];
                if (!g.isCorridor(x, z)) continue;
                m.set(x, y, z, Blocks.AIR.defaultBlockState());
                m.npc(x + 0.5, y, z + 0.5, 90f, NpcRole.STUDENT, names[i], seats[i][2], false);
            }
        }
        MARKS.put("lecture", desk);
        npc(m, desk, -90f, NpcRole.LECTURER, "Доцент Хабибуллина", 0);
    }

    // ------------------------------------------------------------------ the anatomy department (ul. Zaki Validi, 47 k1)

    private static void anatomy(CityData city, BuildingGenerator g) {
        Model m = g.m;
        Entrance e = g.entrances.get(0);
        BlockPos hall = g.halls.get(0);
        BlockPos prof = g.spotNear(hall, 1);
        MARKS.put("exam", prof);
        MARKS.put("exam_out", outside(city, e, 3, 0));
        npc(m, prof, yawOf(e.out()), NpcRole.PROFESSOR, "Профессор Латыпов", 0);
        npc(m, g.spotNear(hall, 3), 0f, NpcRole.STUDENT, "Одногруппник Ильнур", 1);
        npc(m, g.spotNear(hall, 5), 180f, NpcRole.STUDENT, "Одногруппница Алина", 0);
    }

    private static void canteen(BuildingGenerator g) {
        BlockPos hall = g.halls.get(0);
        BlockPos cook = g.spotNear(hall, 0);
        MARKS.put("canteen", cook);
        npc(g.m, cook, yawOf(g.entrances.get(0).out()), NpcRole.COOK, "Повар Зульфия-апа", 0);
    }

    private static void library(BuildingGenerator g) {
        Model m = g.m;
        BlockPos hall = g.halls.get(0);
        BlockPos lib = g.spotNear(hall, 0);
        MARKS.put("library", lib);
        npc(m, lib, yawOf(g.entrances.get(0).out()), NpcRole.LIBRARIAN, "Библиотекарь Роза Маратовна", 0);
        BlockPos lectern = g.spotNear(hall, 2);
        if (!lectern.equals(lib) && !lectern.equals(hall)) {
            m.set(lectern.getX(), lectern.getY(), lectern.getZ(), Blocks.LECTERN.defaultBlockState()
                    .setValue(LecternBlock.FACING, g.entrances.get(0).out()).setValue(LecternBlock.HAS_BOOK, true));
            m.lecternBook(lectern.getX(), lectern.getY(), lectern.getZ(), Loot.atlas());
        }
    }
}
