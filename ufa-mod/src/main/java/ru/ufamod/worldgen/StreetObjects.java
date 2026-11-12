package ru.ufamod.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import ru.ufamod.building.Model;
import ru.ufamod.city.CityData;
import ru.ufamod.city.Ground;
import ru.ufamod.city.MapPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Trees, street lights, benches, bus stops, traffic lights, street name plates, monuments and kiosks. */
public final class StreetObjects {
    private StreetObjects() {}

    public static void paint(CityData city, ChunkSink sink, int chunkX, int chunkZ, java.util.function.Predicate<MapPoint> skip) {
        List<MapPoint> pts = new ArrayList<>(city.points(chunkX, chunkZ));
        // trees first, so that lamps and signs are never buried in leaves
        pts.sort((a, b) -> Boolean.compare(!isTree(a), !isTree(b)));
        for (MapPoint p : pts) {
            if (skip.test(p)) continue;
            int y = city.height(p.x(), p.z());
            int g = city.ground(p.x(), p.z());
            int base = y + 1; // first block above the ground (replaces the pavement slab)
            Random r = new Random(StreetPainter.hash(p.x(), p.z(), 99));
            switch (p.kind()) {
                case TREE -> tree(sink, p.x(), base, p.z(), r, g);
                case BIRCH -> birch(sink, p.x(), base, p.z(), r, g);
                case CONIFER -> spruce(sink, p.x(), base, p.z(), r, g);
                case LAMP -> lamp(sink, p, base);
                case BENCH -> bench(sink, city, p.x(), base, p.z(), r);
                case WASTE -> sink.set(p.x(), base, p.z(), Blocks.CAULDRON.defaultBlockState());
                case BICYCLE -> {
                    for (int i = 0; i < 3; i++) sink.set(p.x() + i, base, p.z(), Blocks.IRON_BARS.defaultBlockState());
                }
                case BUS_STOP -> busStop(sink, p, base);
                case SIGNALS -> signals(sink, p, base);
                case STREET_SIGN -> streetSign(sink, p, base);
                case FOUNTAIN -> fountain(sink, p, y);
                case STATUE, BUST, STELE, MEMORIAL, ARTWORK -> monument(sink, city, p, base, r);
                case KIOSK -> kiosk(sink, city, p, base, r);
                default -> { }
            }
        }
    }

    private static boolean isTree(MapPoint p) {
        return p.kind() == MapPoint.Kind.TREE || p.kind() == MapPoint.Kind.BIRCH || p.kind() == MapPoint.Kind.CONIFER;
    }

    // ------------------------------------------------------------------ helpers

    private static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState slabTop(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static void sign(ChunkSink sink, int x, int y, int z, int rotation, String... lines) {
        sink.set(x, y, z, Blocks.BIRCH_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        sink.blockEntity(x, y, z, Model.signTag(lines));
    }

    private static void wallSign(ChunkSink sink, int x, int y, int z, Direction facing, String... lines) {
        sink.set(x, y, z, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        sink.blockEntity(x, y, z, Model.signTag(lines));
    }

    static int rotationOf(Direction d) {
        return switch (d) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            default -> 12;
        };
    }

    static Direction dirOf(float dx, float dz) {
        if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Direction from (x, z) towards the nearest paved cell, for things that should face the street. */
    static Direction towardsStreet(CityData city, int x, int z) {
        for (int rad = 2; rad <= 9; rad++)
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int g = city.ground(x + d.getStepX() * rad, z + d.getStepZ() * rad);
                if ((g == Ground.SIDEWALK || g == Ground.PLAZA || g == Ground.ROAD || g == Ground.PATH) && city.building(x + d.getStepX() * rad, z + d.getStepZ() * rad) == 0)
                    return d;
            }
        return Direction.SOUTH;
    }

    static List<String> wrap(String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word.length() > width ? word.substring(0, width) : word);
        }
        if (line.length() > 0) out.add(line.toString());
        while (out.size() > maxLines) out.remove(out.size() - 1);
        return out;
    }

    // ------------------------------------------------------------------ trees

    private static void pit(ChunkSink sink, int x, int y, int z, int g) {
        if (g == Ground.SIDEWALK || g == Ground.PLAZA) sink.set(x, y - 1, z, st(Blocks.PODZOL));
    }

