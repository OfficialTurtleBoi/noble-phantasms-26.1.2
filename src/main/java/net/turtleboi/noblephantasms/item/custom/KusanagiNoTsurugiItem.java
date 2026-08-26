package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.custom.WindCutterProjectile;
import net.turtleboi.noblephantasms.entity.AfterimageEffect;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;

public class KusanagiNoTsurugiItem extends Item {
    private static final double DASH_SPEED = 1.4;
    private static final int DASH_COOLDOWN_TICKS = 12;
    private static final int WIND_CUTTER_COOLDOWN_TICKS = 20;
    private static final float WIND_CUTTER_SPEED = 1.63F;
    private static final Map<UUID, Long> NEXT_DASH = new HashMap<>();

    public KusanagiNoTsurugiItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 4.0F, -2.0F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    public static void tryDash(ServerPlayer player, int directionId) {
        ItemStack stack = getHeldKusanagi(player);
        long gameTime = player.level().getGameTime();
        if (stack == null
                || gameTime < NEXT_DASH.getOrDefault(player.getUUID(), 0L)
                || player.isSpectator()) {
            return;
        }

        Vec3 forward = player.getViewVector(1.0F).multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-6) {
            return;
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 direction = switch (directionId) {
            case 0 -> forward;
            case 1 -> forward.scale(-1.0);
            case 2 -> right.scale(-1.0);
            case 3 -> right;
            default -> null;
        };
        if (direction == null) {
            return;
        }

        player.setDeltaMovement(direction.x * DASH_SPEED, player.getDeltaMovement().y, direction.z * DASH_SPEED);
        player.hurtMarked = true;
        AfterimageEffect.activate(player, 8);
        NEXT_DASH.put(player.getUUID(), gameTime + DASH_COOLDOWN_TICKS);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        fireWindCutter(serverPlayer, stack);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void fireWindCutter(ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        WindCutterProjectile projectile = new WindCutterProjectile(player.level());
        projectile.setOwner(player);
        projectile.setPos(
                player.getX() + look.x * 0.75,
                player.getEyeY() - 0.25 + look.y * 0.5,
                player.getZ() + look.z * 0.75);
        projectile.shoot(look.x, look.y, look.z, WIND_CUTTER_SPEED, 0.0F);
        Vec3 playerMovement = player.getDeltaMovement();
        projectile.setDeltaMovement(projectile.getDeltaMovement().add(
                playerMovement.x,
                player.onGround() ? 0.0 : playerMovement.y,
                playerMovement.z));
        projectile.setProjectileDamage((float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        player.level().addFreshEntity(projectile);
        player.getCooldowns().addCooldown(stack, WIND_CUTTER_COOLDOWN_TICKS);
    }

    private static ItemStack getHeldKusanagi(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.KUSANAGI_NO_TSURUGI)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModItems.KUSANAGI_NO_TSURUGI)) {
            return player.getOffhandItem();
        }
        return null;
    }
}
