package net.turtleboi.noblephantasms.item.custom;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.custom.WindslashProjectile;
import net.turtleboi.noblephantasms.entity.AfterimageEffect;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;

public class KusanagiNoTsurugiItem extends Item {
    private static final double DASH_SPEED = 1.4;
    private static final int DASH_COOLDOWN_TICKS = 12;
    private static final int WINDSLASH_COOLDOWN_TICKS = 20;
    private static final float WINDSLASH_SPEED = 1.63F;
    private static final Map<ServerPlayer, Long> NEXT_DASH = new WeakHashMap<>();

    public KusanagiNoTsurugiItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 4.0F, -2.0F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    public static void tryDash(ServerPlayer player, int directionId) {
        long gameTime = player.level().getGameTime();
        if (!player.isAlive() || player.isSpectator() || !isHoldingKusanagi(player) || gameTime < NEXT_DASH.getOrDefault(player, 0L)) {
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
        NEXT_DASH.put(player, gameTime + DASH_COOLDOWN_TICKS);
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

        return fireWindslash(serverPlayer, stack, hand) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    private static boolean fireWindslash(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 look = player.getLookAngle();
        WindslashProjectile projectile = new WindslashProjectile(player.level());
        projectile.setOwner(player);
        projectile.setPos(
                player.getX() + look.x * 0.75,
                player.getEyeY() - 0.25 + look.y * 0.5,
                player.getZ() + look.z * 0.75);
        Vec3 playerMovement = player.getDeltaMovement();
        Vec3 launchMovement = look.scale(WINDSLASH_SPEED).add(
                playerMovement.x,
                player.onGround() ? 0.0 : playerMovement.y,
                playerMovement.z);
        projectile.shoot(launchMovement.x, launchMovement.y, launchMovement.z, (float) launchMovement.length(), 0.0F);
        projectile.setProjectileDamage(getAttackDamage(player, stack, hand));
        projectile.setWeapon(stack);
        if (!player.level().addFreshEntity(projectile)) {
            return false;
        }
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.0F, 1.15F);
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.15F);
        player.getCooldowns().addCooldown(stack, WINDSLASH_COOLDOWN_TICKS);
        return true;
    }

    private static float getAttackDamage(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return (float) modifiers.compute(Attributes.ATTACK_DAMAGE, player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), EquipmentSlot.MAINHAND);
    }

    private static boolean isHoldingKusanagi(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.KUSANAGI_NO_TSURUGI) || player.getOffhandItem().is(ModItems.KUSANAGI_NO_TSURUGI);
    }
}