    private static void blob(ChunkSink sink, int x, int y, int z, int rx, int ry, BlockState l, Random r) {
        for (int dy = -ry; dy <= ry; dy++)
            for (int dx = -rx; dx <= rx; dx++)
                for (int dz = -rx; dz <= rx; dz++) {
                    double d = (dx * dx + dz * dz) / (double) (rx * rx + 0.5) + (dy * dy) / (double) (ry * ry + 0.5);
                    if (d <= 1.0 && (d < 0.7 || r.nextInt(4) != 0)) sink.set(x + dx, y + dy, z + dz, l);
                }
    }

    private static void tree(ChunkSink sink, int x, int y, int z, Random r, int g) {
        pit(sink, x, y, z, g);
        int kind = r.nextInt(10);
        if (kind < 3) { // poplar: tall and narrow, common on Ufa streets
            int h = 9 + r.nextInt(4);
            sink.column(x, z, y, y + h - 3, st(Blocks.OAK_LOG));
            BlockState l = leaves(Blocks.OAK_LEAVES);
            for (int dy = 3; dy <= h; dy++) {
                int rad = dy < 5 || dy > h - 2 ? 1 : 2;
                for (int dx = -rad; dx <= rad; dx++)
                    for (int dz = -rad; dz <= rad; dz++)
                        if ((dx * dx + dz * dz <= rad * rad) && !(dx == 0 && dz == 0 && dy <= h - 3) && r.nextInt(6) != 0) sink.set(x + dx, y + dy, z + dz, l);
            }
            return;
        }
        boolean linden = kind < 7;
        Block log = linden ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG;
        BlockState l = leaves(linden ? Blocks.DARK_OAK_LEAVES : Blocks.OAK_LEAVES);
        int h = 5 + r.nextInt(3);
        sink.column(x, z, y, y + h, st(log));
        // a couple of boughs
        for (int i = 0; i < 2; i++) {
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            sink.set(x + d.getStepX(), y + h - 1 - i, z + d.getStepZ(), StreetPainter.log(log, d.getAxis()));
        }
        blob(sink, x, y + h, z, 3, 2, l, r);
        sink.column(x, z, y, y + h, st(log));
    }

    private static void birch(ChunkSink sink, int x, int y, int z, Random r, int g) {
        pit(sink, x, y, z, g);
        int h = 7 + r.nextInt(3);
        sink.column(x, z, y, y + h, st(Blocks.BIRCH_LOG));
        blob(sink, x, y + h - 1, z, 2, 3, leaves(Blocks.BIRCH_LEAVES), r);
        sink.column(x, z, y, y + h - 1, st(Blocks.BIRCH_LOG));
    }

