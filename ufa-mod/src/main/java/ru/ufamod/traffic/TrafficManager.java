package ru.ufamod.traffic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.ufamod.block.TrafficLightBlock;
import ru.ufamod.city.CityData;
import ru.ufamod.city.MapPoint;
import ru.ufamod.city.RoadGraph;
import ru.ufamod.entity.VehicleEntity;
import ru.ufamod.registry.ModBlocks;
import ru.ufamod.registry.ModEntities;
import ru.ufamod.world.UfaWorld;

import javax.annotation.Nullable;
import java.util.Random;

/** Keeps some traffic around every player in the city and switches the traffic lights. */
public final class TrafficManager {
    public static final int[] COLOURS = {0xEDEDED, 0xEDEDED, 0xC9CCD1, 0xC9CCD1, 0x2A2C30, 0x2A2C30, 0x7A7D82, 0x1F4E9E, 0x9E2A24, 0x2E6E5A, 0x6B4A33};
    private static final int WANTED = 14;
    private final Random rnd = new Random();
    private int lastState = -1;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != UfaWorld.UFA) return;
        long time = level.getGameTime();
        int state = Signals.state(time);
        boolean refresh = time % 100 == 7;
        if (state != lastState || refresh) {
            lastState = state;
            for (ServerPlayer p : level.players()) switchLights(level, p, state);
        }
        if (time % 20 == 3) for (ServerPlayer p : level.players()) keepTraffic(level, p);
    }

    private static boolean inCity(ServerPlayer p) {
        CityData city = CityData.get();
        return p.getX() > city.x0 && p.getX() < city.x0 + city.width && p.getZ() > city.z0 && p.getZ() < city.z0 + city.depth;
    }

    private void switchLights(ServerLevel level, ServerPlayer p, int state) {
        if (!inCity(p)) return;
        CityData city = CityData.get();
        BlockState s = ModBlocks.TRAFFIC_LIGHT.get().defaultBlockState().setValue(TrafficLightBlock.STATE, state);
        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = pcx - 7; cx <= pcx + 7; cx++)
            for (int cz = pcz - 7; cz <= pcz + 7; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (MapPoint mp : city.points(cx, cz)) {
                    if (mp.kind() != MapPoint.Kind.SIGNALS || mp.x() >> 4 != cx || mp.z() >> 4 != cz) continue;
                    pos.set(mp.x(), city.height(mp.x(), mp.z()) + 4, mp.z());
                    BlockState cur = level.getBlockState(pos);
                    if (cur.is(ModBlocks.TRAFFIC_LIGHT.get()) && cur != s) level.setBlock(pos, s, 2);
                }
            }
    }

    private void keepTraffic(ServerLevel level, ServerPlayer p) {
        if (!inCity(p)) return;
        int around = level.getEntitiesOfClass(VehicleEntity.class, new AABB(p.blockPosition()).inflate(150, 60, 150)).size();
        for (int i = around; i < WANTED && i < around + 3; i++) spawn(level, p);
    }

    private void spawn(ServerLevel level, ServerPlayer p) {
        RoadGraph g = CityData.get().graph;
        int node = g.randomNear(p.getX(), p.getZ(), 55, 120, rnd);
        if (node < 0 || g.out(node).length == 0) return;
        if (!level.isLoaded(new BlockPos(g.x[node], p.getBlockY(), g.z[node]))) return;
        int he = g.out(node)[rnd.nextInt(g.out(node).length)];
        // not on top of another vehicle
        if (!level.getEntitiesOfClass(VehicleEntity.class, new AABB(g.x[node] - 9, -64, g.z[node] - 9, g.x[node] + 9, 320, g.z[node] + 9)).isEmpty()) return;
        int roll = rnd.nextInt(100);
        int kind = roll < 80 ? VehicleEntity.CAR : roll < 92 ? VehicleEntity.MINIBUS : VehicleEntity.BUS;
        int colour = kind == VehicleEntity.CAR ? COLOURS[rnd.nextInt(COLOURS.length)] : kind == VehicleEntity.MINIBUS ? (rnd.nextBoolean() ? 0xF2C230 : 0xEDEDED) : 0xEDEDED;
        create(level, kind, colour, he);
    }

    @Nullable
    public static VehicleEntity create(ServerLevel level, int kind, int colour, int halfEdge) {
        VehicleEntity v = ModEntities.VEHICLE.get().create(level);
        if (v == null) return null;
        v.setLook(kind, colour);
        v.placeOn(halfEdge, 0);
        level.addFreshEntity(v);
        return v;
    }

    /** Starts a bus ride for the player from where they stand to a place; false when no way was found. */
    public static boolean ride(ServerPlayer player, BlockPos dest) {
        ServerLevel level = player.serverLevel();
        RoadGraph g = CityData.get().graph;
        int from = g.nearest(player.getX(), player.getZ(), 120), to = g.nearest(dest.getX(), dest.getZ(), 400);
        if (from < 0 || to < 0 || from == to) return false;
        int[] route = g.route(from, to);
        if (route == null || route.length == 0) return false;
        VehicleEntity bus = ModEntities.VEHICLE.get().create(level);
        if (bus == null) return false;
        bus.setLook(VehicleEntity.BUS, 0xEDEDED);
        bus.setRoute(route, dest);
        level.addFreshEntity(bus);
        return player.startRiding(bus, true);
    }
}
