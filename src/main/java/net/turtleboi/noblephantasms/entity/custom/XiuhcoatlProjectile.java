package net.turtleboi.noblephantasms.entity.custom;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;

public final class XiuhcoatlProjectile extends AbstractArrow
        implements ItemSupplier {
    public static final float HEAD_HITBOX_WIDTH = 9.3F / 16.0F;
    public static final float HEAD_HITBOX_HEIGHT = 7.3F / 16.0F;

    private static final EntityDataAccessor<Integer> TARGET =
            SynchedEntityData.defineId(XiuhcoatlProjectile.class, EntityDataSerializers.INT);
    private static final double TARGET_RANGE = 12.0;
    private static final double TURN_RADIUS = 3.0;
    private static final double TERMINAL_TURN_RADIUS = 0.65;
    private static final double TERMINAL_GUIDANCE_DISTANCE = 4.0;
    private static final double TERMINAL_ALIGNMENT = 0.35;
    private static final double CRUISING_SPEED = 0.4;
    private static final float OWNER_LOOK_PRIORITY_ANGLE = 15.0F;
    private static final int MAX_TOTAL_HITS = 13;
    private static final int MAX_HITS_PER_TARGET = 3;
    private static final int REHIT_DELAY_TICKS = 10;
    private static final int NO_TARGET_TIMEOUT_TICKS = 160;
    private static final int ROTATION_HISTORY_LENGTH = 8;

    private final Map<Integer, Integer> targetHitCounts = new HashMap<>();
    private final Map<Integer, Integer> targetLastHitTicks = new HashMap<>();
    private final float[] xRotationHistory = new float[ROTATION_HISTORY_LENGTH];
    private final float[] yRotationHistory = new float[ROTATION_HISTORY_LENGTH];
    private int totalHits;
    private int ticksWithoutTarget;
    private int rotationHistoryHead = -1;

    public XiuhcoatlProjectile(EntityType<? extends XiuhcoatlProjectile> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
    }

    public XiuhcoatlProjectile(ServerLevel level, LivingEntity owner) {
        super(ModEntities.XIUHCOATL.get(), owner, level,
                new ItemStack(ModItems.XIUHCOATL.get()), new ItemStack(ModItems.XIUHCOATL.get()));
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET, -1);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        double halfWidth = getBbWidth() * 0.5;
        double halfHeight = getBbHeight() * 0.5;
        return new AABB(position.x - halfWidth, position.y - halfHeight, position.z - halfWidth,
                position.x + halfWidth, position.y + halfHeight, position.z + halfWidth);
    }

    public void acquireTarget() {
        Entity currentOwner = getOwner();
        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(TARGET_RANGE), candidate ->
                        candidate.isAlive() && candidate != currentOwner && candidate instanceof Enemy
                                && (currentOwner == null || !currentOwner.isAlliedTo(candidate)));
        Comparator<LivingEntity> targetPriority = Comparator.<LivingEntity>comparingInt(
                        candidate -> isWithinOwnerLookPriority(candidate, currentOwner) ? 0 : 1)
                .thenComparingDouble(this::distanceToSqr);
        LivingEntity target = candidates.stream()
                .filter(candidate -> targetHitCounts.getOrDefault(candidate.getId(), 0) == 0)
                .min(targetPriority)
                .orElseGet(() -> candidates.stream()
                        .filter(candidate -> targetHitCounts.getOrDefault(candidate.getId(), 0)
                                < MAX_HITS_PER_TARGET)
                        .min(targetPriority)
                        .orElse(null));
        entityData.set(TARGET, target == null ? -1 : target.getId());
    }

    private boolean isWithinOwnerLookPriority(LivingEntity candidate, Entity currentOwner) {
        if (!(currentOwner instanceof LivingEntity livingOwner)) {
            return false;
        }
        Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(livingOwner.getEyePosition());
        Vec3 look = livingOwner.getLookAngle();
        if (toTarget.lengthSqr() < 1.0E-8 || look.lengthSqr() < 1.0E-8) {
            return true;
        }
        float targetYaw = (float) (Mth.atan2(toTarget.x, toTarget.z) * Mth.RAD_TO_DEG);
        float lookYaw = (float) (Mth.atan2(look.x, look.z) * Mth.RAD_TO_DEG);
        float targetPitch = (float) (Mth.atan2(toTarget.y, toTarget.horizontalDistance()) * Mth.RAD_TO_DEG);
        float lookPitch = (float) (Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG);
        return Mth.abs(Mth.wrapDegrees(targetYaw - lookYaw)) <= OWNER_LOOK_PRIORITY_ANGLE
                && Mth.abs(Mth.wrapDegrees(targetPitch - lookPitch)) <= OWNER_LOOK_PRIORITY_ANGLE;
    }

    @Override
    public void tick() {
        boolean trackingTarget = false;
        if (!isInGround()) {
            trackingTarget = steerTowardTarget(!isNoPhysics());
        }
        super.tick();
        setNoGravity(true);
        if (isNoPhysics() && level().noCollision(getBoundingBox())) {
            setNoPhysics(false);
        }
        maintainCruisingSpeed();
        recordRotation();
        if (level().isClientSide()) {
            Vec3 movement = getDeltaMovement();
            for (int i = 0; i < 3; i++) {
                double trail = i / 3.0;
                level().addParticle(i == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        getX() - movement.x * trail, getY() - movement.y * trail,
                        getZ() - movement.z * trail, 0.0, 0.0, 0.0);
            }
        }
        if (!level().isClientSide()) {
            if (trackingTarget) {
                ticksWithoutTarget = 0;
            } else if (++ticksWithoutTarget > NO_TARGET_TIMEOUT_TICKS) {
                discard();
            }
        }
    }

    private void maintainCruisingSpeed() {
        Vec3 movement = getDeltaMovement();
        if (isAlive() && !isInGround() && movement.lengthSqr() > 1.0E-4) {
            setDeltaMovement(movement.normalize().scale(CRUISING_SPEED));
        }
    }

    private boolean steerTowardTarget(boolean allowTargetAcquisition) {
        Entity entity = level().getEntity(entityData.get(TARGET));
        if (allowTargetAcquisition && !level().isClientSide() && entity != null
                && targetHitCounts.getOrDefault(entity.getId(), 0) > 0 && hasUnhitEnemyAround()) {
            acquireTarget();
            entity = level().getEntity(entityData.get(TARGET));
        }
        if (!isDetectedTarget(entity)) {
            if (!level().isClientSide()) {
                entityData.set(TARGET, -1);
                if (allowTargetAcquisition) {
                    acquireTarget();
                    entity = level().getEntity(entityData.get(TARGET));
                }
            }
            if (!isDetectedTarget(entity)) {
                return false;
            }
        }
        LivingEntity target = (LivingEntity) entity;
        Vec3 movement = getDeltaMovement();
        Vec3 desired = target.getBoundingBox().getCenter().subtract(position());
        if (movement.lengthSqr() > 1.0E-4 && desired.lengthSqr() > 1.0E-4) {
            double speed = movement.length();
            Vec3 direction = movement.scale(1.0 / speed);
            Vec3 desiredDirection = desired.normalize();
            double distance = desired.length();
            double alignment = direction.dot(desiredDirection);
            double turnRadius = TURN_RADIUS;
            if (distance < TERMINAL_GUIDANCE_DISTANCE) {
                double proximity = Mth.clamp(distance / TERMINAL_GUIDANCE_DISTANCE, 0.0, 1.0);
                turnRadius = Mth.lerp(proximity * proximity, TERMINAL_TURN_RADIUS, TURN_RADIUS);
            }
            if (alignment < TERMINAL_ALIGNMENT) {
                turnRadius = TERMINAL_TURN_RADIUS;
            }
            setDeltaMovement(rotateToward(direction, desiredDirection, speed / turnRadius).scale(speed));
        }
        return true;
    }

    private boolean isDetectedTarget(Entity entity) {
        return entity instanceof LivingEntity livingEntity && livingEntity.isAlive()
                && distanceToSqr(entity) <= TARGET_RANGE * TARGET_RANGE;
    }

    private static Vec3 rotateToward(Vec3 direction, Vec3 desiredDirection, double maximumAngle) {
        double dot = Math.max(-1.0, Math.min(direction.dot(desiredDirection), 1.0));
        double angle = Math.acos(dot);
        if (angle <= maximumAngle) {
            return desiredDirection;
        }
        Vec3 axis = direction.cross(desiredDirection);
        if (axis.lengthSqr() < 1.0E-8) {
            axis = Math.abs(direction.y) < 0.9
                    ? new Vec3(0.0, 1.0, 0.0)
                    : new Vec3(1.0, 0.0, 0.0);
        }
        axis = axis.normalize();
        double cosine = Math.cos(maximumAngle);
        double sine = Math.sin(maximumAngle);
        return direction.scale(cosine).add(axis.cross(direction).scale(sine)).normalize();
    }

    private void recordRotation() {
        float xRotation = getTravelXRot();
        float yRotation = getTravelYRot();
        if (rotationHistoryHead < 0) {
            Arrays.fill(xRotationHistory, xRotation);
            Arrays.fill(yRotationHistory, yRotation);
            rotationHistoryHead = 0;
            return;
        }
        rotationHistoryHead = (rotationHistoryHead + 1) % ROTATION_HISTORY_LENGTH;
        xRotationHistory[rotationHistoryHead] = xRotation;
        yRotationHistory[rotationHistoryHead] = yRotation;
    }

    public float getDelayedXRot(int delay, float partialTick) {
        return getDelayedRotation(xRotationHistory, delay, partialTick, getTravelXRot());
    }

    public float getDelayedYRot(int delay, float partialTick) {
        return getDelayedRotation(yRotationHistory, delay, partialTick, getTravelYRot());
    }

    private float getTravelXRot() {
        Vec3 movement = getDeltaMovement();
        return movement.lengthSqr() > 1.0E-4
                ? (float) (Mth.atan2(movement.y, movement.horizontalDistance()) * Mth.RAD_TO_DEG)
                : getXRot();
    }

    private float getTravelYRot() {
        Vec3 movement = getDeltaMovement();
        return movement.lengthSqr() > 1.0E-4
                ? (float) (Mth.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG)
                : getYRot();
    }

    private float getDelayedRotation(float[] history, int delay, float partialTick, float fallback) {
        if (rotationHistoryHead < 0) {
            return fallback;
        }
        int recentIndex = Math.clamp(delay, 0, ROTATION_HISTORY_LENGTH - 2);
        float recent = history[Math.floorMod(rotationHistoryHead - recentIndex, ROTATION_HISTORY_LENGTH)];
        float previous = history[Math.floorMod(rotationHistoryHead - recentIndex - 1, ROTATION_HISTORY_LENGTH)];
        return Mth.rotLerp(partialTick, previous, recent);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        int targetId = target.getId();
        int targetHits = targetHitCounts.getOrDefault(targetId, 0);
        if (targetHits >= MAX_HITS_PER_TARGET) {
            return;
        }
        targetHitCounts.put(targetId, targetHits + 1);
        targetLastHitTicks.put(targetId, tickCount);
        totalHits++;
        Entity currentOwner = getOwner();
        if (level() instanceof ServerLevel serverLevel) {
            DamageSource source = damageSources().trident(
                    this, currentOwner == null ? this : currentOwner);
            target.hurtServer(serverLevel, source, 10.0F);
            target.igniteForSeconds(6.0F);
            entityData.set(TARGET, -1);
            if (totalHits < MAX_TOTAL_HITS) {
                acquireTarget();
            }
        }
        playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.7F);
        if (totalHits >= MAX_TOTAL_HITS) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        int entityId = entity.getId();
        int hitCount = targetHitCounts.getOrDefault(entityId, 0);
        if (hitCount >= MAX_HITS_PER_TARGET) {
            return false;
        }
        if (hitCount > 0 && (entityData.get(TARGET) != entityId
                || tickCount - targetLastHitTicks.getOrDefault(entityId, tickCount) < REHIT_DELAY_TICKS
                || hasUnhitEnemyAround())) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    private boolean hasUnhitEnemyAround() {
        Entity currentOwner = getOwner();
        return !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(TARGET_RANGE),
                candidate -> candidate.isAlive() && candidate != currentOwner && candidate instanceof Enemy
                        && targetHitCounts.getOrDefault(candidate.getId(), 0) == 0
                        && (currentOwner == null || !currentOwner.isAlliedTo(candidate))).isEmpty();
    }

    @Override
    public byte getPierceLevel() {
        return MAX_TOTAL_HITS - 1;
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        Vec3 movement = getDeltaMovement();
        setInGround(false);
        setNoPhysics(true);
        setPos(hitResult.getLocation().add(movement.normalize().scale(0.01)));
        setDeltaMovement(movement);
    }

    @Override
    public ItemStack getWeaponItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.XIUHCOATL.get());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.XIUHCOATL.get());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(TARGET, input.getIntOr("Target", -1));
        totalHits = input.getIntOr("TotalHits", 0);
        ticksWithoutTarget = input.getIntOr("NoTargetTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Target", entityData.get(TARGET));
        output.putInt("TotalHits", totalHits);
        output.putInt("NoTargetTicks", ticksWithoutTarget);
    }
}
