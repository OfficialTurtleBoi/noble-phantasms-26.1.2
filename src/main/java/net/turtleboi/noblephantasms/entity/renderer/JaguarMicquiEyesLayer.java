package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.turtleboi.noblephantasms.client.model.JaguarMicquiModel;
import net.turtleboi.noblephantasms.client.renderer.EntityTranslucencyRenderer;

public final class JaguarMicquiEyesLayer extends RenderLayer<JaguarMicquiRenderState,
        JaguarMicquiModel> {
    private final JaguarMicquiModel eyesModel;
    private final RenderType renderType;
    private final RenderType concealedRenderType;

    public JaguarMicquiEyesLayer(
            RenderLayerParent<JaguarMicquiRenderState, JaguarMicquiModel> parent,
            JaguarMicquiModel eyesModel, Identifier texture, RenderType renderType) {
        super(parent);
        this.eyesModel = eyesModel;
        this.renderType = renderType;
        this.concealedRenderType = EntityTranslucencyRenderer.eyeRenderType(texture);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       JaguarMicquiRenderState state, float yRot, float xRot) {
        RenderType activeRenderType = state.stealthProgress > 0.0F ? concealedRenderType : renderType;
        collector.order(2).submitModel(eyesModel, state, poseStack, activeRenderType,
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }
}
