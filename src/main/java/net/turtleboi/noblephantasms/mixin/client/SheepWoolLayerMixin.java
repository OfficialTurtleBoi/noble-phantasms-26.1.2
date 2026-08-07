package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepWoolLayer.class)
public class SheepWoolLayerMixin {
    private static final Identifier SHEEP_WOOL = Identifier.withDefaultNamespace(
            "textures/entity/sheep/sheep_wool.png");
    private static final Identifier BABY_SHEEP_WOOL = Identifier.withDefaultNamespace(
            "textures/entity/sheep/sheep_wool_baby.png");

    @Shadow
    @Final
    private EntityModel<SheepRenderState> adultModel;

    @Shadow
    @Final
    private EntityModel<SheepRenderState> babyModel;

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
                    + "Lnet/minecraft/client/renderer/entity/state/SheepRenderState;FF)V",
            at = @At("HEAD"))
    private void noblePhantasms$includeWoolInLuminousOcclusion(
            PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
            SheepRenderState state, float yRot, float xRot, CallbackInfo callbackInfo) {
        if (state.isSheared) {
            return;
        }
        LuminousRenderer.submitOccluder(
                state,
                state.isBaby ? babyModel : adultModel,
                poseStack,
                state.isBaby ? BABY_SHEEP_WOOL : SHEEP_WOOL);
    }
}
