package ru.ufamod.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Remembers that the city has been built and which NPCs still have to be spawned. */
public class UfaData extends SavedData {
    public boolean built;
    public final List<CompoundTag> pendingNpcs = new ArrayList<>();

    public static UfaData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(UfaData::load, UfaData::new, "ufamod_city");
    }

    public static UfaData load(CompoundTag tag) {
        UfaData data = new UfaData();
        data.built = tag.getBoolean("built");
        ListTag list = tag.getList("npcs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) data.pendingNpcs.add(list.getCompound(i));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("built", built);
        ListTag list = new ListTag();
        list.addAll(pendingNpcs);
        tag.put("npcs", list);
        return tag;
    }

    public void addNpcs(Canvas c) {
        for (Canvas.NpcSpec s : c.npcs) {
            CompoundTag t = new CompoundTag();
            t.putDouble("x", c.ox + s.x());
            t.putDouble("y", c.baseY + s.y());
            t.putDouble("z", c.oz + s.z());
            t.putFloat("yaw", s.yaw());
            t.putInt("role", s.role().ordinal());
            t.putString("name", s.name());
            t.putInt("variant", s.variant());
            t.putBoolean("wander", s.wander());
            pendingNpcs.add(t);
        }
        setDirty();
    }
}
