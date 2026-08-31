package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import net.turtleboi.noblephantasms.entity.renderer.states.PridwenBarrierRenderState;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.client.PridwenProjectionAnchor;
import net.turtleboi.noblephantasms.client.renderer.EnergyProjectionRenderer;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PridwenBarrierRenderer
        extends EntityRenderer<PridwenBarrierEntity, PridwenBarrierRenderState> {
    private static final float MODEL_CENTER_Y = 3.0F / 16.0F;
    private static final float MODEL_CENTER_Z = -2.5F / 16.0F;
    private final ItemModelResolver itemModelResolver;
    private @Nullable ItemStack displayItem;

    public PridwenBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRIDWEN_BARRIER.get(), PridwenBarrierRenderer::new);
    }

    @Override
    protected boolean affectedByCulling(PridwenBarrierEntity entity) {
        return false;
    }

    @Override
    public void submit(PridwenBarrierRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float scale = state.modelScale;
        poseStack.pushPose();
        if (state.anchorPose != null) {
            poseStack.translate(state.targetOffsetX, state.targetOffsetY, state.targetOffsetZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRotation));
            poseStack.translate(0.0F, -MODEL_CENTER_Y * PridwenBarrierEntity.MODEL_SCALE,
                    -MODEL_CENTER_Z * PridwenBarrierEntity.MODEL_SCALE);
            poseStack.scale(PridwenBarrierEntity.MODEL_SCALE,
                    PridwenBarrierEntity.MODEL_SCALE, PridwenBarrierEntity.MODEL_SCALE);
            Matrix4f targetPose = new Matrix4f(poseStack.last().pose());
            Matrix4f anchorPose = new Matrix4f(state.anchorPose).translate(0.5F, 0.5F, 0.5F);
            Matrix4f blendedPose = blendTransforms(
                    anchorPose, targetPose, state.projectionProgress);
            poseStack.last().pose().set(blendedPose);
            poseStack.last().normal().set(blendedPose).invert().transpose();
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRotation));
            poseStack.translate(0.0F, -MODEL_CENTER_Y * scale, -MODEL_CENTER_Z * scale);
            poseStack.scale(scale, scale, scale);
        }
        state.item.submit(poseStack, EnergyProjectionRenderer.deferredCollector(
                        submitNodeCollector, state.opacityMultiplier),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static Matrix4f blendTransforms(Matrix4f start, Matrix4f end, float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        Vector3f modelCenter = new Vector3f(0.0F, MODEL_CENTER_Y, MODEL_CENTER_Z);
        Vector3f startCenter = start.transformPosition(modelCenter, new Vector3f());
        Vector3f endCenter = end.transformPosition(modelCenter, new Vector3f());
        Vector3f center = startCenter.lerp(endCenter, clamped, new Vector3f());
        Vector3f startScale = start.getScale(new Vector3f());
        Vector3f endScale = end.getScale(new Vector3f());
        Vector3f scale = startScale.lerp(endScale, clamped, new Vector3f());
        Matrix4f result = new Matrix4f(end).scale(
                scale.x / endScale.x,
                scale.y / endScale.y,
                scale.z / endScale.z
        );
        Vector3f centerOffset = result.transformDirection(modelCenter, new Vector3f());
        return result.setTranslation(center.sub(centerOffset));
    }

    @Override
    public PridwenBarrierRenderState createRenderState() {
        return new PridwenBarrierRenderState();
    }

    @Override
    public void extractRenderState(PridwenBarrierEntity entity,
                                   PridwenBarrierRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.xRotation = entity.getXRot(partialTick);
        state.yRotation = entity.getYRot(partialTick);
        state.modelScale = entity.getVisualScale(partialTick);
        state.projectionProgress = entity.getProjectionProgress(partialTick);
        state.opacityMultiplier = entity.getOpacityMultiplier(partialTick);
        net.minecraft.world.phys.Vec3 projectedPosition = entity.getProjectedPosition(partialTick);
        state.targetOffsetX = projectedPosition.x - state.x;
        state.targetOffsetY = projectedPosition.y - state.y;
        state.targetOffsetZ = projectedPosition.z - state.z;
        net.minecraft.world.entity.player.Player owner = entity.getOwnerEntity();
        state.anchorPose = owner == null ? null : PridwenProjectionAnchor.get(owner);
        if (displayItem == null) {
            displayItem = new ItemStack(ModItems.PRIDWEN.get());
            displayItem.set(DataComponents.ITEM_MODEL,
                    Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pridwen_barrier"));
        }
        itemModelResolver.updateForNonLiving(state.item, displayItem, ItemDisplayContext.FIXED, entity);
    }
}
