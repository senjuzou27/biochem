package ru.ufamod.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import ru.ufamod.city.CityData;
import ru.ufamod.city.Ground;

/** Terrain, river, pavements, road markings and fences: everything that is decided column by column. */
public final class StreetPainter {
    private StreetPainter() {}

    private static final BlockState STONE = Blocks.STONE.defaultBlockState(), DIRT = Blocks.DIRT.defaultBlockState(),
            BEDROCK = Blocks.BEDROCK.defaultBlockState(), WATER = Blocks.WATER.defaultBlockState(), AIR = Blocks.AIR.defaultBlockState();

    static int hash(int x, int z, int salt) {
        int h = x * 73856093 ^ z * 19349663 ^ salt * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    static BlockState slab(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    public static void paint(CityData city, ChunkSink sink) {
        for (int dz = 0; dz < 16; dz++)
            for (int dx = 0; dx < 16; dx++) column(city, sink, sink.minX + dx, sink.minZ + dz);
    }

    /** The y the player stands on above ground at (x, z): one above the ground block, a slab higher on sidewalks. */
    public static int standY(CityData city, int x, int z) {
        return city.height(x, z) + 1;
    }

    private static void column(CityData city, ChunkSink sink, int x, int z) {
        int y = city.height(x, z);
        int g = city.ground(x, z);
        int m = city.mark(x, z);
        int f = city.feat(x, z);
        int b = city.building(x, z);
        sink.set(x, sink.minY, z, BEDROCK);
        if (g == Ground.WATER && b == 0) {
            int depth = 5;
            if (city.ground(x + 1, z) != Ground.WATER || city.ground(x - 1, z) != Ground.WATER || city.ground(x, z + 1) != Ground.WATER || city.ground(x, z - 1) != Ground.WATER)
                depth = 1;
            else if (city.ground(x + 3, z) != Ground.WATER || city.ground(x - 3, z) != Ground.WATER || city.ground(x, z + 3) != Ground.WATER || city.ground(x, z - 3) != Ground.WATER)
                depth = 3;
            int bed = city.waterY - depth;
            sink.column(x, z, sink.minY + 1, bed - 3, STONE);
            sink.column(x, z, bed - 2, bed, (hash(x, z, 1) % 5 == 0 ? Blocks.GRAVEL : hash(x, z, 2) % 7 == 0 ? Blocks.CLAY : Blocks.SAND).defaultBlockState());
            sink.column(x, z, bed + 1, city.waterY, WATER);
            if (depth >= 3 && hash(x, z, 3) % 9 == 0) sink.set(x, bed + 1, z, Blocks.SEAGRASS.defaultBlockState());
            return;
        }
        sink.column(x, z, sink.minY + 1, y - 4, STONE);
        if (b != 0) {
            // the building model draws its own floor; give it a solid foundation
            sink.column(x, z, y - 3, y - 1, Blocks.STONE_BRICKS.defaultBlockState());
            return;
        }
        boolean paved = Ground.paved(g);
        sink.column(x, z, y - 3, y - 1, paved ? STONE : DIRT);
        int h = hash(x, z, 7);
        switch (g) {
            case Ground.ROAD, Ground.PARKING, Ground.YARD -> {
                BlockState s = Blocks.GRAY_CONCRETE.defaultBlockState();
                if (m == Ground.M_LINE || m == Ground.M_ZEBRA || m == Ground.M_PARKING) s = Blocks.WHITE_CONCRETE.defaultBlockState();
                else if (m == Ground.M_RAIL) s = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                else if (h % 997 == 0) s = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(); // manhole
                else if (h % 61 == 0) s = Blocks.GRAY_CONCRETE_POWDER.defaultBlockState();          // patched asphalt
                sink.set(x, y, z, s);
            }
            case Ground.SIDEWALK -> {
                sink.set(x, y, z, Blocks.SMOOTH_STONE.defaultBlockState());
                BlockState top;
                if (m == Ground.M_CURB) top = slab(Blocks.POLISHED_DIORITE_SLAB);
                else if (m == Ground.M_RAIL) top = slab(Blocks.POLISHED_DEEPSLATE_SLAB);
                else if (((x >> 1) + (z >> 1)) % 2 == 0) top = slab(Blocks.SMOOTH_STONE_SLAB);
                else top = h % 23 == 0 ? slab(Blocks.ANDESITE_SLAB) : slab(Blocks.POLISHED_ANDESITE_SLAB);
                sink.set(x, y + 1, z, top);
            }
            case Ground.PLAZA -> {
                Block p = ((x / 3 + z / 3) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE;
                if (Math.floorMod(x, 12) == 0 || Math.floorMod(z, 12) == 0) p = Blocks.POLISHED_GRANITE;
                if (m == Ground.M_RAIL) p = Blocks.POLISHED_DEEPSLATE;
                sink.set(x, y, z, p.defaultBlockState());
            }
            case Ground.STEPS -> sink.set(x, y, z, Blocks.STONE_BRICKS.defaultBlockState());
            case Ground.GARAGES -> sink.set(x, y, z, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            case Ground.PATH -> sink.set(x, y, z, (h % 5 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH).defaultBlockState());
            case Ground.SAND -> sink.set(x, y, z, Blocks.SAND.defaultBlockState());
            case Ground.DIRT -> sink.set(x, y, z, (h % 3 == 0 ? Blocks.GRAVEL : Blocks.COARSE_DIRT).defaultBlockState());
            case Ground.PITCH -> sink.set(x, y, z, ((x / 4) % 2 == 0 ? Blocks.GREEN_CONCRETE : Blocks.LIME_TERRACOTTA).defaultBlockState());
            case Ground.TRACK -> sink.set(x, y, z, Blocks.RED_TERRACOTTA.defaultBlockState());
            case Ground.PLAYGROUND -> {
                Block[] rub = {Blocks.RED_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER};
                sink.set(x, y, z, rub[hash(x >> 2, z >> 2, 9) % rub.length].defaultBlockState());
            }
            case Ground.FOUNTAIN -> {
                boolean rim = city.ground(x + 1, z) != Ground.FOUNTAIN || city.ground(x - 1, z) != Ground.FOUNTAIN
                        || city.ground(x, z + 1) != Ground.FOUNTAIN || city.ground(x, z - 1) != Ground.FOUNTAIN;
                if (rim) {
                    sink.set(x, y, z, Blocks.SMOOTH_QUARTZ.defaultBlockState());
                    sink.set(x, y + 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB));
                } else {
                    sink.set(x, y - 1, z, Blocks.PRISMARINE.defaultBlockState());
                    sink.set(x, y, z, WATER);
                    if (h % 29 == 0) {
                        sink.set(x, y - 1, z, Blocks.SEA_LANTERN.defaultBlockState());
                        sink.set(x, y + 1, z, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState());
                        sink.set(x, y + 2, z, Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState());
                    }
                }
            }
            case Ground.FLOWERBED -> {
                sink.set(x, y, z, Blocks.PODZOL.defaultBlockState());
                Block[] fl = {Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.PINK_TULIP, Blocks.WHITE_TULIP, Blocks.POPPY, Blocks.CORNFLOWER,
                        Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.DANDELION};
                if (h % 4 != 0) sink.set(x, y + 1, z, fl[hash(x >> 1, z >> 1, 11) % fl.length].defaultBlockState());
            }
            case Ground.FOREST -> {
                sink.set(x, y, z, (h % 11 == 0 ? Blocks.PODZOL : Blocks.GRASS_BLOCK).defaultBlockState());
                if (h % 5 == 0) sink.set(x, y + 1, z, Blocks.GRASS.defaultBlockState());
                else if (h % 17 == 0) sink.set(x, y + 1, z, Blocks.FERN.defaultBlockState());
                else if (h % 41 == 0) sink.set(x, y + 1, z, Blocks.LILY_OF_THE_VALLEY.defaultBlockState());
            }
            default -> {
                sink.set(x, y, z, Blocks.GRASS_BLOCK.defaultBlockState());
                if (city.inside(x, z)) {
                    if (h % 9 == 0) sink.set(x, y + 1, z, Blocks.GRASS.defaultBlockState());
                    else if (h % 151 == 0) sink.set(x, y + 1, z, Blocks.DANDELION.defaultBlockState());
                    else if (h % 173 == 0) sink.set(x, y + 1, z, Blocks.OXEYE_DAISY.defaultBlockState());
                }
            }
        }
        int top = g == Ground.SIDEWALK ? y + 1 : y + 1;
        switch (f) {
            case Ground.F_FENCE -> {
                if (h % 6 == 0) {
                    sink.set(x, top, z, Blocks.STONE_BRICKS.defaultBlockState());
                    sink.set(x, top + 1, z, slab(Blocks.STONE_BRICK_SLAB));
                } else sink.set(x, top, z, Blocks.IRON_BARS.defaultBlockState());
            }
            case Ground.F_WOODFENCE -> {
                sink.set(x, top, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                sink.set(x, top + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
            }
            case Ground.F_WALL -> {
                sink.set(x, top, z, Blocks.BRICKS.defaultBlockState());
                sink.set(x, top + 1, z, Blocks.BRICKS.defaultBlockState());
                sink.set(x, top + 2, z, slab(Blocks.BRICK_SLAB));
            }
            case Ground.F_HEDGE -> {
                BlockState l = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                sink.set(x, top, z, l);
                if (h % 3 != 0) sink.set(x, top + 1, z, l);
            }
            case Ground.F_RETAINING -> sink.set(x, top, z, Blocks.STONE_BRICKS.defaultBlockState());
            case Ground.F_GUARD -> sink.set(x, top, z, Blocks.IRON_BARS.defaultBlockState());
            default -> { }
        }
    }

    static BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }
}
