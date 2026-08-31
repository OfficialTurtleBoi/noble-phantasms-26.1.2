package net.turtleboi.noblephantasms.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.model.EcclesiasticModel;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.EcclesiasticEntity;

public final class EcclesiasticRenderer extends IllagerRenderer<EcclesiasticEntity, IllagerRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/ecclesiastic.png");

    public EcclesiasticRenderer(EntityRendererProvider.Context context) {
        super(context, new EcclesiasticModel(context.bakeLayer(EcclesiasticModel.LAYER_LOCATION)), 0.5F);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ECCLESIASTIC.get(), EcclesiasticRenderer::new);
    }

    @Override
    public IllagerRenderState createRenderState() {
        return new IllagerRenderState();
    }

    @Override
    public Identifier getTextureLocation(IllagerRenderState state) {
        return TEXTURE;
    }
}
