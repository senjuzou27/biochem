package ru.ufamod.entity;

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

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 48.0);
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
        if (follow != null && tickCount % 10 == 0) {
            Player target = level().getPlayerByUUID(follow);
            // come up to the player but keep a polite three blocks away
            if (target != null && distanceToSqr(target) > 16.0) getNavigation().moveTo(target, 1.1);
            else if (target == null || distanceToSqr(target) < 10.0) getNavigation().stop();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) QuestManager.onTalk(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
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
