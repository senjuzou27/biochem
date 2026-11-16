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
    private static final EntityDataAccessor<Boolean> PARKED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ROUTE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final String[] ROUTE_NUMBERS = {"3", "10", "51", "101", "110", "226", "247", "290"};
    private static final String[] ROUTE_NAMES = {"Центр", "Сипайлово", "Черниковка", "Затон", "Инорс", "Дема", "Колхозный рынок", "Ж/д вокзал"};

    /** Client side: how far the wheels have turned, for drawing them. */
    public float wheelSpin, wheelSpinO;

    // server side: the lane ahead as a smooth polyline (x, z pairs) with the arc length at each point
    private final java.util.ArrayList<double[]> path = new java.util.ArrayList<>();
    private double along;          // distance travelled along the polyline
    private double speed;
    private int lastHalfEdge = -1; // the last half-edge put into the path
    private int[] route;
    private int routeIndex;
    /** Traffic lights ahead: arc length of the stop line, node, true for traffic along X. */
    private final java.util.ArrayList<double[]> lights = new java.util.ArrayList<>();
    @Nullable
    private BlockPos destination;
    private int stuck, ghost, lonely, redWait;
    private boolean atRed;
    private boolean blocked;

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
        entityData.define(PARKED, false);
        entityData.define(ROUTE, "");
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

    /** The body style of a car, fixed for each vehicle: 0-4 sedan, 5-6 hatchback, 7-8 crossover, 9 taxi. */
    public int style() {
        return Math.floorMod((int) (getUUID().getLeastSignificantBits() >>> 3), 10);
    }

    public boolean parked() {
        return entityData.get(PARKED);
    }

    public void setParked(boolean parked) {
        entityData.set(PARKED, parked);
    }

    public String routeNumber() {
        String r = entityData.get(ROUTE);
        if (!r.isEmpty()) return r.substring(0, r.indexOf('|'));
        return ROUTE_NUMBERS[Math.floorMod(getUUID().hashCode(), ROUTE_NUMBERS.length)];
    }

    public String routeName() {
        String r = entityData.get(ROUTE);
        if (!r.isEmpty()) return r.substring(r.indexOf('|') + 1);
        return ROUTE_NAMES[Math.floorMod(getUUID().hashCode() / 7, ROUTE_NAMES.length)];
    }

    public void setRouteBoard(String number, String name) {
        entityData.set(ROUTE, number + "|" + name);
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

    /** Puts the vehicle at the start of a half-edge of the street network; it then drives on at random. */
    public void placeOn(int he, double offset) {
        path.clear();
        lights.clear();
        along = 0;
        lastHalfEdge = -1;
        append(he);
        while (pathLength() < 60 && extendRandom()) { }
        double[] p = pointAt(0), q = pointAt(3);
        moveTo(p[0], groundY(p[0], p[1]), p[1], yawOf(q[0] - p[0], q[1] - p[1]), 0);
    }

    /** Sends the vehicle along a route; at its end the passengers are set down at the destination. */
    public void setRoute(int[] halfEdges, BlockPos dest) {
        route = halfEdges;
        routeIndex = 0;
        destination = dest;
        path.clear();
        lights.clear();
        along = 0;
        lastHalfEdge = -1;
        for (int he : halfEdges) append(he);
        double[] p = pointAt(0), q = pointAt(3);
        moveTo(p[0], groundY(p[0], p[1]), p[1], yawOf(q[0] - p[0], q[1] - p[1]), 0);
    }

    /** Standing still for more than five seconds (for the developer statistics). */
    public boolean isStuck() {
        return stuck > 100 && !atRed;
    }

    public boolean onRoute() {
        return route != null;
    }

    private static double groundY(double x, double z) {
        return CityData.get().height(Mth.floor(x), Mth.floor(z)) + 1;
    }

    private static float yawOf(double dx, double dz) {
        return (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
    }

    /** How far right of the centre line the vehicle drives on this half-edge. */
    private double laneOffset(int he) {
        RoadGraph g = CityData.get().graph;
        double w = g.width(he);
        if (g.oneway(he)) return 0;
        // cars are parked along the kerb in the outer two metres: keep clear of them
        double half = kind() == BUS ? 1.3 : kind() == MINIBUS ? 1.05 : 0.95;
        return Math.max(kind() == BUS ? 0.3 : 0.6, Math.min(w / 4.0, w / 2.0 - 2.0 - half));
    }

    private double pathLength() {
        return path.isEmpty() ? 0 : path.get(path.size() - 1)[2];
    }

    private void addPoint(double x, double z) {
        if (!path.isEmpty()) {
            double[] last = path.get(path.size() - 1);
            double d = Math.hypot(x - last[0], z - last[1]);
            if (d < 0.05) return;
            path.add(new double[]{x, z, last[2] + d});
        } else path.add(new double[]{x, z, 0});
    }

    /**
     * Adds a half-edge to the path: its lane, offset to the right, joined to the previous one by a short curve
     * instead of a sharp corner.
     */
    private void append(int he) {
        RoadGraph g = CityData.get().graph;
        int a = g.from(he), b = g.to(he);
        double ax = g.x[a] + 0.5, az = g.z[a] + 0.5, bx = g.x[b] + 0.5, bz = g.z[b] + 0.5;
        double len = Math.max(0.01, Math.hypot(bx - ax, bz - az));
        double ux = (bx - ax) / len, uz = (bz - az) / len, off = laneOffset(he);
        double rx = -uz * off, rz = ux * off;
        double r = Math.min(5.0, len / 2.5);
        if (path.isEmpty()) addPoint(ax + rx, az + rz);
        else {
            // a quadratic curve from the end of the last lane to the start of this one
            double[] p0 = path.get(path.size() - 1);
            double p2x = ax + rx + ux * r, p2z = az + rz + uz * r;
            double cx = ax + rx, cz = az + rz;
            for (int i = 1; i <= 5; i++) {
                double t = i / 5.0, m = 1 - t;
                addPoint(m * m * p0[0] + 2 * m * t * cx + t * t * p2x, m * m * p0[1] + 2 * m * t * cz + t * t * p2z);
            }
        }
        // the end of this lane stops short of the junction, where the next curve starts
        addPoint(bx + rx - ux * r, bz + rz - uz * r);
        if (g.signal[b]) {
            double stop = pathLength() - g.width(he) * 0.0 - 1.0;
            boolean alongX = Math.abs(bx - ax) > Math.abs(bz - az);
            lights.add(new double[]{stop, b, alongX ? 1 : 0});
        }
        lastHalfEdge = he;
    }

    /** Picks the next street at random, mostly straight on; false at a dead end. */
    private boolean extendRandom() {
        RoadGraph g = CityData.get().graph;
        if (lastHalfEdge < 0) return false;
        int node = g.to(lastHalfEdge), back = g.from(lastHalfEdge);
        int pick = -1, seen = 0;
        for (int he : g.out(node)) {
            if (g.to(he) == back) continue;
            int a = back, c = g.to(he);
            double dot = (g.x[node] - g.x[a]) * (double) (g.x[c] - g.x[node]) + (g.z[node] - g.z[a]) * (double) (g.z[c] - g.z[node]);
            double l1 = Math.hypot(g.x[node] - g.x[a], g.z[node] - g.z[a]), l2 = Math.hypot(g.x[c] - g.x[node], g.z[c] - g.z[node]);
            double cos = dot / Math.max(1e-6, l1 * l2);
            if (cos < -0.3) continue;   // no U-turns and no hairpins
            int weight = cos > 0.7 ? 6 : 1;
            seen += weight;
            if (random.nextInt(seen) < weight) pick = he;
        }
        if (pick < 0) return false;
        append(pick);
        return true;
    }

    /** The point of the path at arc length s. */
    private double[] pointAt(double s) {
        if (path.isEmpty()) return new double[]{getX(), getZ()};
        if (s <= path.get(0)[2]) return path.get(0);
        int lo = 0, hi = path.size() - 1;
        if (s >= path.get(hi)[2]) return path.get(hi);
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (path.get(mid)[2] <= s) lo = mid;
            else hi = mid;
        }
        double[] p = path.get(lo), q = path.get(hi);
        double t = (s - p[2]) / Math.max(1e-6, q[2] - p[2]);
        return new double[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, s};
    }

    /** Forgets the part of the path already driven, keeping the arc lengths consistent. */
    private void trim() {
        int drop = 0;
        while (drop + 2 < path.size() && path.get(drop + 1)[2] < along - 10) drop++;
        if (drop > 20) path.subList(0, drop).clear();
        lights.removeIf(l -> l[0] < along - 2);
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

    /** How fast the vehicle may go now, considering lights, the vehicle ahead and people on the road. */
    private double allowedSpeed(double cruise) {
        double limit = cruise;
        boolean story = route != null;
        // red and yellow lights: stop before the line, unless already too close to stop
        atRed = false;
        for (double[] l : lights) {
            double dist = l[0] - along;
            if (dist < 0 || dist > 30) continue;
            // the bus of the story does not keep the player waiting for long
            if (!Signals.green(l[2] > 0, level().getGameTime()) && dist > 1.5 && !(story && redWait > 60)) {
                limit = Math.min(limit, Math.max(0, (dist - 2.0) * 0.06));
                atRed = dist < 12;
            }
            break;
        }
        redWait = atRed && speed < 0.05 ? redWait + 1 : 0;
        // slow down where the path bends
        double[] a = pointAt(along), b = pointAt(along + 4), c = pointAt(along + 8);
        double h1 = Math.atan2(b[1] - a[1], b[0] - a[0]), h2 = Math.atan2(c[1] - b[1], c[0] - b[0]);
        double bend = Math.abs(Mth.wrapDegrees((h2 - h1) * 180 / Math.PI));
        if (bend > 25) limit = Math.min(limit, 0.28);
        if (ghost > 0) return limit;
        if (tickCount % 2 == 0) {
            blocked = false;
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
            double reach = halfLength() + 3 + speed * 12;
            double gap = Double.MAX_VALUE;
            for (Entity e : level().getEntities(this, getBoundingBox().inflate(reach + 4, 3, reach + 4), e -> e instanceof LivingEntity || e instanceof VehicleEntity)) {
                if (hasPassenger(e)) continue;
                double dx = e.getX() - getX(), dz = e.getZ() - getZ();
                double ahead = dx * fx + dz * fz, side = Math.abs(dx * -fz + dz * fx);
                if (e instanceof VehicleEntity v) {
                    if (v.parked()) continue;
                    // only a vehicle in this lane, driving the same way, holds us up
                    if (Math.abs(Mth.wrapDegrees(v.getYRot() - getYRot())) > 50 || side > 1.6) continue;
                    double d = ahead - halfLength() - v.halfLength();
                    if (ahead > 0 && d < gap) gap = d;
                } else {
                    // a person on the carriageway right in front
                    if (side > 1.4 || ahead <= 0) continue;
                    if (!(e instanceof Player) && (story || stuck > 60)) continue;
                    int gnd = CityData.get().ground(Mth.floor(e.getX()), Mth.floor(e.getZ()));
                    if (gnd != ru.ufamod.city.Ground.ROAD) continue;
                    double d = ahead - halfLength() - 0.5;
                    if (d < gap) gap = d;
                }
            }
            blocked = gap < 1.5;
            if (gap < Double.MAX_VALUE) limit = Math.min(limit, Math.max(0, (gap - 1.5) * 0.08));
        } else if (blocked) limit = 0;
        return limit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double ox = getX(), oz = getZ();
            wheelSpinO = wheelSpin;
            if (lerpSteps > 0) {
                double f = 1.0 / lerpSteps;
                setPos(getX() + (lerpX - getX()) * f, getY() + (lerpY - getY()) * f, getZ() + (lerpZ - getZ()) * f);
                setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) * (float) f);
                lerpSteps--;
            }
            double moved = Math.hypot(getX() - ox, getZ() - oz);
            float r = kind() == BUS ? 0.5f : kind() == MINIBUS ? 0.37f : 0.33f;
            wheelSpin -= (float) (moved / r);
            return;
        }
        if (parked()) return;
        if (path.isEmpty()) {
            discard();
            return;
        }
        boolean story = route != null;
        double cruise = switch (kind()) {
            case BUS -> story ? 1.0 : 0.45;
            case MINIBUS -> 0.55;
            default -> 0.6;
        };
        double limit = allowedSpeed(cruise);
        speed = limit < speed ? Math.max(limit, speed - 0.06) : Math.min(limit, speed + 0.02);
        entityData.set(BRAKING, limit < speed + 0.01 && speed < cruise * 0.6);
        // a vehicle that cannot move on: the story bus pushes through, others give way or leave
        stuck = speed < 0.02 ? stuck + 1 : 0;
        if (ghost > 0) ghost--;
        if (stuck > (story ? 60 : 200)) {
            if (story) ghost = 80;
            else {
                Player near = level().getNearestPlayer(this, 40);
                if (near == null) {
                    discard();
                    return;
                }
                ghost = 60;
            }
            stuck = 0;
        }
        along += speed;
        if (!story && pathLength() - along < 50) extendRandom();
        if (along >= pathLength() - 0.2) {
            if (story) {
                arrive();
                speed = 0;
                if (!extendRandom()) {
                    discard();
                    return;
                }
            } else {
                discard();
                return;
            }
        }
        trim();
        double[] p = pointAt(along), ahead = pointAt(along + Math.max(2.5, halfLength()));
        BlockPos at = BlockPos.containing(p[0], getY(), p[1]);
        if (!level().isLoaded(at)) {
            if (story) arrive();
            discard();
            return;
        }
        // follow the ground smoothly; jump onto a different level (a bridge, a tunnel) at once
        double gy = groundY(p[0], p[1]), ny = Math.abs(gy - getY()) > 3 ? gy : getY() + Mth.clamp(gy - getY(), -0.2, 0.2);
        float yaw = getYRot();
        if (Math.hypot(ahead[0] - p[0], ahead[1] - p[1]) > 0.3) {
            float want = yawOf(ahead[0] - p[0], ahead[1] - p[1]);
            yaw += Mth.clamp(Mth.wrapDegrees(want - yaw), -12f, 12f);
        }
        setPos(p[0], ny, p[1]);
        setYRot(yaw);
        // ordinary traffic disappears when nobody is around
        if (!story && tickCount % 40 == 0) {
            Player pl = level().getNearestPlayer(this, 170);
            lonely = pl == null ? lonely + 1 : 0;
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
        return parked();
    }

    /** A parked car is solid: one bumps into it and can stand on its roof. */
    @Override
    public boolean canBeCollidedWith() {
        return parked();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setLook(tag.getInt("Kind"), tag.getInt("Colour"));
        setParked(tag.getBoolean("Parked"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind());
        tag.putInt("Colour", colour());
        tag.putBoolean("Parked", parked());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
