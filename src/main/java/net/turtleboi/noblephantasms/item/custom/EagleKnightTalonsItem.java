package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.turtleboi.noblephantasms.item.ModArmorMaterials;
import net.turtleboi.noblephantasms.item.ModItems;

public final class EagleKnightTalonsItem extends Item {
    private static final String SLAMMING_KEY = "noblephantasms:eagle_knight_talons_slamming";
    private static final String SLAM_DISTANCE_KEY = "noblephantasms:eagle_knight_talons_distance";
    private static final String IMPACT_TICK_KEY = "noblephantasms:eagle_knight_talons_impact_tick";
    private static final double SLAM_SPEED = -1.5;
    private static final double IMPACT_RADIUS = 4.0;
    private static final float DAMAGE_PER_BLOCK = 1.5F;
    private static final float MAX_DAMAGE = 40.0F;

    public EagleKnightTalonsItem(Properties properties) {
        super(properties.humanoidArmor(ModArmorMaterials.MESOAMERICAN_MYTH_MATERIAL, ArmorType.BOOTS)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public static void handlePlayerTick(Player player) {
        if (!isWearing(player)) {
            resetSlam(player);
            return;
        }

        boolean slamming = isSlamming(player);
        if (player.onGround()) {
            if (slamming) {
                finishSlam(player, getSlamDistance(player));
            } else {
                clearExpiredImpact(player);
            }
            return;
        }

        if (cannotSlam(player)
                || !player.isShiftKeyDown()
                || player.getDeltaMovement().y >= 0.0) {
            if (slamming) {
                resetSlam(player);
            }
            return;
        }

        if (!slamming) {
            player.getPersistentData().putBoolean(SLAMMING_KEY, true);
        }
        player.getPersistentData().putDouble(SLAM_DISTANCE_KEY,
                Math.max(getSlamDistance(player), player.fallDistance));
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.min(movement.y, SLAM_SPEED), movement.z);
        player.hurtMarked = true;
    }

    public static void handleFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isWearing(player)) {
            return;
        }

        boolean slamming = isSlamming(player);
        boolean recentImpact = isRecentImpact(player);
        if (!slamming && !recentImpact) {
            return;
        }

        event.setCanceled(true);
        if (slamming) {
            finishSlam(player, Math.max(getSlamDistance(player), event.getDistance()));
        }
        player.getPersistentData().remove(IMPACT_TICK_KEY);
    }

    private static void finishSlam(Player player, double distance) {
        player.getPersistentData().remove(SLAMMING_KEY);
        player.getPersistentData().remove(SLAM_DISTANCE_KEY);
        player.getPersistentData().putLong(IMPACT_TICK_KEY, player.level().getGameTime());
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        float fallHeight = (float) distance;
        float damage = Math.min(MAX_DAMAGE, fallHeight * DAMAGE_PER_BLOCK);
        if (damage > 0.0F) {
            for (LivingEntity target : level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(IMPACT_RADIUS, 2.5, IMPACT_RADIUS),
                    target -> target != player && target.isAlive() && !player.isAlliedTo(target))) {
                target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            }
        }

        var groundPos = player.blockPosition().below();
        var groundState = level.getBlockState(groundPos);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState, groundPos),
                player.getX(), player.getY() + 0.1, player.getZ(), 80,
                IMPACT_RADIUS * 0.55, 0.25, IMPACT_RADIUS * 0.55, 0.18);
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.15, player.getZ(),
                8, IMPACT_RADIUS * 0.35, 0.15, IMPACT_RADIUS * 0.35, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY,
                SoundSource.PLAYERS, 1.1F, 0.9F);
    }

    private static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.EAGLE_KNIGHT_TALONS.get());
    }

    private static boolean isSlamming(Player player) {
        return player.getPersistentData().getBooleanOr(SLAMMING_KEY, false);
    }

    private static double getSlamDistance(Player player) {
        return player.getPersistentData().getDoubleOr(SLAM_DISTANCE_KEY, 0.0);
    }

    private static boolean cannotSlam(Player player) {
        return player.isInWater()
                || player.onClimbable()
                || player.isFallFlying()
                || player.isPassenger()
                || player.getAbilities().flying;
    }

    private static boolean isRecentImpact(Player player) {
        long impactTick = player.getPersistentData().getLongOr(IMPACT_TICK_KEY, -10L);
        return player.level().getGameTime() - impactTick <= 2L;
    }

    private static void clearExpiredImpact(Player player) {
        if (!isRecentImpact(player)) {
            player.getPersistentData().remove(IMPACT_TICK_KEY);
        }
    }

    private static void resetSlam(Player player) {
        player.getPersistentData().remove(SLAMMING_KEY);
        player.getPersistentData().remove(SLAM_DISTANCE_KEY);
        player.getPersistentData().remove(IMPACT_TICK_KEY);
    }
}
