package net.turtleboi.noblephantasms.effect.custom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.mixin.LivingEntityAccessor;

public final class FrozenEffect extends MobEffect {
    private static final String IMMOBILIZE_REASON = "frozen";
    private static final double FROZEN_GROUND_FRICTION = 1.01;
    private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "frozen_movement_speed");
    private static final Map<UUID, PlayerLockState> PLAYER_STATES = new HashMap<>();
    private static final Map<UUID, MobLockState> MOB_STATES = new HashMap<>();

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
        if (entity instanceof Player player) {
            if (duration > 2) {
                applyToPlayer(player, IMMOBILIZE_REASON);
                applyFrozenSpeed(player);
            }
            tickPlayer(player);
        } else if (entity instanceof Mob mob && duration > 2) {
            applyToMob(mob, IMMOBILIZE_REASON);
        }
        applyIceFriction(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onMobRemoved(ServerLevel level, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        PLAYER_STATES.remove(entity.getUUID());
        MOB_STATES.remove(entity.getUUID());
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

    public static void handlePlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        clearState(event.getEntity());
    }

    public static void handlePlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearState(event.getEntity());
    }

    public static void clearState(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerLockState state = PLAYER_STATES.remove(player.getUUID());
            if (state != null) {
                state.restore(player);
            }
        } else if (entity instanceof Mob mob) {
            MobLockState state = MOB_STATES.remove(mob.getUUID());
            if (state != null) {
                mob.setNoAi(state.originalNoAi);
                mob.hurtMarked = true;
            }
        }
        removeFrozenSpeed(entity);
    }

    private static void applyToPlayer(Player player, String reason) {
        PlayerLockState state = PLAYER_STATES.computeIfAbsent(
                player.getUUID(), ignored -> PlayerLockState.capture(player));
        state.reasons.add(reason);
        state.enforce(player);
    }

    private static void tickPlayer(Player player) {
        PlayerLockState state = PLAYER_STATES.get(player.getUUID());
        if (state != null) {
            state.enforce(player);
            player.hurtMarked = true;
        }
    }

    private static void applyToMob(Mob mob, String reason) {
        MobLockState state = MOB_STATES.computeIfAbsent(
                mob.getUUID(), ignored -> new MobLockState(mob.isNoAi()));
        state.reasons.add(reason);
        if (mob instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
        }
        mob.setNoAi(true);
        mob.hurtMarked = true;
        mob.setSprinting(false);
        mob.setJumping(false);
        mob.getNavigation().stop();
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

    private static void applyFrozenSpeed(LivingEntity entity) {
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeFrozenSpeed(LivingEntity entity) {
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPEED_MODIFIER);
        }
    }

    private static void release(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerLockState state = PLAYER_STATES.get(player.getUUID());
            if (state != null) {
                state.reasons.remove(IMMOBILIZE_REASON);
                if (state.reasons.isEmpty()) {
                    PLAYER_STATES.remove(player.getUUID());
                    state.restore(player);
                } else {
                    state.enforce(player);
                }
            }
        } else if (entity instanceof Mob mob) {
            MobLockState state = MOB_STATES.get(mob.getUUID());
            if (state != null) {
                state.reasons.remove(IMMOBILIZE_REASON);
                if (state.reasons.isEmpty()) {
                    MOB_STATES.remove(mob.getUUID());
                    mob.setNoAi(state.originalNoAi);
                    mob.hurtMarked = true;
                } else {
                    mob.setNoAi(true);
                }
            }
        }
        removeFrozenSpeed(entity);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    private static final class PlayerLockState {
        private final Set<String> reasons = new HashSet<>();
        private final float yaw;
        private final float pitch;
        private final int selectedSlot;
        private final float walkingSpeed;
        private final float flyingSpeed;
        private final boolean mayBuild;
        private final boolean invulnerable;
        private final boolean flying;
        private final InteractionHand usedHand;
        private final ItemStack usedItem;
        private final int useRemaining;

        private PlayerLockState(float yaw, float pitch, int selectedSlot, float walkingSpeed,
                                float flyingSpeed, boolean mayBuild, boolean invulnerable, boolean flying,
                                InteractionHand usedHand, ItemStack usedItem, int useRemaining) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.selectedSlot = selectedSlot;
            this.walkingSpeed = walkingSpeed;
            this.flyingSpeed = flyingSpeed;
            this.mayBuild = mayBuild;
            this.invulnerable = invulnerable;
            this.flying = flying;
            this.usedHand = usedHand;
            this.usedItem = usedItem;
            this.useRemaining = useRemaining;
        }

        private static PlayerLockState capture(Player player) {
            boolean using = player.isUsingItem();
            return new PlayerLockState(player.getYRot(), player.getXRot(), player.getInventory().getSelectedSlot(),
                    player.getAbilities().getWalkingSpeed(), player.getAbilities().getFlyingSpeed(),
                    player.getAbilities().mayBuild, player.getAbilities().invulnerable,
                    player.getAbilities().flying, using ? player.getUsedItemHand() : null,
                    using ? player.getUseItem().copy() : ItemStack.EMPTY,
                    using ? player.getUseItemRemainingTicks() : 0);
        }

        private void enforce(Player player) {
            applyLockedRotation(player, yaw, pitch);
            player.getInventory().setSelectedSlot(selectedSlot);
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            player.getAbilities().mayBuild = false;
            player.getAbilities().flying = false;
            player.getAbilities().setWalkingSpeed(0.0F);
            if (!player.isCreative()) {
                player.getAbilities().setFlyingSpeed(0.0F);
                player.getAbilities().invulnerable = false;
            }
            player.onUpdateAbilities();
            player.setSprinting(false);
            player.setJumping(false);
        }

        private void restore(Player player) {
            player.getAbilities().setWalkingSpeed(walkingSpeed);
            player.getAbilities().setFlyingSpeed(flyingSpeed);
            player.getAbilities().mayBuild = mayBuild;
            player.getAbilities().invulnerable = invulnerable;
            player.getAbilities().flying = flying;
            releasePausedItemUse(player);
            player.onUpdateAbilities();
            player.hurtMarked = true;
        }

        private void releasePausedItemUse(Player player) {
            if (usedHand == null || useRemaining <= 0 || usedItem.isEmpty()) {
                return;
            }
            ItemStack current = player.getItemInHand(usedHand);
            if (!ItemStack.matches(current, usedItem)) {
                return;
            }
            player.startUsingItem(usedHand);
            LivingEntityAccessor accessor = (LivingEntityAccessor) player;
            accessor.noblePhantasms$setUseItem(current);
            accessor.noblePhantasms$setUseItemRemaining(useRemaining);
            player.releaseUsingItem();
        }
    }

    private static final class MobLockState {
        private final Set<String> reasons = new HashSet<>();
        private final boolean originalNoAi;

        private MobLockState(boolean originalNoAi) {
            this.originalNoAi = originalNoAi;
        }
    }

    private static void applyLockedRotation(Player player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRotO = yaw;
        player.yBodyRotO = yaw;
    }
}
