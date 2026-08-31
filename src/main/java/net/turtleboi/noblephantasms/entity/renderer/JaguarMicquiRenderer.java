package net.turtleboi.noblephantasms.entity.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.model.JaguarMicquiModel;
import net.turtleboi.noblephantasms.client.renderer.EntityTranslucencyRenderer;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.JaguarMicquiEntity;

public final class JaguarMicquiRenderer extends HumanoidMobRenderer<JaguarMicquiEntity,
        JaguarMicquiRenderState, JaguarMicquiModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/jaguar_micqui.png");
    private static final Identifier EYES_TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/jaguar_micqui_eyes.png");
    private static final RenderType EYES = RenderTypes.eyes(EYES_TEXTURE);

    public JaguarMicquiRenderer(EntityRendererProvider.Context context) {
        super(context, new JaguarMicquiModel(context.bakeLayer(JaguarMicquiModel.LAYER_LOCATION)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this,
                ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
        addLayer(new JaguarMicquiEyesLayer(this,
                new JaguarMicquiModel(context.bakeLayer(JaguarMicquiModel.EYES_LAYER_LOCATION)),
                EYES_TEXTURE, EYES));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JAGUAR_MICQUI.get(), JaguarMicquiRenderer::new);
    }

    @Override
    public JaguarMicquiRenderState createRenderState() {
        return new JaguarMicquiRenderState();
    }

    @Override
    public void extractRenderState(JaguarMicquiEntity entity, JaguarMicquiRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.stealthProgress = entity.getStealthProgress(partialTick);
        EntityTranslucencyRenderer.setTranslucencyState(state, state.stealthProgress, 0.1F);
    }

    @Override
    public Identifier getTextureLocation(JaguarMicquiRenderState state) {
        return TEXTURE;
    }

}
