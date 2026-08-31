package net.turtleboi.noblephantasms.client;

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
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.animation.RelicTransform;
import net.turtleboi.noblephantasms.client.animation.RelicWeaponAnimations;

public class BertilakExtensions implements IClientItemExtensions {
    public static final float TARGETING_TRANSITION_TICKS = 10.0F;
    private static final float RECOVERY_TRANSITION_TICKS = 8.0F;
    public static final float TARGETING_TRANSLATION_X = -0.08206835F;
    public static final float TARGETING_TRANSLATION_Y = 0.15765251F;
    public static final float TARGETING_TRANSLATION_Z = 0.1965506F;
    public static final float TARGETING_ROTATION_X = 103.96179F;
    public static final float TARGETING_ROTATION_Y = -75.43994F;
    public static final float TARGETING_ROTATION_Z = 177.19794F;
    public static final float THIRD_PERSON_TRANSLATION_X = -0.1033F;
    public static final float THIRD_PERSON_TRANSLATION_Y = 0.3011F;
    public static final float THIRD_PERSON_TRANSLATION_Z = -0.2967F;
    public static final float THIRD_PERSON_ROTATION_X = 0.0F;
    public static final float THIRD_PERSON_ROTATION_Y = 75.0F;
    public static final float THIRD_PERSON_ROTATION_Z = -52.0F;
    private float recoveryWeight;
    private float lastPoseTick;
    private float lastFrameTime = Float.NaN;
    private HumanoidArm animatedArm = HumanoidArm.RIGHT;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new BertilakExtensions(), ModItems.BERTILAK.get());
    }

    public static void applyThirdPersonCovenantTransform(ArmedEntityRenderState state,
                                                         PoseStack poseStack, HumanoidArm arm,
                                                         ItemStack itemStack, float timeHeld) {
        RelicTransform transform = RelicWeaponAnimations.sampleThirdPersonBertilakCovenant(
                itemStack, timeHeld);
        RelicTransform editorTransform = ItemPoseEditor.getThirdPersonTransform(
                state, itemStack, "covenant");
        if (editorTransform != null) {
            transform = editorTransform;
        }
        if (transform != null) {
            applyTransform(poseStack, arm, transform, 1.0F);
            return;
        }

        float progress = Ease.inOutSine(Mth.clamp(timeHeld / TARGETING_TRANSITION_TICKS, 0.0F, 1.0F));
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(direction * THIRD_PERSON_TRANSLATION_X * progress,
                THIRD_PERSON_TRANSLATION_Y * progress, THIRD_PERSON_TRANSLATION_Z * progress);
        poseStack.mulPose(Axis.XP.rotationDegrees(THIRD_PERSON_ROTATION_X * progress));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * THIRD_PERSON_ROTATION_Y * progress));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * THIRD_PERSON_ROTATION_Z * progress));
    }

    private static void applyTransform(PoseStack poseStack, HumanoidArm arm,
                                       RelicTransform transform, float progress) {
        transform.apply(poseStack, arm, progress);
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProgress, float swingProgress) {
        InteractionHand renderedHand = arm == player.getMainArm()
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        RelicTransform editorTransform = ItemPoseEditor.getFirstPersonTransform(
                renderedHand, itemInHand, "covenant");
        boolean targeting = editorTransform != null || player.isUsingItem()
                && player.getUseItem().getItem() instanceof BertilakItem
                && arm == getUsedArm(player);
        if (targeting) {
            animatedArm = arm;
        }

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

        if (!targeting && (arm != animatedArm || recoveryWeight <= 0.0F)) {
            return false;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float progress = targeting ? 1.0F : Ease.inOutSine(recoveryWeight);
        float visibleEquipProgress = targeting ? 0.0F : equipProgress;
        poseStack.translate(direction * 0.56F, -0.52F + visibleEquipProgress * -0.6F, -0.72F);
        RelicTransform transform = editorTransform != null ? editorTransform
                : RelicWeaponAnimations.sampleFirstPersonBertilakCovenant(itemInHand, lastPoseTick);
        if (transform != null) {
            applyTransform(poseStack, arm, transform, progress);
            return true;
        }
        poseStack.translate(direction * TARGETING_TRANSLATION_X * progress, TARGETING_TRANSLATION_Y * progress,
                TARGETING_TRANSLATION_Z * progress);
        poseStack.mulPose(Axis.XP.rotationDegrees(TARGETING_ROTATION_X * progress));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * TARGETING_ROTATION_Y * progress));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * TARGETING_ROTATION_Z * progress));
        return true;
    }

    private static HumanoidArm getUsedArm(LocalPlayer player) {
        return player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }
}
