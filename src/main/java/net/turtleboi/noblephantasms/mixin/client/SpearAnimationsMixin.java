package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.effects.SpearAnimations")
public class SpearAnimationsMixin {
    private static final float RHONGOMYNIAD_JOUST_INWARD_ROTATION = 15.0F;
    private static final float RHONGOMYNIAD_JOUST_DOWNWARD_ROTATION = -15.0F;
    private static final float RHONGOMYNIAD_JOUST_EXTENSION = 0.15F;

    @Inject(method = "thirdPersonHandUse", at = @At("TAIL"))
    private static void holdRhongomyniadPhaseTwo(ModelPart arm, ModelPart head, boolean holdingInRightArm,
                                                 ItemStack itemStack, HumanoidRenderState state, CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof RhongomyniadItem)) {
            return;
        }

        float lowerProgressCorrection =
                RhongomyniadSpinState.getLowerProgressCorrection(itemStack, state.ticksUsingItem);
        arm.xRot -= 20.0F * lowerProgressCorrection * ((float) Math.PI / 180.0F);
    }

    @Inject(method = "thirdPersonUseItem", at = @At("TAIL"))
    private static void adjustRhongomyniadJoust(ArmedEntityRenderState state, PoseStack poseStack, float timeHeld,
                                                HumanoidArm arm, ItemStack itemStack, CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof RhongomyniadItem)) {
            return;
        }

        float joustProgress = calculateJoustProgress(itemStack, timeHeld)
                * RhongomyniadSpinState.getPoseWeight(itemStack, timeHeld);
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * RHONGOMYNIAD_JOUST_INWARD_ROTATION * joustProgress));
        poseStack.mulPose(Axis.XP.rotationDegrees(RHONGOMYNIAD_JOUST_DOWNWARD_ROTATION * joustProgress));
        poseStack.translate(0.0F, RHONGOMYNIAD_JOUST_EXTENSION * joustProgress, 0.0F);
    }

    @Unique
    private static float calculateJoustProgress(ItemStack itemStack, float timeHeld) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return 0.0F;
        }

        float raiseProgress = Mth.clamp(timeHeld / Math.max(kineticWeapon.delayTicks(), 1), 0.0F, 1.0F);
        return Ease.inOutSine(raiseProgress);
    }
}
