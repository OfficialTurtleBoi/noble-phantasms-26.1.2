package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.item.ModRarities;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class SmokingMirrorItem extends Item {
    private static final int MARK_DURATION = 20 * 8;
    private static final int THREAT_AMPLIFIER = 2;

    public SmokingMirrorItem(Properties properties) {
        super(properties.stacksTo(1).rarity(ModRarities.LEGENDARY.getValue()).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel level) || target == player) {
            return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        target.addEffect(new MobEffectInstance(
                ModEffects.THREAT, MARK_DURATION, THREAT_AMPLIFIER, false, true, true), player);
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 80, 0.7, 1.0, 0.7, 0.15);
        level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, 1.0F, 0.75F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
