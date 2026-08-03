package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.custom.KazagurumaProjectile;

public class KazagurumaItem extends Item {
    private static final ToolMaterial KAZAGURUMA_MATERIAL = new ToolMaterial(
            ToolMaterial.IRON.incorrectBlocksForDrops(),
            ToolMaterial.DIAMOND.durability(),
            ToolMaterial.IRON.speed(),
            ToolMaterial.IRON.attackDamageBonus(),
            ToolMaterial.IRON.enchantmentValue(),
            ToolMaterial.IRON.repairItems());
    private static final int COOLDOWN_TICKS = 60;
    private static final float THROW_SPEED = 1.5F;

    public KazagurumaItem(Properties properties) {
        super(properties
                .sword(KAZAGURUMA_MATERIAL, 3.0F, -2.4F)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Vec3 look = player.getLookAngle();
        KazagurumaProjectile projectile = new KazagurumaProjectile(serverLevel, player, stack, hand);
        projectile.setPos(player.getX() + look.x * 0.75,
                player.getEyeY() - 0.25 + look.y * 0.5,
                player.getZ() + look.z * 0.75);
        projectile.shoot(look.x, look.y, look.z, THROW_SPEED, 0.0F);
        projectile.lockLaunchRotation();
        projectile.setHookDamage((float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        serverLevel.addFreshEntity(projectile);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.swing(hand, true);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.PLAYERS, 0.9F, 0.75F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
