package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.particle.ModParticles;

public final class StunnedEffect extends MobEffect {
    private static final String IMMOBILIZE_REASON = "stunned";

    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 13676558);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        hideDefaultParticles(entity);
        ImmobilizationLock.apply(entity, IMMOBILIZE_REASON, false);
        ImmobilizationLock.tick(entity);
        int tick = entity.tickCount;
        if (tick % 20 == 0 || tick % 18 == 0 || tick % 16 == 0) {
            double x = entity.getX() + (level.getRandom().nextDouble() - 0.5) * 0.5;
            double z = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.5;
            level.sendParticles(ModParticles.STUNNED.get(), x, entity.getY() + entity.getBbHeight(), z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onMobRemoved(ServerLevel level, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        ImmobilizationLock.clear(entity);
    }

    public static void apply(LivingEntity target, Entity source, int duration) {
        target.addEffect(new MobEffectInstance(ModEffects.STUNNED, duration, 0,
                false, false, true), source);
    }

    public static boolean isImmobilized(LivingEntity entity) {
        return entity != null && (entity.hasEffect(ModEffects.STUNNED)
                || entity.hasEffect(ModEffects.FROZEN));
    }

    public static void handleAdded(MobEffectEvent.Added event) {
        if (!event.getEffectInstance().getEffect().is(ModEffects.STUNNED.getKey())) {
            return;
        }
        MobEffectInstance added = event.getEffectInstance();
        if (added.isVisible()) {
            added.update(new MobEffectInstance(ModEffects.STUNNED, added.getDuration(), added.getAmplifier(),
                    added.isAmbient(), false, added.showIcon()));
        }
    }

    public static void handleParticleModification(EffectParticleModificationEvent event) {
        if (event.getEffect().getEffect().is(ModEffects.STUNNED.getKey())) {
            event.setVisible(false);
        }
    }

    public static void handleAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity().hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
        }
    }

    public static void handleInteraction(PlayerInteractEvent event) {
        if (event.getEntity().hasEffect(ModEffects.STUNNED) && event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    public static void handleBlockBreak(BreakBlockEvent event) {
        if (event.getPlayer().hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
        }
    }

    public static void handleSwapHands(LivingSwapItemsEvent.Hands event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
        }
    }

    public static void handleUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
        }
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.STUNNED.getKey())) {
            ImmobilizationLock.release(event.getEntity(), IMMOBILIZE_REASON);
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.STUNNED.getKey())) {
            ImmobilizationLock.release(event.getEntity(), IMMOBILIZE_REASON);
        }
    }

    public static void clearState(LivingEntity entity) {
        ImmobilizationLock.clear(entity);
    }

    private static void hideDefaultParticles(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.STUNNED);
        if (instance != null && instance.isVisible()) {
            instance.update(new MobEffectInstance(ModEffects.STUNNED, instance.getDuration(),
                    instance.getAmplifier(), instance.isAmbient(), false, instance.showIcon()));
        }
    }
}
