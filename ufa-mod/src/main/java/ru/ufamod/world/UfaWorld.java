package ru.ufamod.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.registry.ModEntities;
import ru.ufamod.worldgen.UfaChunkGenerator;

import javax.annotation.Nullable;

/** The "ufamod:ufa" dimension and its chunk generator. */
public final class UfaWorld {
    private UfaWorld() {}

    public static final ResourceKey<Level> UFA = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(UfaMod.MODID, "ufa"));

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, UfaMod.MODID);
    public static final RegistryObject<Codec<? extends ChunkGenerator>> UFA_GENERATOR = GENERATORS.register("ufa", () -> UfaChunkGenerator.CODEC);

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(UFA);
    }

    /** Works both in a live world and while a chunk is being generated. */
    @Nullable
    public static NpcEntity spawnNpc(ServerLevelAccessor level, double x, double y, double z, float yaw, NpcRole role, String name, int variant, boolean wander) {
        NpcEntity npc = ModEntities.NPC.get().create(level.getLevel());
        if (npc == null) return null;
        npc.moveTo(x, y, z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setRole(role);
        npc.setVariant(variant);
        npc.setWander(wander);
        npc.setCustomName(Component.literal(name));
        npc.setCustomNameVisible(role != NpcRole.CITIZEN);
        level.addFreshEntityWithPassengers(npc);
        return npc;
    }
}
