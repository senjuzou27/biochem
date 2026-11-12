package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.registry.ModEntities;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Iterator;

/** The "ufamod:ufa" dimension: builds the city once per world and populates it with NPCs. */
@Mod.EventBusSubscriber(modid = UfaMod.MODID)
public final class UfaWorld {
    private UfaWorld() {}

    public static final ResourceKey<Level> UFA = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(UfaMod.MODID, "ufa"));

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(UFA);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = level(server);
        if (level == null) {
            UfaMod.LOGGER.error("Dimension {} is missing; the city of Ufa cannot be built", UFA.location());
            return;
        }
        UfaData data = UfaData.get(level);
        String dump = System.getProperty("ufamod.dump");
        if (data.built && dump == null) return;

        long t0 = System.currentTimeMillis();
        Canvas city = CityBuilder.buildCity();
        Canvas camp = CampBuilder.buildCamp();
        UfaMod.LOGGER.info("Ufa drawn in {} ms", System.currentTimeMillis() - t0);
        if (dump != null) {
            try {
                MapRenderer.dumpAll(city, camp, new File(dump));
            } catch (Exception e) {
                UfaMod.LOGGER.error("Map dump failed", e);
            }
        }
        if (!data.built) {
            long t1 = System.currentTimeMillis();
            int blocks = CityPaster.paste(level, city) + CityPaster.paste(level, camp);
            data.addNpcs(city);
            data.addNpcs(camp);
            data.built = true;
            data.setDirty();
            UfaMod.LOGGER.info("Ufa built: {} blocks placed in {} ms, {} NPCs queued", blocks, System.currentTimeMillis() - t1, data.pendingNpcs.size());
        }
        if (Boolean.getBoolean("ufamod.exitAfterBuild")) server.halt(false);
    }

    /** NPCs are spawned lazily, when the chunk they belong to is loaded and ticking. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        ServerLevel level = level(event.getServer());
        if (level == null || level.players().isEmpty()) return;
        UfaData data = UfaData.get(level);
        if (data.pendingNpcs.isEmpty()) return;
        Iterator<CompoundTag> it = data.pendingNpcs.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            CompoundTag t = it.next();
            BlockPos pos = BlockPos.containing(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
            if (!level.isLoaded(pos) || !level.isPositionEntityTicking(pos)) continue;
            spawnNpc(level, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"),
                    NpcRole.byId(t.getInt("role")), t.getString("name"), t.getInt("variant"), t.getBoolean("wander"));
            it.remove();
            changed = true;
        }
        if (changed) data.setDirty();
    }

    @Nullable
    public static NpcEntity spawnNpc(ServerLevel level, double x, double y, double z, float yaw, NpcRole role, String name, int variant, boolean wander) {
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc == null) return null;
        npc.moveTo(x, y, z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setRole(role);
        npc.setVariant(variant);
        npc.setWander(wander);
        npc.setCustomName(Component.literal(name));
        npc.setCustomNameVisible(true);
        level.addFreshEntity(npc);
        return npc;
    }
}
