package net.turtleboi.noblephantasms.entity.custom;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.TecpatlOfTheFifthSunItem;

public final class TecpatlShardEntity extends AbstractArrow {
    public static final int OUTBOUND = 0;
    public static final int WAITING = 1;
    public static final int RISING = 2;
    public static final int RETURNING = 3;
    public static final int DROPPING = 4;
    public static final int PREPARING = 5;
    private static final int LAST_SHARD_TRAVEL_TICKS = 18;
    private static final int RETURN_PREPARE_TICKS = 5;
    private static final double RETURN_QUEUE_RADIUS = 1.25;
    private static final double FULL_RETURN_SPEED_DISTANCE = 32.0;
    private static final double FRONT_RETURN_SPEED_MULTIPLIER = 1.8;
    private static final double RETURN_QUEUE_SPEED_STEP = 0.14;
    private static final double MIN_RETURN_SPEED_MULTIPLIER = 0.55;
    private static final double CLOSE_RETURN_DISTANCE_MULTIPLIER = 0.85;
    private static final double DISTANT_RETURN_DISTANCE_MULTIPLIER = 1.25;
    private static final double AUTO_RECALL_DISTANCE = 96.0;
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PIECE_INDEX =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PIECE_COUNT =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SHATTER_SEED =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> RETURN_SPEED =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HAS_FOIL =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURN_QUEUED =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RETURN_QUEUE_POSITION =
            SynchedEntityData.defineId(TecpatlShardEntity.class, EntityDataSerializers.INT);
    private final Set<UUID> hitEntities = new HashSet<>();
    private final Set<UUID> returnHitEntities = new HashSet<>();
    private UUID batchId = UUID.randomUUID();
    private float shardDamage;
    private boolean mainHand = true;
    private boolean lastShard;
    private boolean hitEntity;
    private int phaseTicks;
    private int recallDelay = -1;

    public TecpatlShardEntity(EntityType<? extends TecpatlShardEntity> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
    }

    public TecpatlShardEntity(ServerLevel level, Player owner, UUID batchId,
                              int index, float damage, ItemStack dagger,
                              InteractionHand hand) {
        super(ModEntities.TECPATL_SHARD.get(), owner, level,
                dagger.copyWithCount(1), dagger);
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
        this.batchId = batchId;
        shardDamage = damage;
        entityData.set(RETURN_SPEED, 0.96F + owner.getRandom().nextFloat() * 0.08F);
        mainHand = hand == InteractionHand.MAIN_HAND;
        entityData.set(PIECE_INDEX, index);
        entityData.set(PIECE_COUNT, TecpatlOfTheFifthSunItem.SHARD_COUNT);
        entityData.set(SHATTER_SEED, TecpatlOfTheFifthSunItem.SHATTER_SEED);
        entityData.set(HAS_FOIL, dagger.hasFoil());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, OUTBOUND);
        builder.define(PIECE_INDEX, 0);
        builder.define(PIECE_COUNT, TecpatlOfTheFifthSunItem.SHARD_COUNT);
        builder.define(SHATTER_SEED, TecpatlOfTheFifthSunItem.SHATTER_SEED);
        builder.define(RETURN_SPEED, 1.0F);
        builder.define(HAS_FOIL, false);
        builder.define(RETURN_QUEUED, false);
        builder.define(RETURN_QUEUE_POSITION, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == PHASE && level().isClientSide()) {
            phaseTicks = 0;
        }
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            setNoPhysics(true);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (level() instanceof ServerLevel && owner instanceof Player player
                && !TecpatlOfTheFifthSunItem.isDeploymentActive(player, batchId)) {
            discard();
            return;
        }
        if (tickCount > 6000) {
            recoverDeployment(owner);
            return;
        }
        if (recallDelay < 0
                && distanceToSqr(owner) > AUTO_RECALL_DISTANCE * AUTO_RECALL_DISTANCE
                && recallBatch(owner)) {
            return;
        }

