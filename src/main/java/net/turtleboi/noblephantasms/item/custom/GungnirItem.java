package net.turtleboi.noblephantasms.item.custom;

import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;
import net.turtleboi.noblephantasms.item.ModRarities;

public class GungnirItem extends SpearRelicItem {
    public static final int FULL_CHARGE_TICKS = 60;
    private static final float MIN_THROW_SPEED = 0.75F;
    private static final double HOMING_RANGE = 32.0;
    private static final int TARGET_MEMORY_TICKS = 60;
    private static final WeakHashMap<Player, TargetMemory> TARGET_MEMORY = new WeakHashMap<>();

    public GungnirItem(Properties properties) {
        super(properties, ModRarities.LEGENDARY.getValue());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int ticksRemaining) {
        int timeHeld = getUseDuration(itemStack, entity) - ticksRemaining;
        if (!level.isClientSide() && timeHeld == FULL_CHARGE_TICKS) {
            level.playSound(null, entity.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.PLAYERS, 0.8F, 1.35F);
            level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 1.1F, 0.85F);
            level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                    SoundSource.PLAYERS, 0.35F, 1.45F);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        int timeHeld = getUseDuration(itemStack, entity) - remainingTime;
        if (itemStack.nextDamageWillBreak()) {
            return false;
        }

        Holder<SoundEvent> sound = EnchantmentHelper.pickHighestLevel(itemStack,
                EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (level instanceof ServerLevel serverLevel) {
            itemStack.hurtWithoutBreaking(1, player);
            ItemStack thrownItemStack = itemStack.consumeAndReturn(1, player);
            float charge = Mth.clamp(timeHeld / (float) FULL_CHARGE_TICKS, 0.0F, 1.0F);
            float throwSpeed = Mth.lerp(charge, MIN_THROW_SPEED, PROJECTILE_SHOOT_POWER);
            GungnirProjectile projectile = Projectile.spawnProjectileFromRotation(GungnirProjectile::new,
                    serverLevel, thrownItemStack, player, 0.0F, throwSpeed, 1.0F);
            projectile.setThrowCharge(charge);
            if (player.hasInfiniteMaterials()) {
                projectile.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            if (timeHeld >= FULL_CHARGE_TICKS) {
                projectile.setChargedThrow(timeHeld - FULL_CHARGE_TICKS);
                LivingEntity target = selectHomingTarget(serverLevel, player);
                if (target != null) {
                    projectile.setHomingTarget(target);
                }
            }

            level.playSound(null, projectile, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }

        return false;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
        GungnirProjectile projectile = new GungnirProjectile(
                level, position.x(), position.y(), position.z(), itemStack.copyWithCount(1));
        projectile.pickup = AbstractArrow.Pickup.ALLOWED;
        return projectile;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!(owner instanceof Player player) || player.getMainHandItem() != itemStack && player.getOffhandItem() != itemStack) {
            return;
        }

        LivingEntity target = findLookTarget(player);
        if (target != null) {
            TARGET_MEMORY.put(player, new TargetMemory(target.getUUID(), level.getGameTime()));
        }
    }

    private static LivingEntity selectHomingTarget(ServerLevel level, Player player) {
        LivingEntity currentTarget = findLookTarget(player);
        if (currentTarget != null) {
            TARGET_MEMORY.put(player, new TargetMemory(currentTarget.getUUID(), level.getGameTime()));
            return currentTarget;
        }

        TargetMemory targetMemory = TARGET_MEMORY.get(player);
        if (targetMemory == null || level.getGameTime() - targetMemory.gameTime() > TARGET_MEMORY_TICKS) {
            TARGET_MEMORY.remove(player);
            return null;
        }

        Entity lastTarget = level.getEntity(targetMemory.targetId());
        if (lastTarget instanceof LivingEntity livingTarget && isValidTarget(player, livingTarget)
                && player.distanceToSqr(livingTarget) <= HOMING_RANGE * HOMING_RANGE) {
            return livingTarget;
        }

        TARGET_MEMORY.remove(player);
        return null;
    }

    private static LivingEntity findLookTarget(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F).scale(HOMING_RANGE);
        Vec3 endPosition = eyePosition.add(viewVector);
        HitResult blockHitResult = player.level().clipIncludingBorder(new ClipContext(
                eyePosition, endPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            endPosition = blockHitResult.getLocation();
        }

        AABB searchArea = player.getBoundingBox().expandTowards(viewVector).inflate(1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(player.level(), player, eyePosition,
                endPosition, searchArea,
                entity -> entity instanceof LivingEntity livingTarget && isValidTarget(player, livingTarget), 0.0F);
        return entityHitResult != null ? (LivingEntity) entityHitResult.getEntity() : null;
    }

    private static boolean isValidTarget(Player player, LivingEntity target) {
        return target != player
                && target.isAlive()
                && !target.isSpectator()
                && target.isAttackable()
                && !player.isAlliedTo(target)
                && (!(target instanceof Player targetPlayer) || player.canHarmPlayer(targetPlayer));
    }

    private record TargetMemory(UUID targetId, long gameTime) {
    }
}
