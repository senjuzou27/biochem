package ru.ufamod.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.worldgen.ChunkSink;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A box of blocks with absolute world coordinates: one building, monument or other prefab.
 * Cells that were never set are transparent when the model is pasted into a chunk.
 */
public class Model {
    public final int x0, y0, z0, sx, sy, sz;
    private final short[] data;
    private final List<BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Short> ids = new IdentityHashMap<>();
    public final Map<Long, CompoundTag> blockEntities = new HashMap<>();
    public final List<NpcSpec> npcs = new ArrayList<>();
    /** Named positions for the story (doors, rooms, desks). */
    public final Map<String, BlockPos> marks = new HashMap<>();

    public record NpcSpec(double x, double y, double z, float yaw, NpcRole role, String name, int variant, boolean wander) {}

    public Model(int x0, int y0, int z0, int sx, int sy, int sz) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.data = new short[sx * sy * sz];
        palette.add(null);
    }

    public int volume() {
        return data.length;
    }

    public boolean contains(int x, int y, int z) {
        return x >= x0 && y >= y0 && z >= z0 && x < x0 + sx && y < y0 + sy && z < z0 + sz;
    }

    private int index(int x, int y, int z) {
        return ((y - y0) * sz + (z - z0)) * sx + (x - x0);
    }

    public void set(int x, int y, int z, BlockState s) {
        if (!contains(x, y, z)) return;
        Short id = ids.get(s);
        if (id == null) {
            id = (short) palette.size();
            palette.add(s);
            ids.put(s, id);
        }
        int i = index(x, y, z);
        data[i] = id;
        if (!blockEntities.isEmpty()) blockEntities.remove(BlockPos.asLong(x, y, z));
    }

    @Nullable
    public BlockState get(int x, int y, int z) {
        if (!contains(x, y, z)) return null;
        return palette.get(data[index(x, y, z)]);
    }

    public boolean isAir(int x, int y, int z) {
        BlockState s = get(x, y, z);
        return s == null || s.isAir();
    }

    // ------------------------------------------------------------------ block entities

    public void blockEntity(int x, int y, int z, CompoundTag tag) {
        if (!contains(x, y, z)) return;
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        blockEntities.put(BlockPos.asLong(x, y, z), tag);
    }

    public static CompoundTag signTag(String... lines) {
        CompoundTag text = new CompoundTag();
        ListTag msgs = new ListTag();
        for (int i = 0; i < 4; i++) {
            String l = i < lines.length && lines[i] != null ? lines[i] : "";
            msgs.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(l))));
        }
        text.put("messages", msgs);
        text.putString("color", "black");
        text.putBoolean("has_glowing_text", false);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:sign");
        tag.put("front_text", text);
        tag.put("back_text", text.copy());
        tag.putBoolean("is_waxed", true);
        return tag;
    }

    public void signText(int x, int y, int z, String... lines) {
        blockEntity(x, y, z, signTag(lines));
    }

    public void chestItems(int x, int y, int z, List<ItemStack> items) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:chest");
        ListTag list = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            CompoundTag it = items.get(i).save(new CompoundTag());
            it.putByte("Slot", (byte) i);
            list.add(it);
        }
        tag.put("Items", list);
        blockEntity(x, y, z, tag);
    }

    public void lecternBook(int x, int y, int z, ItemStack book) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:lectern");
        tag.put("Book", book.save(new CompoundTag()));
        tag.putInt("Page", 0);
        blockEntity(x, y, z, tag);
    }

    public void npc(double x, double y, double z, float yaw, NpcRole role, String name, int variant, boolean wander) {
        npcs.add(new NpcSpec(x, y, z, yaw, role, name, variant, wander));
    }

    // ------------------------------------------------------------------ pasting

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int cx0 = chunkX << 4, cz0 = chunkZ << 4;
        return x0 < cx0 + 16 && x0 + sx > cx0 && z0 < cz0 + 16 && z0 + sz > cz0;
    }

    public void paste(ChunkSink sink) {
        int ax = Math.max(x0, sink.minX), bx = Math.min(x0 + sx, sink.minX + 16);
        int az = Math.max(z0, sink.minZ), bz = Math.min(z0 + sz, sink.minZ + 16);
        if (ax >= bx || az >= bz) return;
        for (int y = y0; y < y0 + sy; y++)
            for (int z = az; z < bz; z++) {
                int row = ((y - y0) * sz + (z - z0)) * sx;
                for (int x = ax; x < bx; x++) {
                    short id = data[row + x - x0];
                    if (id != 0) sink.set(x, y, z, palette.get(id));
                }
            }
        for (CompoundTag tag : blockEntities.values()) {
            int x = tag.getInt("x"), z = tag.getInt("z");
            if (x >= ax && x < bx && z >= az && z < bz) sink.blockEntity(x, tag.getInt("y"), z, tag.copy());
        }
    }
}
