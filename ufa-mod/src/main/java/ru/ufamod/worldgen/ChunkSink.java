package ru.ufamod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Writes blocks straight into a chunk that is being generated. Blocks whose shape depends on their neighbours
 * are queued for the game's own post-processing; every block entity gets its NBT.
 */
public final class ChunkSink {
    public final ChunkAccess chunk;
    public final int minX, minZ, minY, maxY;
    private final Map<Long, CompoundTag> blockEntities = new HashMap<>();
    private final Set<Long> shaped = new HashSet<>();

    public ChunkSink(ChunkAccess chunk) {
        this.chunk = chunk;
        this.minX = chunk.getPos().getMinBlockX();
        this.minZ = chunk.getPos().getMinBlockZ();
        this.minY = chunk.getMinBuildHeight();
        this.maxY = chunk.getMaxBuildHeight();
    }

    public boolean inside(int x, int z) {
        return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16;
    }

    public void set(int x, int y, int z, BlockState s) {
        if (y < minY || y >= maxY || !inside(x, z)) return;
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        section.setBlockState(x & 15, y & 15, z & 15, s, false);
        long key = BlockPos.asLong(x, y, z);
        Block b = s.getBlock();
        if (b instanceof StairBlock || b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof FenceGateBlock || b instanceof ChestBlock)
            shaped.add(key);
        else if (!shaped.isEmpty()) shaped.remove(key);
        if (s.hasBlockEntity()) {
            if (!blockEntities.containsKey(key)) blockEntities.put(key, null);
        } else if (!blockEntities.isEmpty()) blockEntities.remove(key);
    }

    public BlockState get(int x, int y, int z) {
        return chunk.getSection(chunk.getSectionIndex(y)).getBlockState(x & 15, y & 15, z & 15);
    }

    public void blockEntity(int x, int y, int z, CompoundTag tag) {
        if (y < minY || y >= maxY || !inside(x, z)) return;
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        blockEntities.put(BlockPos.asLong(x, y, z), tag);
    }

    /** Fills the vertical run [y1, y2] of one column. */
    public void column(int x, int z, int y1, int y2, BlockState s) {
        for (int y = Math.max(y1, minY); y <= Math.min(y2, maxY - 1); y++) set(x, y, z, s);
    }

    public void finish() {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long key : shaped) chunk.markPosForPostprocessing(p.set(key));
        for (Map.Entry<Long, CompoundTag> e : blockEntities.entrySet()) {
            p.set(e.getKey());
            BlockState state = get(p.getX(), p.getY(), p.getZ());
            if (!state.hasBlockEntity()) continue;
            CompoundTag tag = e.getValue();
            if (tag == null && state.getBlock() instanceof EntityBlock eb) {
                BlockEntity be = eb.newBlockEntity(p.immutable(), state);
                if (be == null) continue;
                tag = be.saveWithFullMetadata();
            }
            if (tag != null) chunk.setBlockEntityNbt(tag);
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
    }
}
