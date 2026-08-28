package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.turtleboi.noblephantasms.client.renderer.HulioshjalmrRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class EntityTranslucencyLivingEntityRendererMixin<S extends LivingEntityRenderState> {
    @Shadow
    public abstract Identifier getTextureLocation(S state);

    @Inject(method = "submit", at = @At("HEAD"))
    private void beginEntityTranslucency(S state, PoseStack poseStack,
                                         SubmitNodeCollector submitNodeCollector,
                                         CameraRenderState camera, CallbackInfo callbackInfo) {
        HulioshjalmrRenderer.beginRendering(state);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void endEntityTranslucency(S state, PoseStack poseStack,
                                       SubmitNodeCollector submitNodeCollector,
                                       CameraRenderState camera, CallbackInfo callbackInfo) {
        HulioshjalmrRenderer.endRendering();
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void useEntityTranslucency(S state, boolean bodyVisible,
                                       boolean translucent, boolean glowing,
                                       CallbackInfoReturnable<RenderType> callbackInfo) {
        if (HulioshjalmrRenderer.getProgress(state) > 0.0F) {
            callbackInfo.setReturnValue(RenderTypes.entityTranslucent(this.getTextureLocation(state)));
        }
    }

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void fadeEntityBody(S state, CallbackInfoReturnable<Integer> callbackInfo) {
        float progress = HulioshjalmrRenderer.getProgress(state);
        if (progress > 0.0F) {
            callbackInfo.setReturnValue(HulioshjalmrRenderer.applyFade(callbackInfo.getReturnValue(), progress));
        }
    }
}
