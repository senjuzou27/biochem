package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import ru.ufamod.UfaMod;

import javax.annotation.Nullable;

/**
 * The training camp with the military mods when they are installed: rifles from Timeless and Classics Zero
 * ("tacz"), equipment, vehicles, sandbags and targets from Superb Warfare ("superbwarfare").
 * Without them the camp keeps its own simple props.
 */
public final class Military {
    private Military() {}

    public static boolean guns() {
        return loaded("tacz");
    }

    public static boolean warfare() {
        return loaded("superbwarfare");
    }

    private static boolean loaded(String mod) {
        return ModList.get() != null && ModList.get().isLoaded(mod);
    }

    @Nullable
    private static Item item(String id) {
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return i == null || i == Items.AIR ? null : i;
    }

    @Nullable
    public static BlockState block(String id) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return b == null || b == Blocks.AIR ? null : b.defaultBlockState();
    }

    @Nullable
    private static EntityType<?> entity(String id) {
        ResourceLocation rl = new ResourceLocation(id);
        return ForgeRegistries.ENTITY_TYPES.containsKey(rl) ? ForgeRegistries.ENTITY_TYPES.getValue(rl) : null;
    }

    // ------------------------------------------------------------------ the soldier's kit

    /** The AK-74M, loaded, or null without TaCZ. */
    @Nullable
    public static ItemStack rifle() {
        Item gun = guns() ? item("tacz:modern_kinetic_gun") : null;
        if (gun == null) return null;
        ItemStack s = new ItemStack(gun);
        CompoundTag t = s.getOrCreateTag();
        t.putString("GunId", "tacz:ak74m");
        t.putString("GunFireMode", "SEMI");
        t.putInt("GunCurrentAmmoCount", 30);
        t.putBoolean("HasBulletInBarrel", true);
        return s;
    }

    /** 5.45x39 rounds for the rifle. */
    @Nullable
    public static ItemStack ammo(int count) {
        Item ammo = guns() ? item("tacz:ammo") : null;
        if (ammo == null) return null;
        ItemStack s = new ItemStack(ammo, count);
        s.getOrCreateTag().putString("AmmoId", "tacz:545x39");
        return s;
    }

    @Nullable
    public static ItemStack helmet() {
        Item i = warfare() ? item("superbwarfare:ru_helmet_6b47") : null;
        return i == null ? null : new ItemStack(i);
    }

    @Nullable
    public static ItemStack vest() {
        Item i = warfare() ? item("superbwarfare:ru_chest_6b43") : null;
        return i == null ? null : new ItemStack(i);
    }

    @Nullable
    public static ItemStack warfareItem(String id, int count) {
        Item i = warfare() ? item("superbwarfare:" + id) : null;
        return i == null ? null : new ItemStack(i, count);
    }

    // ------------------------------------------------------------------ vehicles and targets in the camp

    /** Vehicles parked between the parade ground and the trenches: camp x, z, yaw. */
    private static final Object[][] PARK = {
            {"superbwarfare:bmp_2", 72, 84, 90f},
            {"superbwarfare:bmp_2", 84, 84, 90f},
            {"superbwarfare:t_90a", 98, 85, 90f},
            {"superbwarfare:mi_28", 114, 88, 90f},
    };

    /** Places of the targets on the range: the posts of the plain camp. */
    public static final int[][] TARGETS = {{98, 58}, {106, 61}, {98, 64}, {106, 67}, {98, 70}};

    public static boolean isTarget(Entity e) {
        ResourceLocation k = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return k != null && k.toString().equals("superbwarfare:target");
    }

    /** Spawns what the camp needs from Superb Warfare; does nothing for things already there. */
    public static void setUpCamp(ServerLevel level) {
        if (!warfare()) return;
        AABB camp = new AABB(Plan.CAMP_OX, Plan.BASE_Y - 4, Plan.CAMP_OZ, Plan.CAMP_OX + Plan.CAMP_W, Plan.BASE_Y + 40, Plan.CAMP_OZ + Plan.CAMP_D);
        for (Object[] v : PARK) spawnOnce(level, camp, (String) v[0], (Integer) v[1], (Integer) v[2], (Float) v[3], 0);
        for (int[] t : TARGETS) spawnOnce(level, camp, "superbwarfare:target", t[0], t[1], -90f, 2);
    }

    private static void spawnOnce(ServerLevel level, AABB camp, String id, int cx, int cz, float yaw, int up) {
        EntityType<?> type = entity(id);
        if (type == null) return;
        double x = Plan.CAMP_OX + cx + 0.5, z = Plan.CAMP_OZ + cz + 0.5, y = Plan.BASE_Y + 1;
        AABB here = new AABB(x - 2, y - 2, z - 2, x + 2, y + 6, z + 2);
        if (!level.getEntities(type, here, e -> true).isEmpty()) return;
        level.getChunk(BlockPos.containing(x, y, z));
        Entity e = type.create(level);
        if (e == null) return;
        e.moveTo(x, y, z, yaw, 0f);
        e.setYHeadRot(yaw);
        if (up > 0) {
            // the targets stand on the old posts
            for (int k = 1; k <= up; k++) level.setBlock(BlockPos.containing(x, y + k - 1, z), Blocks.AIR.defaultBlockState(), 2);
        }
        if (!level.addFreshEntity(e)) UfaMod.LOGGER.warn("Could not place {} in the camp", id);
    }
}
