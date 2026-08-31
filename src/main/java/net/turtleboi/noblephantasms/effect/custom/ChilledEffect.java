package net.turtleboi.noblephantasms.effect.custom;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.particle.ModParticles;

public final class ChilledEffect extends MobEffect {
    public static final int MAX_STACKS = 5;
    public static final int FREEZE_DURATION = 60;
    private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "chilled_movement_speed");

    public ChilledEffect() {
        super(MobEffectCategory.HARMFUL, 59903);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        hideDefaultParticles(entity);
        int stacks = Math.min(MAX_STACKS, amplifier + 1);
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double slow = entity instanceof Player ? -0.2 * stacks : -0.125 * stacks;
            movementSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER, slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (entity.tickCount % 5 == 0) {
            for (int index = 0; index < stacks * 2; index++) {
                double x = entity.getX() + (level.getRandom().nextDouble() - 0.5) * 0.5;
                double y = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.5;
                level.sendParticles(ModParticles.CHILLED.get(), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (stacks >= MAX_STACKS) {
            Entity source = getSource(level, entity);
            clearSlow(entity);
            entity.removeEffect(ModEffects.CHILLED);
            entity.addEffect(new MobEffectInstance(ModEffects.FROZEN, FREEZE_DURATION, 0,
                    false, false, true), source);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    public static void applyStack(LivingEntity target, Entity source, int duration) {
        if (target.hasEffect(ModEffects.FROZEN)) {
            return;
        }
        MobEffectInstance current = target.getEffect(ModEffects.CHILLED);
        int amplifier = current == null ? 0 : Math.min(MAX_STACKS - 1, current.getAmplifier() + 1);
        Entity responsibleSource = resolveResponsibleEntity(source);
        if (amplifier >= MAX_STACKS - 1) {
            clearSlow(target);
            target.removeEffect(ModEffects.CHILLED);
            target.addEffect(new MobEffectInstance(ModEffects.FROZEN, FREEZE_DURATION, 0,
                    false, false, true), responsibleSource);
            return;
        }
        if (responsibleSource != null) {
            target.setData(ModAttachments.CHILLED_SOURCE, Optional.of(responsibleSource.getUUID()));
        } else {
            target.removeData(ModAttachments.CHILLED_SOURCE);
        }
        target.addEffect(new MobEffectInstance(ModEffects.CHILLED, duration, amplifier,
                false, false, true), responsibleSource);
    }

    public static void handleAdded(MobEffectEvent.Added event) {
        if (!event.getEffectInstance().getEffect().is(ModEffects.CHILLED.getKey())) {
            return;
        }
        Entity source = resolveResponsibleEntity(event.getEffectSource());
        if (source != null) {
            event.getEntity().setData(ModAttachments.CHILLED_SOURCE, Optional.of(source.getUUID()));
        }
        MobEffectInstance added = event.getEffectInstance();
        if (added.isVisible()) {
            added.update(new MobEffectInstance(ModEffects.CHILLED, added.getDuration(), added.getAmplifier(),
                    added.isAmbient(), false, added.showIcon()));
        }
        hideDefaultParticles(event.getEntity());
    }

    public static void handleParticleModification(EffectParticleModificationEvent event) {
        if (event.getEffect().getEffect().is(ModEffects.CHILLED.getKey())) {
            event.setVisible(false);
        }
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.CHILLED.getKey())) {
            clearSlow(event.getEntity());
            event.getEntity().removeData(ModAttachments.CHILLED_SOURCE);
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.CHILLED.getKey())) {
            clearSlow(event.getEntity());
            event.getEntity().removeData(ModAttachments.CHILLED_SOURCE);
        }
    }

    public static void clearSlow(LivingEntity entity) {
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPEED_MODIFIER);
        }
    }

    private static void hideDefaultParticles(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.CHILLED);
        if (instance == null || !instance.isVisible()) {
            return;
        }
        instance.update(new MobEffectInstance(ModEffects.CHILLED, instance.getDuration(), instance.getAmplifier(),
                instance.isAmbient(), false, instance.showIcon()));
    }

    private static Entity getSource(ServerLevel level, LivingEntity entity) {
        UUID sourceId = entity.getData(ModAttachments.CHILLED_SOURCE).orElse(null);
        return sourceId == null ? null : level.getEntityInAnyDimension(sourceId);
    }

    private static Entity resolveResponsibleEntity(Entity source) {
        if (source instanceof Projectile projectile && projectile.getOwner() != null) {
            return resolveResponsibleEntity(projectile.getOwner());
        }
        if (source instanceof AreaEffectCloud cloud && cloud.getOwner() != null) {
            return resolveResponsibleEntity(cloud.getOwner());
        }
        return source;
    }
}
