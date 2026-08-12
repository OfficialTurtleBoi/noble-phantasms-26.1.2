package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void beginColoredGlint(MultiBufferSource.BufferSource bufferSource,
                                   OutlineBufferSource outlineBufferSource,
                                   SubmitNodeStorage.ItemSubmit submit, CallbackInfo callbackInfo) {
        ColoredGlintRenderer.beginRender(submit);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void endColoredGlint(MultiBufferSource.BufferSource bufferSource,
                                 OutlineBufferSource outlineBufferSource,
                                 SubmitNodeStorage.ItemSubmit submit, CallbackInfo callbackInfo) {
        ColoredGlintRenderer.endRender();
    }

    @Inject(
            method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("HEAD"), cancellable = true)
    private static void replaceColoredGlintBuffer(MultiBufferSource bufferSource,
                                                  RenderType baseRenderType,
                                                  PoseStack.Pose foilDecalPose,
                                                  CallbackInfoReturnable<VertexConsumer> callbackInfo) {
        VertexConsumer coloredBuffer = ColoredGlintRenderer.getFoilBuffer(
                bufferSource, baseRenderType, foilDecalPose);
        if (coloredBuffer != null) {
            callbackInfo.setReturnValue(coloredBuffer);
        }
    }
}
