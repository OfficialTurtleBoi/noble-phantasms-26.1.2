package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.particle.ModParticles;

public class CovenantEffect extends MobEffect {
    public CovenantEffect() {
        super(MobEffectCategory.NEUTRAL, 0x2C5F34);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && !(entity instanceof Player)) {
            entity.setData(ModAttachments.BERTILAK_COVENANT_GLOW, true);
        }
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player) && !entity.getData(ModAttachments.BERTILAK_COVENANT_GLOW)) {
            entity.setData(ModAttachments.BERTILAK_COVENANT_GLOW, true);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
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

    public static void handleParticleModification(EffectParticleModificationEvent event) {
        if (event.getEffect().getEffect().is(ModEffects.COVENANT.getKey())) {
            event.setParticleOptions(ModParticles.COVENANT_LEAF.get());
        }
    }
}
