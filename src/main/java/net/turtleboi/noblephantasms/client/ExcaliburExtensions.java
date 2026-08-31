package net.turtleboi.noblephantasms.client;

import net.turtleboi.noblephantasms.client.renderer.outline.ExcaliburOutline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.animation.RelicTransform;
import net.turtleboi.noblephantasms.client.animation.RelicWeaponAnimations;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.ExcaliburItem;

public final class ExcaliburExtensions implements IClientItemExtensions {
    public static final float TARGETING_TRANSITION_TICKS = 12.0F;
    private static final float RECOVERY_TRANSITION_TICKS = 4.0F;
    public static final float TARGETING_TRANSLATION_X = -0.08206835F;
    public static final float TARGETING_TRANSLATION_Y = 0.15765251F;
    public static final float TARGETING_TRANSLATION_Z = 0.1965506F;
    public static final float TARGETING_ROTATION_X = 103.96179F;
    public static final float TARGETING_ROTATION_Y = -75.43994F;
    public static final float TARGETING_ROTATION_Z = 177.19794F;
    private static final float RELEASE_RECOIL_TICKS = 5.0F;
    private static final float RELEASE_RECOIL_Z = 0.1F;
    private float recoveryWeight;
    private float lastPoseTick;
    private float lastFrameTime = Float.NaN;
    private HumanoidArm animatedArm = HumanoidArm.RIGHT;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new ExcaliburExtensions(), ModItems.EXCALIBUR.get());
        ExcaliburOutline.register();
    }

    public static void applyThirdPersonUseTransform(ArmedEntityRenderState state,
                                                     PoseStack poseStack, HumanoidArm arm,
                                                     ItemStack itemStack, float timeHeld) {
        RelicTransform transform = ItemPoseEditor.getThirdPersonTransform(state, itemStack, "use");
        if (transform == null) {
            transform = RelicWeaponAnimations.sampleThirdPersonExcaliburUse(itemStack, timeHeld);
        }
        if (transform != null) {
            transform.apply(poseStack, arm);
        }
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick, float equipProgress,
                                           float swingProgress) {
        InteractionHand renderedHand = arm == player.getMainArm()
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        RelicTransform editorTransform = ItemPoseEditor.getFirstPersonTransform(
                renderedHand, itemInHand, "use");
        boolean targeting = editorTransform != null || player.isUsingItem()
                && player.getUseItem().getItem() instanceof ExcaliburItem
                && arm == getUsedArm(player);
        if (targeting) {
            animatedArm = arm;
        }

        if (!targeting && arm != animatedArm) {
            return false;
        }

        float recoil = releaseRecoil(itemInHand, player, partialTick);

        float frameTime = player.tickCount + partialTick;
        float delta = Float.isNaN(lastFrameTime) ? 0.0F : Math.max(frameTime - lastFrameTime, 0.0F);
        lastFrameTime = frameTime;
        if (targeting) {
            lastPoseTick = itemInHand.getUseDuration(player)
                    - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
            recoveryWeight = 1.0F;
        } else if (arm == animatedArm) {
            recoveryWeight = Mth.clamp(recoveryWeight - delta / RECOVERY_TRANSITION_TICKS, 0.0F, 1.0F);
        }

        if (!targeting && recoveryWeight <= 0.0F && recoil <= 0.0F) {
            return false;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float progress = targeting ? 1.0F : Ease.inOutSine(recoveryWeight);
        float visibleEquipProgress = targeting ? 0.0F : equipProgress;
        poseStack.translate(direction * 0.56F, -0.52F + visibleEquipProgress * -0.6F, -0.72F);
        poseStack.translate(0.0F, 0.0F, RELEASE_RECOIL_Z * recoil);
        RelicTransform transform = editorTransform != null ? editorTransform
                : RelicWeaponAnimations.sampleFirstPersonExcaliburUse(itemInHand, lastPoseTick);
        if (transform != null) {
            transform.apply(poseStack, arm, progress);
            return true;
        }
        poseStack.translate(direction * TARGETING_TRANSLATION_X * progress,
                TARGETING_TRANSLATION_Y * progress,
                TARGETING_TRANSLATION_Z * progress);
        poseStack.mulPose(Axis.XP.rotationDegrees(TARGETING_ROTATION_X * progress));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * TARGETING_ROTATION_Y * progress));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * TARGETING_ROTATION_Z * progress));
        return true;
    }

    private static float releaseRecoil(ItemStack stack, LocalPlayer player, float partialTick) {
        Long releaseTick = stack.get(ModDataComponents.EXCALIBUR_RELEASE_TICK.get());
        if (releaseTick == null) {
            return 0.0F;
        }
        float age = player.level().getGameTime() + partialTick - releaseTick;
        if (age < 0.0F || age >= RELEASE_RECOIL_TICKS) {
            return 0.0F;
        }
        float progress = Mth.clamp(age / RELEASE_RECOIL_TICKS, 0.0F, 1.0F);
        return 1.0F - Ease.outCubic(progress);
    }

    private static HumanoidArm getUsedArm(LocalPlayer player) {
        return player.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
    }
}
