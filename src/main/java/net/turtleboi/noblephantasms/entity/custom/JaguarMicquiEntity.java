package net.turtleboi.noblephantasms.entity.custom;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;

public final class JaguarMicquiEntity extends Monster {
    private static final EntityDataAccessor<Float> STEALTH_PROGRESS = SynchedEntityData.defineId(
            JaguarMicquiEntity.class, EntityDataSerializers.FLOAT);
    private static final Identifier AMBUSH_SPEED_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "jaguar_micqui_ambush_speed");
    private static final AttributeModifier AMBUSH_SPEED = new AttributeModifier(
            AMBUSH_SPEED_ID, 0.55, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final int MODE_STALKING = 0;
    private static final int MODE_FLEEING = 1;
    private static final int MODE_AMBUSHING = 2;
    private static final int FLEE_DURATION = 70;
    private static final int COMBAT_TIMEOUT = 300;
    private static final double EYE_CONTACT_DOT = 0.975;
    private static final double PLAYER_VIEW_DOT = 0.55;
    private static final double FORCED_AMBUSH_DISTANCE_SQUARED = 12.25;
    private static final double AMBUSH_DISTANCE_SQUARED = 100.0;
    private static final double AMBUSH_DISENGAGE_DISTANCE_SQUARED = 1600.0;
    private static final double FLEE_CLEAR_DISTANCE_SQUARED = 256.0;
    private float previousStealthProgress;
    private int behaviorMode = MODE_STALKING;
    private int fleeTicks;
    private int combatIdleTicks;
    private int pathUpdateTicks;

    public JaguarMicquiEntity(EntityType<? extends JaguarMicquiEntity> type, Level level) {
        super(type, level);
        xpReward = 12;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MACUAHUITL.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 128.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new JaguarMicquiMeleeGoal());
        goalSelector.addGoal(3, new JaguarMicquiStalkGoal());
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        NearestAttackableTargetGoal<Player> playerTargetGoal =
                new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null);
        playerTargetGoal.setUnseenMemoryTicks(1200);
        targetSelector.addGoal(2, playerTargetGoal);
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void tick() {
        previousStealthProgress = entityData.get(STEALTH_PROGRESS);
        super.tick();
        if (level() instanceof ServerLevel level) {
            updateBehavior(level);
        }
    }

    private void updateBehavior(ServerLevel level) {
        if (!(getTarget() instanceof Player player) || !player.isAlive()) {
            leavePlayerHunt();
            return;
        }
        if (behaviorMode == MODE_AMBUSHING) {
            updateAmbush(player);
            return;
        }
        setPose(Pose.CROUCHING);
        setStealthProgress(Math.min(1.0F, getStealthProgress() + 0.055F));
        if (behaviorMode == MODE_FLEEING) {
            updateFlee(player);
            return;
        }
        removeAmbushSpeed();
        setSprinting(false);
        if (hasEyeContact(player)) {
            beginFlee();
            return;
        }
        double distance = distanceToSqr(player);
        if ((!isInPlayerView(player) && distance <= AMBUSH_DISTANCE_SQUARED
                && getStealthProgress() >= 0.75F)
                || distance <= FORCED_AMBUSH_DISTANCE_SQUARED) {
            beginAmbush(level);
        }
    }

    private void updateAmbush(Player player) {
        setPose(Pose.STANDING);
        setStealthProgress(Math.max(0.0F, getStealthProgress() - 0.13F));
        applyAmbushSpeed();
        setSprinting(true);
        combatIdleTicks++;
        if (distanceToSqr(player) > AMBUSH_DISENGAGE_DISTANCE_SQUARED
                || combatIdleTicks >= COMBAT_TIMEOUT) {
            beginFlee();
        }
    }

    private void updateFlee(Player player) {
        applyAmbushSpeed();
        setSprinting(true);
        fleeTicks--;
        if (fleeTicks <= 0 && distanceToSqr(player) >= FLEE_CLEAR_DISTANCE_SQUARED
                && !hasEyeContact(player)) {
            behaviorMode = MODE_STALKING;
            removeAmbushSpeed();
            setSprinting(false);
        }
    }

    private void leavePlayerHunt() {
        behaviorMode = MODE_STALKING;
        fleeTicks = 0;
        combatIdleTicks = 0;
        pathUpdateTicks = 0;
        setPose(Pose.STANDING);
        setStealthProgress(Math.max(0.0F, getStealthProgress() - 0.08F));
        removeAmbushSpeed();
        setSprinting(false);
    }

    private void beginAmbush(ServerLevel level) {
        behaviorMode = MODE_AMBUSHING;
        combatIdleTicks = 0;
        pathUpdateTicks = 0;
        setPose(Pose.STANDING);
        applyAmbushSpeed();
        setSprinting(true);
        level.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING,
                SoundSource.HOSTILE, 1.5F, 0.8F + random.nextFloat() * 0.1F);
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.75,
                getZ(), 12, 0.45, 0.5, 0.45, 0.04);
    }

    private void beginFlee() {
        behaviorMode = MODE_FLEEING;
        fleeTicks = FLEE_DURATION;
        combatIdleTicks = 0;
        pathUpdateTicks = 0;
        setPose(Pose.CROUCHING);
        applyAmbushSpeed();
        setSprinting(true);
        getNavigation().stop();
    }

    private boolean isInPlayerView(Player player) {
        return player.hasLineOfSight(this) && playerViewDot(player) >= PLAYER_VIEW_DOT;
    }

    private boolean hasEyeContact(Player player) {
        return player.hasLineOfSight(this) && playerViewDot(player) >= EYE_CONTACT_DOT;
    }

    private double playerViewDot(Player player) {
        Vec3 direction = getEyePosition().subtract(player.getEyePosition());
        if (direction.lengthSqr() < 1.0E-6) {
            return 1.0;
        }
        return player.getLookAngle().normalize().dot(direction.normalize());
    }

    private void applyAmbushSpeed() {
        AttributeInstance movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.addOrUpdateTransientModifier(AMBUSH_SPEED);
        }
    }

    private void removeAmbushSpeed() {
        AttributeInstance movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(AMBUSH_SPEED_ID);
        }
    }

    private boolean isAmbushing() {
        return behaviorMode == MODE_AMBUSHING;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && behaviorMode == MODE_AMBUSHING && target == getTarget()) {
            combatIdleTicks = 0;
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && behaviorMode == MODE_AMBUSHING && source.getEntity() == getTarget()) {
            combatIdleTicks = 0;
        }
        return hurt;
    }

    public float getStealthProgress() {
        return entityData.get(STEALTH_PROGRESS);
    }

    public float getStealthProgress(float partialTick) {
        return Mth.lerp(partialTick, previousStealthProgress, getStealthProgress());
    }

    private void setStealthProgress(float progress) {
        entityData.set(STEALTH_PROGRESS, Mth.clamp(progress, 0.0F, 1.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STEALTH_PROGRESS, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setStealthProgress(input.getFloatOr("StealthProgress", 0.0F));
        behaviorMode = Mth.clamp(input.getIntOr("BehaviorMode", MODE_STALKING),
                MODE_STALKING, MODE_AMBUSHING);
        fleeTicks = input.getIntOr("FleeTicks", 0);
        combatIdleTicks = input.getIntOr("CombatIdleTicks", 0);
        if (behaviorMode != MODE_STALKING) {
            applyAmbushSpeed();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("StealthProgress", getStealthProgress());
        output.putInt("BehaviorMode", behaviorMode);
        output.putInt("FleeTicks", fleeTicks);
        output.putInt("CombatIdleTicks", combatIdleTicks);
    }

    private final class JaguarMicquiMeleeGoal extends MeleeAttackGoal {
        private JaguarMicquiMeleeGoal() {
            super(JaguarMicquiEntity.this, 1.1, false);
        }

        @Override
        public boolean canUse() {
            return (!(getTarget() instanceof Player) || isAmbushing()) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return (!(getTarget() instanceof Player) || isAmbushing()) && super.canContinueToUse();
        }
    }

    private final class JaguarMicquiStalkGoal extends Goal {
        private JaguarMicquiStalkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() instanceof Player player && player.isAlive() && !isAmbushing();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(getTarget() instanceof Player player)) {
                return;
            }
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (pathUpdateTicks > 0) {
                pathUpdateTicks--;
            }
            if (behaviorMode == MODE_FLEEING) {
                moveAwayFrom(player, 1.2);
            } else if (isInPlayerView(player)) {
                moveToCover(player);
            } else {
                moveTowardAmbush(player);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        private void moveAwayFrom(Player player, double speed) {
            if (pathUpdateTicks > 0 && !getNavigation().isDone()) {
                return;
            }
            Vec3 destination = DefaultRandomPos.getPosAway(
                    JaguarMicquiEntity.this, 20, 8, player.position());
            if (destination == null) {
                Vec3 away = position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
                if (away.lengthSqr() < 1.0E-4) {
                    away = getLookAngle().reverse().multiply(1.0, 0.0, 1.0);
                }
                destination = position().add(away.normalize().scale(16.0));
            }
            getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
            pathUpdateTicks = 8;
        }

        private void moveToCover(Player player) {
            if (distanceToSqr(player) < 196.0) {
                moveAwayFrom(player, 0.95);
                return;
            }
            if (pathUpdateTicks > 0 && !getNavigation().isDone()) {
                return;
            }
            Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
            if (look.lengthSqr() < 1.0E-4) {
                moveAwayFrom(player, 0.95);
                return;
            }
            look = look.normalize();
            double sideDistance = 5.0 + random.nextDouble() * 7.0;
            if (random.nextBoolean()) {
                sideDistance = -sideDistance;
            }
            Vec3 side = new Vec3(-look.z, 0.0, look.x).scale(sideDistance);
            Vec3 destination = player.position().subtract(look.scale(18.0)).add(side);
            getNavigation().moveTo(destination.x, destination.y, destination.z, 0.95);
            pathUpdateTicks = 12;
        }

        private void moveTowardAmbush(Player player) {
            double distance = distanceToSqr(player);
            if (distance <= AMBUSH_DISTANCE_SQUARED || pathUpdateTicks > 0) {
                return;
            }
            getNavigation().moveTo(player, 1.0);
            pathUpdateTicks = 6;
        }
    }
}
