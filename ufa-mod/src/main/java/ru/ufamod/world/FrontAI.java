package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;

import java.util.List;

/**
 * The soldiers of the front. The enemy advances by bounds from crater to crater, keeps low and fires from
 * cover, stays down after being hit, cuts the wire and finally storms the trench through the loopholes.
 * The platoon in the trench fires back.
 */
public final class FrontAI {
    private FrontAI() {}

    private static final int FAR = 0, MID = 1, NEAR = 2, ASSAULT = 3;

    private static int[][] tier(int t) {
        return t == FAR ? Plan.FRONT_COVER : t == MID ? Plan.FRONT_COVER_MID : Plan.FRONT_COVER_NEAR;
    }

    @javax.annotation.Nullable
    private static Player target(NpcEntity e) {
        Player p = e.level().getNearestPlayer(e, 120);
        return p == null || p.isCreative() || p.isSpectator() ? null : p;
    }

    public static void enemyTick(NpcEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return;
        Player p = target(e);
        if (p == null) {
            e.getNavigation().stop();
            return;
        }
        CompoundTag d = e.getPersistentData();
        e.getLookControl().setLookAt(p, 30f, 30f);
        int t = d.getInt("tier");
        if (!d.contains("tx")) chooseCover(e, d, p, t);
        double tx = d.getDouble("tx"), tz = d.getDouble("tz");
        double dist2 = e.distanceToSqr(tx, e.getY(), tz);
        // was just hit: keep the head down
        if (e.hurtTime > 0 && d.getInt("wait") < 60 && t < ASSAULT) d.putInt("wait", 80 + e.getRandom().nextInt(60));
        if (t == ASSAULT) {
            assault(e, level, p, d);
            return;
        }
        if (dist2 > 3.0 && d.getInt("wait") <= 0) {
            // a bound to the next crater, at a run
            e.setShiftKeyDown(false);
            e.setSprinting(true);
            if (e.tickCount % 15 == 0 || e.getNavigation().isDone()) e.getNavigation().moveTo(tx, e.getY(), tz, 1.2);
            cutWire(e, level, d);
            if (e.tickCount % 50 == 0) shoot(e, p, 0.5);
            return;
        }
        // in cover: crouch, fire now and then, then move on
        e.getNavigation().stop();
        e.setSprinting(false);
        e.setShiftKeyDown(true);
        if (!d.contains("wait") || d.getInt("wait") <= 0 && dist2 <= 3.0 && !d.getBoolean("settled")) {
            d.putInt("wait", 160 + e.getRandom().nextInt(160));
            d.putBoolean("settled", true);
        }
        int wait = d.getInt("wait") - 1;
        d.putInt("wait", wait);
        int cd = d.getInt("cd") - 1;
        if (cd <= 0) {
            cd = 30 + e.getRandom().nextInt(45);
            shoot(e, p, 1.0);
        }
        d.putInt("cd", cd);
        if (wait <= 0) {
            // from the last craters only some go on to storm the trench; the rest keep firing from there,
            // until the attack has dragged on for a minute and a half
            long age = level.getGameTime() - d.getLong("born");
            boolean storm = t < NEAR || d.getBoolean("stormer") || age > 20 * 90;
            if (storm && t + 1 == ASSAULT && level.getEntitiesOfClass(NpcEntity.class, e.getBoundingBox().inflate(90, 12, 90),
                    n -> n.getRole() == NpcRole.ENEMY && n.getPersistentData().getInt("tier") == ASSAULT).size() >= 2 && age <= 20 * 90) storm = false;
            if (storm) d.putInt("tier", t + 1);
            d.remove("tx");
            d.putBoolean("settled", false);
        }
    }

    private static void chooseCover(NpcEntity e, CompoundTag d, Player p, int t) {
        double x, z;
        if (t >= ASSAULT) {
            // the loophole nearest to where the player is, or a spot inside the trench behind it
            int lz = Mth.floor(p.getZ()) - Plan.FRONT_OZ + e.getRandom().nextInt(9) - 4;
            lz = Math.max(26, Math.min(134, lz - Math.floorMod(lz, 3)));
            x = Plan.FRONT_OX + Plan.frontLoopholeX(lz) + 0.5;
            z = Plan.FRONT_OZ + lz + 0.5;
        } else {
            int[][] c = tier(t);
            // the crater nearest to the soldier's own line of advance, with a little choice
            int[] best = c[0];
            double bd = Double.MAX_VALUE;
            for (int[] q : c) {
                double dd = Math.abs(Plan.FRONT_OZ + q[1] - e.getZ()) + e.getRandom().nextInt(12);
                if (dd < bd) {
                    bd = dd;
                    best = q;
                }
            }
            x = Plan.FRONT_OX + best[0] + 0.5 + e.getRandom().nextInt(3) - 1;
            z = Plan.FRONT_OZ + best[1] + 0.5 + e.getRandom().nextInt(3) - 1;
        }
        d.putDouble("tx", x);
        d.putDouble("tz", z);
    }

    /** Wire in the way is cut through, which takes a couple of seconds. */
    private static void cutWire(NpcEntity e, ServerLevel level, CompoundTag d) {
        Vec3 look = e.getDeltaMovement().horizontalDistanceSqr() > 1e-4 ? e.getDeltaMovement().normalize() : e.getViewVector(1f);
        BlockPos feet = e.blockPosition();
        BlockPos[] check = {feet, feet.above(), BlockPos.containing(e.getX() + look.x, e.getY(), e.getZ() + look.z), BlockPos.containing(e.getX() + look.x, e.getY() + 1, e.getZ() + look.z)};
        for (BlockPos b : check) {
            if (!isWire(level.getBlockState(b))) continue;
            e.getNavigation().stop();
            e.setShiftKeyDown(true);
            int cut = d.getInt("cut") + 1;
            d.putInt("cut", cut);
            if (cut % 10 == 0) level.playSound(null, b, SoundEvents.SHEEP_SHEAR, SoundSource.HOSTILE, 0.8f, 0.6f);
            if (cut >= 40) {
                level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
                d.putInt("cut", 0);
            }
            return;
        }
    }

