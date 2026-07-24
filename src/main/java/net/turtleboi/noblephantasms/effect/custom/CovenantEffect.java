package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;

public class CovenantEffect extends MobEffect {
    public CovenantEffect() {
        super(MobEffectCategory.NEUTRAL, 0x4F8A3C);
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.COVENANT.getKey())) {
            BertilakItem.handleCovenantEffectRemoved(event.getEntity());
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.COVENANT.getKey())) {
            BertilakItem.handleCovenantEffectRemoved(event.getEntity());
        }
    }
}
