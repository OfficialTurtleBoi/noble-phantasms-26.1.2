package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class JudgementEffect extends MobEffect {
    private static final float DAMAGE_MULTIPLIER = 1.25F;

    public JudgementEffect() {
        super(MobEffectCategory.HARMFUL, 0xE6B84A);
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.JUDGEMENT)) {
            event.setNewDamage(event.getNewDamage() * DAMAGE_MULTIPLIER);
        }
    }
}
