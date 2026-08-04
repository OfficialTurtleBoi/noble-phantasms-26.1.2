package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class HolyGrailItem extends Item {
    private static final int DRINK_DURATION = 32;
    private static final int UNDYING_DURATION = 20 * 5;
    private static final int COOLDOWN = 20 * 60 * 3;

    public HolyGrailItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(ModEffects.UNDYING, UNDYING_DURATION, 0, false, true, true));
            player.getCooldowns().addCooldown(stack, COOLDOWN);
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK.value(),
                    SoundSource.PLAYERS, 1.0F, 0.9F);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }
}
