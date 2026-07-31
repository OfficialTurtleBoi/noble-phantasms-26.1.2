package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    protected EntityModel<?> model;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$captureTrophyModel(LivingEntityRenderState state, PoseStack poseStack,
                                                   SubmitNodeCollector submitNodeCollector,
                                                   CameraRenderState camera, CallbackInfo callbackInfo) {
        if (TrophyHeadRenderer.captureSelectedModel(model)) {
            callbackInfo.cancel();
        }
    }
}
