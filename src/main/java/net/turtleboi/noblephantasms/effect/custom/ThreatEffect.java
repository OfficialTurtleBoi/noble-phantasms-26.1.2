package net.turtleboi.noblephantasms.effect.custom;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class ThreatEffect extends MobEffect {
    public static final double ATTRACTION_RADIUS = 14.0;
    public static final float PLAYER_DAMAGE_MULTIPLIER = 1.10F;
    private static final int TARGET_REFRESH_INTERVAL = 5;

    public ThreatEffect() {
        super(MobEffectCategory.HARMFUL, 0xB51F24);
    }

    public static void handleMobTick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)
                || !(mob instanceof Enemy)
                || Math.floorMod(mob.tickCount + mob.getId(), TARGET_REFRESH_INTERVAL) != 0) {
            return;
        }

        LivingEntity current = mob.getTarget();
        LivingEntity preferred = null;
        int preferredAmplifier = -1;
        double preferredDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(ATTRACTION_RADIUS),
                candidate -> candidate != mob && candidate.isAlive()
                        && !candidate.isSpectator() && candidate.hasEffect(ModEffects.THREAT))) {
            MobEffectInstance threat = candidate.getEffect(ModEffects.THREAT);
            if (threat == null) {
                continue;
            }

            int amplifier = threat.getAmplifier();
            double distance = mob.distanceToSqr(candidate);
            boolean replacesPreferred = amplifier > preferredAmplifier
                    || amplifier == preferredAmplifier && candidate == current
                    || amplifier == preferredAmplifier && preferred != current
                    && distance < preferredDistance;
            if (replacesPreferred) {
                preferred = candidate;
                preferredAmplifier = amplifier;
                preferredDistance = distance;
            }
        }

        Optional<UUID> forcedTarget = mob.getExistingDataOrNull(ModAttachments.THREAT_TARGET);
        if (preferred != null) {
            mob.setTarget(preferred);
            mob.setData(ModAttachments.THREAT_TARGET, Optional.of(preferred.getUUID()));
        } else if (forcedTarget != null && forcedTarget.isPresent()) {
            if (current != null && forcedTarget.get().equals(current.getUUID())) {
                mob.setTarget(null);
            }
            mob.removeData(ModAttachments.THREAT_TARGET);
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player
                && event.getEntity().hasEffect(ModEffects.THREAT)) {
            event.setAmount(event.getAmount() * PLAYER_DAMAGE_MULTIPLIER);
        }
    }
}
