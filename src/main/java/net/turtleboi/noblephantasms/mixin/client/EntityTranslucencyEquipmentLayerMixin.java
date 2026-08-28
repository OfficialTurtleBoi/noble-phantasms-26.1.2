package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.turtleboi.noblephantasms.client.renderer.HulioshjalmrRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EquipmentLayerRenderer.class)
public abstract class EntityTranslucencyEquipmentLayerMixin {
    @Redirect(
            method = "renderLayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull"
                            + "(Lnet/minecraft/resources/Identifier;)"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType useActiveEntityArmorTranslucency(Identifier texture) {
        return HulioshjalmrRenderer.getActiveProgress() > 0.0F
                ? RenderTypes.armorTranslucent(texture)
                : RenderTypes.armorCutoutNoCull(texture);
    }

    @ModifyArg(
            method = "renderLayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel"
                            + "(Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;III"
                            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;I"
                            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6)
    private int fadeActiveEntityArmor(int color) {
        float progress = HulioshjalmrRenderer.getActiveProgress();
        return progress > 0.0F ? HulioshjalmrRenderer.applyFade(color, progress) : color;
    }
}
