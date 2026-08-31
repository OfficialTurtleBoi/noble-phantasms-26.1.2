package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.model.DraugrModel;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.DraugrEntity;

public final class DraugrRenderer extends HumanoidMobRenderer<DraugrEntity,
        DraugrRenderState, DraugrModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/draugr.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/draugr_eyes.png"));

    public DraugrRenderer(EntityRendererProvider.Context context) {
        super(context, new DraugrModel(context.bakeLayer(DraugrModel.LAYER_LOCATION)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRAUGR.get(), DraugrRenderer::new);
    }

    @Override
    public DraugrRenderState createRenderState() {
        return new DraugrRenderState();
    }

    @Override
    public void extractRenderState(DraugrEntity entity, DraugrRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.reviving = entity.isReviving();
        state.revivalTicks = entity.getRevivalTicks() - partialTick;
    }

    @Override
    protected void setupRotations(DraugrRenderState state, PoseStack poseStack,
                                  float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (!state.reviving) {
            return;
        }
        float remaining = Mth.clamp(state.revivalTicks, 0.0F, DraugrEntity.REVIVAL_DURATION);
        float elapsed = DraugrEntity.REVIVAL_DURATION - remaining;
        float collapse;
        if (elapsed < 10.0F) {
            collapse = smooth(elapsed / 10.0F);
        } else if (remaining < 20.0F) {
            collapse = smooth(remaining / 20.0F);
        } else {
            collapse = 1.0F;
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F * collapse));
    }

    private static float smooth(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    @Override
    public Identifier getTextureLocation(DraugrRenderState state) {
        return TEXTURE;
    }
}
