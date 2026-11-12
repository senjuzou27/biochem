package ru.ufamod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ru.ufamod.UfaMod;
import ru.ufamod.building.BuildingCache;
import ru.ufamod.building.Model;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;
import ru.ufamod.city.Ground;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.world.Stamps;
import ru.ufamod.world.UfaWorld;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Generates the dimension "ufamod:ufa": central Ufa at 1:1 from the bundled map data. */
public class UfaChunkGenerator extends ChunkGenerator {
    public static final Codec<UfaChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, UfaChunkGenerator::new));

    public UfaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        try {
            CityData city = CityData.get();
            ChunkSink sink = new ChunkSink(chunk);
            int cx = chunk.getPos().x, cz = chunk.getPos().z;
            StreetPainter.paint(city, sink);
            StreetObjects.paint(city, sink, cx, cz, Stamps::replacesPoint);
            for (BuildingInfo b : city.buildings(cx, cz)) {
                Model m = BuildingCache.get(b);
                if (m != null && m.intersectsChunk(cx, cz)) m.paste(sink);
            }
            for (Model m : Stamps.all()) if (m.intersectsChunk(cx, cz)) m.paste(sink);
            sink.finish();
        } catch (Throwable t) {
            UfaMod.LOGGER.error("Failed to generate chunk {} of Ufa", chunk.getPos(), t);
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        try {
            CityData city = CityData.get();
            int cx = region.getCenter().x, cz = region.getCenter().z;
            int x0 = cx << 4, z0 = cz << 4;
            for (BuildingInfo b : city.buildings(cx, cz)) {
                Model m = BuildingCache.get(b);
                if (m != null) spawn(region, m.npcs, x0, z0);
            }
            for (Model m : Stamps.all()) spawn(region, m.npcs, x0, z0);
            // passers-by on pavements and squares
            Random r = new Random(StreetPainter.hash(cx, cz, 4242));
            if (city.inside(x0, z0) && r.nextInt(3) == 0) {
                for (int tries = 0; tries < 6; tries++) {
                    int x = x0 + r.nextInt(16), z = z0 + r.nextInt(16);
                    int g = city.ground(x, z);
                    if ((g == Ground.SIDEWALK || g == Ground.PLAZA) && city.building(x, z) == 0) {
                        int y = city.height(x, z) + 1;
                        UfaWorld.spawnNpc(region, x + 0.5, y + 0.5, z + 0.5, r.nextFloat() * 360f, NpcRole.CITIZEN,
                                Names.citizen(r), r.nextInt(4), true);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            UfaMod.LOGGER.error("Failed to populate chunk {} of Ufa", region.getCenter(), t);
        }
    }

    private static void spawn(WorldGenRegion region, List<Model.NpcSpec> npcs, int x0, int z0) {
        for (Model.NpcSpec s : npcs) {
            if (s.x() < x0 || s.x() >= x0 + 16 || s.z() < z0 || s.z() >= z0 + 16) continue;
            UfaWorld.spawnNpc(region, s.x(), s.y(), s.z(), s.yaw(), s.role(), s.name(), s.variant(), s.wander());
        }
    }

    // ------------------------------------------------------------------ the rest of the vanilla pipeline is switched off

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
    }

    @Override
    public void createStructures(RegistryAccess access, ChunkGeneratorStructureState state, StructureManager structures, ChunkAccess chunk, StructureTemplateManager templates) {
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structures, ChunkAccess chunk) {
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome, StructureManager structures, MobCategory category, BlockPos pos) {
        return WeightedRandomList.create();
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return CityData.get().waterY + 1;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return CityData.get().defaultY + 1;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return CityData.get().height(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int y = CityData.get().height(x, z);
        int min = level.getMinBuildHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) states[i] = min + i <= y ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        return new NoiseColumn(min, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        CityData c = CityData.get();
        int b = c.building(pos.getX(), pos.getZ());
        BuildingInfo bi = c.buildingInfo(b);
        info.add("Ufa: ground " + c.ground(pos.getX(), pos.getZ()) + (bi == null ? "" : " building " + bi.address() + " " + bi.type + " osm " + bi.osm));
    }
}
