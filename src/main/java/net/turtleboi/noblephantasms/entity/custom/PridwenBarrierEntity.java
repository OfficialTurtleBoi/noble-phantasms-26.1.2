package net.turtleboi.noblephantasms.entity.custom;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.custom.PridwenItem;
import org.jspecify.annotations.Nullable;

public final class PridwenBarrierEntity extends Entity {
    private static final float MODEL_WIDTH = 12.0F / 16.0F;
    private static final float MODEL_HEIGHT = 14.0F / 16.0F;
    private static final float PLATE_CENTER_X = 8.0F / 16.0F;
    private static final float PLATE_CENTER_Y = 11.0F / 16.0F;
    private static final float PLATE_DEPTH = 1.0F / 16.0F;
    private static final CollisionColumn[] COLLISION_COLUMNS = {
            column(2, 3, 9, 16),
            column(3, 4, 7, 17),
            column(4, 5, 6, 18),
            column(5, 6, 5, 18),
            column(6, 7, 4, 18),
            column(7, 8, 4, 18),
            column(8, 9, 4, 18),
            column(9, 10, 4, 18),
            column(10, 11, 5, 18),
            column(11, 12, 6, 18),
            column(12, 13, 7, 17),
            column(13, 14, 9, 16)
    };
    private static final CollisionColumn[] COLLISION_TILES = createCollisionTiles();
    private static final float HELD_SCALE = 1.25F;
    public static final float MODEL_SCALE = 4.0F;
    public static final double WIDTH = MODEL_WIDTH * MODEL_SCALE;
    public static final double HEIGHT = MODEL_HEIGHT * MODEL_SCALE;
    private static final int DEPLOYMENT_TICKS = 8;
    private static final int RETRACTION_TICKS = 8;
    private static final int CONTACT_DAMAGE_INTERVAL_TICKS = 20;
    private static final int CLIENT_RETURN_LATCH_TICKS = 4;
    private static final Map<UUID, ClientReturnLatch> CLIENT_RETURN_LATCHES = new HashMap<>();
    private static final double RAISED_DISTANCE = 0.7;
    private static final double DISTANCE = 2.25;
    private static final double VERTICAL_OFFSET = -0.1;
    private static final EntityDataAccessor<Float> PROJECTION_PROGRESS =
            SynchedEntityData.defineId(PridwenBarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(PridwenBarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RETRACTING =
            SynchedEntityData.defineId(PridwenBarrierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> OFF_HAND =
            SynchedEntityData.defineId(PridwenBarrierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HEALTH_PROGRESS =
            SynchedEntityData.defineId(PridwenBarrierEntity.class, EntityDataSerializers.FLOAT);
    private @Nullable UUID ownerId;
    private float previousProjectionProgress;
    private float projectionProgress;
    private float previousHealthProgress = 1.0F;
    private float healthProgress = 1.0F;
    private final Map<UUID, Long> contactDamageCooldowns = new HashMap<>();
    private final PridwenBarrierPart[] collisionParts;

    public PridwenBarrierEntity(EntityType<? extends PridwenBarrierEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        collisionParts = new PridwenBarrierPart[COLLISION_TILES.length];
        for (int i = 0; i < collisionParts.length; i++) {
            collisionParts[i] = new PridwenBarrierPart(this);
        }
        setId(ENTITY_COUNTER.getAndAdd(collisionParts.length + 1) + 1);
        updateCollisionParts();
    }

    public PridwenBarrierEntity(ServerLevel level, Player owner) {
        this(ModEntities.PRIDWEN_BARRIER.get(), level);
        ownerId = owner.getUUID();
        entityData.set(OFF_HAND, owner.getUsedItemHand() == InteractionHand.OFF_HAND);
        entityData.set(OWNER_ID, owner.getId());
        healthProgress = PridwenItem.getBarrierHealthProgress(owner.getUseItem());
        previousHealthProgress = healthProgress;
        entityData.set(HEALTH_PROGRESS, healthProgress);
        updatePosition(owner, 0.0F);
    }

    public static void ensureActive(ServerLevel level, Player owner) {
        ItemStack shield = owner.getUseItem();
        if (!(shield.getItem() instanceof PridwenItem) || PridwenItem.isBarrierBroken(shield)) {
            return;
        }
        if (!level.getEntitiesOfClass(PridwenBarrierEntity.class,
                owner.getBoundingBox().inflate(8.0), barrier -> barrier.isOwnedBy(owner)).isEmpty()) {
            return;
        }
        level.addFreshEntity(new PridwenBarrierEntity(level, owner));
    }

    public static boolean beginRetraction(Player owner) {
        InteractionHand hand = owner.getUsedItemHand();
        if (owner.level().isClientSide()) {
            CLIENT_RETURN_LATCHES.put(owner.getUUID(), new ClientReturnLatch(
                    hand,
                    owner.level().getGameTime() + CLIENT_RETURN_LATCH_TICKS
            ));
        }
        boolean found = false;
        for (PridwenBarrierEntity barrier : owner.level().getEntitiesOfClass(PridwenBarrierEntity.class,
                owner.getBoundingBox().inflate(8.0), candidate -> candidate.isOwnedBy(owner))) {
            barrier.entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
            barrier.entityData.set(RETRACTING, true);
            found = true;
        }
        return found;
    }

    public static @Nullable InteractionHand getReturningHand(LivingEntity owner) {
        for (PridwenBarrierEntity barrier : owner.level().getEntitiesOfClass(PridwenBarrierEntity.class,
                owner.getBoundingBox().inflate(8.0), candidate -> candidate.isOwnedBy(owner)
                        && candidate.entityData.get(RETRACTING)
                        && candidate.entityData.get(PROJECTION_PROGRESS) > 0.0F)) {
            return barrier.getShieldHand();
        }
        if (owner.level().isClientSide()) {
            ClientReturnLatch latch = CLIENT_RETURN_LATCHES.get(owner.getUUID());
            if (latch != null) {
                if (owner.level().getGameTime() <= latch.expiresAt()
                        && owner.getItemInHand(latch.hand()).getItem() instanceof PridwenItem) {
                    return latch.hand();
                }
                CLIENT_RETURN_LATCHES.remove(owner.getUUID());
            }
        }
        return null;
    }

    public static boolean shouldKeepRaised(LivingEntity owner, InteractionHand hand, ItemStack itemStack) {
        return itemStack.getItem() instanceof PridwenItem && getReturningHand(owner) == hand;
    }

    public static boolean tryBlockDamage(ServerLevel level, DamageSource source, Entity target, float amount) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null) {
            return false;
        }
        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof net.minecraft.world.entity.LivingEntity) {
            return false;
        }
        BarrierHit hit = findHit(level, sourcePosition, target.getBoundingBox().getCenter(), source.getEntity());
        if (hit == null) {
            return false;
        }
        hit.barrier.absorb(level, amount, hit.position);
        if (directEntity instanceof Projectile projectile) {
            projectile.discard();
        }
        return true;
    }

    private static @Nullable BarrierHit findHit(ServerLevel level, Vec3 start, Vec3 end,
                                                 @Nullable Entity attacker) {
        AABB searchArea = new AABB(start, end).inflate(WIDTH * 0.5, HEIGHT * 0.5, WIDTH * 0.5);
        BarrierHit nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PridwenBarrierEntity barrier : level.getEntitiesOfClass(
                PridwenBarrierEntity.class, searchArea, Entity::isAlive)) {
            if (!barrier.blocks(attacker)) {
                continue;
            }
            Vec3 intersection = barrier.intersection(start, end);
            if (intersection == null) {
                continue;
            }
            double distance = start.distanceToSqr(intersection);
            if (distance < nearestDistance) {
                nearest = new BarrierHit(barrier, intersection);
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private @Nullable Vec3 intersection(Vec3 start, Vec3 end) {
        Vec3 forward = barrierForward();
        double startDepth = start.subtract(position()).dot(forward);
        double endDepth = end.subtract(position()).dot(forward);
        if (startDepth < 0.0 || endDepth > 0.0 || startDepth == endDepth) {
            return null;
        }
        double progress = startDepth / (startDepth - endDepth);
        if (progress < 0.0 || progress > 1.0) {
            return null;
        }
        Vec3 intersection = start.lerp(end, progress);
        Vec3 offset = intersection.subtract(position());
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        if (right.lengthSqr() < 1.0E-7) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        float scale = getVisualScale(1.0F);
        double localX = offset.dot(right) / scale;
        double localY = offset.dot(up) / scale;
        for (CollisionColumn column : COLLISION_COLUMNS) {
            if (column.contains(localX, localY)) {
                return intersection;
            }
        }
        return null;
    }

    private boolean blocks(@Nullable Entity attacker) {
        Player owner = getOwnerEntity();
        return owner != null && (isUsingPridwen(owner) || entityData.get(RETRACTING))
                && attacker != owner && (attacker == null || !owner.isAlliedTo(attacker));
    }

    boolean canBlockProjectile(Projectile projectile) {
        return blocks(projectile.getOwner())
                && projectile.getDeltaMovement().dot(barrierForward()) < -1.0E-7;
    }

    boolean absorbProjectileImpact(ServerLevel level, DamageSource source,
                                   float amount, Vec3 impact) {
        if (!blocks(source.getEntity())) {
            return false;
        }
        absorb(level, amount, impact);
        if (source.getDirectEntity() instanceof Projectile projectile) {
            projectile.discard();
        }
        return true;
    }

    private boolean absorb(ServerLevel level, float damage, Vec3 impact) {
        Player owner = getOwnerEntity();
        if (owner == null) {
            return false;
        }
        InteractionHand hand = getShieldHand();
        ItemStack shield = owner.getItemInHand(hand);
        if (!(shield.getItem() instanceof PridwenItem)) {
            return false;
        }
        if (PridwenItem.isBarrierBroken(shield)) {
            return true;
        }
        boolean broken = PridwenItem.damageBarrier(shield, damage);
        healthProgress = PridwenItem.getBarrierHealthProgress(shield);
        entityData.set(HEALTH_PROGRESS, healthProgress);
        level.playSound(null, impact.x, impact.y, impact.z,
                broken ? SoundEvents.SHIELD_BREAK : SoundEvents.SHIELD_BLOCK,
                owner.getSoundSource(), broken ? 1.3F : 1.0F, broken ? 0.75F : 1.25F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z,
                broken ? 28 : 8, broken ? 0.45 : 0.18, broken ? 0.45 : 0.18,
                broken ? 0.45 : 0.18, broken ? 0.1 : 0.04);
        if (broken) {
            entityData.set(RETRACTING, true);
            if (owner.isUsingItem()) {
                owner.releaseUsingItem();
            }
        }
        return broken;
    }

    private boolean isOwnedBy(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    private boolean isOwnedBy(LivingEntity entity) {
        if (level().isClientSide()) {
            return entity.getId() == entityData.get(OWNER_ID);
        }
        return ownerId != null && ownerId.equals(entity.getUUID());
    }

    private InteractionHand getShieldHand() {
        return entityData.get(OFF_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private record ClientReturnLatch(InteractionHand hand, long expiresAt) {
    }

    public @Nullable Player getOwnerEntity() {
        if (level().isClientSide()) {
            Entity owner = level().getEntity(entityData.get(OWNER_ID));
            return owner instanceof Player player ? player : null;
        }
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity owner = serverLevel.getEntity(ownerId);
        return owner instanceof Player player ? player : null;
    }

    private Vec3 barrierForward() {
        return Vec3.directionFromRotation(getXRot(), getYRot()).normalize();
    }

    private void updateCollisionParts() {
        if (collisionParts == null) {
            return;
        }
        setBoundingBox(makeBoundingBox(position()));
        Vec3 forward = barrierForward();
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        if (right.lengthSqr() < 1.0E-7) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        double scale = getVisualScale(1.0F);
        double halfDepth = PLATE_DEPTH * scale * 0.5;
        for (int i = 0; i < collisionParts.length; i++) {
            CollisionColumn column = COLLISION_TILES[i];
            double halfWidth = column.width() * scale * 0.5;
            double halfHeight = column.height() * scale * 0.5;
            Vec3 center = position()
                    .add(right.scale(column.centerX() * scale))
                    .add(up.scale(column.centerY() * scale));
            double extentX = Math.abs(right.x) * halfWidth
                    + Math.abs(up.x) * halfHeight + Math.abs(forward.x) * halfDepth;
            double extentY = Math.abs(right.y) * halfWidth
                    + Math.abs(up.y) * halfHeight + Math.abs(forward.y) * halfDepth;
            double extentZ = Math.abs(right.z) * halfWidth
                    + Math.abs(up.z) * halfHeight + Math.abs(forward.z) * halfDepth;
            collisionParts[i].updateBounds(center, new AABB(
                    center.x - extentX, center.y - extentY, center.z - extentZ,
                    center.x + extentX, center.y + extentY, center.z + extentZ
            ));
        }
    }

    private void holdBackEnemies(ServerLevel level, Player owner) {
        long gameTime = level.getGameTime();
        if (tickCount % 40 == 0) {
            contactDamageCooldowns.entrySet().removeIf(
                    entry -> entry.getValue() + CONTACT_DAMAGE_INTERVAL_TICKS < gameTime);
        }
        float scale = getVisualScale(1.0F);
        AABB searchArea = AABB.ofSize(
                position(),
                MODEL_WIDTH * scale + 4.0,
                MODEL_HEIGHT * scale + 4.0,
                MODEL_WIDTH * scale + 4.0
        );
        Vec3 forward = barrierForward();
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        if (right.lengthSqr() < 1.0E-7) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0, forward.z);
        double horizontalInfluence = horizontalForward.length();
        if (horizontalInfluence < 1.0E-4) {
            return;
        }
        Vec3 horizontalNormal = horizontalForward.scale(1.0 / horizontalInfluence);
        for (LivingEntity enemy : level.getEntitiesOfClass(
                LivingEntity.class, searchArea, target -> isEnemy(owner, target))) {
            AABB bounds = enemy.getBoundingBox();
            Vec3 center = bounds.getCenter();
            double halfX = bounds.getXsize() * 0.5;
            double halfY = bounds.getYsize() * 0.5;
            double halfZ = bounds.getZsize() * 0.5;
            double normalRadius = projectedRadius(forward, halfX, halfY, halfZ);
            Vec3 offset = center.subtract(position());
            double depth = offset.dot(forward);
            Vec3 previousCenter = center.add(
                    enemy.xOld - enemy.getX(),
                    enemy.yOld - enemy.getY(),
                    enemy.zOld - enemy.getZ()
            );
            double previousDepth = previousCenter.subtract(position()).dot(forward);
            boolean overlapping = Math.abs(depth) <= normalRadius + 0.05;
            boolean crossed = previousDepth > normalRadius && depth < -normalRadius;
            if ((!overlapping && !crossed)
                    || !overlapsShield(
                    offset.dot(right) / scale,
                    offset.dot(up) / scale,
                    projectedRadius(right, halfX, halfY, halfZ) / scale,
                    projectedRadius(up, halfX, halfY, halfZ) / scale)) {
                continue;
            }
            double correction = (normalRadius + 0.05 - depth) / horizontalInfluence;
            Vec3 correctedPosition = enemy.position().add(horizontalNormal.scale(correction));
            enemy.setPos(correctedPosition.x, enemy.getY(), correctedPosition.z);
            Vec3 movement = enemy.getDeltaMovement();
            double inwardSpeed = movement.dot(horizontalNormal);
            if (inwardSpeed < 0.0) {
                enemy.setDeltaMovement(movement.subtract(horizontalNormal.scale(inwardSpeed)));
            }
            enemy.hurtMarked = true;
            if (enemy instanceof Mob
                    && !PridwenItem.isBarrierBroken(owner.getItemInHand(getShieldHand()))
                    && gameTime >= contactDamageCooldowns.getOrDefault(enemy.getUUID(), 0L)) {
                contactDamageCooldowns.put(
                        enemy.getUUID(), gameTime + CONTACT_DAMAGE_INTERVAL_TICKS);
                float contactDamage = (float) enemy.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (contactDamage > 0.0F
                        && absorb(level, contactDamage,
                        center.subtract(forward.scale(depth)))) {
                    return;
                }
            }
        }
    }

    private static boolean overlapsShield(double x, double y, double marginX, double marginY) {
        for (CollisionColumn column : COLLISION_COLUMNS) {
            if (x >= column.minX - marginX && x <= column.maxX + marginX
                    && y >= column.minY - marginY && y <= column.maxY + marginY) {
                return true;
            }
        }
        return false;
    }

    private static double projectedRadius(Vec3 axis, double halfX, double halfY, double halfZ) {
        return Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
    }

    private static boolean isEnemy(Player owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator() || owner.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player player) {
            return owner.canHarmPlayer(player);
        }
        if (target instanceof Enemy) {
            return true;
        }
        return target instanceof Mob mob && mob.getTarget() != null
                && owner.isAlliedTo(mob.getTarget());
    }

    private void updatePosition(Player owner, float progress) {
        Vec3 forward = owner.getLookAngle().normalize();
        Vec3 heldPosition = owner.getEyePosition().add(forward.scale(RAISED_DISTANCE))
                .add(0.0, VERTICAL_OFFSET, 0.0);
        Vec3 projectedPosition = owner.getEyePosition().add(forward.scale(DISTANCE))
                .add(0.0, VERTICAL_OFFSET, 0.0);
        Vec3 center = heldPosition.lerp(projectedPosition, travelProgress(progress));
        setPos(center.x, center.y, center.z);
        setRot(owner.getYRot(), owner.getXRot());
        updateCollisionParts();
    }

    public Vec3 getProjectedPosition(float partialTick) {
        Player owner = getOwnerEntity();
        if (owner == null) {
            return position();
        }
        return owner.getEyePosition(partialTick)
                .add(owner.getViewVector(partialTick).normalize().scale(DISTANCE))
                .add(0.0, VERTICAL_OFFSET, 0.0);
    }

    public float getProjectionProgress(float partialTick) {
        float progress = Mth.lerp(partialTick, previousProjectionProgress, projectionProgress);
        return travelProgress(progress);
    }

    public float getVisualScale(float partialTick) {
        float progress = Mth.lerp(partialTick, previousProjectionProgress, projectionProgress);
        return Mth.lerp(travelProgress(progress), HELD_SCALE, MODEL_SCALE);
    }

    public float getOpacityMultiplier(float partialTick) {
        float progress = Mth.clamp(
                Mth.lerp(partialTick, previousHealthProgress, healthProgress), 0.0F, 1.0F);
        float rawProjection = Mth.lerp(
                partialTick, previousProjectionProgress, projectionProgress);
        float reveal = smooth(Mth.clamp(rawProjection / 0.5F, 0.0F, 1.0F));
        return Mth.lerp(progress, 1.0F / 3.0F, 1.0F) * reveal;
    }

    private float travelProgress(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        if (entityData.get(RETRACTING)) {
            return smooth(clamped);
        }
        if (clamped < 0.2F) {
            return Mth.lerp(smooth(clamped / 0.2F), 0.0F, 0.04F);
        }
        if (clamped < 0.72F) {
            return Mth.lerp(snap((clamped - 0.2F) / 0.52F), 0.04F, 1.0F);
        }
        return 1.0F;
    }

    private static float snap(float progress) {
        float remaining = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - remaining * remaining * remaining * remaining;
    }

    private static float smooth(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static boolean isUsingPridwen(Player owner) {
        return owner.isAlive() && owner.isUsingItem()
                && owner.getUseItem().getItem() instanceof PridwenItem
                && !PridwenItem.isBarrierBroken(owner.getUseItem());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            previousProjectionProgress = projectionProgress;
            projectionProgress = entityData.get(PROJECTION_PROGRESS);
            previousHealthProgress = healthProgress;
            healthProgress = entityData.get(HEALTH_PROGRESS);
            updateCollisionParts();
            return;
        }
        Player owner = getOwnerEntity();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        previousProjectionProgress = projectionProgress;
        previousHealthProgress = healthProgress;
        ItemStack shield = owner.getItemInHand(getShieldHand());
        healthProgress = shield.getItem() instanceof PridwenItem
                ? PridwenItem.getBarrierHealthProgress(shield) : 0.0F;
        entityData.set(HEALTH_PROGRESS, healthProgress);
        boolean usingPridwen = isUsingPridwen(owner);
        boolean retracting = entityData.get(RETRACTING);
        if (!retracting && !usingPridwen) {
            retracting = true;
            entityData.set(RETRACTING, true);
        }
        float direction = retracting ? -1.0F / RETRACTION_TICKS : 1.0F / DEPLOYMENT_TICKS;
        projectionProgress = Mth.clamp(
                projectionProgress + direction, 0.0F, 1.0F);
        entityData.set(PROJECTION_PROGRESS, projectionProgress);
        updatePosition(owner, projectionProgress);
        if (projectionProgress > 0.0F && level() instanceof ServerLevel serverLevel) {
            holdBackEnemies(serverLevel, owner);
        }
        if (retracting && projectionProgress <= 0.0F) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PROJECTION_PROGRESS, 0.0F);
        builder.define(OWNER_ID, -1);
        builder.define(RETRACTING, false);
        builder.define(OFF_HAND, true);
        builder.define(HEALTH_PROGRESS, 1.0F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String savedOwner = input.getStringOr("Owner", "");
        try {
            ownerId = savedOwner.isEmpty() ? null : UUID.fromString(savedOwner);
        } catch (IllegalArgumentException ignored) {
            ownerId = null;
        }
        projectionProgress = Mth.clamp(input.getFloatOr("ProjectionProgress", 0.0F), 0.0F, 1.0F);
        previousProjectionProgress = projectionProgress;
        entityData.set(PROJECTION_PROGRESS, projectionProgress);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
        output.putFloat("ProjectionProgress", projectionProgress);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        Vec3 forward = barrierForward();
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        if (right.lengthSqr() < 1.0E-7) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        double scale = getVisualScale(1.0F);
        double halfWidth = scale / 16.0;
        double halfHeight = scale / 16.0;
        double halfDepth = PLATE_DEPTH * scale * 0.5;
        double extentX = Math.abs(right.x) * halfWidth
                + Math.abs(up.x) * halfHeight + Math.abs(forward.x) * halfDepth;
        double extentY = Math.abs(right.y) * halfWidth
                + Math.abs(up.y) * halfHeight + Math.abs(forward.y) * halfDepth;
        double extentZ = Math.abs(right.z) * halfWidth
                + Math.abs(up.z) * halfHeight + Math.abs(forward.z) * halfDepth;
        return new AABB(
                position.x - extentX, position.y - extentY, position.z - extentZ,
                position.x + extentX, position.y + extentY, position.z + extentZ
        );
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (collisionParts != null) {
            for (int i = 0; i < collisionParts.length; i++) {
                collisionParts[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return collisionParts;
    }

    private static CollisionColumn column(float minX, float maxX, float minY, float maxY) {
        return new CollisionColumn(
                minX / 16.0F - PLATE_CENTER_X,
                maxX / 16.0F - PLATE_CENTER_X,
                minY / 16.0F - PLATE_CENTER_Y,
                maxY / 16.0F - PLATE_CENTER_Y
        );
    }

    private static CollisionColumn[] createCollisionTiles() {
        boolean[][] used = new boolean[14][12];
        ArrayList<CollisionColumn> tiles = new ArrayList<>();
        for (int y = 4; y < 18; y++) {
            for (int x = 2; x < 14; x++) {
                int row = y - 4;
                int column = x - 2;
                if (used[row][column] || !isShieldPixel(x, y)) {
                    continue;
                }
                int width = x + 1 < 14 && !used[row][column + 1]
                        && isShieldPixel(x + 1, y) ? 2 : 1;
                int height = 1;
                if (y + 1 < 18) {
                    boolean canExtend = true;
                    for (int offset = 0; offset < width; offset++) {
                        if (used[row + 1][column + offset]
                                || !isShieldPixel(x + offset, y + 1)) {
                            canExtend = false;
                            break;
                        }
                    }
                    if (canExtend) {
                        height = 2;
                    }
                }
                for (int offsetY = 0; offsetY < height; offsetY++) {
                    for (int offsetX = 0; offsetX < width; offsetX++) {
                        used[row + offsetY][column + offsetX] = true;
                    }
                }
                tiles.add(column(x, x + width, y, y + height));
            }
        }
        return tiles.toArray(CollisionColumn[]::new);
    }

    private static boolean isShieldPixel(int x, int y) {
        return x >= 4 && x < 12 && y >= 6 && y < 18
                || x >= 3 && x < 13 && y >= 7 && y < 17
                || x >= 2 && x < 14 && y >= 9 && y < 16
                || x >= 5 && x < 11 && y >= 5 && y < 6
                || x >= 6 && x < 10 && y >= 4 && y < 5;
    }

    private record CollisionColumn(float minX, float maxX, float minY, float maxY) {
        private boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }

        private float centerX() {
            return (minX + maxX) * 0.5F;
        }

        private float centerY() {
            return (minY + maxY) * 0.5F;
        }

        private float width() {
            return maxX - minX;
        }

        private float height() {
            return maxY - minY;
        }
    }

    private record BarrierHit(PridwenBarrierEntity barrier, Vec3 position) {
    }
}
