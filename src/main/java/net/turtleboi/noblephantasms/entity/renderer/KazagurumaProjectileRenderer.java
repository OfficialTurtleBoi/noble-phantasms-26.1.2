package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.custom.KazagurumaProjectile;
import net.turtleboi.noblephantasms.entity.renderer.states.KazagurumaProjectileRenderState;
import net.turtleboi.noblephantasms.item.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class KazagurumaProjectileRenderer
        extends EntityRenderer<KazagurumaProjectile, KazagurumaProjectileRenderState> {
    private static final Identifier CHAIN_TEXTURE =
            Identifier.withDefaultNamespace("textures/block/iron_chain.png");
    private static final RenderType CHAIN_RENDER_TYPE = RenderTypes.entityCutout(CHAIN_TEXTURE, false);
    private static final float CHAIN_PLANE_WIDTH = 3.0F / 16.0F;
    private static final float TEXTURE_REPEAT_LENGTH = 1.0F;
    private static final float CHAIN_ORIGIN_DISTANCE = 8.0F / 16.0F;
    private static final float FIRST_STRIP_START_U = 0.0F;
    private static final float FIRST_STRIP_END_U = 3.0F / 16.0F;
    private static final float SECOND_STRIP_START_U = 3.0F / 16.0F;
    private static final float SECOND_STRIP_END_U = 6.0F / 16.0F;
    private final ItemModelResolver itemModelResolver;

    public KazagurumaProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.KAZAGURUMA.get(), KazagurumaProjectileRenderer::new);
    }

    @Override
    public void submit(KazagurumaProjectileRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Vec3 chainVector = renderState.chainVector;
        if (chainVector.lengthSqr() > 1.0E-4) {
            submitNodeCollector.submitCustomGeometry(poseStack, CHAIN_RENDER_TYPE,
                    (pose, buffer) -> drawChain(
                            pose, buffer, renderState.chainOrigin, chainVector, renderState.lightCoords));
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - renderState.xRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        renderState.item.submit(poseStack, submitNodeCollector, renderState.lightCoords,
                OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
        super.submit(renderState, poseStack, submitNodeCollector, camera);
    }

    private static void drawChain(PoseStack.Pose pose, VertexConsumer buffer,
                                  Vec3 origin, Vec3 destination, int light) {
        Vec3 vector = destination.subtract(origin);
        double length = vector.length();
        Vec3 direction = vector.scale(1.0 / length);
        Vec3 reference = Math.abs(direction.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 horizontal = direction.cross(reference).normalize();
        Vec3 vertical = direction.cross(horizontal).normalize();
        Vec3 firstSide = horizontal.add(vertical).normalize().scale(CHAIN_PLANE_WIDTH * 0.5F);
        Vec3 secondSide = horizontal.subtract(vertical).normalize().scale(CHAIN_PLANE_WIDTH * 0.5F);
        int segments = Math.max(1, Mth.ceil(length / TEXTURE_REPEAT_LENGTH));

        for (int segment = 0; segment < segments; segment++) {
            double startDistance = segment * TEXTURE_REPEAT_LENGTH;
            double endDistance = Math.min(length, (segment + 1) * TEXTURE_REPEAT_LENGTH);
            Vec3 start = origin.add(direction.scale(startDistance));
            Vec3 end = origin.add(direction.scale(endDistance));
            float endV = (float) ((endDistance - startDistance) / TEXTURE_REPEAT_LENGTH);
            drawTexturedPlane(pose, buffer, start, end, firstSide,
                    FIRST_STRIP_START_U, FIRST_STRIP_END_U, endV, light);
            drawTexturedPlane(pose, buffer, start, end, secondSide,
                    SECOND_STRIP_START_U, SECOND_STRIP_END_U, endV, light);
        }
    }

    private static void drawTexturedPlane(PoseStack.Pose pose, VertexConsumer buffer,
                                          Vec3 start, Vec3 end, Vec3 side,
                                          float startU, float endU, float endV, int light) {
        Vec3 normal = end.subtract(start).normalize().cross(side.normalize());
        vertex(pose, buffer, start.subtract(side), startU, 0.0F, normal, light);
        vertex(pose, buffer, end.subtract(side), startU, endV, normal, light);
        vertex(pose, buffer, end.add(side), endU, endV, normal, light);
        vertex(pose, buffer, start.add(side), endU, 0.0F, normal, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 position,
                               float u, float v, Vec3 normal, int light) {
        buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public KazagurumaProjectileRenderState createRenderState() {
        return new KazagurumaProjectileRenderState();
    }

    @Override
    public void extractRenderState(KazagurumaProjectile projectile, KazagurumaProjectileRenderState renderState,
                                   float partialTick) {
        super.extractRenderState(projectile, renderState, partialTick);
        renderState.xRotation = projectile.getLaunchXRotation();
        renderState.yRotation = projectile.getLaunchYRotation();
        renderState.chainOrigin = getChainOrigin(renderState.xRotation, renderState.yRotation);
        renderState.chainVector = getHandPosition(projectile, partialTick)
                .subtract(projectile.getPosition(partialTick));
        itemModelResolver.updateForNonLiving(renderState.item, new ItemStack(ModItems.KAZAGURUMA.get()),
                ItemDisplayContext.FIXED, projectile);
    }

    private static Vec3 getChainOrigin(float xRotation, float yRotation) {
        float diagonal = CHAIN_ORIGIN_DISTANCE / Mth.sqrt(2.0F);
        Vector3f offset = new Vector3f(diagonal, -diagonal, 0.0F);
        new Quaternionf()
                .rotationY(yRotation * Mth.DEG_TO_RAD)
                .rotateX((90.0F - xRotation) * Mth.DEG_TO_RAD)
                .rotateZ(-45.0F * Mth.DEG_TO_RAD)
                .transform(offset);
        return new Vec3(offset.x, offset.y, offset.z);
    }

    private static Vec3 getHandPosition(KazagurumaProjectile projectile, float partialTick) {
        Entity owner = projectile.getOwner();
        if (owner == null) {
            return projectile.getPosition(partialTick);
        }

        Vec3 holdPosition = owner.getRopeHoldPosition(partialTick);
        if (!isHeldInOffhand(projectile, owner)) {
            return holdPosition;
        }

        Vec3 ownerPosition = owner.getPosition(partialTick);
        return new Vec3(ownerPosition.x * 2.0 - holdPosition.x, holdPosition.y,
                ownerPosition.z * 2.0 - holdPosition.z);
    }

    private static boolean isHeldInOffhand(KazagurumaProjectile projectile, Entity owner) {
        if (owner instanceof LivingEntity livingEntity) {
            UUID offhandDeployment = livingEntity.getOffhandItem().get(ModDataComponents.KAZAGURUMA_DEPLOYMENT.get());
            if (projectile.getUUID().equals(offhandDeployment)) {
                return true;
            }
            UUID mainhandDeployment = livingEntity.getMainHandItem().get(ModDataComponents.KAZAGURUMA_DEPLOYMENT.get());
            if (projectile.getUUID().equals(mainhandDeployment)) {
                return false;
            }
        }
        return projectile.wasThrownFromOffhand();
    }
}
