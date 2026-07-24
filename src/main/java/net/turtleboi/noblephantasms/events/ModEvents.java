package net.turtleboi.noblephantasms.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.custom.CovenantEffect;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID)
public final class ModEvents {
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        BertilakItem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    static void onDamageFinalized(LivingDamageEvent.Pre event) {
        BertilakItem.handleDamageFinalized(event);
    }

    @SubscribeEvent
    static void onDamageComplete(LivingDamageEvent.Post event) {
        BertilakItem.handleDamageComplete(event);
    }

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        BertilakItem.handleLivingDeath(event);
    }

    @SubscribeEvent
    static void onMobEffectRemoved(MobEffectEvent.Remove event) {
        CovenantEffect.handleRemoval(event);
    }

    @SubscribeEvent
    static void onMobEffectExpired(MobEffectEvent.Expired event) {
        CovenantEffect.handleExpiration(event);
    }
}
