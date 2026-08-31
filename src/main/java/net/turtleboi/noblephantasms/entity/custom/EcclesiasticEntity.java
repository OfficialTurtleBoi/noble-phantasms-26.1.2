package net.turtleboi.noblephantasms.entity.custom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class EcclesiasticEntity extends SpellcasterIllager {
    private static final double WARD_ACQUISITION_RADIUS = 12.0;
    private static final double WARD_RETENTION_RADIUS = 16.0;
    private static final double WARD_SCALING_RADIUS = 32.0;
    private static final int BASE_SHARED_WARD_SLOTS = 3;
    private static final int WARD_SLOTS_PER_EXTRA_PLAYER = 2;
    private static final int MAX_SHARED_WARD_SLOTS = 12;
    private static final int WARD_LEASE_DURATION = 30;
    private static final int WARD_REFRESH_INTERVAL = 10;
    private static final int EMPTY_SLOT_COOLDOWN = 30;
    private static final int WARD_CAST_TICKS = 12;
    private final List<WardSlot> wardSlots = new ArrayList<>();

    public EcclesiasticEntity(EntityType<? extends EcclesiasticEntity> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof EcclesiasticEntity) && super.canAttack(target);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WardCastingGoal());
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.6, 1.0));
        goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel && tickCount % WARD_REFRESH_INTERVAL == 0) {
            maintainWardSlots(serverLevel);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isDeadOrDying() && level() instanceof ServerLevel serverLevel) {
            clearAllWards(serverLevel);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
                && level() instanceof ServerLevel serverLevel) {
            clearAllWards(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent getCastingSoundEvent() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.hasEffect(ModEffects.WARD)) {
            return;
        }
        Optional<UUID> sourceId = target.getExistingDataOrNull(ModAttachments.ECCLESIASTIC_WARD_SOURCE);
        if (sourceId == null || sourceId.isEmpty()
                || !(target.level() instanceof ServerLevel serverLevel)) {
            clearWard(target);
            return;
        }

        Entity source = serverLevel.getEntity(sourceId.get());
        if (!(source instanceof EcclesiasticEntity ecclesiastic) || !ecclesiastic.isAlive()) {
            clearWard(target);
            return;
        }

        event.setCanceled(true);
    }

    private void maintainWardSlots(ServerLevel level) {
        resizeWardSlots(level, calculateWardCapacity(level));
        long gameTime = level.getGameTime();
        Set<UUID> assignedTargets = new HashSet<>();

        for (WardSlot slot : wardSlots) {
            if (slot.targetId == null) {
                continue;
            }
            LivingEntity target = resolveTarget(level, slot.targetId);
            if (target == null || !canRetainWard(target)) {
                releaseSlot(slot, target, gameTime);
                continue;
            }

            Optional<UUID> sourceId = target.getExistingDataOrNull(ModAttachments.ECCLESIASTIC_WARD_SOURCE);
            if (target.hasEffect(ModEffects.WARD)
                    && (sourceId == null || sourceId.isEmpty() || !sourceId.get().equals(getUUID()))) {
                releaseSlot(slot, null, gameTime);
                continue;
            }

            refreshWard(target);
            assignedTargets.add(target.getUUID());
        }

        for (WardSlot slot : wardSlots) {
            if (slot.targetId != null || slot.readyAt > gameTime) {
                continue;
            }
            Mob target = findWardTarget(assignedTargets);
            if (target == null) {
                break;
            }
            bindWard(slot, target);
            beginWardCast();
            break;
        }
    }

    private int calculateWardCapacity(ServerLevel level) {
        int nearbyPlayers = level.getEntitiesOfClass(
                Player.class,
                getBoundingBox().inflate(WARD_SCALING_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator()).size();
        int sharedWardSlots = Math.min(
                MAX_SHARED_WARD_SLOTS,
                BASE_SHARED_WARD_SLOTS + Math.max(0, nearbyPlayers - 1) * WARD_SLOTS_PER_EXTRA_PLAYER);
        List<EcclesiasticEntity> nearbyEcclesiastics = new ArrayList<>(level.getEntitiesOfClass(
                EcclesiasticEntity.class,
                getBoundingBox().inflate(WARD_SCALING_RADIUS),
                EcclesiasticEntity::isAlive));
        if (!nearbyEcclesiastics.contains(this)) {
            nearbyEcclesiastics.add(this);
        }
        nearbyEcclesiastics.sort(Comparator.comparing(Entity::getUUID));
        int casterCount = nearbyEcclesiastics.size();
        int rank = nearbyEcclesiastics.indexOf(this);
        int capacity = sharedWardSlots / casterCount;
        if (rank < sharedWardSlots % casterCount) {
            capacity++;
        }
        return Math.max(1, capacity);
    }

    private void resizeWardSlots(ServerLevel level, int capacity) {
        while (wardSlots.size() < capacity) {
            wardSlots.add(new WardSlot());
        }
        while (wardSlots.size() > capacity) {
            WardSlot removed = wardSlots.removeLast();
            clearOwnedWard(resolveTarget(level, removed.targetId));
        }
    }

    private Mob findWardTarget(Set<UUID> assignedTargets) {
        return level().getEntitiesOfClass(
                        Mob.class,
                        getBoundingBox().inflate(WARD_ACQUISITION_RADIUS),
                        target -> canAcquireWard(target) && !assignedTargets.contains(target.getUUID()))
                .stream()
                .min(Comparator.<Mob>comparingInt(target -> target.getTarget() instanceof Player ? 0 : 1)
                        .thenComparingDouble(target -> target.getHealth() / Math.max(1.0F, target.getMaxHealth()))
                        .thenComparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    private boolean canAcquireWard(Mob target) {
        if (target == this || target instanceof EcclesiasticEntity
                || !(target instanceof Enemy) || !target.isAlive()) {
            return false;
        }
        return !target.hasEffect(ModEffects.WARD);
    }

    private boolean canRetainWard(LivingEntity target) {
        return target != this
                && !(target instanceof EcclesiasticEntity)
                && target instanceof Enemy
                && target.isAlive()
                && distanceToSqr(target) <= WARD_RETENTION_RADIUS * WARD_RETENTION_RADIUS;
    }

    private void bindWard(WardSlot slot, Mob target) {
        slot.targetId = target.getUUID();
        refreshWard(target);
    }

    private void refreshWard(LivingEntity target) {
        MobEffectInstance currentWard = target.getEffect(ModEffects.WARD);
        Optional<UUID> sourceId = target.getExistingDataOrNull(ModAttachments.ECCLESIASTIC_WARD_SOURCE);
        if (currentWard != null && currentWard.getDuration() > WARD_REFRESH_INTERVAL * 2
                && sourceId != null && sourceId.isPresent() && sourceId.get().equals(getUUID())) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.WARD, WARD_LEASE_DURATION, 0, false, false, false), this);
        target.setData(ModAttachments.ECCLESIASTIC_WARD_SOURCE, Optional.of(getUUID()));
    }

    private void releaseSlot(WardSlot slot, LivingEntity target, long gameTime) {
        clearOwnedWard(target);
        slot.targetId = null;
        slot.readyAt = gameTime + EMPTY_SLOT_COOLDOWN;
    }

    private void beginWardCast() {
        spellCastingTickCount = Math.max(spellCastingTickCount, WARD_CAST_TICKS);
        setIsCastingSpell(IllagerSpell.SUMMON_VEX);
        playSound(getCastingSoundEvent(), 1.0F, 1.0F);
    }

    private void clearAllWards(ServerLevel level) {
        for (WardSlot slot : wardSlots) {
            clearOwnedWard(resolveTarget(level, slot.targetId));
        }
        wardSlots.clear();
    }

    private LivingEntity resolveTarget(ServerLevel level, UUID targetId) {
        if (targetId == null) {
            return null;
        }
        Entity entity = level.getEntity(targetId);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private void clearOwnedWard(LivingEntity target) {
        if (target == null) {
            return;
        }
        Optional<UUID> sourceId = target.getExistingDataOrNull(ModAttachments.ECCLESIASTIC_WARD_SOURCE);
        if (sourceId != null && sourceId.isPresent() && sourceId.get().equals(getUUID())) {
            clearWard(target);
        }
    }

    private static void clearWard(LivingEntity target) {
        target.removeEffect(ModEffects.WARD);
        target.removeData(ModAttachments.ECCLESIASTIC_WARD_SOURCE);
        target.removeData(ModAttachments.ECCLESIASTIC_WARD_VISUAL);
    }

    private final class WardCastingGoal extends SpellcasterCastingSpellGoal {
    }

    private static final class WardSlot {
        private UUID targetId;
        private long readyAt;
    }
}
