package ru.ufamod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import ru.ufamod.city.CityData;
import ru.ufamod.city.RoadGraph;
import ru.ufamod.traffic.Signals;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A car, a minibus or a bus that drives along the streets of the map: keeps to the right, stops at red
 * lights, behind other vehicles and in front of people. A bus on a route carries the player to a stop.
 */
public class VehicleEntity extends Entity {
    public static final int CAR = 0, MINIBUS = 1, BUS = 2;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BRAKING = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);

    // server side: where on the network the vehicle is
    private int halfEdge = -1;
    private double along;
    private double speed;
    private int[] route;
    private int routeIndex;
    @Nullable
    private BlockPos destination;
    private boolean blocked;
    private int stuck;
    private int lonely;

    // client side: smooth movement between position updates
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    public VehicleEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, CAR);
        entityData.define(COLOUR, 0xE8E8E8);
        entityData.define(BRAKING, false);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public int colour() {
        return entityData.get(COLOUR);
    }

    public boolean braking() {
        return entityData.get(BRAKING);
    }

    public void setLook(int kind, int colour) {
        entityData.set(KIND, kind);
        entityData.set(COLOUR, colour);
    }

    public double halfLength() {
        return switch (kind()) {
            case BUS -> 5.2;
            case MINIBUS -> 3.0;
            default -> 2.1;
        };
    }

    // ------------------------------------------------------------------ driving

    /** Puts the vehicle at the start of a half-edge of the street network. */
    public void placeOn(int he, double offset) {
        halfEdge = he;
        along = offset;
        Vec3 p = lanePoint();
        float yaw = heading();
        moveTo(p.x, groundY(p.x, p.z), p.z, yaw, 0);
    }

    /** Sends the vehicle along a route; at its end the passengers are set down at the destination. */
    public void setRoute(int[] halfEdges, BlockPos dest) {
        route = halfEdges;
        routeIndex = 0;
        destination = dest;
        placeOn(halfEdges[0], 0);
    }

    public boolean onRoute() {
        return route != null;
    }

    private static double groundY(double x, double z) {
        return CityData.get().height(Mth.floor(x), Mth.floor(z)) + 1;
    }

    private float heading() {
        RoadGraph g = CityData.get().graph;
        int a = g.from(halfEdge), b = g.to(halfEdge);
        return (float) (Mth.atan2(-(g.x[b] - g.x[a]), g.z[b] - g.z[a]) * 180.0 / Math.PI);
    }

    /** The point of the right-hand lane that corresponds to the position along the current half-edge. */
    private Vec3 lanePoint() {
        RoadGraph g = CityData.get().graph;
        int a = g.from(halfEdge), b = g.to(halfEdge);
        double len = Math.max(0.01, g.length(halfEdge));
        double ux = (g.x[b] - g.x[a]) / len, uz = (g.z[b] - g.z[a]) / len;
        double w = g.width(halfEdge);
        // room is left for the cars parked along the kerb
        double off = g.oneway(halfEdge) ? 0 : Mth.clamp(w / 2 - 2.9, 0.95, w / 4);
        if (kind() == BUS) off = Math.max(off, 1.35);
        double t = Math.min(along, len);
        return new Vec3(g.x[a] + 0.5 + ux * t - uz * off, 0, g.z[a] + 0.5 + uz * t + ux * off);
    }

    private boolean nextEdge() {
        RoadGraph g = CityData.get().graph;
        along -= g.length(halfEdge);
        if (route != null) {
            routeIndex++;
            if (routeIndex >= route.length) return false;
            halfEdge = route[routeIndex];
            return true;
        }
        int node = g.to(halfEdge), back = g.from(halfEdge);
        int[] out = g.out(node);
        int pick = -1, seen = 0;
        for (int he : out) {
            if (g.to(he) == back) continue;
            // mostly straight on
            int a = g.from(halfEdge), b = node, c = g.to(he);
            double dot = (g.x[b] - g.x[a]) * (double) (g.x[c] - g.x[b]) + (g.z[b] - g.z[a]) * (double) (g.z[c] - g.z[b]);
            int weight = dot > 0 ? 5 : 1;
            seen += weight;
            if (random.nextInt(seen) < weight) pick = he;
        }
        if (pick < 0) return false;
        halfEdge = pick;
        return true;
    }

    private void arrive() {
        for (Entity e : List.copyOf(getPassengers())) {
            e.stopRiding();
            if (destination != null && e instanceof ServerPlayer sp)
                sp.teleportTo((ServerLevel) level(), destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, sp.getYRot(), 0);
        }
        route = null;
        destination = null;
    }

    /** Developer aid: skip the rest of the ride. */
    public void finishRide() {
        if (route != null) arrive();
    }

    private boolean mustStop(RoadGraph g) {
        double len = g.length(halfEdge), rem = len - along;
        int node = g.to(halfEdge);
        if (g.signal[node]) {
            double line = g.width(halfEdge) / 2 + 5 + halfLength();
            if (rem < line + 4 && rem > line - 4) {
                int a = g.from(halfEdge);
                boolean alongX = Math.abs(g.x[node] - g.x[a]) > Math.abs(g.z[node] - g.z[a]);
                if (!Signals.green(alongX, level().getGameTime())) return true;
            }
        }
        if (tickCount % 3 == 0) {
            // anything in the way within a few metres of the front bumper?
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
            double reach = halfLength() + 2.5 + speed * 6;
            AABB box = new AABB(getX() + fx * reach - 1.6, getY() - 1, getZ() + fz * reach - 1.6, getX() + fx * reach + 1.6, getY() + 3, getZ() + fz * reach + 1.6)
                    .minmax(new AABB(getX() + fx * (halfLength() + 1) - 1.2, getY() - 1, getZ() + fz * (halfLength() + 1) - 1.2,
                            getX() + fx * (halfLength() + 1) + 1.2, getY() + 3, getZ() + fz * (halfLength() + 1) + 1.2));
            blocked = false;
            for (Entity e : level().getEntities(this, box, e -> e instanceof LivingEntity || e instanceof VehicleEntity)) {
                if (hasPassenger(e)) continue;
                if (e instanceof VehicleEntity v) {
                    // only those driving the same way count, oncoming traffic passes
                    if (Math.abs(Mth.wrapDegrees(v.getYRot() - getYRot())) > 100) continue;
                }
                blocked = true;
                break;
            }
        }
        return blocked;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                double f = 1.0 / lerpSteps;
                setPos(getX() + (lerpX - getX()) * f, getY() + (lerpY - getY()) * f, getZ() + (lerpZ - getZ()) * f);
                setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) * (float) f);
                lerpSteps--;
            }
            return;
        }
        if (halfEdge < 0) {
            discard();
            return;
        }
        RoadGraph g = CityData.get().graph;
        boolean story = route != null;
        double cruise = switch (kind()) {
            case BUS -> story ? 0.62 : 0.42;
            case MINIBUS -> 0.48;
            default -> 0.52;
        };
        boolean stop = mustStop(g);
        // a jam must not hold the bus with the player for ever
        stuck = stop && speed < 0.02 ? stuck + 1 : 0;
        if (stuck > 400) {
            stop = false;
            if (stuck > 460) stuck = 0;
        }
        // slow down for sharp corners
        double rem = g.length(halfEdge) - along;
        if (rem < 9 && !stop) cruise = Math.min(cruise, 0.3);
        speed = stop ? Math.max(0, speed - 0.07) : Math.min(cruise, speed + 0.02);
        entityData.set(BRAKING, stop);
        along += speed;
        while (along >= g.length(halfEdge)) {
            if (!nextEdge()) {
                if (story) {
                    arrive();
                    along = 0;
                    // goes on as ordinary traffic
                    int node = g.to(halfEdge);
                    if (g.out(node).length == 0) {
                        discard();
                        return;
                    }
                    halfEdge = g.out(node)[0];
                } else {
                    discard();
                    return;
                }
            }
        }
        // chase the lane point: this rounds the corners
        Vec3 target = lanePoint();
        double dx = target.x - getX(), dz = target.z - getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double step = Math.min(d, Math.max(speed * 1.25, d > 3 ? 0.6 : 0.05));
        double nx = getX(), nz = getZ();
        if (d > 1e-4) {
            nx += dx / d * step;
            nz += dz / d * step;
        }
        if (d > 30) {
            nx = target.x;
            nz = target.z;
        }
        if (!level().isLoaded(BlockPos.containing(nx, getY(), nz))) {
            if (story) arrive();
            discard();
            return;
        }
        double ny = getY() + Mth.clamp(groundY(nx, nz) - getY(), -0.25, 0.25);
        float yaw = getYRot();
        if (step > 0.02) {
            float want = (float) (Mth.atan2(-(nx - getX()), nz - getZ()) * 180.0 / Math.PI);
            yaw += Mth.clamp(Mth.wrapDegrees(want - yaw), -9f, 9f);
        }
        setPos(nx, ny, nz);
        setYRot(yaw);
        // ordinary traffic disappears when nobody is around
        if (!story && tickCount % 40 == 0) {
            Player p = level().getNearestPlayer(this, 170);
            lonely = p == null ? lonely + 1 : 0;
            if (lonely >= 2 && getPassengers().isEmpty()) discard();
        }
    }

    // ------------------------------------------------------------------ passengers

    @Override
    public double getPassengersRidingOffset() {
        return kind() == CAR ? 0.35 : 0.85;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 4;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // step out to the right, towards the kerb
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        for (double off = 2.5; off <= 6; off += 1) {
            double x = getX() + rx * off, z = getZ() + rz * off;
            if (CityData.get().building(Mth.floor(x), Mth.floor(z)) == 0) return new Vec3(x, groundY(x, z) + 1, z);
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ------------------------------------------------------------------ plumbing

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yRot;
        lerpSteps = steps + 2;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
