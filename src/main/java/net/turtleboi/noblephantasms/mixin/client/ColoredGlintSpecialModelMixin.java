package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.class)
public class ColoredGlintSpecialModelMixin {
    @Unique
    private static boolean noblePhantasms$submittingColoredGlint;

    @Inject(method = "submitModel", at = @At("HEAD"), cancellable = true)
    private void handleColoredModelGlint(Model<?> model, Object state, PoseStack poseStack,
                                         RenderType renderType, int lightCoords, int overlayCoords,
                                         int tintedColor, TextureAtlasSprite sprite, int outlineColor,
                                         ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                         CallbackInfo callbackInfo) {
        if (noblePhantasms$submittingColoredGlint || !ColoredGlintRenderer.hasSubmittingStyle()) {
            return;
        }
        if (ColoredGlintRenderer.isVanillaGlint(renderType)) {
            callbackInfo.cancel();
            return;
        }
        noblePhantasms$submittingColoredGlint = true;
        try {
            ColoredGlintRenderer.submitSpecialModel(
                    (SubmitNodeCollector) (Object) this, model, state, poseStack, lightCoords);
        } finally {
            noblePhantasms$submittingColoredGlint = false;
        }
    }

    @Inject(method = "submitModelPart", at = @At("HEAD"))
    private void handleColoredPartGlint(ModelPart modelPart, PoseStack poseStack,
                                        RenderType renderType, int lightCoords, int overlayCoords,
                                        TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil,
                                        int tintedColor,
                                        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                        int outlineColor, CallbackInfo callbackInfo) {
        if (noblePhantasms$submittingColoredGlint || !ColoredGlintRenderer.hasSubmittingStyle()) {
            return;
        }
        noblePhantasms$submittingColoredGlint = true;
        try {
            ColoredGlintRenderer.submitSpecialPart(
                    (SubmitNodeCollector) (Object) this, modelPart, poseStack, lightCoords);
        } finally {
            noblePhantasms$submittingColoredGlint = false;
        }
    }

    @ModifyVariable(method = "submitModelPart", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean suppressVanillaPartGlint(boolean hasFoil) {
        return hasFoil && !ColoredGlintRenderer.hasSubmittingStyle();
    }
}
