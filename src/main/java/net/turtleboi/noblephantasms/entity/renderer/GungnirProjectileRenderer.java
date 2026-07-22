package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;
import net.turtleboi.noblephantasms.entity.renderer.states.GungnirProjectileRenderState;

public class GungnirProjectileRenderer extends EntityRenderer<GungnirProjectile, GungnirProjectileRenderState> {
    private static final float MODEL_CHOKE_OFFSET = 1.0F;
    private final ItemModelResolver itemModelResolver;

    public GungnirProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void submit(GungnirProjectileRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRotation - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.xRotation + 90.0F));
        poseStack.translate(0.0F, MODEL_CHOKE_OFFSET, 0.0F);
        renderState.item.submit(
                poseStack,
                submitNodeCollector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor);
        poseStack.popPose();
        super.submit(renderState, poseStack, submitNodeCollector, camera);
    }

    @Override
    public GungnirProjectileRenderState createRenderState() {
        return new GungnirProjectileRenderState();
    }

    @Override
    public void extractRenderState(GungnirProjectile entity, GungnirProjectileRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        renderState.xRotation = entity.getXRot(partialTick);
        renderState.yRotation = entity.getYRot(partialTick);
        itemModelResolver.updateForNonLiving(
                renderState.item,
                entity.getItem(),
                ItemDisplayContext.FIXED,
                entity);
    }
}
