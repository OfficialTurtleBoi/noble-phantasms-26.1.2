package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.model.XiuhcoatlModel;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.XiuhcoatlProjectile;
import net.turtleboi.noblephantasms.entity.renderer.states.XiuhcoatlProjectileRenderState;

public final class XiuhcoatlProjectileRenderer
        extends EntityRenderer<XiuhcoatlProjectile, XiuhcoatlProjectileRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/xiuhcoatl.png");

    private final XiuhcoatlModel model;

    public XiuhcoatlProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new XiuhcoatlModel(context.bakeLayer(XiuhcoatlModel.LAYER_LOCATION));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.XIUHCOATL.get(), XiuhcoatlProjectileRenderer::new);
    }

    @Override
    public XiuhcoatlProjectileRenderState createRenderState() {
        return new XiuhcoatlProjectileRenderState();
    }

    @Override
    public void extractRenderState(XiuhcoatlProjectile projectile,
                                   XiuhcoatlProjectileRenderState state, float partialTick) {
        super.extractRenderState(projectile, state, partialTick);
        state.xRotation = projectile.getDelayedXRot(0, partialTick);
        state.yRotation = projectile.getDelayedYRot(0, partialTick);
        state.body1XRotation = projectile.getDelayedXRot(1, partialTick);
        state.body1YRotation = projectile.getDelayedYRot(1, partialTick);
        state.body2XRotation = projectile.getDelayedXRot(2, partialTick);
        state.body2YRotation = projectile.getDelayedYRot(2, partialTick);
        state.tailXRotation = projectile.getDelayedXRot(3, partialTick);
        state.tailYRotation = projectile.getDelayedYRot(3, partialTick);
    }

    @Override
    public void submit(XiuhcoatlProjectileRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRotation - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRotation));
        model.setupAnim(state);
        collector.submitModel(model, state, poseStack, model.renderType(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    @Override
    protected int getBlockLightLevel(XiuhcoatlProjectile projectile, BlockPos position) {
        return 15;
    }
}
