package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import net.turtleboi.noblephantasms.client.renderer.HulioshjalmrRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void beginColoredGlint(MultiBufferSource.BufferSource bufferSource,
                                   OutlineBufferSource outlineBufferSource,
                                   SubmitNodeStorage.ItemSubmit submit, CallbackInfo callbackInfo) {
        HulioshjalmrRenderer.beginItemRendering(submit);
        ItemOutlineRenderer.render(bufferSource, submit);
        ColoredGlintRenderer.beginRender(submit);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void endColoredGlint(MultiBufferSource.BufferSource bufferSource,
                                 OutlineBufferSource outlineBufferSource,
                                 SubmitNodeStorage.ItemSubmit submit, CallbackInfo callbackInfo) {
        ColoredGlintRenderer.endRender();
        HulioshjalmrRenderer.endItemRendering();
    }

    @Inject(method = "hasTranslucency", at = @At("HEAD"), cancellable = true)
    private static void classifyConcealedHeldItemAsTranslucent(
            SubmitNodeStorage.ItemSubmit submit,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (HulioshjalmrRenderer.hasCapturedItemAlpha(submit)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Redirect(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;"
                            + "itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType useConcealedHeldItemTranslucency(BakedQuad.MaterialInfo materialInfo) {
        if (HulioshjalmrRenderer.getActiveItemAlpha() < 1.0F) {
            return HulioshjalmrRenderer.itemRenderType(materialInfo.sprite().atlasLocation());
        }
        return materialInfo.itemRenderType();
    }

    @ModifyArg(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"),
            index = 0)
    private int fadeConcealedHeldItem(int color) {
        return HulioshjalmrRenderer.applyAlpha(color, HulioshjalmrRenderer.getActiveItemAlpha());
    }

    @Inject(
            method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;",
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
