package net.turtleboi.noblephantasms.effect.custom;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class BleedEffect extends MobEffect {
    private static final int MAX_STACKS = 5;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(0xB80508, 0.75F);
    private static final ResourceKey<DamageType> BLEED_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "bleed"));

    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 0xEB5050);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        int stacks = Math.min(MAX_STACKS, amplifier + 1);
        Entity source = getSource(level, entity);
        DamageSource damageSource = new DamageSource(level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(BLEED_DAMAGE), source, source);
        entity.hurtServer(level, damageSource, stacks);
        level.sendParticles(BLOOD, entity.getX(), entity.getY(0.65), entity.getZ(),
                stacks, 0.2, 0.3, 0.2, 0.015);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    public static void applyOrAmplifyBleed(LivingEntity entity, int duration, int amplifier, Entity source) {
        MobEffectInstance existingEffect = entity.getEffect(ModEffects.BLEEDING);
        int addedStacks = Math.max(1, amplifier + 1);
        int currentStacks = existingEffect == null ? 0 : existingEffect.getAmplifier() + 1;
        int newStacks = Math.min(MAX_STACKS, currentStacks + addedStacks);
        int newDuration = existingEffect == null ? duration : Math.max(existingEffect.getDuration(), duration);
        entity.setData(ModAttachments.BLEED_SOURCE, Optional.of(source.getUUID()));
        entity.addEffect(new MobEffectInstance(ModEffects.BLEEDING, newDuration, newStacks - 1), source);
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.BLEEDING.getKey())) {
            event.getEntity().removeData(ModAttachments.BLEED_SOURCE);
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.BLEEDING.getKey())) {
            event.getEntity().removeData(ModAttachments.BLEED_SOURCE);
        }
    }

    private static Entity getSource(ServerLevel level, LivingEntity entity) {
        UUID sourceId = entity.getData(ModAttachments.BLEED_SOURCE).orElse(null);
        return sourceId == null ? null : level.getEntityInAnyDimension(sourceId);
    }
}
