package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import org.jspecify.annotations.Nullable;

public final class PridwenItem extends ShieldItem {
    public static final float MAX_BARRIER_HEALTH = 256.0F;
    private static final int RECHARGE_INTERVAL_TICKS = 4;

    public PridwenItem(Properties properties) {
        super(properties
                .durability(8192)
                .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                .repairable(ItemTags.WOODEN_TOOL_MATERIALS)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F,
                        1.0F,
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (isBarrierBroken(player.getItemInHand(hand))) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity,
                              @Nullable EquipmentSlot slot) {
        float health = getBarrierHealth(stack);
        if (health >= MAX_BARRIER_HEALTH) {
            stack.remove(ModDataComponents.PRIDWEN_ENERGY.get());
            stack.remove(ModDataComponents.PRIDWEN_BROKEN.get());
            stack.remove(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get());
            return;
        }
        if (entity instanceof LivingEntity livingEntity
                && livingEntity.isUsingItem() && livingEntity.getUseItem() == stack) {
            stack.remove(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get());
            return;
        }

        long gameTime = level.getGameTime();
        Long nextRechargeTick = stack.get(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get());
        if (nextRechargeTick == null) {
            stack.set(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get(),
                    gameTime + RECHARGE_INTERVAL_TICKS);
            return;
        }
        if (gameTime < nextRechargeTick) {
            return;
        }

        float rechargedHealth = Math.min(MAX_BARRIER_HEALTH, health + 1.0F);
        if (rechargedHealth >= MAX_BARRIER_HEALTH) {
            stack.remove(ModDataComponents.PRIDWEN_ENERGY.get());
            stack.remove(ModDataComponents.PRIDWEN_BROKEN.get());
            stack.remove(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get());
        } else {
            stack.set(ModDataComponents.PRIDWEN_ENERGY.get(), rechargedHealth);
            stack.set(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get(),
                    gameTime + RECHARGE_INTERVAL_TICKS);
        }

        LivingEntity livingEntity = entity instanceof LivingEntity living ? living : null;
        stack.hurtAndBreak(1, level, livingEntity, item -> {
            if (livingEntity != null && slot != null) {
                livingEntity.onEquippedItemBroken(item, slot);
            }
        });
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack,
                          int remainingUseDuration) {
        super.onUseTick(level, entity, itemStack, remainingUseDuration);
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            if (isBarrierBroken(itemStack)) {
                player.releaseUsingItem();
                return;
            }
            PridwenBarrierEntity.ensureActive(serverLevel, player);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity,
                                int remainingUseDuration) {
        return entity instanceof Player player && PridwenBarrierEntity.beginRetraction(player);
    }

    public static float getBarrierHealth(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(
                ModDataComponents.PRIDWEN_ENERGY.get(), MAX_BARRIER_HEALTH),
                0.0F, MAX_BARRIER_HEALTH);
    }

    public static float getBarrierHealthProgress(ItemStack stack) {
        return getBarrierHealth(stack) / MAX_BARRIER_HEALTH;
    }

    public static boolean isBarrierBroken(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PRIDWEN_BROKEN.get(), false);
    }

    public static boolean damageBarrier(ItemStack stack, float damage) {
        float remaining = Math.max(0.0F, getBarrierHealth(stack) - Math.max(0.0F, damage));
        stack.set(ModDataComponents.PRIDWEN_ENERGY.get(), remaining);
        stack.remove(ModDataComponents.PRIDWEN_NEXT_RECHARGE_TICK.get());
        if (remaining <= 0.0F) {
            stack.set(ModDataComponents.PRIDWEN_BROKEN.get(), true);
            return true;
        }
        return false;
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && PridwenBarrierEntity.tryBlockDamage(
                        level, event.getSource(), event.getEntity(), event.getAmount())) {
            event.setCanceled(true);
        }
    }
}
