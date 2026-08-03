package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags;

public final class ScalesOfMaatItem extends Item {
    private static final int COOLDOWN = 20 * 30;
    private static final float BOSS_MAX_PERCENT_LOSS = 0.1F;

    public ScalesOfMaatItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (!target.isAlive() || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        float playerPercent = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0.0F, 1.0F);
        float targetPercent = Mth.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F);
        float averagePercent = (playerPercent + targetPercent) * 0.5F;
        float balancedTargetPercent = averagePercent;
        if (target.getType().getTags().anyMatch(Tags.EntityTypes.BOSSES::equals)
                && averagePercent < targetPercent) {
            balancedTargetPercent = Math.max(averagePercent, targetPercent - BOSS_MAX_PERCENT_LOSS);
        }

        player.setHealth(player.getMaxHealth() * averagePercent);
        target.setHealth(target.getMaxHealth() * balancedTargetPercent);
        player.getCooldowns().addCooldown(stack, COOLDOWN);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
                    24, 0.4, 0.7, 0.4, 0.1);
            level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 24, target.getBbWidth() * 0.4, target.getBbHeight() * 0.35,
                    target.getBbWidth() * 0.4, 0.1);
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 1.0F, 0.75F);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
