package net.turtleboi.noblephantasms.effect.custom;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class FrozenEffect extends MobEffect {
    private static final String IMMOBILIZE_REASON = "frozen";
    private static final double FROZEN_GROUND_FRICTION = 1.01;
    private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "frozen_movement_speed");

    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 8752371);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        ChilledEffect.clearSlow(entity);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.hasEffect(ModEffects.CHILLED)) {
            entity.removeEffect(ModEffects.CHILLED);
        }
        MobEffectInstance instance = entity.getEffect(ModEffects.FROZEN);
        int duration = instance == null ? 0 : instance.getDuration();
        if (duration > 2) {
            ImmobilizationLock.apply(entity, IMMOBILIZE_REASON, true);
        }
        ImmobilizationLock.tick(entity);
        applyIceFriction(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onMobRemoved(ServerLevel level, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        ImmobilizationLock.clear(entity);
        removeFrozenSpeed(entity);
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.FROZEN) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        entity.removeEffect(ModEffects.FROZEN);
        float shatterDamage = Math.min(10.0F, entity.getMaxHealth() / 4.0F);
        event.setNewDamage(event.getNewDamage() + shatterDamage);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GLASS_BREAK,
                SoundSource.AMBIENT, 1.25F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        float height = entity.getBbHeight();
        int count = (int) (height * entity.getBbWidth() * 60.0F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                entity.getX(), entity.getY() + height / 2.0F, entity.getZ(), count,
                0.25, height / 4.0F, 0.25, 0.6);
    }

    public static void handleAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity().hasEffect(ModEffects.FROZEN)) {
            event.setCanceled(true);
        }
    }

    public static void handleInteraction(PlayerInteractEvent event) {
        if (event.getEntity().hasEffect(ModEffects.FROZEN) && event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    public static void handleBlockBreak(BreakBlockEvent event) {
        if (event.getPlayer().hasEffect(ModEffects.FROZEN)) {
            event.setCanceled(true);
        }
    }

    public static void handleSwapHands(LivingSwapItemsEvent.Hands event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.FROZEN)) {
            event.setCanceled(true);
        }
    }

    public static void handleUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.FROZEN)) {
            event.setCanceled(true);
        }
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.FROZEN.getKey())) {
            release(event.getEntity());
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.FROZEN.getKey())) {
            release(event.getEntity());
        }
    }

    public static void clearState(LivingEntity entity) {
        ImmobilizationLock.clear(entity);
        removeFrozenSpeed(entity);
    }

    private static void applyIceFriction(LivingEntity entity) {
        if (!entity.onGround()) {
            return;
        }
        BlockState supportingState = entity.getBlockStateOn();
        float supportingFriction = supportingState.getFriction(entity.level(), entity.getOnPos(), entity);
        if (supportingFriction <= 0.0F) {
            return;
        }
        Vec3 velocity = entity.getDeltaMovement();
        double correction = FROZEN_GROUND_FRICTION / supportingFriction;
        entity.setDeltaMovement(velocity.x * correction, velocity.y, velocity.z * correction);
        entity.hurtMarked = true;
    }

    private static void removeFrozenSpeed(LivingEntity entity) {
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPEED_MODIFIER);
        }
    }

    private static void release(LivingEntity entity) {
        ImmobilizationLock.release(entity, IMMOBILIZE_REASON);
        removeFrozenSpeed(entity);
    }
}
