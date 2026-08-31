package net.turtleboi.noblephantasms.events;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.custom.BleedEffect;
import net.turtleboi.noblephantasms.effect.custom.CovenantEffect;
import net.turtleboi.noblephantasms.effect.custom.FearedEffect;
import net.turtleboi.noblephantasms.effect.custom.JudgementEffect;
import net.turtleboi.noblephantasms.effect.custom.LuminousEffect;
import net.turtleboi.noblephantasms.effect.custom.UndyingEffect;
import net.turtleboi.noblephantasms.effect.custom.WardEffect;
import net.turtleboi.noblephantasms.effect.custom.ChilledEffect;
import net.turtleboi.noblephantasms.effect.custom.FrozenEffect;
import net.turtleboi.noblephantasms.item.custom.AndvaranautItem;
import net.turtleboi.noblephantasms.item.custom.AnkhItem;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.BiaEnPetItem;
import net.turtleboi.noblephantasms.item.custom.CarnwennanItem;
import net.turtleboi.noblephantasms.item.custom.ClawsOfTepeyollotlItem;
import net.turtleboi.noblephantasms.item.custom.MedjuNetjerItem;
import net.turtleboi.noblephantasms.item.custom.EagleKnightTalonsItem;
import net.turtleboi.noblephantasms.item.custom.HekaItem;
import net.turtleboi.noblephantasms.item.custom.HofskorItem;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;
import net.turtleboi.noblephantasms.item.custom.GramItem;
import net.turtleboi.noblephantasms.item.custom.MegingjordItem;
import net.turtleboi.noblephantasms.item.custom.NekhakhaItem;
import net.turtleboi.noblephantasms.item.custom.PridwenItem;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import net.turtleboi.noblephantasms.entity.custom.EcclesiasticEntity;
import net.turtleboi.noblephantasms.item.custom.ScabbardItem;
import net.turtleboi.noblephantasms.item.custom.UchideNoKozuchiItem;
import net.turtleboi.noblephantasms.item.custom.YamawariItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;
import net.turtleboi.noblephantasms.item.custom.SmokingMirrorItem;
import net.turtleboi.noblephantasms.item.custom.TyrfingItem;
import net.turtleboi.noblephantasms.item.custom.TecpatlOfTheFifthSunItem;
import net.turtleboi.noblephantasms.item.custom.YasakaniNoMagatamaItem;
import net.turtleboi.noblephantasms.item.custom.YataNoKagamiItem;
import net.turtleboi.noblephantasms.world.ArtificialOreSavedData;
import net.minecraft.world.entity.Mob;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID)
public final class ModEvents {
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        EcclesiasticEntity.handleIncomingDamage(event);
        AnkhItem.handleIncomingDamage(event);
        BertilakItem.handleIncomingDamage(event);
        BiaEnPetItem.handleIncomingDamage(event);
        HofskorItem.handleIncomingDamage(event);
        PridwenItem.handleIncomingDamage(event);
        YasakaniNoMagatamaItem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    static void onDamageFinalized(LivingDamageEvent.Pre event) {
        FrozenEffect.handleDamage(event);
        JudgementEffect.handleDamage(event);
        UndyingEffect.handleDamage(event);
        EyeOfHorusItem.handleDamage(event);
        AndvaranautItem.handleDamage(event);
        BertilakItem.handleDamageFinalized(event);
        AnkhItem.handleDamageFinalized(event);
        HulioshjalmrItem.handleDamage(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDamageFinalizedLowest(LivingDamageEvent.Pre event) {
        CarnwennanItem.handleDamage(event);
        GramItem.handleDamage(event);
    }

    @SubscribeEvent
    static void onDamageComplete(LivingDamageEvent.Post event) {
        MegingjordItem.handleDamageComplete(event);
        BertilakItem.handleDamageComplete(event);
        YasakaniNoMagatamaItem.handleDamageComplete(event);
    }

    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        AndvaranautItem.handleDrops(event);
    }

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        BertilakItem.handleLivingDeath(event);
        EyeOfHorusItem.handleLivingDeath(event);
        GramItem.handleLivingDeath(event);
        TyrfingItem.handleLivingDeath(event);
        YasakaniNoMagatamaItem.handleLivingDeath(event);
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            TecpatlOfTheFifthSunItem.handlePlayerDeath(player);
        }
    }

    @SubscribeEvent
    static void onLivingFall(LivingFallEvent event) {
        EagleKnightTalonsItem.handleFall(event);
    }

    @SubscribeEvent
    static void onMobEffectAdded(MobEffectEvent.Added event) {
        ChilledEffect.handleAdded(event);
    }

    @SubscribeEvent
    static void onMobEffectRemoved(MobEffectEvent.Remove event) {
        BleedEffect.handleRemoval(event);
        CovenantEffect.handleRemoval(event);
        JudgementEffect.handleRemoval(event);
        LuminousEffect.handleRemoval(event);
        FearedEffect.handleRemoval(event);
        WardEffect.handleRemoval(event);
        ChilledEffect.handleRemoval(event);
        FrozenEffect.handleRemoval(event);
    }

    @SubscribeEvent
    static void onMobEffectExpired(MobEffectEvent.Expired event) {
        BleedEffect.handleExpiration(event);
        CovenantEffect.handleExpiration(event);
        JudgementEffect.handleExpiration(event);
        LuminousEffect.handleExpiration(event);
        FearedEffect.handleExpiration(event);
        WardEffect.handleExpiration(event);
        ChilledEffect.handleExpiration(event);
        FrozenEffect.handleExpiration(event);
    }

    @SubscribeEvent
    static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        ScabbardItem.handleEffectApplicable(event);
    }

    @SubscribeEvent
    static void onEffectParticleModification(EffectParticleModificationEvent event) {
        CovenantEffect.handleParticleModification(event);
        ChilledEffect.handleParticleModification(event);
    }

    @SubscribeEvent
    static void onAttackEntity(AttackEntityEvent event) {
        FrozenEffect.handleAttack(event);
    }

    @SubscribeEvent
    static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        FrozenEffect.handleSwapHands(event);
    }

    @SubscribeEvent
    static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        FrozenEffect.handleUseItemStart(event);
    }

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        FrozenEffect.handlePlayerLogin(event);
    }

    @SubscribeEvent
    static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FrozenEffect.handlePlayerLogout(event);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        EagleKnightTalonsItem.handlePlayerTick(event.getEntity());
        BertilakItem.handlePlayerTick(event.getEntity());
        HekaItem.handlePlayerTick(event.getEntity());
        HulioshjalmrItem.handlePlayerTick(event.getEntity());
        NekhakhaItem.handlePlayerTick(event.getEntity());
        TyrfingItem.handlePlayerTick(event.getEntity());
        YasakaniNoMagatamaItem.handlePlayerTick(event.getEntity());
        TecpatlOfTheFifthSunItem.handlePlayerTick(event.getEntity());
    }

    @SubscribeEvent
    static void onShieldBlock(LivingShieldBlockEvent event) {
        YataNoKagamiItem.handleShieldBlock(event);
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob) {
            SmokingMirrorItem.handleMobTick(mob);
            HulioshjalmrItem.handleMobTick(mob);
        }
    }

    @SubscribeEvent
    static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        FearedEffect.handleTargetChange(event);
        HulioshjalmrItem.handleTargetChange(event);
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
    }

    @SubscribeEvent
    static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
    }

    @SubscribeEvent
    static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
    }

    @SubscribeEvent
    static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
    }

    @SubscribeEvent
    static void onBreakBlock(BreakBlockEvent event) {
        FrozenEffect.handleBlockBreak(event);
        if (event.isCanceled()) {
            return;
        }
        MedjuNetjerItem.handleBlockBreak(event);
        UchideNoKozuchiItem.handleBlockBreak(event);
        YamawariItem.handleBlockBreak(event);
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ClawsOfTepeyollotlItem.handleBreakSpeed(event);
    }

    @SubscribeEvent
    static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        ClawsOfTepeyollotlItem.handleHarvestCheck(event);
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        FrozenEffect.handleInteraction(event);
        HulioshjalmrItem.handleInteraction(event.getEntity());
        MedjuNetjerItem.handleTableInteraction(event);
        UchideNoKozuchiItem.handleRightClickBlock(event);
    }

    @SubscribeEvent
    static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        ArtificialOreSavedData.handleBlockPlaced(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onPistonMove(PistonEvent.Pre event) {
        ArtificialOreSavedData.handlePistonMove(event);
    }
}