        switch (getPhase()) {
            case OUTBOUND -> {
                super.tick();
                setNoGravity(true);
                if (isAlive() && getPhase() == OUTBOUND
                        && lastShard && tickCount >= LAST_SHARD_TRAVEL_TICKS) {
                    if (!recallBatch(owner)) {
                        beginPhase(WAITING);
                    }
                }
            }
            case WAITING -> tickWaiting();
            case PREPARING -> tickPreparing();
            case RISING -> tickRising(owner);
            case RETURNING -> tickReturning(owner);
            default -> tickDropping();
        }
    }

    private void tickWaiting() {
        phaseTicks++;
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        setNoGravity(true);
        if (recallDelay >= 0 && phaseTicks > recallDelay) {
            beginPhase(PREPARING);
        }
    }

    private void tickPreparing() {
        phaseTicks++;
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        setNoGravity(true);
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.ENCHANT,
                    getX() + random.nextGaussian() * 0.08,
                    getY() + random.nextGaussian() * 0.08,
                    getZ() + random.nextGaussian() * 0.08,
                    0.0, 0.015, 0.0);
        }
        if (phaseTicks >= RETURN_PREPARE_TICKS) {
            beginPhase(RISING);
        }
    }

    private void tickRising(Entity owner) {
        if (shouldDropAndWait(owner)) {
            beginPhase(DROPPING);
            return;
        }
        phaseTicks++;
        double angle = (entityData.get(PIECE_INDEX) * 0.9) + phaseTicks * 0.35;
        setDeltaMovement(Math.cos(angle) * 0.025, 0.13, Math.sin(angle) * 0.025);
        damageReturningEntities();
        super.tick();
        setNoGravity(true);
        if (phaseTicks >= 8) {
            beginPhase(RETURNING);
        }
    }

    private void tickReturning(Entity owner) {
        if (shouldDropAndWait(owner)) {
            beginPhase(DROPPING);
            return;
        }
        phaseTicks++;
        setNoPhysics(true);
        Vec3 destination = owner.getEyePosition().add(0.0, -0.25, 0.0);
        Vec3 towardOwner = destination.subtract(position());
        if (level() instanceof ServerLevel serverLevel) {
            int queuePosition = getReturnQueuePosition(serverLevel);
            entityData.set(RETURN_QUEUE_POSITION, queuePosition);
            boolean approachingQueue = position().add(getDeltaMovement())
                    .distanceToSqr(destination) < RETURN_QUEUE_RADIUS * RETURN_QUEUE_RADIUS;
            entityData.set(RETURN_QUEUED, queuePosition > 0
                    && (towardOwner.lengthSqr() < RETURN_QUEUE_RADIUS * RETURN_QUEUE_RADIUS
                    || approachingQueue));
        }
        if (entityData.get(RETURN_QUEUED)) {
            tickReturnQueue(destination);
            return;
        }
        if (towardOwner.lengthSqr() < 0.8) {
            if (level() instanceof ServerLevel serverLevel
                    && isNextReturningShard(serverLevel)) {
                returnShard(owner);
                discard();
                return;
            }
            double angle = getPieceIndex() * 0.9 + phaseTicks * 0.35;
            setDeltaMovement(Math.cos(angle) * 0.06, 0.015,
                    Math.sin(angle) * 0.06);
            damageReturningEntities();
            super.tick();
            setNoGravity(true);
            return;
        }
        double speed = Math.clamp(towardOwner.length() * 0.075, 0.65, 3.25)
                * getOrderedReturnSpeedMultiplier(towardOwner.length());
        setDeltaMovement(getDeltaMovement().scale(0.65)
                .add(towardOwner.normalize().scale(speed * 0.35)));
        damageReturningEntities();
        super.tick();
        setNoGravity(true);
    }

    private void tickReturnQueue(Vec3 destination) {
        int queueRank = entityData.get(RETURN_QUEUE_POSITION);
        double queueRadius = RETURN_QUEUE_RADIUS
                + Math.min(queueRank - 1, 5) * 0.12;
        double angle = getPieceIndex() * 2.399963229728653 + phaseTicks * 0.015;
        Vec3 queueTarget = destination.add(
                Math.cos(angle) * queueRadius,
                0.1 + Math.sin(phaseTicks * 0.12) * 0.15,
                Math.sin(angle) * queueRadius);
        Vec3 towardQueue = queueTarget.subtract(position());
        double speed = Math.min(0.08, towardQueue.length());
        setDeltaMovement(getDeltaMovement().scale(0.05)
                .add(towardQueue.normalize().scale(speed * 0.95)));
        damageReturningEntities();
        super.tick();
        setNoGravity(true);
    }

    private double getOrderedReturnSpeedMultiplier(double distance) {
        double randomVariation = Math.clamp(entityData.get(RETURN_SPEED), 0.96, 1.04);
        int queuePosition = entityData.get(RETURN_QUEUE_POSITION);
        double distanceProgress = Math.clamp(
                (distance - RETURN_QUEUE_RADIUS)
                        / (FULL_RETURN_SPEED_DISTANCE - RETURN_QUEUE_RADIUS),
                0.0, 1.0);
        double distanceMultiplier = CLOSE_RETURN_DISTANCE_MULTIPLIER
                + (DISTANT_RETURN_DISTANCE_MULTIPLIER
                - CLOSE_RETURN_DISTANCE_MULTIPLIER) * distanceProgress;
        double queueMultiplier = Math.max(MIN_RETURN_SPEED_MULTIPLIER,
                FRONT_RETURN_SPEED_MULTIPLIER
                        - queuePosition * RETURN_QUEUE_SPEED_STEP);
        return queueMultiplier * distanceMultiplier * randomVariation;
    }

    private void tickDropping() {
        phaseTicks++;
        setNoPhysics(true);
        setDeltaMovement(0.0, -0.11, 0.0);
        super.tick();
        setNoGravity(true);
        if (phaseTicks >= 6) {
            beginPhase(WAITING);
        }
    }

    private boolean shouldDropAndWait(Entity owner) {
        return isOwnerShootingBatch(owner);
    }

    private boolean isOwnerShootingBatch(Entity owner) {
        if (!(owner instanceof LivingEntity livingEntity) || !livingEntity.isUsingItem()) {
            return false;
        }
        return batchId.equals(livingEntity.getUseItem().get(
                ModDataComponents.TECPATL_DEPLOYMENT.get()));
    }

    private boolean isNextReturningShard(ServerLevel level) {
        return getReturnQueuePosition(level) == 0;
    }

    private int getReturnQueuePosition(ServerLevel level) {
        int queuePosition = 0;
        double pieceOrder = TecpatlOfTheFifthSunItem.getPieceBreakOrder(getPieceIndex());
        for (TecpatlShardEntity shard : level.getEntitiesOfClass(TecpatlShardEntity.class,
                getBoundingBox().inflate(256.0),
                shard -> shard != this && batchId.equals(shard.batchId))) {
            double otherOrder = TecpatlOfTheFifthSunItem.getPieceBreakOrder(shard.getPieceIndex());
            if (otherOrder > pieceOrder
                    || otherOrder == pieceOrder && shard.getPieceIndex() > getPieceIndex()) {
                queuePosition++;
            }
        }
        return queuePosition;
    }

    private void beginPhase(int phase) {
        entityData.set(PHASE, phase);
        if (phase != RETURNING) {
            entityData.set(RETURN_QUEUED, false);
            entityData.set(RETURN_QUEUE_POSITION, 0);
        }
        phaseTicks = 0;
        setNoPhysics(true);
        setDeltaMovement(Vec3.ZERO);
        if (phase == WAITING) {
            playSound(SoundEvents.AMETHYST_BLOCK_HIT, 0.55F, 0.65F + random.nextFloat() * 0.4F);
        }
    }

    @Override
    protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult hitResult) {
        if (getPhase() == OUTBOUND && hitResult.getType() == HitResult.Type.ENTITY) {
            onHit(hitResult);
            return ProjectileDeflection.NONE;
        }
        return super.hitTargetOrDeflectSelf(hitResult);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!(entity instanceof LivingEntity target) || entity == getOwner()
                || hitEntities.contains(entity.getUUID())) {
            return false;
        }
        return (!(getOwner() instanceof LivingEntity owner) || owner.canAttack(target))
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (getPhase() != OUTBOUND || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity target = hitResult.getEntity();
        Entity owner = getOwner();
        hitEntity = true;
        damageEntity(serverLevel, target, hitEntities);
        if (lastShard) {
            recallBatch(owner);
        }
    }

    private void damageReturningEntities() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        AABB path = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.2);
        Entity owner = getOwner();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, path,
                target -> target != owner
                        && target.isAlive()
                        && !returnHitEntities.contains(target.getUUID())
                        && (!(owner instanceof LivingEntity livingOwner)
                        || livingOwner.canAttack(target)))) {
            if (target.getBoundingBox().inflate(0.2).clip(start, end).isPresent()) {
                damageEntity(serverLevel, target, returnHitEntities);
            }
        }
    }

    private void damageEntity(ServerLevel serverLevel, Entity target,
                              Set<UUID> struckEntities) {
        Entity owner = getOwner();
        ItemStack weapon = getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            weapon = getPickupItemStackOrigin();
        }
        DamageSource damageSource = owner instanceof Player player
                ? new WeaponDamageSource(
                        damageSources().playerAttack(player), player, weapon)
                : damageSources().thrown(this, owner);
        float damage = EnchantmentHelper.modifyDamage(
                serverLevel, weapon, target, damageSource, shardDamage);
        if (target.hurtServer(serverLevel, damageSource, damage)) {
            struckEntities.add(target.getUUID());
            if (owner instanceof LivingEntity livingOwner) {
                livingOwner.setLastHurtMob(target);
            }
            if (target instanceof LivingEntity livingTarget) {
                doKnockback(livingTarget, damageSource);
                doPostHurtEffects(livingTarget);
                livingTarget.hurtDuration = 1;
                livingTarget.hurtTime = 1;
                livingTarget.invulnerableTime = 1;
            }
            EnchantmentHelper.doPostAttackEffectsWithItemSource(
                    serverLevel, target, damageSource, weapon);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (getPhase() == OUTBOUND) {
            setPos(hitResult.getLocation());
            if (!lastShard || !recallBatch(getOwner())) {
                beginPhase(WAITING);
            }
        }
    }

    public void dropAndWait() {
        recallDelay = -1;
        if (getPhase() == RISING || getPhase() == RETURNING) {
            beginPhase(DROPPING);
        } else if (getPhase() == WAITING) {
            phaseTicks = 0;
        }
    }

    public void beginReconstructionWait() {
        recallDelay = -1;
        beginPhase(DROPPING);
    }

    public void beginRecall(int delayTicks) {
        if (recallDelay >= 0) {
            return;
        }
        recallDelay = Math.max(0, delayTicks);
        if (getPhase() == WAITING) {
            phaseTicks = 0;
            return;
        }
        beginPhase(WAITING);
    }

    public void markLastShard() {
        lastShard = true;
        if (getPhase() != OUTBOUND || hitEntity
                || tickCount >= LAST_SHARD_TRAVEL_TICKS) {
            if (!recallBatch(getOwner())) {
                beginPhase(WAITING);
            }
        }
    }

    private boolean recallBatch(Entity owner) {
        return level() instanceof ServerLevel serverLevel && owner instanceof Player player
                && TecpatlOfTheFifthSunItem.recallShards(
                serverLevel, player, batchId);
    }

    private void returnShard(Entity owner) {
        if (!(level() instanceof ServerLevel serverLevel) || !(owner instanceof Player player)) {
            return;
        }
        boolean complete = TecpatlOfTheFifthSunItem.returnShard(player, batchId, getPieceIndex(),
                mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                getPickupItemStackOrigin());
        serverLevel.playSound(null, player.blockPosition(), complete
                        ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_HIT,
                player.getSoundSource(), complete ? 0.8F : 0.45F,
                complete ? 0.75F : 1.1F + getPieceIndex() * 0.035F);
    }

    private void recoverDeployment(Entity owner) {
        if (!(level() instanceof ServerLevel serverLevel) || !(owner instanceof Player player)) {
            discard();
            return;
        }
        boolean recovered = TecpatlOfTheFifthSunItem.recoverDagger(player, batchId,
                mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                getPickupItemStackOrigin(), getPieceIndex() == 0);
        if (!recovered) {
            discard();
            return;
        }
        for (TecpatlShardEntity shard : serverLevel.getEntitiesOfClass(TecpatlShardEntity.class,
                getBoundingBox().inflate(256.0), shard -> batchId.equals(shard.batchId))) {
            shard.discard();
        }
        discard();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                player.getSoundSource(), 0.8F, 0.75F);
    }

    public int getPhase() {
        return entityData.get(PHASE);
    }

    public int getPieceIndex() {
        return entityData.get(PIECE_INDEX);
    }

    public int getPieceCount() {
        return entityData.get(PIECE_COUNT);
    }

    public long getShatterSeed() {
        return entityData.get(SHATTER_SEED);
    }

    public UUID getBatchId() {
        return batchId;
    }

    public boolean hasFoil() {
        return entityData.get(HAS_FOIL);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.TECPATL_OF_THE_FIFTH_SUN.get());
    }

    private static final class WeaponDamageSource extends DamageSource {
        private final ItemStack weapon;

        private WeaponDamageSource(DamageSource damageSource,
                                   LivingEntity owner, ItemStack weapon) {
            super(damageSource.typeHolder(), owner);
            this.weapon = weapon;
        }

        @Override
        public ItemStack getWeaponItem() {
            return weapon;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        try {
            batchId = UUID.fromString(input.getStringOr("Batch", UUID.randomUUID().toString()));
        } catch (IllegalArgumentException ignored) {
            batchId = UUID.randomUUID();
        }
        shardDamage = input.getFloatOr("Damage", 0.0F);
        entityData.set(RETURN_SPEED, input.getFloatOr("ReturnSpeed", 1.0F));
        mainHand = input.getBooleanOr("MainHand", true);
        lastShard = input.getBooleanOr("LastShard", false);
        hitEntity = input.getBooleanOr("HitEntity", false);
        phaseTicks = input.getIntOr("PhaseTicks", 0);
        recallDelay = input.getIntOr("RecallDelay", -1);
        entityData.set(PHASE, input.getIntOr("Phase", OUTBOUND));
        entityData.set(PIECE_INDEX, input.getIntOr("PieceIndex", 0));
        entityData.set(PIECE_COUNT, input.getIntOr(
                "PieceCount", TecpatlOfTheFifthSunItem.SHARD_COUNT));
        entityData.set(SHATTER_SEED, input.getLongOr(
                "ShatterSeed", TecpatlOfTheFifthSunItem.SHATTER_SEED));
        entityData.set(HAS_FOIL, getPickupItemStackOrigin().hasFoil());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Batch", batchId.toString());
        output.putFloat("Damage", shardDamage);
        output.putFloat("ReturnSpeed", entityData.get(RETURN_SPEED));
        output.putBoolean("MainHand", mainHand);
        output.putBoolean("LastShard", lastShard);
        output.putBoolean("HitEntity", hitEntity);
        output.putInt("PhaseTicks", phaseTicks);
        output.putInt("RecallDelay", recallDelay);
        output.putInt("Phase", getPhase());
        output.putInt("PieceIndex", getPieceIndex());
        output.putInt("PieceCount", getPieceCount());
        output.putLong("ShatterSeed", getShatterSeed());
    }
}
