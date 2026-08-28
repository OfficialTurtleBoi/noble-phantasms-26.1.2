package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.turtleboi.noblephantasms.client.renderer.HulioshjalmrRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class EntityTranslucencyItemInHandLayerMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void hideFullyConcealedHeldItems(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                             int light, ArmedEntityRenderState state, float yRot, float xRot,
                                             CallbackInfo callbackInfo) {
        if (HulioshjalmrRenderer.getActiveProgress() > 0.0F
                && HulioshjalmrRenderer.getActiveAlpha() <= 0.0F) {
            callbackInfo.cancel();
        }
    }
}
