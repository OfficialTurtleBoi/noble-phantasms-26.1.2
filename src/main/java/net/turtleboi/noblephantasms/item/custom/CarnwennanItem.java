package net.turtleboi.noblephantasms.item.custom;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class CarnwennanItem extends Item {
    private static final double SHADOW_STEP_RANGE = 16.0;
    private static final int SHADOW_STEP_COOLDOWN = 20 * 5;
    private static final float BACKSTAB_MULTIPLIER = 1.5F;
    private static final double REAR_ARC_THRESHOLD = -0.5;
    private static final double[] DISTANCE_OFFSETS = {0.0, 0.5, 1.0, 1.5};
    private static final double[] HEIGHT_OFFSETS = {0.0, 0.5, 1.0, -0.5, -1.0};

    public CarnwennanItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 0.0F, -1.2F)
                .rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        Mob target = findLookTarget(player);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        Vec3 destination = findDestination(serverPlayer, target);
        if (destination == null) {
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = serverPlayer.level();
        Vec3 departure = serverPlayer.position();
        float destinationYaw = yawToward(destination, target.position());
        spawnShadowSmoke(serverLevel, departure, serverPlayer.getBbWidth(), serverPlayer.getBbHeight());
        if (!serverPlayer.teleportTo(serverLevel, destination.x, destination.y, destination.z,
                Set.of(), destinationYaw, serverPlayer.getXRot(), false)) {
            return InteractionResult.FAIL;
        }

        serverPlayer.setDeltaMovement(Vec3.ZERO);
        serverPlayer.fallDistance = 0.0F;
        spawnShadowSmoke(serverLevel, serverPlayer.position(), serverPlayer.getBbWidth(), serverPlayer.getBbHeight());
        serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 0.65F);
        serverPlayer.getCooldowns().addCooldown(stack, SHADOW_STEP_COOLDOWN);
        serverPlayer.swing(hand, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(player.getMainHandItem().getItem() instanceof CarnwennanItem)
                || !isBehind(player, event.getEntity())) {
            return;
        }

        event.setNewDamage(event.getNewDamage() * BACKSTAB_MULTIPLIER);
    }

    private static Mob findLookTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = start.add(view.scale(SHADOW_STEP_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB searchArea = player.getBoundingBox().expandTowards(view.scale(SHADOW_STEP_RANGE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player.level(), player, start, end, searchArea,
                entity -> isValidTarget(player, entity), 0.0F);
        return entityHit != null && entityHit.getEntity() instanceof Mob mob
                && player.hasLineOfSight(mob) ? mob : null;
    }

    private static boolean isValidTarget(Player player, Entity entity) {
        return entity instanceof Mob mob
                && mob.isAlive()
                && !mob.isSpectator()
                && mob.isAttackable()
                && !player.isAlliedTo(mob);
    }

    private static Vec3 findDestination(ServerPlayer player, Mob target) {
        Vec3 targetForward = horizontalDirection(target.getLookAngle(), target.getYRot());
        double baseDistance = target.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + 0.45;

        for (double distanceOffset : DISTANCE_OFFSETS) {
            Vec3 horizontalPosition = target.position().subtract(targetForward.scale(baseDistance + distanceOffset));
            for (double heightOffset : HEIGHT_OFFSETS) {
                Vec3 candidate = new Vec3(horizontalPosition.x, target.getY() + heightOffset, horizontalPosition.z);
                if (isSafeDestination(player, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isSafeDestination(ServerPlayer player, Vec3 destination) {
        ServerLevel level = player.level();
        if (!level.hasChunkAt(BlockPos.containing(destination))) {
            return false;
        }

        AABB destinationBounds = player.getBoundingBox().move(
                destination.x - player.getX(),
                destination.y - player.getY(),
                destination.z - player.getZ());
        return level.getWorldBorder().isWithinBounds(destinationBounds)
                && level.noCollision(player, destinationBounds);
    }

    private static boolean isBehind(Player attacker, LivingEntity target) {
        Vec3 targetForward = horizontalDirection(target.getLookAngle(), target.getYRot());
        Vec3 targetToAttacker = new Vec3(
                attacker.getX() - target.getX(),
                0.0,
                attacker.getZ() - target.getZ());
        if (targetToAttacker.lengthSqr() < 1.0E-6) {
            return false;
        }
        return targetForward.dot(targetToAttacker.normalize()) <= REAR_ARC_THRESHOLD;
    }

    private static Vec3 horizontalDirection(Vec3 lookAngle, float yaw) {
        Vec3 horizontal = new Vec3(lookAngle.x, 0.0, lookAngle.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            return horizontal.normalize();
        }
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        return (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
    }

    private static void spawnShadowSmoke(ServerLevel level, Vec3 position, float width, float height) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                position.x, position.y + height * 0.5, position.z,
                28, width * 0.65, height * 0.4, width * 0.65, 0.035);
        level.sendParticles(ParticleTypes.SQUID_INK,
                position.x, position.y + height * 0.55, position.z,
                12, width * 0.45, height * 0.3, width * 0.45, 0.025);
    }
}
