package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;

public class KusanagiNoTsurugiItem extends Item {
    private static final double DASH_SPEED = 1.4;
    private static final int DASH_COOLDOWN_TICKS = 12;

    public KusanagiNoTsurugiItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 4.0F, -2.0F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    public static void tryDash(ServerPlayer player, int directionId) {
        ItemStack stack = getHeldKusanagi(player);
        if (stack == null || player.getCooldowns().isOnCooldown(stack) || player.isSpectator()) {
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
        player.getCooldowns().addCooldown(stack, DASH_COOLDOWN_TICKS);
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
