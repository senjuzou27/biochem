package ru.ufamod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.ufamod.quest.QuestManager;

import javax.annotation.Nullable;
import java.util.UUID;

/** A talking human NPC: citizens, students, teachers, soldiers. */
public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);

    private boolean wander;
    private boolean wanderGoalAdded;
    /** Ticks left to live; negative means forever. Used for the soldiers of the arrest scene. */
    private int lifetime = -1;
    @Nullable
    private UUID follow;
    /** An enemy soldier: the crater it advances to, and the time to its next shot. */
    @Nullable
    private BlockPos cover;
    private int shotCooldown = 40;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, 0);
        entityData.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public NpcRole getRole() {
        return NpcRole.byId(entityData.get(ROLE));
    }

    public void setRole(NpcRole role) {
        entityData.set(ROLE, role.ordinal());
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        entityData.set(VARIANT, variant);
    }

    public void setWander(boolean wander) {
        this.wander = wander;
        if (wander && !wanderGoalAdded) {
            goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.6));
            wanderGoalAdded = true;
        }
    }

    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    /** Makes an enemy soldier advance to a place in cover and fire from there. */
    public void advanceTo(BlockPos spot) {
        this.cover = spot;
    }

    public void follow(@Nullable Player player) {
        this.follow = player == null ? null : player.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (lifetime > 0 && --lifetime == 0) {
            discard();
            return;
        }
        if (getRole() == NpcRole.ENEMY) {
            enemyTick();
            return;
        }
        if (follow != null && tickCount % 4 == 0) {
            Player target = level().getPlayerByUUID(follow);
            // come up to the player but keep a polite three blocks away
            if (target != null && distanceToSqr(target) > 25.0) getNavigation().moveTo(target, 1.0);
            else getNavigation().stop();
        }
    }

    private void enemyTick() {
        Player p = level().getNearestPlayer(this, 90);
        if (p == null || p.isCreative() || p.isSpectator()) {
            getNavigation().stop();
            return;
        }
        getLookControl().setLookAt(p, 30f, 30f);
        if (cover != null) {
            double d = distanceToSqr(cover.getX() + 0.5, getY(), cover.getZ() + 0.5);
            if (d > 3.0) {
                setShiftKeyDown(false);
                if (tickCount % 20 == 0 || getNavigation().isDone()) getNavigation().moveTo(cover.getX() + 0.5, cover.getY(), cover.getZ() + 0.5, 0.9);
                return;
            }
            getNavigation().stop();
            setShiftKeyDown(true);
        }
        if (--shotCooldown > 0) return;
        shotCooldown = 30 + random.nextInt(50);
        if (!getSensing().hasLineOfSight(p)) return;
        double dist = distanceTo(p);
        level().playSound(null, getX(), getEyeY(), getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.5f, 0.55f + random.nextFloat() * 0.2f);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            net.minecraft.world.phys.Vec3 look = getViewVector(1f);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, getX() + look.x * 0.9, getEyeY() - 0.1 + look.y * 0.9, getZ() + look.z * 0.9, 3, 0.05, 0.05, 0.05, 0.01);
        }
        // a rifle at fifty metres does not hit often, a soldier keeping low in the trench even less
        double chance = net.minecraft.util.Mth.clamp(0.34 - dist / 200.0, 0.06, 0.3);
        if (p.isCrouching()) chance *= 0.45;
        if (random.nextDouble() < chance) p.hurt(damageSources().mobAttack(this), 3.0f);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) QuestManager.onTalk(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (getRole() == NpcRole.ENEMY) return super.isInvulnerableTo(source);
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", entityData.get(ROLE));
        tag.putInt("Variant", entityData.get(VARIANT));
        tag.putBoolean("Wander", wander);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        entityData.set(VARIANT, tag.getInt("Variant"));
        setWander(tag.getBoolean("Wander"));
        lifetime = tag.contains("Lifetime") ? tag.getInt("Lifetime") : -1;
        // Soldiers of an interrupted arrest scene do not outlive a restart.
        if (lifetime > 0) lifetime = 1;
    }
}
