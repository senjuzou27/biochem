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
        BuildingGenerator g = generate(city, b);
        return g == null ? null : g.m;
    }

    @Nullable
    public static BuildingGenerator generate(CityData city, BuildingInfo b) {
        if (b == null) return null;
        String role = roleOf(city, b);
        BuildingGenerator.Hints hints = new BuildingGenerator.Hints();
        if (role != null) {
            switch (role) {
                case "bsmu_main" -> {
                    // building 1 (1940, the "biocorpus"): sand-coloured walls, a ground floor in red and
                    // cream bands, white pilasters; the columned entrance is built in bsmuFront()
                    hints.era = ru.ufamod.building.Style.Era.STALIN;
                    hints.wall = Blocks.SMOOTH_SANDSTONE;
                    hints.trim = Blocks.SMOOTH_QUARTZ;
                    hints.base = Blocks.BRICKS;
                    hints.stripe = Blocks.SMOOTH_SANDSTONE;
                    hints.noPortico = true;
                    hints.pitched = Boolean.FALSE;
                    hints.levels = 3;
                    hints.floorSigns = new String[][]{
                            {"БГМУ, корпус №1", "ул. Ленина, 3", "«Биокорпус»", "построен в 1940"},
                            {"2 этаж", "", "", ""},
                            {"3 этаж", "", "", ""}};
                }
                case "bsmu_k7" -> {
                    // building 7 (1978-80): six storeys, ochre walls with white pilasters, a dark ground
                    // floor and a red-brown mansard roof with dormers
                    hints.era = ru.ufamod.building.Style.Era.STALIN;
                    hints.wall = Blocks.YELLOW_TERRACOTTA;
                    hints.trim = Blocks.SMOOTH_QUARTZ;
                    hints.base = Blocks.POLISHED_DEEPSLATE;
                    hints.pilasters = true;
                    hints.noPortico = true;
                    hints.mansard = true;
                    hints.roofBlock = Blocks.RED_NETHER_BRICKS;
                    hints.roofStairs = Blocks.RED_NETHER_BRICK_STAIRS;
                    hints.roofSlab = Blocks.RED_NETHER_BRICK_SLAB;
                    hints.levels = 6;
                    hints.roomType = "class";
                    // which department sits on which floor is taken from the university's own site
                    hints.floorSigns = new String[][]{
                            {"БГМУ, корпус №7", "1 этаж", "Научная", "библиотека"},
                            {"2 этаж", "Биохимия <-", "(левое крыло)", "Фармакология"},
                            {"3 этаж", "Общая химия", "Мед. физика", "Патофизиология"},
                            {"4 этаж", "", "", ""},
                            {"5 этаж", "", "", ""},
                            {"6 этаж", "Кафедра", "иностранных", "языков"}};
                }
                case "bsmu_k2" -> {
                    // building 2, set back from ul. Zaki Validi behind a fenced court: pale yellow, white pilasters
                    hints.era = ru.ufamod.building.Style.Era.STALIN;
                    hints.wall = Blocks.SMOOTH_SANDSTONE;
                    hints.trim = Blocks.SMOOTH_QUARTZ;
                    hints.base = Blocks.LIGHT_GRAY_CONCRETE;
                    hints.pilasters = true;
                    hints.noPortico = true;
                    hints.pitched = Boolean.FALSE;
                    hints.levels = 3;
                    hints.roomType = "class";
                    hints.floorSigns = new String[][]{
                            {"БГМУ, корпус №2", "1 этаж", "Микробиология", "Гигиена ->"},
                            {"2 этаж", "Кафедра", "философии", ""},
                            {"3 этаж", "", "", ""}};
                }
                case "opera" -> {
                    // Aksakov People's House: red brick with white details and a pediment over the entrance
                    hints.era = ru.ufamod.building.Style.Era.STALIN;
                    hints.wall = Blocks.BRICKS;
                    hints.trim = Blocks.SMOOTH_QUARTZ;
                    hints.portico = true;
                    hints.pitched = Boolean.FALSE;
                    hints.roomType = "hall";
                }
                case "gostiny" -> {
                    // cream trading rows with white arcades under a red roof
                    hints.era = ru.ufamod.building.Style.Era.MERCHANT;
                    hints.wall = Blocks.SMOOTH_SANDSTONE;
                    hints.trim = Blocks.SMOOTH_QUARTZ;
                    hints.arcade = true;
                    hints.pitched = Boolean.TRUE;
                    hints.roofBlock = Blocks.BRICKS;
                    hints.roofStairs = Blocks.BRICK_STAIRS;
                    hints.roofSlab = Blocks.BRICK_SLAB;
                    hints.roomType = "shop";
                }
                case "bsmu_anatomy" -> {
                    // building 4: three storeys, pale yellow with white trim over a grey ground floor
                    hints.roomType = "anatomy";
                    hints.era = ru.ufamod.building.Style.Era.MERCHANT;
                    hints.wall = Blocks.SMOOTH_SANDSTONE;
                    hints.trim = Blocks.SMOOTH_QUARTZ;
                    hints.base = Blocks.LIGHT_GRAY_CONCRETE;
                    hints.pitched = Boolean.FALSE;
                    hints.floorSigns = new String[][]{
                            {"БГМУ, корпус №4", "Кафедра", "анатомии", "человека"},
                            {"2 этаж", "Анатомический", "музей", ""},
                            {"3 этаж", "", "", ""}};
                }
                case "dorm" -> {
                    // a plain cream nine-storey block, as it stands on ul. Mingazheva
                    hints.wall = Blocks.SMOOTH_SANDSTONE;
                    hints.plain = true;
                }
                case "bsmu_canteen" -> hints.roomType = "canteen";
                case "library" -> {
                    hints.roomType = "library";
                    hints.era = ru.ufamod.building.Style.Era.STALIN;
                    hints.wall = Blocks.SMOOTH_QUARTZ;
                    hints.trim = Blocks.POLISHED_ANDESITE;
                    hints.portico = true;
                }
                default -> { }
            }
        }
        BuildingGenerator g = BuildingGenerator.generate(city, b, hints);
        if (g == null) return null;
        if (role != null && !g.entrances.isEmpty()) {
            switch (role) {
                case "dorm" -> dorm(city, g);
                case "bsmu_main" -> bsmuMain(city, g);
                case "bsmu_k7" -> bsmuSeven(city, g);
                case "bsmu_anatomy" -> anatomy(city, g);
                case "bsmu_canteen" -> canteen(g);
                case "library" -> library(g);
                default -> { }
            }
        }
        return g;
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
        final Entrance door = e;
        java.util.List<RoomRef> candidates = new java.util.ArrayList<>();
        for (RoomRef r : g.rooms) if (r.storey() == storey && r.depth() >= 3) candidates.add(r);
        candidates.sort(java.util.Comparator.comparingInt(r -> Math.abs(r.ax() - door.x()) + Math.abs(r.az() - door.z())));
        boolean placed = false;
        for (RoomRef best : candidates) {
            int last = best.width() - 1, mid = best.width() / 2, back = best.depth() - 1;
            Direction up = best.out().getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
            BlockPos wardrobe = BuildingGenerator.roomCell(best, last, back);
            BlockPos fridge = BuildingGenerator.roomCell(best, 1, back);
            BlockPos wake = BuildingGenerator.roomCell(best, mid, back);
            BlockPos mate = BuildingGenerator.roomCell(best, Math.max(2, mid - 1), 1);
            // remember what the generator put there, in case the room turns out to be a dead end
            BlockPos[] touched = {wardrobe, wardrobe.above(), fridge, fridge.above(), mate};
            net.minecraft.world.level.block.state.BlockState[] before = new net.minecraft.world.level.block.state.BlockState[touched.length];
            for (int i = 0; i < touched.length; i++) before[i] = m.get(touched[i].getX(), touched[i].getY(), touched[i].getZ());
            m.set(wardrobe.getX(), wardrobe.getY(), wardrobe.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, up.getOpposite()));
            m.set(wardrobe.getX(), wardrobe.getY() + 1, wardrobe.getZ(), Blocks.AIR.defaultBlockState());
            m.set(fridge.getX(), fridge.getY(), fridge.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, up));
            m.set(fridge.getX(), fridge.getY() + 1, fridge.getZ(), Blocks.AIR.defaultBlockState());
            m.set(mate.getX(), mate.getY(), mate.getZ(), Blocks.AIR.defaultBlockState());
            // the player has to be able to walk from the bed to the street without jumping
            if (!ru.ufamod.building.WalkTest.canLeave(city, g, wake, false)) {
                for (int i = 0; i < touched.length; i++) if (before[i] != null) m.set(touched[i].getX(), touched[i].getY(), touched[i].getZ(), before[i]);
                continue;
            }
            m.chestItems(wardrobe.getX(), wardrobe.getY(), wardrobe.getZ(), Loot.of("wardrobe"));
            m.chestItems(fridge.getX(), fridge.getY(), fridge.getZ(), Loot.of("fridge"));
            MARKS.put("wake", wake);
            MARKS.put("bed", BuildingGenerator.roomCell(best, 1, 1));
            MARKS.put("wardrobe", wardrobe);
            MARKS.put("fridge", fridge);
            npc(m, mate, yawOf(best.out().getOpposite()), NpcRole.ROOMMATE, "Сосед Азамат", 1);
            placed = true;
            break;
        }
        if (!placed) {
            ru.ufamod.UfaMod.LOGGER.warn("No walkable dormitory room found; the story starts in the lobby");
            MARKS.put("wake", e.lobby());
        }
        MARKS.put("dorm_out", outside(city, e, 3, 0));
        MARKS.put("dorm_view", outside(city, e, 26, 0));
        MARKS.put("dorm_lobby", e.lobby());
        BlockPos hall = g.halls.get(g.entrances.indexOf(e));
        npc(m, g.spotNear(hall, 0), yawOf(e.out()), NpcRole.WATCHMAN, "Вахтёр тётя Фая", 1);
        BlockPos bus = outside(city, e, 6, 3);
        MARKS.put("bus_from", bus);
        npc(m, bus, yawOf(e.out().getOpposite()), NpcRole.DRIVER, "Водитель автобуса", 2);
    }

    // ------------------------------------------------------------------ BSMU, building 1 (ul. Lenina, 3)

    private static void bsmuMain(CityData city, BuildingGenerator g) {
        Model m = g.m;
        Entrance e = g.entrances.get(0);
        BlockPos hall = g.halls.get(0);
        MARKS.put("main_door", outside(city, e, 5, 0));
        MARKS.put("main_view", outside(city, e, 24, 0));
        npc(m, g.spotNear(hall, 0), yawOf(e.out()), NpcRole.WATCHMAN, "Вахтёр корпуса №1", 0);
        npc(m, g.spotNear(hall, 3), 0f, NpcRole.STUDENT, "Студент Айдар", 1);
        bsmuFront(city, g, e);
    }

    /**
     * The entrance of building 1 as it looks from the alley on ul. Lenina: the middle block of the facade
     * carries four white pillars two storeys high above a porch on banded piers, with a frieze and an attic.
     */
    private static void bsmuFront(CityData city, BuildingGenerator g, Entrance e) {
        Model m = g.m;
        Direction out = e.out(), side = out.getClockWise();
        int base = g.base, f1 = g.floorY[1], top = g.top;
        int[] piers = {-6, -2, 2, 6};
        int half = 7;
        // the whole front has to be one straight wall with free ground before it
        for (int k = -half; k <= half; k++) {
            int wx = e.x() + side.getStepX() * k, wz = e.z() + side.getStepZ() * k;
            if (city.building(wx, wz) != g.b.index) return;
            for (int f = 1; f <= 2; f++)
                if (city.building(wx + out.getStepX() * f, wz + out.getStepZ() * f) != 0) return;
        }
        var white = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        for (int k = -half; k <= half; k++) {
            boolean pier = false;
            for (int q : piers) if (q == k) pier = true;
            for (int f = 0; f <= 2; f++) {
                int x = e.x() + side.getStepX() * k + out.getStepX() * f, z = e.z() + side.getStepZ() * k + out.getStepZ() * f;
                if (f > 0) {
                    // the porch floor and its banded piers
                    m.set(x, base, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
                    for (int y = base + 1; y < f1; y++)
                        m.set(x, y, z, pier ? ((y - base) % 2 == 0 ? Blocks.SMOOTH_SANDSTONE : Blocks.BRICKS).defaultBlockState() : Blocks.AIR.defaultBlockState());
                    m.set(x, f1, z, white);
                    // the loggia above: pillars at the front edge, a balustrade between them
                    for (int y = f1 + 1; y < top; y++)
                        m.set(x, y, z, pier && f == 2 ? Blocks.QUARTZ_PILLAR.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    if (!pier && f == 2 && Math.abs(k) < 6) m.set(x, f1 + 1, z, Blocks.DIORITE_WALL.defaultBlockState());
                }
                // entablature: a white band, a grey frieze, a cornice and the attic
                m.set(x, top, z, white);
                m.set(x, top + 1, z, (f == 2 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_QUARTZ).defaultBlockState());
                m.set(x, top + 2, z, white);
                if (f <= 1 && Math.abs(k) < half) m.set(x, top + 3, z, white);
            }
            // the cornice overhangs
            int cx = e.x() + side.getStepX() * k + out.getStepX() * 3, cz = e.z() + side.getStepZ() * k + out.getStepZ() * 3;
            if (city.building(cx, cz) == 0) m.set(cx, top + 2, cz, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        }
        // two more doors in the side bays, where the lobby really is behind the wall
        for (int k : new int[]{-4, 4}) {
            int x = e.x() + side.getStepX() * k, z = e.z() + side.getStepZ() * k;
            int ix = x - out.getStepX(), iz = z - out.getStepZ();
            if (!m.isAir(ix, base + 1, iz) || !m.isAir(ix, base + 2, iz)) continue;
            var door = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, out.getOpposite());
            m.set(x, base + 1, z, door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
            m.set(x, base + 2, z, door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            m.set(x, base + 3, z, white);
        }
        // white statues beside the porch
        for (int k : new int[]{-8, 8}) {
            int x = e.x() + side.getStepX() * k + out.getStepX(), z = e.z() + side.getStepZ() * k + out.getStepZ();
            if (city.building(x, z) != 0 || city.building(x - out.getStepX(), z - out.getStepZ()) != g.b.index) continue;
            m.set(x, base + 1, z, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
            m.set(x, base + 2, z, Blocks.QUARTZ_PILLAR.defaultBlockState());
            m.set(x, base + 3, z, Blocks.DIORITE_WALL.defaultBlockState());
        }
        // the name on the middle piers
        for (int i = 0; i < 2; i++) {
            int k = i == 0 ? -2 : 2;
            int x = e.x() + side.getStepX() * k + out.getStepX() * 3, z = e.z() + side.getStepZ() * k + out.getStepZ() * 3;
            if (city.building(x, z) != 0) continue;
            if (i == 0) g.wallSignAt(x, base + 2, z, out, "БАШКИРСКИЙ", "ГОСУДАРСТВЕННЫЙ", "МЕДИЦИНСКИЙ", "УНИВЕРСИТЕТ");
            else g.wallSignAt(x, base + 2, z, out, "Корпус №1", "ул. Ленина, 3", "Построен", "в 1940 году");
        }
    }

    // ------------------------------------------------------------------ BSMU, building 7 (ul. Pushkina, 96/98)

    /** The biochemistry department really is here: second floor, left wing (seen from the street). */
    private static void bsmuSeven(CityData city, BuildingGenerator g) {
        Model m = g.m;
        Entrance e = g.entrances.get(0);
        for (Entrance c : g.entrances)
            if (c.stairs()) {
                e = c;
                break;
            }
        BlockPos hall = g.halls.get(g.entrances.indexOf(e));
        MARKS.put("bsmu_door", outside(city, e, 3, 0));
        MARKS.put("bsmu_view", outside(city, e, 30, 0));
        MARKS.put("bsmu_hall", hall);
        BlockPos guard = g.spotNear(hall, 0);
        MARKS.put("guard", guard);
        npc(m, guard, yawOf(e.out()), NpcRole.WATCHMAN, "Охранник Ринат", 0);
        npc(m, g.spotNear(hall, 3), 0f, NpcRole.STUDENT, "Студент Айдар", 1);
        npc(m, g.spotNear(hall, 6), 90f, NpcRole.STUDENT, "Студентка Алина", 0);
        BlockPos stop = outside(city, e, 9, 4);
        MARKS.put("bus_to", stop);
        npc(m, stop, yawOf(e.out().getOpposite()), NpcRole.DRIVER, "Водитель автобуса", 3);

        // a classroom of the department on the second floor, in the wing to the left of the entrance
        Direction left = e.out().getClockWise();   // on the left hand of someone facing the facade
        int storey = Math.min(1, g.levels - 1);
        final Entrance door = e;
        java.util.List<RoomRef> candidates = new java.util.ArrayList<>();
        for (RoomRef r : g.rooms) if (r.storey() == storey && r.depth() >= 4 && r.width() >= 7) candidates.add(r);
        candidates.sort(java.util.Comparator.comparingInt((RoomRef r) -> {
            int along = (r.ax() - door.x()) * left.getStepX() + (r.az() - door.z()) * left.getStepZ();
            // prefer the left wing, not too far from the stairs
            return along >= 6 ? along : 1000 - along;
        }));
        BlockPos desk = guard;
        for (RoomRef r : candidates) {
            int midV = r.depth() / 2;
            BlockPos lecturer = BuildingGenerator.roomCell(r, 2, midV);
            int[][] seats = {{4, 0, 0}, {5, r.depth() - 2, 1}, {6, 1, 0}, {4, r.depth() - 2, 1}};
            java.util.List<BlockPos> cells = new java.util.ArrayList<>();
            cells.add(lecturer);
            for (int[] seat : seats) cells.add(BuildingGenerator.roomCell(r, seat[0], seat[1]));
            net.minecraft.world.level.block.state.BlockState[] before = new net.minecraft.world.level.block.state.BlockState[cells.size() * 2];
            for (int i = 0; i < cells.size(); i++) {
                BlockPos c = cells.get(i);
                before[i * 2] = m.get(c.getX(), c.getY(), c.getZ());
                before[i * 2 + 1] = m.get(c.getX(), c.getY() + 1, c.getZ());
                m.set(c.getX(), c.getY(), c.getZ(), Blocks.AIR.defaultBlockState());
                m.set(c.getX(), c.getY() + 1, c.getZ(), Blocks.AIR.defaultBlockState());
            }
            if (!ru.ufamod.building.WalkTest.canLeave(city, g, lecturer, false)) {
                for (int i = 0; i < cells.size(); i++) {
                    BlockPos c = cells.get(i);
                    if (before[i * 2] != null) m.set(c.getX(), c.getY(), c.getZ(), before[i * 2]);
                    if (before[i * 2 + 1] != null) m.set(c.getX(), c.getY() + 1, c.getZ(), before[i * 2 + 1]);
                }
                continue;
            }
            desk = lecturer;
            float face = yawOf(BuildingGenerator.uPos(r.out()));
            String[] names = {"Студентка Лейсан", "Студент Данис", "Студентка Камила", "Студент Ильнур"};
            for (int i = 0; i < seats.length; i++) {
                BlockPos c = cells.get(i + 1);
                m.npc(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, yawOf(BuildingGenerator.uPos(r.out()).getOpposite()), NpcRole.STUDENT, names[i], seats[i][2], false);
            }
            npc(m, lecturer, face, NpcRole.LECTURER, "Доцент Хабибуллина", 0);
            MARKS.put("lecture_room", BuildingGenerator.roomCell(r, r.width() / 2, r.depth() - 1));
            break;
        }
        if (desk == guard) {
            ru.ufamod.UfaMod.LOGGER.warn("No walkable classroom on the second floor of building 7; the lecture is held in the lobby");
            desk = g.spotNear(hall, 8);
            npc(m, desk, yawOf(e.out()), NpcRole.LECTURER, "Доцент Хабибуллина", 0);
        }
        MARKS.put("lecture", desk);
    }

    // ------------------------------------------------------------------ the anatomy department (ul. Zaki Validi, 47 k1)

    private static void anatomy(CityData city, BuildingGenerator g) {
        Model m = g.m;
        Entrance e = g.entrances.get(0);
        BlockPos hall = g.halls.get(0);
        BlockPos prof = g.spotNear(hall, 1);
        MARKS.put("exam", prof);
        MARKS.put("exam_out", outside(city, e, 3, 0));
        MARKS.put("exam_hall", hall);
        npc(m, prof, yawOf(e.out()), NpcRole.PROFESSOR, "Профессор Латыпов", 0);
        npc(m, g.spotNear(hall, 3), 0f, NpcRole.STUDENT, "Одногруппник Ильнур", 1);
        npc(m, g.spotNear(hall, 5), 180f, NpcRole.STUDENT, "Одногруппница Алина", 0);
    }

    private static void canteen(BuildingGenerator g) {
        BlockPos hall = g.halls.get(0);
        BlockPos cook = g.spotNear(hall, 0);
        MARKS.put("canteen", cook);
        MARKS.put("canteen_hall", hall);
        npc(g.m, cook, yawOf(g.entrances.get(0).out()), NpcRole.COOK, "Повар Зульфия-апа", 0);
    }

    private static void library(BuildingGenerator g) {
        Model m = g.m;
        BlockPos hall = g.halls.get(0);
        BlockPos lib = g.spotNear(hall, 0);
        MARKS.put("library", lib);
        MARKS.put("library_hall", hall);
        npc(m, lib, yawOf(g.entrances.get(0).out()), NpcRole.LIBRARIAN, "Библиотекарь Роза Маратовна", 0);
        BlockPos lectern = g.spotNear(hall, 2);
        if (!lectern.equals(lib) && !lectern.equals(hall)) {
            m.set(lectern.getX(), lectern.getY(), lectern.getZ(), Blocks.LECTERN.defaultBlockState()
                    .setValue(LecternBlock.FACING, g.entrances.get(0).out()).setValue(LecternBlock.HAS_BOOK, true));
            m.lecternBook(lectern.getX(), lectern.getY(), lectern.getZ(), Loot.atlas());
        }
    }
}
