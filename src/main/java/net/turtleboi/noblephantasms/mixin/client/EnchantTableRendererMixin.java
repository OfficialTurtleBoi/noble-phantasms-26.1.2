package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.client.renderer.MedjuNetjerBookRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantTableRenderer.class)
public abstract class EnchantTableRendererMixin {
    @Unique
    private static final SpriteId noblePhantasms$bookOfThothTexture =
            Sheets.BLOCK_ENTITIES_MAPPER.apply(Identifier.fromNamespaceAndPath(
                    NoblePhantasms.MOD_ID, "book_of_thoth"));
    @Unique
    private boolean noblePhantasms$renderBookOfThoth;

    @Inject(
            method = "extractRenderState("
                    + "Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;"
                    + "Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;F"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void noblePhantasms$extractMedjuNetjerState(
            EnchantingTableBlockEntity table, EnchantTableRenderState state,
            float partialTick, Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        ((MedjuNetjerBookRenderState)state).noblePhantasms$setMedjuNetjer(
                table.getData(ModAttachments.MEDJU_NETJER_INSTALLED.get()));
    }

    @Inject(
            method = "submit("
                    + "Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void noblePhantasms$captureBookTexture(
            EnchantTableRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        noblePhantasms$renderBookOfThoth =
                ((MedjuNetjerBookRenderState)state).noblePhantasms$hasMedjuNetjer();
    }

    @ModifyArg(
            method = "submit("
                    + "Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel("
                            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;"
                            + "Lnet/minecraft/client/resources/model/sprite/SpriteGetter;I"
                            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6)
    private SpriteId noblePhantasms$useBookOfThothTexture(SpriteId original) {
        return noblePhantasms$renderBookOfThoth
                ? noblePhantasms$bookOfThothTexture
                : original;
    }
}
