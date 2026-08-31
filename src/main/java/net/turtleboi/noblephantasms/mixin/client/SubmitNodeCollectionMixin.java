package net.turtleboi.noblephantasms.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import net.turtleboi.noblephantasms.client.renderer.EntityTranslucencyRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @Shadow
    @Final
    private List<SubmitNodeStorage.ItemSubmit> itemSubmits;

    @Inject(method = "submitItem", at = @At("RETURN"))
    private void captureColoredGlintSubmit(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                           ItemDisplayContext displayContext, int lightCoords,
                                           int overlayCoords, int outlineColor, int[] tintLayers,
                                           List<BakedQuad> quads, ItemStackRenderState.FoilType foilType,
                                           CallbackInfo callbackInfo) {
        SubmitNodeStorage.ItemSubmit submit = itemSubmits.getLast();
        EntityTranslucencyRenderer.captureItemSubmit(submit);
        ColoredGlintRenderer.capture(submit);
        ItemOutlineRenderer.capture(submit);
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void clearConcealedItemSubmits(CallbackInfo callbackInfo) {
        EntityTranslucencyRenderer.clearItemSubmits();
    }
}
