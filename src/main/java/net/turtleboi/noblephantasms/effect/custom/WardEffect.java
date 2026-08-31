package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class WardEffect extends MobEffect {
    public WardEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.setData(ModAttachments.ECCLESIASTIC_WARD_VISUAL, true);
        }
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.WARD.getKey())) {
            clearSource(event.getEntity());
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.WARD.getKey())) {
            clearSource(event.getEntity());
        }
    }

    private static void clearSource(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.removeData(ModAttachments.ECCLESIASTIC_WARD_SOURCE);
            entity.removeData(ModAttachments.ECCLESIASTIC_WARD_VISUAL);
        }
    }
}
