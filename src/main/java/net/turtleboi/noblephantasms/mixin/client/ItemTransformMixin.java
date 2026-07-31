package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.turtleboi.noblephantasms.client.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemTransform.class)
public class ItemTransformMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void applyEditedItemPose(boolean applyLeftHandFix, PoseStack.Pose pose, CallbackInfo callbackInfo) {
        if (ItemPoseEditor.applyModelTransform((ItemTransform) (Object) this, applyLeftHandFix, pose)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;scale(FFF)V", shift = At.Shift.AFTER))
    private void applyRhongomyniadSpin(boolean applyLeftHandFix, PoseStack.Pose pose, CallbackInfo callbackInfo) {
        RhongomyniadSpinState.apply(pose);
    }
}
