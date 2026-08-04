package net.turtleboi.noblephantasms.entity.custom;

import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.component.ModDataComponents;

public class KazagurumaProjectile extends AbstractArrow {
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(KazagurumaProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOOKED_TARGET =
            SynchedEntityData.defineId(KazagurumaProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OFFHAND =
            SynchedEntityData.defineId(KazagurumaProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> LAUNCH_X_ROTATION =
            SynchedEntityData.defineId(KazagurumaProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LAUNCH_Y_ROTATION =
            SynchedEntityData.defineId(KazagurumaProjectile.class, EntityDataSerializers.FLOAT);
    private static final int OUTBOUND = 0;
    private static final int RETRACTING = 2;
    private static final double MAX_RANGE = 16.0;
    private static final double MELEE_RANGE = 2.75;
    private static final double RETURN_SPEED = 1.8;
    private static final double PULL_SPEED = 1.25;
    private static final double MIN_OUTBOUND_SPEED_SQUARED = 0.01;
    private static final int MAX_LIFETIME = 100;
    private float hookDamage;
    private ItemStack deploymentStack = ItemStack.EMPTY;

    public KazagurumaProjectile(EntityType<? extends KazagurumaProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public KazagurumaProjectile(Level level, LivingEntity owner, ItemStack stack, InteractionHand hand) {
        super(ModEntities.KAZAGURUMA.get(), owner, level, stack.copyWithCount(1), stack);
        setNoGravity(true);
        pickup = Pickup.DISALLOWED;
        deploymentStack = stack;
        entityData.set(OFFHAND, hand == InteractionHand.OFF_HAND);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(PHASE, OUTBOUND);
        entityData.define(HOOKED_TARGET, -1);
        entityData.define(OFFHAND, false);
        entityData.define(LAUNCH_X_ROTATION, 0.0F);
        entityData.define(LAUNCH_Y_ROTATION, 0.0F);
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (owner == null || !owner.isAlive() || tickCount > MAX_LIFETIME) {
            discard();
            return;
        }

        int phase = entityData.get(PHASE);
        if (phase == OUTBOUND) {
            super.tick();
            if (isAlive() && entityData.get(PHASE) == OUTBOUND
                    && (position().distanceTo(owner.getRopeHoldPosition(1.0F)) >= MAX_RANGE
                    || getDeltaMovement().lengthSqr() <= MIN_OUTBOUND_SPEED_SQUARED)) {
                beginRetracting();
            }
            return;
        }

        Entity hooked = level().getEntity(entityData.get(HOOKED_TARGET));
        if (!(hooked instanceof LivingEntity target) || !target.isAlive()) {
            entityData.set(HOOKED_TARGET, -1);
            returnToOwner(owner);
            return;
        }

        pullTarget(owner, target);
    }

    private void pullTarget(Entity owner, LivingEntity target) {
        Vec3 destination = owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0);
        Vec3 pull = destination.subtract(target.position());
        if (pull.length() <= MELEE_RANGE) {
            discard();
            return;
        }

        Vec3 velocity = pull.normalize().scale(PULL_SPEED);
        target.setDeltaMovement(velocity.x, Math.min(velocity.y, 0.4), velocity.z);
        target.hurtMarked = true;
        followTarget(target);
    }

    private void followTarget(LivingEntity target) {
        setDeltaMovement(Vec3.ZERO);
        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ());
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ());
    }

    private void returnToOwner(Entity owner) {
        entityData.set(PHASE, RETRACTING);
        setNoPhysics(true);
        Vec3 destination = owner.getRopeHoldPosition(1.0F);
        Vec3 returnVector = destination.subtract(position());
        double returnDistance = returnVector.length();
        if (returnDistance <= 0.75) {
            discard();
            return;
        }
        setDeltaMovement(returnVector.scale(Math.min(RETURN_SPEED, returnDistance) / returnDistance));
        super.tick();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!(entity instanceof LivingEntity target) || entity == getOwner()) {
            return false;
        }
        return (!(getOwner() instanceof LivingEntity owner) || owner.canAttack(target)) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (entityData.get(PHASE) != OUTBOUND || !(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, damageSources().thrown(this, getOwner()), hookDamage);
        }
        entityData.set(HOOKED_TARGET, target.getId());
        entityData.set(PHASE, RETRACTING);
        setNoPhysics(true);
        setDeltaMovement(Vec3.ZERO);
        playSound(SoundEvents.CHAIN_HIT, 1.0F, 0.9F);
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (entityData.get(PHASE) == OUTBOUND) {
            setPos(hitResult.getLocation());
            beginRetracting();
            playSound(SoundEvents.CHAIN_HIT, 0.7F, 1.2F);
        }
    }

    private void beginRetracting() {
        entityData.set(PHASE, RETRACTING);
        setNoPhysics(true);
        setDeltaMovement(Vec3.ZERO);
    }

    public void setHookDamage(float hookDamage) {
        this.hookDamage = hookDamage;
    }

    public void lockLaunchRotation() {
        entityData.set(LAUNCH_X_ROTATION, getXRot());
        entityData.set(LAUNCH_Y_ROTATION, getYRot());
    }

    public float getLaunchXRotation() {
        return entityData.get(LAUNCH_X_ROTATION);
    }

    public float getLaunchYRotation() {
        return entityData.get(LAUNCH_Y_ROTATION);
    }

    public boolean wasThrownFromOffhand() {
        return entityData.get(OFFHAND);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            clearDeploymentState();
        }
        super.remove(reason);
    }

    private void clearDeploymentState() {
        boolean changed = clearDeploymentState(deploymentStack);
        if (getOwner() instanceof Player player) {
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                changed |= clearDeploymentState(inventory.getItem(slot));
            }
            if (changed) {
                inventory.setChanged();
            }
        }
        deploymentStack = ItemStack.EMPTY;
    }

    private boolean clearDeploymentState(ItemStack stack) {
        UUID deploymentId = stack.get(ModDataComponents.KAZAGURUMA_DEPLOYMENT.get());
        if (!getUUID().equals(deploymentId)) {
            return false;
        }
        stack.remove(ModDataComponents.KAZAGURUMA_DEPLOYMENT.get());
        return true;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.KAZAGURUMA.get());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        hookDamage = input.getFloatOr("HookDamage", 0.0F);
        entityData.set(PHASE, input.getIntOr("Phase", OUTBOUND));
        entityData.set(HOOKED_TARGET, input.getIntOr("HookedTarget", -1));
        entityData.set(OFFHAND, input.getBooleanOr("Offhand", false));
        entityData.set(LAUNCH_X_ROTATION, input.getFloatOr("LaunchXRotation", 0.0F));
        entityData.set(LAUNCH_Y_ROTATION, input.getFloatOr("LaunchYRotation", 0.0F));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("HookDamage", hookDamage);
        output.putInt("Phase", entityData.get(PHASE));
        output.putInt("HookedTarget", entityData.get(HOOKED_TARGET));
        output.putBoolean("Offhand", wasThrownFromOffhand());
        output.putFloat("LaunchXRotation", getLaunchXRotation());
        output.putFloat("LaunchYRotation", getLaunchYRotation());
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
