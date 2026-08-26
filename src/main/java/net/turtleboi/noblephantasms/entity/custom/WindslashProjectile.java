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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.ModEntities;

public final class WindslashProjectile extends Projectile {
    private static final float BASE_COLLISION_WIDTH = 1.0F / 16.0F;
    private static final float BASE_COLLISION_HEIGHT = 1.75F;
    private static final float BASE_VISUAL_WIDTH = 1.0F;
    private static final float BASE_VISUAL_HEIGHT = 1.75F;
    private static final float INITIAL_GROWTH_SCALE = 0.25F;
    private static final float GROWTH_RANGE = 2.0F;
    private static final float FADE_START = 0.9F;
    private static final float FINAL_SPEED_MULTIPLIER = 0.55F;
    private static final float MAX_TILT = 30.0F;
    private static final int LIFESPAN = 20;
    public static final float INITIAL_COLLISION_WIDTH = BASE_COLLISION_WIDTH * INITIAL_GROWTH_SCALE;
    public static final float INITIAL_COLLISION_HEIGHT = BASE_COLLISION_HEIGHT * INITIAL_GROWTH_SCALE;
    private static final float[] TRAVEL_PROGRESS = createTravelProgress();
    private static final EntityDataAccessor<Float> DATA_TILT =
            SynchedEntityData.defineId(WindslashProjectile.class, EntityDataSerializers.FLOAT);
    private final Set<UUID> hitEntities = new HashSet<>();
    private float projectileDamage;
    private ItemStack weapon = ItemStack.EMPTY;
    private float growthScale = INITIAL_GROWTH_SCALE;
    private float tilt;

    public WindslashProjectile(EntityType<? extends WindslashProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        if (!level.isClientSide()) {
            entityData.set(DATA_TILT, Mth.randomBetween(RandomSource.create(getUUID().getLeastSignificantBits()), -MAX_TILT, MAX_TILT));
        }
    }

    public WindslashProjectile(Level level) {
        this(ModEntities.WINDSLASH.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_TILT, 0.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == DATA_TILT) {
            tilt = entityData.get(DATA_TILT);
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > LIFESPAN) {
            discard();
            return;
        }
        setGrowthScale(getGrowthScaleForAge(tickCount));
        float life = Mth.clamp(tickCount / (float) LIFESPAN, 0.0F, 1.0F);
        Vec3 movement = getDeltaMovement().scale(getSpeedMultiplier(life));
        if (level() instanceof ServerLevel serverLevel) {
            AABB path = getBoundingBox().expandTowards(movement);
            damageTargets(serverLevel, path);
            breakGrass(serverLevel, path);
        }

        Vec3 nextPosition = position().add(movement);
        setDeltaMovement(movement);
        needsSync = true;
        setPos(nextPosition);
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

            var damageSource = damageSources().thrown(this, owner);
            float damage = weapon.isEmpty()
                    ? projectileDamage
                    : EnchantmentHelper.modifyDamage(level, weapon, target, damageSource, projectileDamage);
            if (target.hurtServer(level, damageSource, damage)) {
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

    private void setGrowthScale(float scale) {
        growthScale = Mth.clamp(scale, INITIAL_GROWTH_SCALE, INITIAL_GROWTH_SCALE + GROWTH_RANGE);
        refreshDimensions();
    }

    private static float getSpeedMultiplier(float life) {
        return Mth.lerp(getFadeProgress(life), 1.0F, FINAL_SPEED_MULTIPLIER);
    }

    private static float[] createTravelProgress() {
        float[] progress = new float[LIFESPAN + 1];
        float speed = 1.0F;
        for (int age = 1; age <= LIFESPAN; age++) {
            speed *= getSpeedMultiplier(age / (float) LIFESPAN);
            progress[age] = progress[age - 1] + speed;
        }
        float totalDistance = progress[LIFESPAN];
        for (int age = 1; age <= LIFESPAN; age++) {
            progress[age] /= totalDistance;
        }
        return progress;
    }

    private static float getGrowthScaleForAge(float age) {
        float clampedAge = Mth.clamp(age, 0.0F, LIFESPAN);
        int previousAge = Mth.floor(clampedAge);
        int nextAge = Math.min(previousAge + 1, LIFESPAN);
        float distanceProgress = Mth.lerp(clampedAge - previousAge, TRAVEL_PROGRESS[previousAge], TRAVEL_PROGRESS[nextAge]);
        return INITIAL_GROWTH_SCALE + distanceProgress * GROWTH_RANGE;
    }

    public void setProjectileDamage(float projectileDamage) {
        this.projectileDamage = projectileDamage;
    }

    public void setWeapon(ItemStack weapon) {
        this.weapon = weapon.copy();
    }

    public float getTilt() {
        return tilt;
    }

    public float getGrowthScale(float partialTick) {
        return getGrowthScaleForAge(tickCount - 1.0F + partialTick);
    }

    public float getLifeProgress(float partialTick) {
        return Mth.clamp((tickCount - 1.0F + partialTick) / LIFESPAN, 0.0F, 1.0F);
    }

    public static float getVisualWidth(float scale) {
        return BASE_VISUAL_WIDTH * scale;
    }

    public static float getVisualHeight(float scale) {
        return BASE_VISUAL_HEIGHT * scale;
    }

    public static float getVisualThickness(float scale) {
        return BASE_COLLISION_WIDTH * scale;
    }

    public static float getFadeProgress(float life) {
        return Mth.clamp((life - FADE_START) / (1.0F - FADE_START), 0.0F, 1.0F);
    }

    @Override
    public ItemStack getWeaponItem() {
        return weapon;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float radians = tilt * Mth.DEG_TO_RAD;
        float baseWidth = BASE_COLLISION_WIDTH * Math.abs(Mth.cos(radians))
                + BASE_COLLISION_HEIGHT * Math.abs(Mth.sin(radians));
        return EntityDimensions.fixed(baseWidth * growthScale, BASE_COLLISION_HEIGHT * growthScale);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        projectileDamage = input.getFloatOr("Damage", 0.0F);
        weapon = input.read("Weapon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        tickCount = Mth.clamp(input.getIntOr("Age", 0), 0, LIFESPAN);
        entityData.set(DATA_TILT, input.getFloatOr("Tilt", tilt));
        setGrowthScale(getGrowthScaleForAge(tickCount));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", projectileDamage);
        if (!weapon.isEmpty()) {
            output.store("Weapon", ItemStack.CODEC, weapon);
        }
        output.putInt("Age", tickCount);
        output.putFloat("Tilt", tilt);
    }
}
