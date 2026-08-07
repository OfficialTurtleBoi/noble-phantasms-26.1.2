package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SheepWoolUndercoatLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepWoolUndercoatLayer.class)
public class SheepWoolUndercoatLayerMixin {
    private static final Identifier SHEEP_WOOL_UNDERCOAT = Identifier.withDefaultNamespace(
            "textures/entity/sheep/sheep_wool_undercoat.png");

    @Shadow
    @Final
    private EntityModel<SheepRenderState> model;

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
                    + "Lnet/minecraft/client/renderer/entity/state/SheepRenderState;FF)V",
            at = @At("HEAD"))
    private void noblePhantasms$includeUndercoatInLuminousOcclusion(
            PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
            SheepRenderState state, float yRot, float xRot, CallbackInfo callbackInfo) {
        if (state.isInvisible || state.isBaby
                || !state.isJebSheep && state.woolColor == DyeColor.WHITE) {
            return;
        }
        LuminousRenderer.submitOccluder(state, model, poseStack, SHEEP_WOOL_UNDERCOAT);
    }
}
