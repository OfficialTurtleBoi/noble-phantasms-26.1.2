package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.entity.custom.XiuhcoatlProjectile;

public final class XiuhcoatlItem extends Item {
    private static final int COOLDOWN_TICKS = 20 * 8;
    private static final float PROJECTILE_SPEED = 0.75F;

    public XiuhcoatlItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        XiuhcoatlProjectile projectile = new XiuhcoatlProjectile(serverLevel, player);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                0.0F, PROJECTILE_SPEED, 0.0F);
        projectile.acquireTarget();
        serverLevel.addFreshEntity(projectile);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.75F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