    private static void spruce(ChunkSink sink, int x, int y, int z, Random r, int g) {
        pit(sink, x, y, z, g);
        int h = 9 + r.nextInt(5);
        BlockState l = leaves(Blocks.SPRUCE_LEAVES);
        for (int dy = 2; dy <= h + 1; dy++) {
            int fromTop = h + 1 - dy;
            int rad = Math.min(3, fromTop == 0 ? 0 : (fromTop % 2 == 1 ? 1 + fromTop / 4 : fromTop / 3 + 1));
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= rad + 1 && !(rad > 1 && Math.abs(dx) == rad && Math.abs(dz) == rad))
                        sink.set(x + dx, y + dy, z + dz, l);
        }
        sink.column(x, z, y, y + h, st(Blocks.SPRUCE_LOG));
    }

    // ------------------------------------------------------------------ street furniture

    private static void lamp(ChunkSink sink, MapPoint p, int y) {
        int x = p.x(), z = p.z();
        if (p.dx() == 0 && p.dz() == 0) { // park lamp
            sink.column(x, z, y, y + 2, st(Blocks.POLISHED_DEEPSLATE_WALL));
            sink.set(x, y + 3, z, st(Blocks.LANTERN));
            return;
        }
        Direction d = dirOf(p.dx(), p.dz());
        sink.column(x, z, y, y + 6, st(Blocks.POLISHED_DEEPSLATE_WALL));
        sink.set(x + d.getStepX(), y + 6, z + d.getStepZ(), slabTop(Blocks.POLISHED_DEEPSLATE_SLAB));
        sink.set(x + 2 * d.getStepX(), y + 6, z + 2 * d.getStepZ(), st(Blocks.OCHRE_FROGLIGHT));
    }

    private static void bench(ChunkSink sink, CityData city, int x, int y, int z, Random r) {
        Direction face = towardsStreet(city, x, z);
        Direction side = face.getClockWise();
        BlockState s = stair(Blocks.SPRUCE_STAIRS, face.getOpposite());
        sink.set(x, y, z, s);
        sink.set(x + side.getStepX(), y, z + side.getStepZ(), s);
        sink.set(x - side.getStepX(), y, z - side.getStepZ(), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, side.getOpposite()).setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true));
        if (r.nextInt(3) == 0) sink.set(x + 2 * side.getStepX(), y, z + 2 * side.getStepZ(), st(Blocks.CAULDRON));
    }

    private static void busStop(ChunkSink sink, MapPoint p, int y) {
        Direction toRoad = dirOf(p.dx(), p.dz());
        if (p.dx() == 0 && p.dz() == 0) toRoad = Direction.SOUTH;
        Direction along = toRoad.getClockWise();
        Direction back = toRoad.getOpposite();
        int x = p.x(), z = p.z();
        for (int i = -2; i <= 2; i++) {
            int ax = x + along.getStepX() * i, az = z + along.getStepZ() * i;
            int bx = ax + back.getStepX(), bz = az + back.getStepZ();
            // back wall of glass, posts at the ends, a roof over both rows
            if (Math.abs(i) == 2) sink.column(bx, bz, y, y + 2, st(Blocks.POLISHED_DEEPSLATE_WALL));
            else sink.column(bx, bz, y, y + 2, st(Blocks.GLASS_PANE));
            if (Math.abs(i) == 2) sink.column(ax, az, y, y + 2, st(Blocks.POLISHED_DEEPSLATE_WALL));
            sink.set(ax, y + 3, az, slabTop(Blocks.SMOOTH_STONE_SLAB));
            sink.set(bx, y + 3, bz, slabTop(Blocks.SMOOTH_STONE_SLAB));
            if (Math.abs(i) <= 1) sink.set(ax, y, az, stair(Blocks.SPRUCE_STAIRS, back));
        }
        String name = p.name().isEmpty() ? "Остановка" : p.name();
        List<String> l = wrap("«" + name + "»", 15, 2);
        int sx = x + along.getStepX() * 3, sz = z + along.getStepZ() * 3;
        sink.column(sx, sz, y, y + 1, st(Blocks.POLISHED_DEEPSLATE_WALL));
        sign(sink, sx, y + 2, sz, rotationOf(toRoad), "Остановка", l.size() > 0 ? l.get(0) : "", l.size() > 1 ? l.get(1) : "", "Автобус · Тролл.");
    }

    private static void signals(ChunkSink sink, MapPoint p, int y) {
        Direction d = dirOf(p.dx(), p.dz());
        int x = p.x(), z = p.z();
        sink.column(x, z, y, y + 2, st(Blocks.POLISHED_DEEPSLATE_WALL));
        sink.set(x, y + 3, z, st(Blocks.VERDANT_FROGLIGHT));
        sink.set(x, y + 4, z, st(Blocks.OCHRE_FROGLIGHT));
        sink.set(x, y + 5, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        sink.set(x, y + 6, z, slabTop(Blocks.POLISHED_DEEPSLATE_SLAB));
        // pedestrian button box on the pole
        sink.set(x - d.getStepX(), y + 1, z - d.getStepZ(), Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(net.minecraft.world.level.block.ButtonBlock.FACING, d.getOpposite()));
    }

    private static void streetSign(ChunkSink sink, MapPoint p, int y) {
        int x = p.x(), z = p.z();
        sink.column(x, z, y, y + 1, st(Blocks.POLISHED_DEEPSLATE_WALL));
        String a = ru.ufamod.city.BuildingInfo.shortStreet(p.name());
        String b = p.name2() == null ? "" : ru.ufamod.city.BuildingInfo.shortStreet(p.name2());
        sign(sink, x, y + 2, z, StreetPainter.hash(x, z, 5) % 16, a.length() > 15 ? a.substring(0, 15) : a, "↔", b.length() > 15 ? b.substring(0, 15) : b, "");
    }

    private static void fountain(ChunkSink sink, MapPoint p, int y) {
        int x = p.x(), z = p.z();
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                int d = dx * dx + dz * dz;
                if (d > 18) continue;
                sink.set(x + dx, y, z + dz, st(Blocks.PRISMARINE_BRICKS));
                if (d > 11) {
                    sink.set(x + dx, y + 1, z + dz, st(Blocks.SMOOTH_QUARTZ));
                    sink.set(x + dx, y + 2, z + dz, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
                } else sink.set(x + dx, y + 1, z + dz, Blocks.WATER.defaultBlockState());
            }
        sink.column(x, z, y + 1, y + 2, st(Blocks.QUARTZ_PILLAR));
        sink.set(x, y + 3, z, st(Blocks.SMOOTH_QUARTZ));
        for (Direction d : Direction.Plane.HORIZONTAL) sink.set(x + d.getStepX(), y + 3, z + d.getStepZ(), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        sink.column(x, z, y + 4, y + 5, st(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE));
        sink.set(x, y, z, st(Blocks.SEA_LANTERN));
        if (!p.name().isEmpty()) {
            List<String> l = wrap(p.name(), 15, 3);
            sign(sink, x, y + 1, z - 5, 8, l.size() > 0 ? l.get(0) : "", l.size() > 1 ? l.get(1) : "", l.size() > 2 ? l.get(2) : "", "");
        }
    }

    private static void monument(ChunkSink sink, CityData city, MapPoint p, int y, Random r) {
        int x = p.x(), z = p.z();
        Direction face = towardsStreet(city, x, z);
        BlockState ped = st(Blocks.POLISHED_GRANITE), bronze = st(r.nextBoolean() ? Blocks.POLISHED_BLACKSTONE : Blocks.WAXED_EXPOSED_COPPER);
        List<String> l = wrap(p.name(), 15, 4);
        String[] lines = {l.size() > 0 ? l.get(0) : "", l.size() > 1 ? l.get(1) : "", l.size() > 2 ? l.get(2) : "", l.size() > 3 ? l.get(3) : ""};
        int fx = face.getStepX(), fz = face.getStepZ();
        switch (p.kind()) {
            case BUST -> {
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) sink.set(x + dx, y, z + dz, st(Blocks.POLISHED_ANDESITE));
                sink.column(x, z, y + 1, y + 2, ped);
                sink.set(x, y + 3, z, bronze);
                sink.set(x, y + 4, z, Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rotationOf(face)));
                wallSign(sink, x + fx, y + 2, z + fz, face, lines);
            }
            case STELE -> {
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++) sink.set(x + dx, y, z + dz, st(Blocks.POLISHED_ANDESITE));
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) sink.set(x + dx, y + 1, z + dz, ped);
                sink.column(x, z, y + 2, y + 9, st(Blocks.SMOOTH_QUARTZ));
                sink.set(x, y + 10, z, st(Blocks.GOLD_BLOCK));
                wallSign(sink, x + 2 * fx, y + 1, z + 2 * fz, face, lines);
            }
            case MEMORIAL -> {
                Direction side = face.getClockWise();
                for (int i = -2; i <= 2; i++) {
                    int ax = x + side.getStepX() * i, az = z + side.getStepZ() * i;
                    sink.column(ax, az, y, y + 1, ped);
                    sink.set(ax, y + 2, az, Blocks.POLISHED_GRANITE_SLAB.defaultBlockState());
                    sink.set(ax + fx, y, az + fz, Blocks.RED_TULIP.defaultBlockState());
                    sink.set(ax + fx, y - 1, az + fz, st(Blocks.PODZOL));
                }
                wallSign(sink, x + fx, y + 1, z + fz, face, lines);
                String low = p.name().toLowerCase();
                if (low.contains("павш") || low.contains("геро") || low.contains("побед") || low.contains("войн")) {
                    sink.set(x + 2 * fx, y, z + 2 * fz, st(Blocks.CAMPFIRE));
                }
            }
            case ARTWORK -> {
                Block[] col = {Blocks.RED_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE};
                int h = 2 + r.nextInt(3);
                for (int i = 0; i < h; i++) {
                    sink.set(x + (i % 2), y + i, z, st(col[r.nextInt(col.length)]));
                    if (r.nextBoolean()) sink.set(x, y + i, z + 1, st(col[r.nextInt(col.length)]));
                }
                if (!p.name().isEmpty()) sign(sink, x + 2 * fx, y, z + 2 * fz, rotationOf(face), lines);
            }
            default -> statue(sink, x, y, z, face, ped, bronze, lines, r);
        }
    }

    /** A standing figure about five metres tall on a granite pedestal. */
    static void statue(ChunkSink sink, int x, int y, int z, Direction face, BlockState ped, BlockState bronze, String[] lines, Random r) {
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) sink.set(x + dx, y, z + dz, st(Blocks.POLISHED_ANDESITE));
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                sink.set(x + dx, y + 1, z + dz, ped);
                sink.set(x + dx, y + 2, z + dz, ped);
            }
        Direction side = face.getClockWise();
        int sx = side.getStepX(), sz = side.getStepZ();
        int b = y + 3;
        // legs
        sink.column(x + sx, z + sz, b, b + 1, bronze);
        sink.column(x - sx, z - sz, b, b + 1, bronze);
        // coat, shoulders and head
        sink.column(x, z, b + 1, b + 3, bronze);
        sink.set(x + sx, b + 2, z + sz, bronze);
        sink.set(x - sx, b + 2, z - sz, bronze);
        sink.set(x + sx, b + 3, z + sz, bronze);
        sink.set(x - sx, b + 3, z - sz, bronze);
        sink.set(x, b + 4, z, bronze);
        // one arm stretched forward
        int arm = r.nextBoolean() ? 1 : -1;
        sink.set(x + arm * sx + face.getStepX(), b + 3, z + arm * sz + face.getStepZ(), bronze);
        wallSign(sink, x + 2 * face.getStepX(), y + 1, z + 2 * face.getStepZ(), face, lines);
    }

    private static void kiosk(ChunkSink sink, CityData city, MapPoint p, int y, Random r) {
        int x = p.x(), z = p.z();
        Direction face = towardsStreet(city, x, z);
        Block[] wall = {Blocks.WHITE_CONCRETE, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA};
        Block[] roof = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.ORANGE_CONCRETE};
        BlockState w = st(wall[r.nextInt(wall.length)]), rf = st(roof[r.nextInt(roof.length)]);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                sink.set(x + dx, y - 1, z + dz, st(Blocks.SMOOTH_STONE));
                if (edge) sink.column(x + dx, z + dz, y, y + 2, w);
                else sink.column(x, z, y, y + 2, Blocks.AIR.defaultBlockState());
                sink.set(x + dx, y + 3, z + dz, rf);
            }
        int fx = x + face.getStepX(), fz = z + face.getStepZ();
        sink.set(fx, y, fz, st(Blocks.SMOOTH_QUARTZ));
        sink.set(fx, y + 1, fz, st(Blocks.GLASS_PANE));
        sink.set(fx, y + 2, fz, st(Blocks.GLASS_PANE));
        sink.set(x, y + 2, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        String kind = switch (p.shop() == null ? "" : p.shop()) {
            case "cafe" -> "Кафе";
            case "fast_food" -> "Фастфуд";
            case "pharmacy" -> "Аптека";
            case "newsagent", "kiosk" -> "Пресса";
            case "bakery" -> "Пекарня";
            case "florist" -> "Цветы";
            case "ice_cream" -> "Мороженое";
            case "atm" -> "Банкомат";
            default -> "Киоск";
        };
        List<String> l = wrap(p.name().isEmpty() ? kind : p.name(), 15, 2);
        int sx = fx + face.getStepX(), sz = fz + face.getStepZ();
        wallSign(sink, fx, y + 3, fz, face, kind, l.size() > 0 && !p.name().isEmpty() ? l.get(0) : "", l.size() > 1 ? l.get(1) : "", "");
        sink.set(fx, y + 3, fz, rf);
        wallSign(sink, sx, y + 3, sz, face, kind, l.size() > 0 && !p.name().isEmpty() ? l.get(0) : "", l.size() > 1 ? l.get(1) : "", "");
    }

    static BlockState stairTop(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.TOP);
    }
}
