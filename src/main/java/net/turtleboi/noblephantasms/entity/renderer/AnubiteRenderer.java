package net.turtleboi.noblephantasms.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.model.AnubiteModel;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.AnubiteEntity;

public final class AnubiteRenderer extends HumanoidMobRenderer<AnubiteEntity,
        HumanoidRenderState, AnubiteModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/anubite.png");

    public AnubiteRenderer(EntityRendererProvider.Context context) {
        super(context, new AnubiteModel(context.bakeLayer(AnubiteModel.LAYER_LOCATION)), 0.5F);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ANUBITE.get(), AnubiteRenderer::new);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
