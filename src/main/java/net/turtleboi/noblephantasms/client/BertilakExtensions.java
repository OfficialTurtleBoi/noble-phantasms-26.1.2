package net.turtleboi.noblephantasms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;

public class BertilakExtensions implements IClientItemExtensions {
    private static final float TARGETING_TRANSITION_TICKS = 6.0F;
    private static final float RECOVERY_TRANSITION_TICKS = 8.0F;
    private static final float TARGETING_TRANSLATION_X = -0.08F;
    private static final float TARGETING_TRANSLATION_Y = 0.12F;
    private static final float TARGETING_TRANSLATION_Z = 0.3F;
    private static final float TARGETING_ROTATION_X = -70.0F;
    private static final float TARGETING_ROTATION_Y = 0.0F;
    private static final float TARGETING_ROTATION_Z = -25.0F;
    private float poseWeight;
    private float lastFrameTime = Float.NaN;
    private HumanoidArm animatedArm = HumanoidArm.RIGHT;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new BertilakExtensions(), ModItems.BERTILAK.get());
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProgress, float swingProgress) {
        boolean targeting = player.isUsingItem()
                && player.getUseItem().getItem() instanceof BertilakItem
                && arm == getUsedArm(player);
        if (targeting) {
            animatedArm = arm;
        }

        float frameTime = player.tickCount + partialTick;
        float delta = Float.isNaN(lastFrameTime) ? 0.0F : Math.max(frameTime - lastFrameTime, 0.0F);
        lastFrameTime = frameTime;
        if (targeting) {
            poseWeight = Mth.clamp(poseWeight + delta / TARGETING_TRANSITION_TICKS, 0.0F, 1.0F);
        } else if (arm == animatedArm) {
            poseWeight = Mth.clamp(poseWeight - delta / RECOVERY_TRANSITION_TICKS, 0.0F, 1.0F);
        }

        if (!targeting && (arm != animatedArm || poseWeight <= 0.0F)) {
            return false;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float progress = Ease.inOutSine(poseWeight);
        poseStack.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
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
