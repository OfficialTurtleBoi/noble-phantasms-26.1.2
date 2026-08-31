package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.turtleboi.noblephantasms.client.PridwenProjectionAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public class ItemStackRenderStateLayerMixin {
    @Inject(method = "applyTransform", at = @At("RETURN"))
    private void noblePhantasms$capturePridwenPlatePose(PoseStack.Pose pose,
                                                        CallbackInfo callbackInfo) {
        PridwenProjectionAnchor.capture(pose);
    }
}
