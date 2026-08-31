package net.turtleboi.noblephantasms.entity.custom;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;

public final class ExcaliburProjectile extends ThrowableProjectile
        implements ItemSupplier {
    private static final Identifier ENERGY_MODEL = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "excalibur_projectile");
    private static final float DAMAGE = 14.0F;
    private static final int LIFESPAN = 40;
    private final ItemStack displayItem;
    private final Set<UUID> hitEntities = new HashSet<>();
    private ItemStack weapon = ItemStack.EMPTY;

    public ExcaliburProjectile(EntityType<? extends ExcaliburProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        displayItem = new ItemStack(ModItems.EXCALIBUR.get());
        displayItem.set(DataComponents.ITEM_MODEL, ENERGY_MODEL);
    }

    public ExcaliburProjectile(ServerLevel level, LivingEntity owner, ItemStack weapon) {
        this(ModEntities.EXCALIBUR_PROJECTILE.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.15, owner.getZ());
        this.weapon = weapon.copy();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder entityData) {
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount >= LIFESPAN) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity target = hitResult.getEntity();
        if (!hitEntities.add(target.getUUID())) {
            return;
        }
        Entity owner = getOwner();
        if (level() instanceof ServerLevel serverLevel) {
            var damageSource = damageSources().thrown(this, owner == null ? this : owner);
            float damage = weapon.isEmpty()
                    ? DAMAGE
                    : EnchantmentHelper.modifyDamage(serverLevel, weapon, target, damageSource, DAMAGE);
            if (target.hurtServer(serverLevel, damageSource, damage) && !weapon.isEmpty()) {
                EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(
                        serverLevel, target, damageSource, weapon, brokenItem -> weapon = ItemStack.EMPTY);
            }
            playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.8F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!level().isClientSide()) {
            playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.8F);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) {
            return false;
        }
        if (hitEntities.contains(entity.getUUID())) {
            return false;
        }
        Entity owner = getOwner();
        return !(owner instanceof LivingEntity livingOwner)
                || !(entity instanceof LivingEntity livingTarget)
                || livingOwner.canAttack(livingTarget);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public ItemStack getItem() {
        return displayItem;
    }

    @Override
    public ItemStack getWeaponItem() {
        return weapon;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        weapon = input.read("Weapon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        tickCount = Mth.clamp(input.getIntOr("Age", 0), 0, LIFESPAN);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!weapon.isEmpty()) {
            output.store("Weapon", ItemStack.CODEC, weapon);
        }
        output.putInt("Age", tickCount);
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
