package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.custom.ExcaliburProjectile;
import net.turtleboi.noblephantasms.item.ModRarities;
import org.jspecify.annotations.Nullable;

public class ExcaliburItem extends Item {
    public static final int MAX_ENERGY = 7;
    public static final int FULL_CHARGE_TICKS = 20;
    public static final int RECHARGE_DELAY_TICKS = 100;
    public static final int RECHARGE_INTERVAL_TICKS = 60;
    private static final int DECAY_INTERVAL_TICKS = 2;
    private static final float PROJECTILE_SPEED = 1.5F;

    public ExcaliburItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 5.0F, -2.4F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getEnergy(stack) <= 0) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        recharge(stack, level.getGameTime());
        int charge = getCharge(stack);
        boolean charging = owner instanceof LivingEntity livingEntity
                && livingEntity.isUsingItem()
                && livingEntity.getUseItem() == stack;
        int updatedCharge = charge;
        if (charging && charge < FULL_CHARGE_TICKS) {
            updatedCharge++;
        } else if (!charging && charge > 0 && level.getGameTime() % DECAY_INTERVAL_TICKS == 0L) {
            updatedCharge--;
        }

        if (updatedCharge != charge) {
            stack.set(ModDataComponents.EXCALIBUR_CHARGE.get(), updatedCharge);
            if (updatedCharge == FULL_CHARGE_TICKS) {
                level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                        SoundSource.PLAYERS, 1.0F, 1.35F);
                level.playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS, 1.0F, 0.8F);
                if (owner instanceof Player player) {
                    player.releaseUsingItem();
                }
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof Player player)
                || getCharge(stack) < FULL_CHARGE_TICKS
                || getEnergy(stack) <= 0) {
            return false;
        }

        ExcaliburProjectile projectile = new ExcaliburProjectile(serverLevel, player, stack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, PROJECTILE_SPEED, 0.0F);
        Projectile.spawnProjectile(projectile, serverLevel, stack);
        stack.set(ModDataComponents.EXCALIBUR_CHARGE.get(), 0);
        stack.set(ModDataComponents.EXCALIBUR_ENERGY.get(), getEnergy(stack) - 1);
        stack.set(ModDataComponents.EXCALIBUR_NEXT_RECHARGE_TICK.get(),
                serverLevel.getGameTime() + RECHARGE_DELAY_TICKS);
        stack.remove(ModDataComponents.EXCALIBUR_RECHARGE_TICK.get());
        stack.set(ModDataComponents.EXCALIBUR_RELEASE_TICK.get(), serverLevel.getGameTime());
        stack.hurtWithoutBreaking(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS, 1.0F, 0.75F);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.2F, 1.6F);
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    public static int getCharge(ItemStack stack) {
        return Math.max(0, Math.min(
                stack.getOrDefault(ModDataComponents.EXCALIBUR_CHARGE.get(), 0), FULL_CHARGE_TICKS));
    }

    public static float getChargeProgress(ItemStack stack) {
        return getCharge(stack) / (float) FULL_CHARGE_TICKS;
    }

    public static int getEnergy(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(
                ModDataComponents.EXCALIBUR_ENERGY.get(), MAX_ENERGY), 0, MAX_ENERGY);
    }

    public static float getEnergyProgress(ItemStack stack) {
        return getEnergy(stack) / (float) MAX_ENERGY;
    }

    private static void recharge(ItemStack stack, long gameTime) {
        int energy = getEnergy(stack);
        if (energy >= MAX_ENERGY) {
            stack.remove(ModDataComponents.EXCALIBUR_NEXT_RECHARGE_TICK.get());
            return;
        }

        Long nextRechargeTick = stack.get(ModDataComponents.EXCALIBUR_NEXT_RECHARGE_TICK.get());
        if (nextRechargeTick == null) {
            stack.set(ModDataComponents.EXCALIBUR_NEXT_RECHARGE_TICK.get(),
                    gameTime + RECHARGE_DELAY_TICKS);
            return;
        }
        if (gameTime < nextRechargeTick) {
            return;
        }

        int rechargedEnergy = energy + 1;
        stack.set(ModDataComponents.EXCALIBUR_ENERGY.get(), rechargedEnergy);
        stack.set(ModDataComponents.EXCALIBUR_RECHARGE_TICK.get(), gameTime);
        if (rechargedEnergy >= MAX_ENERGY) {
            stack.remove(ModDataComponents.EXCALIBUR_NEXT_RECHARGE_TICK.get());
        } else {
            stack.set(ModDataComponents.EXCALIBUR_NEXT_RECHARGE_TICK.get(),
                    gameTime + RECHARGE_INTERVAL_TICKS);
        }
    }
}
