package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.model.OniModel;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.OniEntity;

public final class OniRenderer extends HumanoidMobRenderer<OniEntity, HumanoidRenderState, OniModel> {
    private static final float MODEL_SCALE = 1.25F;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/oni.png");

    public OniRenderer(EntityRendererProvider.Context context) {
        super(context, new OniModel(context.bakeLayer(OniModel.LAYER_LOCATION)), 0.8125F);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ONI.get(), OniRenderer::new);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    protected void scale(HumanoidRenderState state, PoseStack poseStack) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
