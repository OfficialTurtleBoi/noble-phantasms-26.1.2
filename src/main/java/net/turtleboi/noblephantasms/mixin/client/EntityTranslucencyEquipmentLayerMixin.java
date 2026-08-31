package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.turtleboi.noblephantasms.client.renderer.EntityTranslucencyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EquipmentLayerRenderer.class)
public abstract class EntityTranslucencyEquipmentLayerMixin {
    private static final String RENDER_LAYERS_METHOD = "renderLayers("
            + "Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
            + "Lnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/client/model/Model;"
            + "Ljava/lang/Object;"
            + "Lnet/minecraft/world/item/ItemStack;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "ILnet/minecraft/resources/Identifier;II)V";

    @Redirect(
            method = RENDER_LAYERS_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull"
                            + "(Lnet/minecraft/resources/Identifier;)"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType useActiveEntityArmorTranslucency(Identifier texture) {
        return EntityTranslucencyRenderer.getActiveProgress() > 0.0F
                ? EntityTranslucencyRenderer.armorRenderType(texture)
                : RenderTypes.armorCutoutNoCull(texture);
    }

    @Redirect(
            method = RENDER_LAYERS_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;"
                            + "getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;"
                            + "Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I"))
    private int fadeActiveEntityArmor(
            IClientItemExtensions extensions,
            ItemStack stack,
            EquipmentClientInfo.Layer layer,
            int layerIndex,
            int defaultDyeColor) {
        int color = extensions.getArmorLayerTintColor(stack, layer, layerIndex, defaultDyeColor);
        return EntityTranslucencyRenderer.getActiveProgress() > 0.0F
                ? EntityTranslucencyRenderer.applyAlpha(
                        color, EntityTranslucencyRenderer.getActiveAlpha())
                : color;
    }
}
