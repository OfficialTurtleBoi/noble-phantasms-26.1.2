package net.turtleboi.noblephantasms.entity.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.turtleboi.noblephantasms.effect.custom.ChilledEffect;
import net.turtleboi.noblephantasms.item.ModItems;

public final class DraugrEntity extends Monster {
    public static final int REVIVAL_DURATION = 100;
    public static final int MAX_RISES = 2;
    private static final int CHILLED_DURATION = 120;
    private static final EntityDataAccessor<Boolean> REVIVING = SynchedEntityData.defineId(
            DraugrEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REVIVAL_TICKS = SynchedEntityData.defineId(
            DraugrEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RISES = SynchedEntityData.defineId(
            DraugrEntity.class, EntityDataSerializers.INT);

    public DraugrEntity(EntityType<? extends DraugrEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
        if (!level.isClientSide()) {
            equipVikingLoadout();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 4.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    private void equipVikingLoadout() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.NORTHERN_AXE.get()));
        if (random.nextInt(3) != 0) {
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
            ChilledEffect.applyStack(livingTarget, this, CHILLED_DURATION);
            swing(InteractionHand.MAIN_HAND, true);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!isReviving() && getRiseCount() < MAX_RISES && !isBurningDamage(source)) {
            beginRevival();
            return;
        }
        super.die(source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isReviving()) {
            return super.hurtServer(level, source, amount);
        }
        if (!isBurningDamage(source)) {
            return false;
        }
        entityData.set(REVIVING, false);
        setNoAi(false);
        setHealth(Math.max(1.0F, getHealth()));
        return super.hurtServer(level, source, Math.max(amount, getHealth() + 1.0F));
    }

    private boolean isBurningDamage(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.isOnFire()) {
            return true;
        }
        if (!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)
                || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        var fireAspect = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FIRE_ASPECT);
        return EnchantmentHelper.getItemEnchantmentLevel(fireAspect, attacker.getMainHandItem()) > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isReviving()) {
            return;
        }
        setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
        setSprinting(false);
        setJumping(false);
        getNavigation().stop();
        setTarget(null);
        if (level() instanceof ServerLevel serverLevel) {
            if (isOnFire()) {
                hurtServer(serverLevel, serverLevel.damageSources().onFire(), getHealth() + 1.0F);
                return;
            }
            int ticks = Math.max(0, getRevivalTicks() - 1);
            entityData.set(REVIVAL_TICKS, ticks);
            if (ticks == 0) {
                finishRevival(serverLevel);
            }
        }
    }

    private void beginRevival() {
        setHealth(1.0F);
        deathTime = 0;
        setPose(Pose.STANDING);
        setNoAi(true);
        stopUsingItem();
        removeAllEffects();
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(0.0, 0.0, 0.0);
        entityData.set(REVIVING, true);
        entityData.set(REVIVAL_TICKS, REVIVAL_DURATION);
    }

    private void finishRevival(ServerLevel level) {
        int rises = Math.min(MAX_RISES, getRiseCount() + 1);
        entityData.set(RISES, rises);
        entityData.set(REVIVING, false);
        entityData.set(REVIVAL_TICKS, 0);
        setNoAi(false);
        setPose(Pose.STANDING);
        float healthFraction = rises == 1 ? 0.65F : 0.4F;
        setHealth(getMaxHealth() * healthFraction);
        invulnerableTime = 20;
        level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(0.5), getZ(),
                35, getBbWidth() * 0.6, getBbHeight() * 0.45, getBbWidth() * 0.6, 0.08);
        level.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(),
                18, getBbWidth() * 0.4, getBbHeight() * 0.35, getBbWidth() * 0.4, 0.03);
    }

    public boolean isReviving() {
        return entityData.get(REVIVING);
    }

    public int getRevivalTicks() {
        return entityData.get(REVIVAL_TICKS);
    }

    public int getRiseCount() {
        return entityData.get(RISES);
    }

    @Override
    public boolean isPushable() {
        return !isReviving() && super.isPushable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REVIVING, false);
        builder.define(REVIVAL_TICKS, 0);
        builder.define(RISES, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean reviving = input.getBooleanOr("Reviving", false);
        entityData.set(REVIVING, reviving);
        entityData.set(REVIVAL_TICKS, input.getIntOr("RevivalTicks", 0));
        entityData.set(RISES, input.getIntOr("Rises", 0));
        if (reviving) {
            setHealth(Math.max(1.0F, getHealth()));
            setNoAi(true);
            setPose(Pose.STANDING);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Reviving", isReviving());
        output.putInt("RevivalTicks", getRevivalTicks());
        output.putInt("Rises", getRiseCount());
    }
}
