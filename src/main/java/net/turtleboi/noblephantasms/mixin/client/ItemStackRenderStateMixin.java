package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.turtleboi.noblephantasms.client.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin {
    @Shadow
    ItemDisplayContext displayContext;

    @Inject(method = "submit", at = @At("HEAD"))
    private void noblePhantasms$beginPoseEditorTransform(PoseStack poseStack,
                                                         SubmitNodeCollector submitNodeCollector,
                                                         int lightCoords, int overlayCoords, int outlineColor,
                                                         CallbackInfo callbackInfo) {
        ItemPoseEditor.beginModelTransform((ItemStackRenderState) (Object) this, displayContext);
        RhongomyniadSpinState.beginModelTransform(displayContext);
        ColoredGlintRenderer.beginSubmit((ItemStackRenderState) (Object) this);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void noblePhantasms$endPoseEditorTransform(PoseStack poseStack,
                                                       SubmitNodeCollector submitNodeCollector,
                                                       int lightCoords, int overlayCoords, int outlineColor,
                                                       CallbackInfo callbackInfo) {
        ItemPoseEditor.endModelTransform();
        RhongomyniadSpinState.endModelTransform();
        ColoredGlintRenderer.endSubmit();
    }
}