    private static boolean isWire(BlockState s) {
        if (s.getBlock() instanceof WebBlock) return true;
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock());
        return key != null && key.getPath().equals("barbed_wire");
    }

    private static void assault(NpcEntity e, ServerLevel level, Player p, CompoundTag d) {
        double dist = e.distanceTo(p);
        e.setShiftKeyDown(false);
        if (dist < 2.6) {
            // hand to hand
            e.getNavigation().stop();
            int cd = d.getInt("cd") - 1;
            if (cd <= 0) {
                e.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                p.hurt(e.damageSources().mobAttack(e), 4f);
                cd = 22;
            }
            d.putInt("cd", cd);
            return;
        }
        double tx = d.getDouble("tx"), tz = d.getDouble("tz");
        boolean atLoophole = e.distanceToSqr(tx, e.getY(), tz) < 2.5;
        e.setSprinting(true);
        if (e.tickCount % 10 == 0 || e.getNavigation().isDone())
            e.getNavigation().moveTo(atLoophole || dist < 10 ? p.getX() : tx, p.getY(), atLoophole || dist < 10 ? p.getZ() : tz, 1.25);
        cutWire(e, level, d);
        int cd = d.getInt("cd") - 1;
        if (cd <= 0) {
            cd = 25 + e.getRandom().nextInt(25);
            shoot(e, p, 1.6);
        }
        d.putInt("cd", cd);
    }

    /** A shot at the player: a sound, a puff of smoke, and a hit with a chance that grows with closeness. */
    public static void shoot(NpcEntity e, Player p, double skill) {
        if (!e.getSensing().hasLineOfSight(p)) return;
        ServerLevel level = (ServerLevel) e.level();
        double dist = e.distanceTo(p);
        level.playSound(null, e.getX(), e.getEyeY(), e.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 2.5f, 0.55f + e.getRandom().nextFloat() * 0.2f);
        Vec3 look = e.getViewVector(1f);
        level.sendParticles(ParticleTypes.SMOKE, e.getX() + look.x * 0.9, e.getEyeY() - 0.1 + look.y * 0.9, e.getZ() + look.z * 0.9, 3, 0.05, 0.05, 0.05, 0.01);
        double chance = Mth.clamp((0.36 - dist / 180.0) * skill, 0.05, 0.45);
        if (p.isCrouching()) chance *= 0.45;
        if (e.getRandom().nextDouble() < chance) p.hurt(e.damageSources().mobAttack(e), 3.0f);
    }

    /** A soldier of the platoon on the firing step shoots at the nearest enemy he can see. */
    public static void friendlyTick(NpcEntity e) {
        if (!(e.level() instanceof ServerLevel level) || !Plan.FRONT_BOX.contains(e.position())) return;
        CompoundTag d = e.getPersistentData();
        int cd = d.getInt("cd") - 1;
        d.putInt("cd", cd);
        if (cd > 0) return;
        d.putInt("cd", 45 + e.getRandom().nextInt(50));
        List<NpcEntity> enemies = level.getEntitiesOfClass(NpcEntity.class, e.getBoundingBox().inflate(80, 12, 80), n -> n.isAlive() && n.getRole() == NpcRole.ENEMY);
        NpcEntity best = null;
        double bd = Double.MAX_VALUE;
        for (NpcEntity n : enemies) {
            double dd = n.distanceToSqr(e);
            if (dd < bd && e.getSensing().hasLineOfSight(n)) {
                bd = dd;
                best = n;
            }
        }
        if (best == null) return;
        e.getLookControl().setLookAt(best, 30f, 30f);
        level.playSound(null, e.getX(), e.getEyeY(), e.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 2.2f, 0.7f);
        if (e.getRandom().nextDouble() < Mth.clamp(0.3 - Math.sqrt(bd) / 300.0, 0.08, 0.3)) best.hurt(e.damageSources().mobAttack(e), 6f);
    }

    /** An illumination flare hangs over the field for a few seconds. */
    public static void flare(ServerLevel level, java.util.List<long[]> lit, long now, java.util.Random rnd) {
        int x = Plan.FRONT_OX + 80 + rnd.nextInt(50), z = Plan.FRONT_OZ + 20 + rnd.nextInt(Plan.FRONT_D - 40);
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        level.playSound(null, x, gy + 2, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 3f, 0.8f);
        for (int[] o : new int[][]{{0, 0}, {9, 0}, {-9, 0}, {0, 9}, {0, -9}}) {
            BlockPos b = new BlockPos(x + o[0], gy + 5, z + o[1]);
            if (!level.getBlockState(b).isAir()) continue;
            level.setBlock(b, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13), 3);
            lit.add(new long[]{b.asLong(), now + 200});
        }
        level.sendParticles(ParticleTypes.END_ROD, x + 0.5, gy + 14, z + 0.5, 40, 0.3, 0.3, 0.3, 0.02);
    }

    public static void unlight(ServerLevel level, java.util.List<long[]> lit, long now, boolean all) {
        lit.removeIf(l -> {
            if (!all && l[1] > now) return false;
            BlockPos b = BlockPos.of(l[0]);
            if (level.getBlockState(b).is(Blocks.LIGHT)) level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
            return true;
        });
    }
}
