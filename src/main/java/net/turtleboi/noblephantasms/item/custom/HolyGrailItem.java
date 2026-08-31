package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import org.jspecify.annotations.Nullable;

public final class HolyGrailItem extends Item {
    public static final int MAX_CHARGE = 20;
    private static final int RESTORATION_INTERVAL = 10;
    private static final int RECHARGE_INTERVAL = 20 * 9;
    private static final float HEALTH_PER_PULSE = 1.0F;
    private static final int FOOD_PER_PULSE = 1;
    private static final float SATURATION_PER_PULSE = 1.0F;
    private static final int MAX_VITALITY_DURATION = 20 * 60 * 3;
    private static final int MAX_REGENERATION_DURATION = 20 * 30;

    public HolyGrailItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(ModDataComponents.HOLY_GRAIL_CHARGE.get(), MAX_CHARGE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getCharge(stack) <= 0) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        int usedTicks = getUseDuration(stack, entity) - remainingUseDuration;
        if (usedTicks > 0 && usedTicks % RESTORATION_INTERVAL == 0) {
            int charge = getCharge(stack);
            if (charge <= 0) {
                player.releaseUsingItem();
                return;
            }
            applyRestorationPulse(player);
            setCharge(stack, charge - 1);
            scheduleRecharge(stack, level.getGameTime());
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK.value(),
                    SoundSource.PLAYERS, 0.25F, 1.05F);
        }
        if (getCharge(stack) <= 0 || usedTicks >= MAX_CHARGE * RESTORATION_INTERVAL) {
            player.releaseUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseDuration) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        int usedTicks = getUseDuration(stack, entity) - remainingUseDuration;
        int consumedCharge = Math.min(MAX_CHARGE, usedTicks / RESTORATION_INTERVAL);
        if (consumedCharge <= 0) {
            return false;
        }

        float progress = consumedCharge / (float) MAX_CHARGE;
        int vitalityAmplifier = progress >= 0.5F ? 1 : 0;
        int vitalityDuration = Math.max(RESTORATION_INTERVAL,
                Math.round(MAX_VITALITY_DURATION * progress));
        int regenerationDuration = Math.max(RESTORATION_INTERVAL,
                Math.round(MAX_REGENERATION_DURATION * progress));

        player.addEffect(new MobEffectInstance(
                MobEffects.HEALTH_BOOST, vitalityDuration, vitalityAmplifier, false, true, true));
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, regenerationDuration, 0, false, true, true));
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.65F, 1.15F + progress * 0.35F);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        recharge(stack, level.getGameTime());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_CHARGE * RESTORATION_INTERVAL + 1;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    private static void applyRestorationPulse(ServerPlayer player) {
        player.heal(HEALTH_PER_PULSE);
        int foodLevel = Math.min(20, player.getFoodData().getFoodLevel() + FOOD_PER_PULSE);
        player.getFoodData().setFoodLevel(foodLevel);
        player.getFoodData().setSaturation(Math.min(
                foodLevel, player.getFoodData().getSaturationLevel() + SATURATION_PER_PULSE));
    }

    public static int getCharge(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(
                ModDataComponents.HOLY_GRAIL_CHARGE.get(), MAX_CHARGE), 0, MAX_CHARGE);
    }

    public static float getChargeProgress(ItemStack stack) {
        return getCharge(stack) / (float) MAX_CHARGE;
    }

    private static void setCharge(ItemStack stack, int charge) {
        int clampedCharge = Math.clamp(charge, 0, MAX_CHARGE);
        stack.set(ModDataComponents.HOLY_GRAIL_CHARGE.get(), clampedCharge);
    }

    private static void scheduleRecharge(ItemStack stack, long gameTime) {
        if (getCharge(stack) < MAX_CHARGE
                && stack.get(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get()) == null) {
            stack.set(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get(),
                    gameTime + RECHARGE_INTERVAL);
        }
    }

    private static void recharge(ItemStack stack, long gameTime) {
        int charge = getCharge(stack);
        if (charge >= MAX_CHARGE) {
            stack.remove(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get());
            return;
        }

        Long nextRechargeTick = stack.get(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get());
        if (nextRechargeTick == null) {
            stack.set(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get(),
                    gameTime + RECHARGE_INTERVAL);
            return;
        }
        if (gameTime < nextRechargeTick) {
            return;
        }

        long recovered = 1L + (gameTime - nextRechargeTick) / RECHARGE_INTERVAL;
        int newCharge = Math.min(MAX_CHARGE, charge + (int) recovered);
        setCharge(stack, newCharge);
        if (newCharge >= MAX_CHARGE) {
            stack.remove(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get());
        } else {
            stack.set(ModDataComponents.HOLY_GRAIL_NEXT_RECHARGE_TICK.get(),
                    nextRechargeTick + recovered * RECHARGE_INTERVAL);
        }
    }
}
