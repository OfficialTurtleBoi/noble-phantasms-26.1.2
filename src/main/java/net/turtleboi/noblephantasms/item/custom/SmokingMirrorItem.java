package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.turtleboi.noblephantasms.item.ModRarities;

public final class SmokingMirrorItem extends Item {
    private static final int MARK_DURATION = 20 * 8;
    private static final double PREY_RADIUS = 10.0;
    private static final Map<UUID, MarkedPrey> MARKED_PREY = new HashMap<>();
    private static final Map<UUID, ForcedAttack> FORCED_ATTACKS = new HashMap<>();

    public SmokingMirrorItem(Properties properties) {
        super(properties.stacksTo(1).rarity(ModRarities.LEGENDARY.getValue()).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel level) || target == player) {
            return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        MARKED_PREY.put(target.getUUID(), new MarkedPrey(target.getUUID(), level.getGameTime() + MARK_DURATION));
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 80, 0.7, 1.0, 0.7, 0.15);
        level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, 1.0F, 0.75F);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void handleMobTick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        MARKED_PREY.values().removeIf(mark -> gameTime >= mark.endTick()
                || !(level.getEntity(mark.targetId()) instanceof LivingEntity target) || !target.isAlive());

        LivingEntity prey = null;
        double closestDistance = PREY_RADIUS * PREY_RADIUS;
        for (MarkedPrey mark : MARKED_PREY.values()) {
            if (!(level.getEntity(mark.targetId()) instanceof LivingEntity candidate) || candidate == mob) {
                continue;
            }
            double distance = mob.distanceToSqr(candidate);
            if (distance <= closestDistance) {
                closestDistance = distance;
                prey = candidate;
            }
        }

        ForcedAttack forced = FORCED_ATTACKS.get(mob.getUUID());
        if (prey == null) {
            clearForcedAttack(mob, forced);
            return;
        }

        mob.setTarget(prey);
        if (mob instanceof PathfinderMob pathfinder && forced == null) {
            MeleeAttackGoal goal = new MeleeAttackGoal(pathfinder, 1.25, true);
            mob.goalSelector.addGoal(0, goal);
            FORCED_ATTACKS.put(mob.getUUID(), new ForcedAttack(prey.getUUID(), goal));
        } else if (forced != null && !forced.targetId().equals(prey.getUUID())) {
            FORCED_ATTACKS.put(mob.getUUID(), new ForcedAttack(prey.getUUID(), forced.goal()));
        }
    }

    private static void clearForcedAttack(Mob mob, ForcedAttack forced) {
        if (forced == null) {
            return;
        }
        if (mob.getTarget() != null && mob.getTarget().getUUID().equals(forced.targetId())) {
            mob.setTarget(null);
        }
        if (forced.goal() != null) {
            mob.goalSelector.removeGoal(forced.goal());
        }
        FORCED_ATTACKS.remove(mob.getUUID());
    }

    private record MarkedPrey(UUID targetId, long endTick) {
    }

    private record ForcedAttack(UUID targetId, MeleeAttackGoal goal) {
    }
}
