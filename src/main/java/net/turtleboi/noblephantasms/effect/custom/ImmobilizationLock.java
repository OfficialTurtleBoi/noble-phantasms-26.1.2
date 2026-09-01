package net.turtleboi.noblephantasms.effect.custom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.mixin.LivingEntityAccessor;

/** Shared, reason-aware lock state used by effects that fully immobilize an entity. */
final class ImmobilizationLock {
    private static final Map<UUID, PlayerState> PLAYER_STATES = new HashMap<>();
    private static final Map<UUID, MobState> MOB_STATES = new HashMap<>();

    private ImmobilizationLock() {
    }

    static void apply(LivingEntity entity, String reason, boolean preserveItemUse) {
        if (entity instanceof Player player) {
            PlayerState state = PLAYER_STATES.computeIfAbsent(player.getUUID(),
                    ignored -> PlayerState.capture(player, preserveItemUse));
            state.reasons.add(reason);
            state.enforce(player);
        } else if (entity instanceof Mob mob) {
            MobState state = MOB_STATES.computeIfAbsent(mob.getUUID(),
                    ignored -> new MobState(mob.isNoAi()));
            state.reasons.add(reason);
            enforceMob(mob);
        }
    }

    static void tick(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerState state = PLAYER_STATES.get(player.getUUID());
            if (state != null) {
                state.enforce(player);
                player.hurtMarked = true;
            }
        } else if (entity instanceof Mob mob && MOB_STATES.containsKey(mob.getUUID())) {
            enforceMob(mob);
        }
    }

    static void release(LivingEntity entity, String reason) {
        if (entity instanceof Player player) {
            PlayerState state = PLAYER_STATES.get(player.getUUID());
            if (state != null) {
                state.reasons.remove(reason);
                if (state.reasons.isEmpty()) {
                    PLAYER_STATES.remove(player.getUUID());
                    state.restore(player);
                } else {
                    state.enforce(player);
                }
            }
        } else if (entity instanceof Mob mob) {
            MobState state = MOB_STATES.get(mob.getUUID());
            if (state != null) {
                state.reasons.remove(reason);
                if (state.reasons.isEmpty()) {
                    MOB_STATES.remove(mob.getUUID());
                    mob.setNoAi(state.originalNoAi);
                    mob.hurtMarked = true;
                } else {
                    enforceMob(mob);
                }
            }
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    static void clear(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerState state = PLAYER_STATES.remove(player.getUUID());
            if (state != null) {
                state.restore(player);
            }
        } else if (entity instanceof Mob mob) {
            MobState state = MOB_STATES.remove(mob.getUUID());
            if (state != null) {
                mob.setNoAi(state.originalNoAi);
                mob.hurtMarked = true;
            }
        }
    }

    private static void enforceMob(Mob mob) {
        if (mob instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
        }
        mob.setNoAi(true);
        mob.hurtMarked = true;
        mob.setSprinting(false);
        mob.setJumping(false);
        mob.getNavigation().stop();
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

    private static final class PlayerState {
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

        private PlayerState(float yaw, float pitch, int selectedSlot, float walkingSpeed,
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

        private static PlayerState capture(Player player, boolean preserveItemUse) {
            boolean using = preserveItemUse && player.isUsingItem();
            return new PlayerState(player.getYRot(), player.getXRot(), player.getInventory().getSelectedSlot(),
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

    private static final class MobState {
        private final Set<String> reasons = new HashSet<>();
        private final boolean originalNoAi;

        private MobState(boolean originalNoAi) {
            this.originalNoAi = originalNoAi;
        }
    }
}
