package net.turtleboi.noblephantasms.entity.custom;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.turtleboi.noblephantasms.entity.ModEntities;

public final class WindCutterProjectile extends Projectile {
    private static final float BASE_WIDTH = 0.625F;
    private static final float BASE_HEIGHT = 2.0F;
    private static final int LIFESPAN = 20;
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(WindCutterProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(WindCutterProjectile.class, EntityDataSerializers.FLOAT);
    private final Set<UUID> hitEntities = new HashSet<>();
    private float projectileDamage;
    private float entityWidth = BASE_WIDTH;
    private float entityHeight = BASE_HEIGHT;
    private int lastSize = -1;

    public WindCutterProjectile(EntityType<? extends WindCutterProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public WindCutterProjectile(Level level) {
        this(ModEntities.WIND_CUTTER.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_WIDTH, BASE_WIDTH);
        entityData.define(DATA_HEIGHT, BASE_HEIGHT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == DATA_WIDTH || data == DATA_HEIGHT) {
            entityWidth = entityData.get(DATA_WIDTH);
            entityHeight = entityData.get(DATA_HEIGHT);
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();
        float life = Mth.clamp(tickCount / (float) LIFESPAN, 0.0F, 1.0F);
        float fade = Mth.clamp((life - 0.9F) / 0.1F, 0.0F, 1.0F);
        Vec3 movement = getDeltaMovement().scale(Mth.lerp(fade, 1.0F, 0.55F));
        AABB path = getBoundingBox().expandTowards(movement);
        if (level() instanceof ServerLevel serverLevel) {
            damageTargets(serverLevel, path);
            breakGrass(serverLevel, path);
        }

        Vec3 nextPosition = position().add(movement);
        setDeltaMovement(movement);
        needsSync = true;
        setPos(nextPosition);
        updateSize(life);

        if (tickCount > LIFESPAN) {
            discard();
        }
    }

    private void damageTargets(ServerLevel level, AABB path) {
        Entity owner = getOwner();
        for (Entity entity : level.getEntities(this, path, this::canHitEntity)) {
            if (!(entity instanceof LivingEntity target)
                    || entity == owner
                    || hitEntities.contains(entity.getUUID())
                    || owner instanceof LivingEntity livingOwner && !livingOwner.canAttack(target)) {
                continue;
            }

            if (target.hurtServer(level, damageSources().thrown(this, owner), projectileDamage)) {
                hitEntities.add(entity.getUUID());
            }
        }
    }

    private void breakGrass(ServerLevel level, AABB path) {
        Entity owner = getOwner();
        for (BlockPos pos : BlockPos.betweenClosed(path)) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)) {
                level.destroyBlock(pos, true, owner);
            }
        }
    }

    private void updateSize(float life) {
        float multiplier = life * 2.0F + 0.25F;
        int size = Mth.floor(multiplier * 32.0F);
        if (!level().isClientSide() && size != lastSize) {
            lastSize = size;
            entityWidth = BASE_WIDTH * multiplier;
            entityHeight = BASE_HEIGHT * multiplier;
            entityData.set(DATA_WIDTH, entityWidth);
            entityData.set(DATA_HEIGHT, entityHeight);
            refreshDimensions();
        }
    }

    public void setProjectileDamage(float projectileDamage) {
        this.projectileDamage = projectileDamage;
    }

    public int getLifespan() {
        return LIFESPAN;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(entityWidth, entityHeight);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        projectileDamage = input.getFloatOr("Damage", 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", projectileDamage);
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
