package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;
import top.theillusivec4.curios.api.SlotContext;

public final class ScabbardItem extends CurioRelicItem {
    public ScabbardItem(Properties properties) {
        super(properties.rarity(ModRarities.LEGENDARY.getValue()));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (!(wearer.level() instanceof ServerLevel) || !wearer.isAlive()) {
            return;
        }

        removePreventedEffects(wearer);
        if (wearer.getHealth() >= wearer.getMaxHealth()) {
            return;
        }

        float missingHealth = 1.0F - wearer.getHealth() / wearer.getMaxHealth();
        int interval = Math.max(5, 20 - (int) (missingHealth * 15.0F));
        if (wearer.tickCount % interval == 0) {
            wearer.heal(1.0F);
        }
    }

    public static void handleEffectApplicable(MobEffectEvent.Applicable event) {
        if (isEquipped(event.getEntity(), ModItems.SCABBARD.get())
                && isPrevented(event.getEffectInstance().getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void removePreventedEffects(LivingEntity wearer) {
        for (MobEffectInstance effect : java.util.List.copyOf(wearer.getActiveEffects())) {
            if (isPrevented(effect.getEffect())) {
                wearer.removeEffect(effect.getEffect());
            }
        }
    }

    private static boolean isPrevented(Holder<MobEffect> effect) {
        return effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER) || effect.is(ModEffects.BLEEDING.getKey());
    }
}
