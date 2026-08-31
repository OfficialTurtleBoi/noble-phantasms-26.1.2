package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.client.renderer.AfterimageRenderer;
import net.turtleboi.noblephantasms.client.renderer.FrozenRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LuminousLivingEntityRendererMixin<S extends LivingEntityRenderState> {
    @Shadow
    @Final
    protected EntityModel<? super S> model;

    @Shadow
    public abstract Identifier getTextureLocation(S state);

    @Shadow
    protected abstract int getModelTint(S state);

    @Inject(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType"
                            + "(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private void submitLuminous(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                CameraRenderState cameraRenderState, CallbackInfo callbackInfo) {
        LuminousRenderer.submit(
                state,
                this.model,
                poseStack,
                this.getTextureLocation(state));
        AfterimageRenderer.submit(
                state,
                this.model,
                poseStack,
                this.getTextureLocation(state),
                this.getModelTint(state));
        FrozenRenderer.submit(state, this.model, poseStack, submitNodeCollector);
    }
}
