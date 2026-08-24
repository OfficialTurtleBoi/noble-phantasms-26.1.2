package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class IwatoshiItem extends SpearRelicItem {
    private static final int CHARGE_INTERVAL_TICKS = 20;
    private static final int MAX_CHARGE_LEVEL = 4;
    private static final int MAX_CHARGE_TICKS = CHARGE_INTERVAL_TICKS * MAX_CHARGE_LEVEL;
    private static final double BASE_RADIUS = 2.5;
    private static final double RADIUS_PER_LEVEL = 1.0;
    private static final float BASE_DAMAGE_MULTIPLIER = 0.55F;
    private static final float DAMAGE_PER_LEVEL = 0.2F;
    private static final double MAX_CHARGE_KNOCKBACK = 1.4;

    public IwatoshiItem(Properties properties) {
        super(properties, Rarity.RARE);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }

        int timeHeld = getUseDuration(itemStack, entity) - remainingUseDuration;
        if (timeHeld <= 0 || timeHeld % CHARGE_INTERVAL_TICKS != 0) {
            return;
        }

        int chargeLevel = getChargeLevel(timeHeld);
        spawnChargePulse(serverLevel, player, chargeLevel);
        if (timeHeld >= MAX_CHARGE_TICKS) {
            player.releaseUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return false;
        }

        int timeHeld = getUseDuration(itemStack, entity) - remainingUseDuration;
        int chargeLevel = getChargeLevel(timeHeld);
        if (chargeLevel <= 0) {
            return false;
        }

        performSweep(serverLevel, player, itemStack, chargeLevel);
        return true;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.SPEAR;
    }

    private void performSweep(ServerLevel level, ServerPlayer player, ItemStack itemStack, int chargeLevel) {
        boolean maximumCharge = chargeLevel == MAX_CHARGE_LEVEL;
        double radius = getSweepRadius(chargeLevel);
        double halfAngle = Math.toRadians(45.0 + chargeLevel * 15.0);
        double minimumDot = Math.cos(halfAngle);
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (BASE_DAMAGE_MULTIPLIER + DAMAGE_PER_LEVEL * chargeLevel);
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, 2.5, radius),
                target -> target != player && target.isAlive() && target.isAttackable()
                        && !player.isAlliedTo(target))) {
            Vec3 direction = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            double distance = direction.lengthSqr();
            if (distance > radius * radius || distance < 1.0E-4
                    || !maximumCharge && forward.dot(direction.normalize()) < minimumDot) {
                continue;
            }

            if (target.hurtServer(level, level.damageSources().playerAttack(player), damage)) {
                level.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, target.getBbWidth() * 0.4, target.getBbHeight() * 0.35,
                        target.getBbWidth() * 0.4, 0.15);
                if (maximumCharge) {
                    target.knockback(MAX_CHARGE_KNOCKBACK,
                            player.getX() - target.getX(), player.getZ() - target.getZ());
                }
            }
        }

        spawnSweepParticles(level, player, forward, radius, halfAngle, maximumCharge);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, maximumCharge ? 2.0F : 1.2F, 0.7F + chargeLevel * 0.1F);
        if (maximumCharge) {
            level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY,
                    SoundSource.PLAYERS, 1.25F, 1.2F);
        }
        player.swing(player.getUsedItemHand(), true);
        level.broadcastEntityEvent(player, IwatoshiAttackState.getAttackEvent(chargeLevel));
        player.awardStat(Stats.ITEM_USED.get(this));
        itemStack.hurtWithoutBreaking(1, player);
    }

    public static int getChargeLevel(float timeHeld) {
        return Mth.clamp(Mth.floor(timeHeld / CHARGE_INTERVAL_TICKS), 0, MAX_CHARGE_LEVEL);
    }

    public static float getChargeProgress(float timeHeld) {
        return Mth.clamp(timeHeld / MAX_CHARGE_TICKS, 0.0F, 1.0F);
    }

    public static int getMaxChargeLevel() {
        return MAX_CHARGE_LEVEL;
    }

    public static int getMaxChargeTicks() {
        return MAX_CHARGE_TICKS;
    }

    private static double getSweepRadius(int chargeLevel) {
        return BASE_RADIUS + chargeLevel * RADIUS_PER_LEVEL;
    }

    private static void spawnChargePulse(ServerLevel level, Player player, int chargeLevel) {
        double radius = getSweepRadius(chargeLevel);
        int particleCount = 12 + chargeLevel * 4;
        for (int particle = 0; particle < particleCount; particle++) {
            double angle = Math.PI * 2.0 * particle / particleCount;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(particle % 2 == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.WAX_ON,
                    x, player.getY() + 0.15, z, 1, 0.0, 0.05, 0.0, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.8F, 0.7F + chargeLevel * 0.15F);
    }

    private static void spawnSweepParticles(ServerLevel level, Player player, Vec3 forward,
                                            double radius, double halfAngle, boolean maximumCharge) {
        double facingAngle = Math.atan2(forward.z, forward.x);
        double startAngle = maximumCharge ? 0.0 : facingAngle - halfAngle;
        double sweepAngle = maximumCharge ? Math.PI * 2.0 : halfAngle * 2.0;
        int particleCount = maximumCharge ? 32 : 16;
        for (int particle = 0; particle <= particleCount; particle++) {
            double angle = startAngle + sweepAngle * particle / particleCount;
            double x = player.getX() + Math.cos(angle) * radius * 0.8;
            double z = player.getZ() + Math.sin(angle) * radius * 0.8;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x, player.getY() + 0.8, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (maximumCharge) {
            level.sendParticles(ParticleTypes.GUST,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    12, radius * 0.4, 0.5, radius * 0.4, 0.1);
        }
    }
}
