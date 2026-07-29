package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;

public final class AnkhItem extends CurioRelicItem {
    public static final int REBORN_DURATION = 20 * 60 * 2;
    public static final int INVULNERABILITY_DURATION = 20 * 15;
    private static final double BURST_RADIUS = 8.0;
    private static final float BURST_DAMAGE = 12.0F;
    private static final int BLINDNESS_DURATION = 20 * 8;

    public AnkhItem(Properties properties) {
        super(properties.rarity(ModRarities.LEGENDARY.getValue()));
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isInvulnerable(player)) {
            event.setCanceled(true);
        }
    }

    public static void handleDamageFinalized(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getNewDamage() < player.getHealth() + player.getAbsorptionAmount()
                || player.hasEffect(ModEffects.REBORN)
                || !isEquipped(player, ModItems.ANKH.get())
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        event.setNewDamage(0.0F);
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.addEffect(new MobEffectInstance(ModEffects.REBORN, REBORN_DURATION, 0, false, true, true));

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(BURST_RADIUS),
                target -> isEnemy(player, target))) {
            target.hurtServer(level, level.damageSources().magic(), BURST_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION));
        }

        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                100, BURST_RADIUS * 0.45, 2.0, BURST_RADIUS * 0.45, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean isInvulnerable(Player player) {
        MobEffectInstance reborn = player.getEffect(ModEffects.REBORN);
        return reborn != null && reborn.getDuration() > REBORN_DURATION - INVULNERABILITY_DURATION;
    }

    private static boolean isEnemy(Player player, LivingEntity target) {
        return target != player
                && target.isAlive()
                && (target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == player);
    }
}
