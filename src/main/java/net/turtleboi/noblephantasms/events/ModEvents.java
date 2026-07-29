package net.turtleboi.noblephantasms.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.custom.CovenantEffect;
import net.turtleboi.noblephantasms.effect.custom.JudgementEffect;
import net.turtleboi.noblephantasms.item.custom.AndvaranautItem;
import net.turtleboi.noblephantasms.item.custom.AnkhItem;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.BookOfThothItem;
import net.turtleboi.noblephantasms.item.custom.HekaItem;
import net.turtleboi.noblephantasms.item.custom.MegingjordItem;
import net.turtleboi.noblephantasms.item.custom.NekhakhaItem;
import net.turtleboi.noblephantasms.item.custom.ScabbardItem;
import net.turtleboi.noblephantasms.item.custom.UchideNoKozuchiItem;
import net.turtleboi.noblephantasms.item.custom.YamawariItem;
import net.minecraft.world.entity.Mob;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID)
public final class ModEvents {
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        AnkhItem.handleIncomingDamage(event);
        BertilakItem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    static void onDamageFinalized(LivingDamageEvent.Pre event) {
        JudgementEffect.handleDamage(event);
        AndvaranautItem.handleDamage(event);
        BertilakItem.handleDamageFinalized(event);
        AnkhItem.handleDamageFinalized(event);
    }

    @SubscribeEvent
    static void onDamageComplete(LivingDamageEvent.Post event) {
        MegingjordItem.handleDamageComplete(event);
        BertilakItem.handleDamageComplete(event);
    }

    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        AndvaranautItem.handleDrops(event);
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

    @SubscribeEvent
    static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        ScabbardItem.handleEffectApplicable(event);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        HekaItem.handlePlayerTick(event.getEntity());
        NekhakhaItem.handlePlayerTick(event.getEntity());
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob) {
            NekhakhaItem.handleMobTick(mob);
        }
    }

    @SubscribeEvent
    static void onBreakBlock(BreakBlockEvent event) {
        BookOfThothItem.handleBlockBreak(event);
        UchideNoKozuchiItem.handleBlockBreak(event);
        YamawariItem.handleBlockBreak(event);
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BookOfThothItem.handleTableInteraction(event);
    }
}
