package net.turtleboi.noblephantasms.entity.custom;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.particle.ModParticles;
import org.jspecify.annotations.Nullable;

public class GungnirProjectile extends AbstractArrow implements ItemSupplier {
    private static final EntityDataAccessor<Byte> ID_LOYALTY =
            SynchedEntityData.defineId(GungnirProjectile.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ID_FOIL =
            SynchedEntityData.defineId(GungnirProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ID_HOMING_TARGET =
            SynchedEntityData.defineId(GungnirProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ID_CHARGED_THROW =
            SynchedEntityData.defineId(GungnirProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ID_CHARGED_TICKS =
            SynchedEntityData.defineId(GungnirProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ID_THROW_CHARGE =
            SynchedEntityData.defineId(GungnirProjectile.class, EntityDataSerializers.FLOAT);
    private static final Identifier ARMOR_BYPASS_ID =
            Identifier.parse(NoblePhantasms.MOD_ID + ":gungnir_armor_bypass");
    private static final double HOMING_TURN_RATE = 0.35;
    private static final double ARMOR_BYPASS_FRACTION = 0.25;
    private static final float WOUNDED_HEALTH_THRESHOLD = 0.25F;
    private static final float WOUNDED_DAMAGE_MULTIPLIER = 1.5F;
    private static final float MIN_THROW_DAMAGE = 3.0F;
    private static final float MAX_THROW_DAMAGE = 12.0F;
    private static final int SHIELD_DURABILITY_DAMAGE = 100;
    private static final float SHIELD_DISABLE_SECONDS = 10.0F;
    private static final int RUNE_PARTICLES_PER_TICK = 1;
    private final Set<Integer> piercedEntityIds = new HashSet<>();
    private boolean dealtDamage;
    public int clientSideReturnTickCount;

    public GungnirProjectile(EntityType<? extends GungnirProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GungnirProjectile(Level level, LivingEntity owner, ItemStack gungnirItem) {
        super(ModEntities.GUNGNIR.get(), owner, level, gungnirItem, gungnirItem);
        entityData.set(ID_LOYALTY, getLoyaltyFromItem(gungnirItem));
        entityData.set(ID_FOIL, gungnirItem.hasFoil());
    }

    public GungnirProjectile(Level level, double x, double y, double z, ItemStack gungnirItem) {
        super(ModEntities.GUNGNIR.get(), x, y, z, level, gungnirItem, gungnirItem);
        entityData.set(ID_LOYALTY, getLoyaltyFromItem(gungnirItem));
        entityData.set(ID_FOIL, gungnirItem.hasFoil());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(ID_LOYALTY, (byte) 0);
        entityData.define(ID_FOIL, false);
        entityData.define(ID_HOMING_TARGET, -1);
        entityData.define(ID_CHARGED_THROW, false);
        entityData.define(ID_CHARGED_TICKS, 0);
        entityData.define(ID_THROW_CHARGE, 1.0F);
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) {
            dealtDamage = true;
        }

        if (isChargedThrow() && !dealtDamage && !isInGround() && !isNoPhysics()) {
            steerTowardTarget();
        }

        if (level().isClientSide()) {
            spawnRuneParticles();
        }

        Entity currentOwner = getOwner();
        int loyalty = entityData.get(ID_LOYALTY);
        if (loyalty > 0 && (dealtDamage || isNoPhysics()) && currentOwner != null) {
            if (!isAcceptableReturnOwner()) {
                if (level() instanceof ServerLevel serverLevel && pickup == Pickup.ALLOWED) {
                    spawnAtLocation(serverLevel, getPickupItem(), 0.1F);
                }
                discard();
            } else {
                if (!(currentOwner instanceof Player)
                        && position().distanceTo(currentOwner.getEyePosition()) < currentOwner.getBbWidth() + 1.0) {
                    discard();
                    return;
                }

                setNoPhysics(true);
                Vec3 returnVector = currentOwner.getEyePosition().subtract(position());
                setPosRaw(getX(), getY() + returnVector.y * 0.015 * loyalty, getZ());
                double acceleration = 0.05 * loyalty;
                setDeltaMovement(getDeltaMovement().scale(0.95).add(returnVector.normalize().scale(acceleration)));
                if (clientSideReturnTickCount == 0) {
                    playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }
                clientSideReturnTickCount++;
            }
        }

        super.tick();
    }

    private void spawnRuneParticles() {
        AABB hitBox = getBoundingBox();
        for (int i = 0; i < RUNE_PARTICLES_PER_TICK; i++) {
            double x = Mth.lerp(random.nextDouble(), hitBox.minX, hitBox.maxX);
            double y = Mth.lerp(random.nextDouble(), hitBox.minY, hitBox.maxY);
            double z = Mth.lerp(random.nextDouble(), hitBox.minZ, hitBox.maxZ);
            level().addParticle(ModParticles.GUNGNIR_RUNE.get(),
                    x, y, z, 0.0, 0.005 + random.nextDouble() * 0.005, 0.0);
        }
    }

    private void steerTowardTarget() {
        Entity target = level().getEntity(entityData.get(ID_HOMING_TARGET));
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            if (!level().isClientSide()) {
                entityData.set(ID_HOMING_TARGET, -1);
            }
            return;
        }

        Vec3 movement = getDeltaMovement();
        double speed = movement.length();
        Vec3 toTarget = livingTarget.getBoundingBox().getCenter().subtract(position());
        if (speed < 1.0E-4 || toTarget.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 steered = movement.normalize().scale(1.0 - HOMING_TURN_RATE)
                .add(toTarget.normalize().scale(HOMING_TURN_RATE));
        if (steered.lengthSqr() > 1.0E-4) {
            setDeltaMovement(steered.normalize().scale(speed));
        }
    }

    public void setHomingTarget(LivingEntity target) {
        entityData.set(ID_HOMING_TARGET, target.getId());
    }

    public void setChargedThrow(int chargedTicks) {
        entityData.set(ID_CHARGED_THROW, true);
        entityData.set(ID_CHARGED_TICKS, Math.max(0, chargedTicks));
    }

    public boolean isChargedThrow() {
        return entityData.get(ID_CHARGED_THROW);
    }

    public int getChargedTicks() {
        return entityData.get(ID_CHARGED_TICKS);
    }

    public void setThrowCharge(float charge) {
        entityData.set(ID_THROW_CHARGE, Mth.clamp(charge, 0.0F, 1.0F));
    }

    public float getThrowCharge() {
        return entityData.get(ID_THROW_CHARGE);
    }

    private boolean isAcceptableReturnOwner() {
        Entity currentOwner = getOwner();
        return currentOwner != null
                && currentOwner.isAlive()
                && (!(currentOwner instanceof ServerPlayer serverPlayer) || !serverPlayer.isSpectator());
    }

    public boolean isFoil() {
        return entityData.get(ID_FOIL);
    }

    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        return dealtDamage ? List.of() : super.findHitEntities(from, to);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !piercedEntityIds.contains(entity.getId()) && super.canHitEntity(entity);
    }

    @Override
    protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult hitResult) {
        if (isChargedThrow() && hitResult.getType() == HitResult.Type.ENTITY) {
            onHit(hitResult);
            return ProjectileDeflection.NONE;
        }
        return super.hitTargetOrDeflectSelf(hitResult);
    }

    @Override
    public boolean deflect(ProjectileDeflection deflection, @Nullable Entity deflectingEntity,
                           @Nullable EntityReference<Entity> newOwner, boolean byAttack) {
        return !isChargedThrow() && super.deflect(deflection, deflectingEntity, newOwner, byAttack);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (dealtDamage) {
            return;
        }

        Entity target = hitResult.getEntity();
        float damage = Mth.lerp(getThrowCharge(), MIN_THROW_DAMAGE, MAX_THROW_DAMAGE);
        Entity currentOwner = getOwner();
        DamageSource damageSource = damageSources().trident(this, currentOwner == null ? this : currentOwner);
        if (level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, getWeaponItem(), target, damageSource, damage);
            if (isChargedThrow()
                    && target instanceof LivingEntity livingTarget
                    && livingTarget.getHealth() < livingTarget.getMaxHealth() * WOUNDED_HEALTH_THRESHOLD) {
                damage *= WOUNDED_DAMAGE_MULTIPLIER;
            }
            if (isChargedThrow() && target instanceof LivingEntity livingTarget) {
                sunderShield(serverLevel, livingTarget, damageSource, damage);
            }
        }

        int maxTargets = getPierceLevel() + 1;
        if (getPierceLevel() > 0) {
            piercedEntityIds.add(target.getId());
        }
        boolean stopsHere = getPierceLevel() == 0 || piercedEntityIds.size() >= maxTargets;
        dealtDamage = stopsHere;

        if (hurtWithArmorBypass(target, damageSource, damage)) {
            if (target.is(EntityType.ENDERMAN)) {
                return;
            }

            if (level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(
                        serverLevel, target, damageSource, getWeaponItem(), weapon -> kill(serverLevel));
            }

            if (target instanceof LivingEntity livingTarget) {
                doKnockback(livingTarget, damageSource);
                doPostHurtEffects(livingTarget);
            }
        }

        if (stopsHere) {
            entityData.set(ID_HOMING_TARGET, -1);
            setDeltaMovement(getDeltaMovement().multiply(0.02, 0.2, 0.02));
        }
        playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    private boolean hurtWithArmorBypass(Entity target, DamageSource damageSource, float damage) {
        if (!isChargedThrow() || !(target instanceof LivingEntity livingTarget)) {
            return target.hurtOrSimulate(damageSource, damage);
        }

        AttributeInstance armor = livingTarget.getAttribute(Attributes.ARMOR);
        if (armor == null || armor.getValue() <= 0.0) {
            return target.hurtOrSimulate(damageSource, damage);
        }

        armor.removeModifier(ARMOR_BYPASS_ID);
        armor.addTransientModifier(new AttributeModifier(ARMOR_BYPASS_ID,
                -armor.getValue() * ARMOR_BYPASS_FRACTION, AttributeModifier.Operation.ADD_VALUE));
        try {
            return target.hurtOrSimulate(damageSource, damage);
        } finally {
            armor.removeModifier(ARMOR_BYPASS_ID);
        }
    }

    private void sunderShield(ServerLevel level, LivingEntity target, DamageSource damageSource, float damage) {
        ItemStack blockingItem = target.getItemBlockingWith();
        if (blockingItem == null) {
            return;
        }

        BlocksAttacks blocksAttacks = blockingItem.get(DataComponents.BLOCKS_ATTACKS);
        Vec3 sourcePosition = damageSource.getSourcePosition();
        if (blocksAttacks == null || sourcePosition == null) {
            return;
        }

        Vec3 viewVector = target.calculateViewVector(0.0F, target.getYHeadRot());
        Vec3 vectorToSource = sourcePosition.subtract(target.position());
        vectorToSource = new Vec3(vectorToSource.x, 0.0, vectorToSource.z).normalize();
        double angle = Math.acos(Mth.clamp(vectorToSource.dot(viewVector), -1.0, 1.0));
        if (blocksAttacks.resolveBlockedDamage(damageSource, damage, angle) <= 0.0F) {
            return;
        }

        InteractionHand hand = target.getUsedItemHand();
        blocksAttacks.hurtBlockingItem(level, blockingItem, target, hand, damage, SHIELD_DURABILITY_DAMAGE);
        if (!blockingItem.isEmpty()) {
            blocksAttacks.disable(level, target, SHIELD_DISABLE_SECONDS, blockingItem);
        }
    }

    @Override
    protected void hitBlockEnchantmentEffects(ServerLevel level, BlockHitResult hitResult, ItemStack weapon) {
        Vec3 hitPosition = hitResult.getBlockPos().clampLocationWithin(hitResult.getLocation());
        EnchantmentHelper.onHitBlock(level, weapon,
                getOwner() instanceof LivingEntity livingOwner ? livingOwner : null, this, null, hitPosition,
                level.getBlockState(hitResult.getBlockPos()), item -> kill(level));
    }

    @Override
    public ItemStack getWeaponItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    public ItemStack getItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player) || isNoPhysics() && ownedBy(player) && player.getInventory().add(getPickupItem());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.GUNGNIR.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void playerTouch(Player player) {
        if (ownedBy(player) || getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        dealtDamage = input.getBooleanOr("DealtDamage", false);
        entityData.set(ID_CHARGED_THROW, input.getBooleanOr("ChargedThrow", false));
        entityData.set(ID_CHARGED_TICKS, input.getIntOr("ChargedTicks", 0));
        entityData.set(ID_THROW_CHARGE, Mth.clamp(input.getFloatOr("ThrowCharge", 1.0F), 0.0F, 1.0F));
        entityData.set(ID_LOYALTY, getLoyaltyFromItem(getPickupItemStackOrigin()));
        entityData.set(ID_FOIL, getPickupItemStackOrigin().hasFoil());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", dealtDamage);
        output.putBoolean("ChargedThrow", isChargedThrow());
        output.putInt("ChargedTicks", getChargedTicks());
        output.putFloat("ThrowCharge", getThrowCharge());
    }

    private byte getLoyaltyFromItem(ItemStack gungnirItem) {
        return level() instanceof ServerLevel serverLevel
                ? (byte) Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(
                        serverLevel, gungnirItem, this), 0, 127)
                : 0;
    }

    @Override
    public void tickDespawn() {
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
