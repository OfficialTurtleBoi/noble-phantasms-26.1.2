package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;

public class LuminousEffect extends MobEffect {
    private static final float COLOR_SATURATION = 0.8F;
    private static final float COLOR_VALUE = 1.0F;

    public LuminousEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.setData(ModAttachments.LUMINOUS_COLOR, randomVividColor(entity));
        }
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.getExistingDataOrNull(ModAttachments.LUMINOUS_COLOR) == null) {
            entity.setData(ModAttachments.LUMINOUS_COLOR, randomVividColor(entity));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.LUMINOUS.getKey())) {
            clearColor(event.getEntity());
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.LUMINOUS.getKey())) {
            clearColor(event.getEntity());
        }
    }

    private static void clearColor(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.removeData(ModAttachments.LUMINOUS_COLOR);
        }
    }

    private static int randomVividColor(LivingEntity entity) {
        return Mth.hsvToRgb(entity.getRandom().nextFloat(), COLOR_SATURATION, COLOR_VALUE) & 0xFFFFFF;
    }
}
