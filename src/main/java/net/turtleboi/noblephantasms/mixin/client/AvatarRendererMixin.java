package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Avatar;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void noblePhantasms$previewEditedPose(Avatar entity, AvatarRenderState state,
                                                  float partialTicks, CallbackInfo callbackInfo) {
        ItemPoseEditor.applyThirdPersonPreview(entity, state);
        InteractionHand hand = PridwenBarrierEntity.getReturningHand(entity);
        if (hand == null) {
            return;
        }
        state.isUsingItem = true;
        state.useItemHand = hand;
        state.ticksUsingItem = 1.0F;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? entity.getMainArm() : entity.getMainArm().getOpposite();
        if (arm == HumanoidArm.RIGHT) {
            state.rightArmPose = HumanoidModel.ArmPose.BLOCK;
        } else {
            state.leftArmPose = HumanoidModel.ArmPose.BLOCK;
        }
    }
}
