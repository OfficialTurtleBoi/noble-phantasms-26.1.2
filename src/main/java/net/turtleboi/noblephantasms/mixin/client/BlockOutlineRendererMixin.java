package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class BlockOutlineRendererMixin {
    @ModifyArg(
            method = "renderBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer"
                            + "(Lnet/minecraft/client/renderer/rendertype/RenderType;)"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private RenderType preventBlockOutlineDepthWrite(RenderType renderType) {
        return renderType == RenderTypes.lines() ? RenderTypes.linesTranslucent() : renderType;
    }
}
