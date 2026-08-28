package net.turtleboi.noblephantasms.effect.custom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class FearedEffect extends MobEffect {
    private static final double MOB_FLEE_SPEED = 1.35;
    private static final int FLEE_DISTANCE = 16;
    private static final int FLEE_VERTICAL_DISTANCE = 7;

    public FearedEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A3366);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()
                && entity.getExistingDataOrNull(ModAttachments.FEAR_SOURCE) == null) {
            entity.setData(ModAttachments.FEAR_SOURCE,
                    new FearSourceState(Optional.empty(), entity.position()));
        }
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            driveMobAway(mob, amplifier);
        } else if (entity instanceof Player player) {
            player.setSprinting(true);
            if (player.horizontalCollision && player.onGround() && player.tickCount % 10 == 0) {
                player.jumpFromGround();
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    public static boolean apply(LivingEntity target, Entity source, int duration, int amplifier) {
        if (!canBeFeared(target) || target == source) {
            return false;
        }
        target.setData(ModAttachments.FEAR_SOURCE, FearSourceState.from(source));
        return target.addEffect(new MobEffectInstance(
                ModEffects.FEARED, duration, amplifier, false, true, true), source);
    }

    public static Vec3 forcePlayerTravel(Player player, Vec3 originalInput) {
        if (!player.hasEffect(ModEffects.FEARED) || player.isPassenger()) {
            return originalInput;
        }
        Vec3 away = getAwayDirection(player);
        float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double localX = away.x * cos + away.z * sin;
        double localZ = -away.x * sin + away.z * cos;
        return new Vec3(localX, 0.0, localZ);
    }

    public static void handleTargetChange(LivingChangeTargetEvent event) {
        if (event.getEntity().hasEffect(ModEffects.FEARED)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    public static void handleRemoval(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.FEARED.getKey())) {
            clearSource(event.getEntity());
        }
    }

    public static void handleExpiration(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.FEARED.getKey())) {
            clearSource(event.getEntity());
        }
    }

    public static boolean canBeFeared(LivingEntity entity) {
        return entity.isAlive()
                && !(entity instanceof WitherBoss)
                && !(entity instanceof Warden)
                && !(entity instanceof EnderDragon);
    }

    private static void driveMobAway(Mob mob, int amplifier) {
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        if (mob.tickCount % 8 != 0 && !mob.getNavigation().isDone()) {
            return;
        }

        Vec3 sourcePosition = getSourcePosition(mob);
        Vec3 destination = null;
        if (mob instanceof PathfinderMob pathfinderMob) {
            destination = DefaultRandomPos.getPosAway(
                    pathfinderMob, FLEE_DISTANCE, FLEE_VERTICAL_DISTANCE, sourcePosition);
        }
        if (destination == null) {
            destination = mob.position().add(getAwayDirection(mob).scale(FLEE_DISTANCE));
        }
        mob.getNavigation().moveTo(destination.x, destination.y, destination.z,
                MOB_FLEE_SPEED + amplifier * 0.1);
    }

    private static Vec3 getAwayDirection(LivingEntity entity) {
        Vec3 away = entity.position().subtract(getSourcePosition(entity)).multiply(1.0, 0.0, 1.0);
        if (away.horizontalDistanceSqr() < 1.0E-4) {
            Vec3 look = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
            if (look.horizontalDistanceSqr() < 1.0E-4) {
                return new Vec3(1.0, 0.0, 0.0);
            }
            return look.normalize();
        }
        return away.normalize();
    }

    private static Vec3 getSourcePosition(LivingEntity entity) {
        FearSourceState state = entity.getData(ModAttachments.FEAR_SOURCE);
        Entity source = state.sourceId().map(entity.level()::getEntity).orElse(null);
        return source != null && source.isAlive() ? source.position() : state.origin();
    }

    private static void clearSource(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.removeData(ModAttachments.FEAR_SOURCE);
        }
    }

    public record FearSourceState(Optional<UUID> sourceId, Vec3 origin) {
        public static final FearSourceState EMPTY = new FearSourceState(Optional.empty(), Vec3.ZERO);
        public static final MapCodec<FearSourceState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.optionalFieldOf("source").forGetter(FearSourceState::sourceId),
                Vec3.CODEC.optionalFieldOf("origin", Vec3.ZERO).forGetter(FearSourceState::origin)
        ).apply(instance, FearSourceState::new));
        public static final StreamCodec<ByteBuf, FearSourceState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), FearSourceState::sourceId,
                Vec3.STREAM_CODEC, FearSourceState::origin,
                FearSourceState::new);

        public static FearSourceState from(Entity source) {
            return new FearSourceState(Optional.of(source.getUUID()), source.position());
        }
    }
}
