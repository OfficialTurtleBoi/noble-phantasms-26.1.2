package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.item.ModArmorMaterials;

public class HulioshjalmrItem extends Item {
    private static final int CONCEALMENT_TICKS = 20;
    private static final int RECOVERY_TICKS = 20 * 5;
    private static final float CONCEALMENT_STEP = 1.0F / CONCEALMENT_TICKS;
    private static final float MINIMUM_RELEASE_STEP = 0.055F;
    private static final float MAXIMUM_RELEASE_STEP = 0.14F;
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "hulioshjalmr_concealment_speed");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
            SPEED_MODIFIER_ID, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public HulioshjalmrItem(Properties properties) {
        super(properties.humanoidArmor(ModArmorMaterials.NORSE_MYTH_MATERIAL, ArmorType.HELMET));
    }

    public static void handlePlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        float progress = getConcealmentProgress(serverPlayer);
        if (!isWearing(serverPlayer)) {
            setConcealmentProgress(serverPlayer, 0.0F);
            removeSpeedModifier(serverPlayer);
            return;
        }

        long gameTime = serverPlayer.level().getGameTime();
        if (serverPlayer.swinging || serverPlayer.isUsingItem()) {
            breakConcealment(serverPlayer);
            return;
        }

        float updatedProgress = progress;
        if (gameTime < serverPlayer.getData(ModAttachments.HULIOSHJALMR_LOCKED_UNTIL)) {
            updatedProgress = releaseProgress(progress);
        } else if (progress < 1.0F && serverPlayer.isShiftKeyDown()) {
            updatedProgress = Math.min(1.0F, progress + CONCEALMENT_STEP);
        } else if (progress < 1.0F) {
            updatedProgress = releaseProgress(progress);
        }

        setConcealmentProgress(serverPlayer, updatedProgress);
        if (updatedProgress >= 1.0F) {
            applySpeedModifier(serverPlayer);
        } else {
            removeSpeedModifier(serverPlayer);
        }
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            breakConcealment(player);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            breakConcealment(player);
        }
    }

    public static void handleInteraction(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            breakConcealment(serverPlayer);
        }
    }

    public static void handleTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && isConcealed(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    public static void handleMobTick(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target instanceof Player player && isConcealed(player)) {
            mob.setTarget(null);
        }
        if (!mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            return;
        }
        mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(brainTarget -> {
            if (brainTarget instanceof Player player && isConcealed(player)) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        });
    }

    public static boolean isConcealed(Player player) {
        return isWearing(player) && getConcealmentProgress(player) >= 1.0F;
    }

    public static float getConcealmentProgress(Player player) {
        return Mth.clamp(player.getData(ModAttachments.HULIOSHJALMR_CONCEALMENT), 0.0F, 1.0F);
    }

    private static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HulioshjalmrItem;
    }

    private static void breakConcealment(ServerPlayer player) {
        if (getConcealmentProgress(player) <= 0.0F) {
            return;
        }
        setConcealmentProgress(player, Math.min(getConcealmentProgress(player), 0.999F));
        player.setData(ModAttachments.HULIOSHJALMR_LOCKED_UNTIL,
                player.level().getGameTime() + RECOVERY_TICKS);
        removeSpeedModifier(player);
    }

    private static float releaseProgress(float progress) {
        float step = Mth.lerp(progress, MINIMUM_RELEASE_STEP, MAXIMUM_RELEASE_STEP);
        return Math.max(0.0F, progress - step);
    }

    private static void setConcealmentProgress(ServerPlayer player, float progress) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        if (Math.abs(player.getData(ModAttachments.HULIOSHJALMR_CONCEALMENT) - clampedProgress) > 1.0E-4F) {
            player.setData(ModAttachments.HULIOSHJALMR_CONCEALMENT, clampedProgress);
        }
    }

    private static void applySpeedModifier(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.addOrUpdateTransientModifier(SPEED_MODIFIER);
        }
    }

    private static void removeSpeedModifier(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
