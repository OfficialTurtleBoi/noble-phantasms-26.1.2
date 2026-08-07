package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem.TrophyData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {
    @Shadow
    @Final
    private CustomHeadLayer.Transforms transforms;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$submitTrophyHead(PoseStack poseStack,
                                                  SubmitNodeCollector submitNodeCollector,
                                                  int lightCoords, LivingEntityRenderState state,
                                                  float yRot, float xRot, CallbackInfo callbackInfo) {
        TrophyData trophyData = TrophyHeadRenderer.getWornHead(state);
        if (trophyData == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(transforms.horizontalScale(), 1.0F, transforms.horizontalScale());
        EntityModel<?> parentModel = ((CustomHeadLayer<?, ?>) (Object) this).getParentModel();
        parentModel.root().translateAndRotate(poseStack);
        ((HeadedModel) parentModel).translateToHead(poseStack);
        poseStack.translate(0.0F, transforms.skullYOffset(), 0.0F);
        poseStack.scale(1.1875F, 1.1875F, 1.1875F);
        TrophyHeadRenderer.submitWorn(trophyData, poseStack, submitNodeCollector,
                lightCoords, state.outlineColor);
        poseStack.popPose();
        callbackInfo.cancel();
    }
}
