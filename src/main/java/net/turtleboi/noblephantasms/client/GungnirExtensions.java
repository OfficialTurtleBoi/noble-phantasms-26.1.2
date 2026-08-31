package net.turtleboi.noblephantasms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.turtleboi.noblephantasms.client.animation.RelicTransform;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.animation.RelicWeaponAnimations;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;

public class GungnirExtensions implements IClientItemExtensions {
    public static final float STAB_TRANSLATION_Y = 0.75F;
    public static final float STAB_ROTATION_X = -15.0F;
    public static final float STAB_ROTATION_Z = 15.0F;
    public static final float STAB_MIN_EXTENSION = -0.5F;
    public static final float THROW_TRANSLATION_X = -0.15F;
    public static final float THROW_TRANSLATION_Y = 0.55F;
    public static final float THROW_TRANSLATION_Z = 0.55F;
    public static final float THROW_ROTATION_X = -110.0F;
    public static final float THROW_ROTATION_Y = 25.0F;
    public static final float THROW_ROTATION_Z = 5.0F;
    private static final float CHARGE_ROTATION_Y = 30.0F;
    private static final float CHARGE_FORWARD_EXTENSION = 0.15F;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new GungnirExtensions(), ModItems.GUNGNIR.get());
        GungnirOutline.register();
    }

    public static void applyThirdPersonThrowTransform(ArmedEntityRenderState state,
                                                       PoseStack poseStack, HumanoidArm arm,
                                                       ItemStack itemStack, float timeHeld) {
        RelicTransform transform = ItemPoseEditor.getThirdPersonTransform(
                state, itemStack, "throw");
        if (transform == null) {
            transform = RelicWeaponAnimations.sampleThirdPersonGungnirThrow(itemStack, timeHeld);
        }
        if (transform != null) {
            transform.apply(poseStack, arm);
        }
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProgress, float swingProgress) {
        InteractionHand renderedHand = arm == player.getMainArm()
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        RelicTransform editorTransform = ItemPoseEditor.getFirstPersonTransform(
                renderedHand, itemInHand, "throw");
        boolean throwing = editorTransform != null || player.isUsingItem()
                && player.getUseItem().getItem() instanceof GungnirItem
                && arm == getUsedArm(player);
        if (!throwing) {
            return false;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        float timeHeld = editorTransform != null ? 0.0F : itemInHand.getUseDuration(player)
                - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
        RelicTransform transform = editorTransform != null ? editorTransform
                : RelicWeaponAnimations.sampleFirstPersonGungnirThrow(itemInHand, timeHeld);
        if (transform != null) {
            transform.apply(poseStack, arm);
        } else {
            poseStack.translate(direction * THROW_TRANSLATION_X, THROW_TRANSLATION_Y, THROW_TRANSLATION_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(THROW_ROTATION_X));
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * THROW_ROTATION_Y));
            poseStack.mulPose(Axis.ZP.rotationDegrees(direction * THROW_ROTATION_Z));
        }

        float chargeTime = Mth.clamp(timeHeld / GungnirItem.FULL_CHARGE_TICKS, 0.0F, 1.0F);
        if (chargeTime > 0.1F) {
            float shake = Mth.sin((timeHeld - 0.1F) * 1.3F) * (chargeTime - 0.1F);
            poseStack.translate(0.0F, shake * 0.004F, 0.0F);
        }

        poseStack.translate(0.0F, 0.0F, chargeTime * CHARGE_FORWARD_EXTENSION);
        poseStack.mulPose(Axis.YN.rotationDegrees(direction * chargeTime * CHARGE_ROTATION_Y));
        return true;
    }

    private static HumanoidArm getUsedArm(LocalPlayer player) {
        return player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

}
