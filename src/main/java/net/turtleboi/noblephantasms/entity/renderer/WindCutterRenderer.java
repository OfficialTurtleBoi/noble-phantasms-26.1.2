package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.WindCutterProjectile;
import net.turtleboi.noblephantasms.entity.renderer.states.WindCutterRenderState;

public final class WindCutterRenderer extends EntityRenderer<WindCutterProjectile, WindCutterRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/wind_cutter_projectile.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE, false);
    private static final float HALF_THICKNESS = 1/32.0f;
    private final List<Edge> edges;

    public WindCutterRenderer(EntityRendererProvider.Context context) {
        super(context);
        edges = loadEdges(context.getResourceManager());
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WIND_CUTTER.get(), WindCutterRenderer::new);
    }

    @Override
    public void submit(WindCutterRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        float fade = renderState.life <= 0.9F ? 1.0F : 1.0F - Mth.clamp((renderState.life - 0.9F) / 0.1F, 0.0F, 1.0F);
        int alpha = Mth.clamp(Math.round(196.0F * fade), 0, 255);
        poseStack.pushPose();
        poseStack.translate(0.0F, renderState.boundingBoxHeight * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRotation - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xRotation));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.tilt));
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE,
                (pose, buffer) -> drawModel(
                        pose, buffer, renderState.boundingBoxWidth, renderState.boundingBoxHeight, renderState.lightCoords, alpha));
        poseStack.popPose();
        super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
    }

    private void drawModel(PoseStack.Pose pose, VertexConsumer buffer,
                           float width, float height, int light, int alpha) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        vertex(pose, buffer, -halfWidth, -halfHeight, HALF_THICKNESS,
                0.0F, 0.0F, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(pose, buffer, halfWidth, -halfHeight, HALF_THICKNESS,
                1.0F, 0.0F, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(pose, buffer, halfWidth, halfHeight, HALF_THICKNESS,
                1.0F, 1.0F, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(pose, buffer, -halfWidth, halfHeight, HALF_THICKNESS,
                0.0F, 1.0F, light, alpha, 0.0F, 0.0F, 1.0F);

        vertex(pose, buffer, -halfWidth, halfHeight, -HALF_THICKNESS,
                0.0F, 1.0F, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(pose, buffer, halfWidth, halfHeight, -HALF_THICKNESS,
                1.0F, 1.0F, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(pose, buffer, halfWidth, -halfHeight, -HALF_THICKNESS,
                1.0F, 0.0F, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(pose, buffer, -halfWidth, -halfHeight, -HALF_THICKNESS,
                0.0F, 0.0F, light, alpha, 0.0F, 0.0F, -1.0F);

        for (Edge edge : edges) {
            float x0 = -halfWidth + edge.x0() * width;
            float y0 = -halfHeight + edge.y0() * height;
            float x1 = -halfWidth + edge.x1() * width;
            float y1 = -halfHeight + edge.y1() * height;
            float dx = x1 - x0;
            float dy = y1 - y0;
            float length = Mth.sqrt(dx * dx + dy * dy);
            float nx = length > 0.0F ? -dy / length : 0.0F;
            float ny = length > 0.0F ? dx / length : 0.0F;
            vertex(pose, buffer, x0, y0, HALF_THICKNESS,
                    edge.u0(), edge.v0(), light, alpha, nx, ny, 0.0F);
            vertex(pose, buffer, x0, y0, -HALF_THICKNESS,
                    edge.u0(), edge.v0(), light, alpha, nx, ny, 0.0F);
            vertex(pose, buffer, x1, y1, -HALF_THICKNESS,
                    edge.u1(), edge.v1(), light, alpha, nx, ny, 0.0F);
            vertex(pose, buffer, x1, y1, HALF_THICKNESS,
                    edge.u1(), edge.v1(), light, alpha, nx, ny, 0.0F);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertexConsumer,
                               float x, float y, float z, float u, float v,
                               int light, int alpha, float normalX, float normalY, float normalZ) {
        vertexConsumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static List<Edge> loadEdges(ResourceManager resourceManager) {
        try (InputStream stream = resourceManager.open(TEXTURE);
             NativeImage image = NativeImage.read(stream)) {
            List<Edge> edges = new ArrayList<>();
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!isOpaque(image, x, y)) {
                        continue;
                    }
                    float x0 = x / (float) width;
                    float x1 = (x + 1) / (float) width;
                    float y0 = y / (float) height;
                    float y1 = (y + 1) / (float) height;
                    float centerU = (x + 0.5F) / width;
                    float centerV = (y + 0.5F) / height;
                    if (!isOpaque(image, x - 1, y)) {
                        edges.add(new Edge(x0, y0, x0, y1, centerU, y0, centerU, y1));
                    }
                    if (!isOpaque(image, x + 1, y)) {
                        edges.add(new Edge(x1, y1, x1, y0, centerU, y1, centerU, y0));
                    }
                    if (!isOpaque(image, x, y - 1)) {
                        edges.add(new Edge(x1, y0, x0, y0, x1, centerV, x0, centerV));
                    }
                    if (!isOpaque(image, x, y + 1)) {
                        edges.add(new Edge(x0, y1, x1, y1, x0, centerV, x1, centerV));
                    }
                }
            }
            return List.copyOf(edges);
        } catch (IOException exception) {
            NoblePhantasms.LOGGER.error("Unable to build the Kusanagi wind-cutter model", exception);
            return List.of();
        }
    }

    private static boolean isOpaque(NativeImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight() && ARGB.alpha(image.getPixel(x, y)) > 0;
    }

    @Override
    public WindCutterRenderState createRenderState() {
        return new WindCutterRenderState();
    }

    @Override
    public void extractRenderState(WindCutterProjectile projectileEntity, WindCutterRenderState renderState, float partialTick) {
        super.extractRenderState(projectileEntity, renderState, partialTick);
        renderState.xRotation = projectileEntity.getXRot(partialTick);
        renderState.yRotation = projectileEntity.getYRot(partialTick);
        renderState.tilt = Mth.randomBetween(RandomSource.create(projectileEntity.getUUID().getLeastSignificantBits()), -30.0F, 30.0F);
        renderState.life = Mth.clamp((projectileEntity.tickCount + partialTick) / projectileEntity.getLifespan(), 0.0F, 1.0F);
    }

    private record Edge(float x0, float y0, float x1, float y1,
                        float u0, float v0, float u1, float v1) {
    }
}
