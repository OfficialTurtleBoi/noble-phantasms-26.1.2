package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.turtleboi.noblephantasms.effect.custom.FearedEffect;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.entity.custom.YasakaniGuardianEntity;
import net.turtleboi.noblephantasms.item.ModRarities;

public class GjallarhornItem extends InstrumentItem {
    private static final double FEAR_RADIUS = 24.0;
    private static final int FEAR_DURATION = 20 * 4;
    private static final int RALLY_DURATION = 20 * 8;
    private static final int COOLDOWN = 20 * 30;

    public GjallarhornItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .delayedComponent(DataComponents.INSTRUMENT,
                        context -> new InstrumentComponent(context.getOrThrow(Instruments.PONDER_GOAT_HORN)))
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = super.use(level, player, hand);
        if (result != InteractionResult.CONSUME || level.isClientSide()) {
            return result;
        }

        AABB area = player.getBoundingBox().inflate(FEAR_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target.distanceToSqr(player) > FEAR_RADIUS * FEAR_RADIUS) {
                continue;
            }
            if (isAlly(player, target)) {
                rally(target, player);
            } else if (canFear(target)) {
                FearedEffect.apply(target, player, FEAR_DURATION, 0);
            }
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return result;
    }

    private static void rally(LivingEntity target, Player source) {
        target.removeEffect(ModEffects.FEARED);
        target.addEffect(new MobEffectInstance(
                MobEffects.STRENGTH, RALLY_DURATION, 0, false, true, true), source);
    }

    private static boolean isAlly(Player source, LivingEntity target) {
        return target == source
                || source.isAlliedTo(target)
                || target instanceof YasakaniGuardianEntity guardian && guardian.isOwnedBy(source);
    }

    private static boolean canFear(LivingEntity target) {
        return FearedEffect.canBeFeared(target)
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }
}
