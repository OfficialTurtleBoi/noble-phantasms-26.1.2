package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class UndyingEffect extends MobEffect {
    private static final float MINIMUM_HEALTH = 0.5F;

    public UndyingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF3D98B);
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (!event.getEntity().hasEffect(ModEffects.UNDYING)) {
            return;
        }

        float survivableDamage = Math.max(0.0F,
                event.getEntity().getHealth() + event.getEntity().getAbsorptionAmount() - MINIMUM_HEALTH);
        event.setNewDamage(Math.min(event.getNewDamage(), survivableDamage));
    }
}
