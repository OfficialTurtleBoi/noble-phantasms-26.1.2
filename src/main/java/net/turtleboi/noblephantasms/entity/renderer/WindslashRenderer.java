package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.WindslashProjectile;
import net.turtleboi.noblephantasms.entity.renderer.states.WindslashRenderState;

public final class WindslashRenderer extends EntityRenderer<WindslashProjectile, WindslashRenderState> {
    private static final int MAX_AFTERIMAGES = 4;
    private static final float AFTERIMAGE_LIFETIME = 4.0F;
    private static final float AFTERIMAGE_SAMPLE_INTERVAL = 0.75F;
    private static final double MIN_AFTERIMAGE_DISTANCE_SQUARED = 0.01;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/entity/windslash_projectile.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE, false);
    private static final RenderType AFTERIMAGE_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE, false);
    private final List<Edge> edges;
    private final Map<Integer, Trail> trails = new HashMap<>();

    public WindslashRenderer(EntityRendererProvider.Context context) {
        super(context);
        edges = loadEdges(context.getResourceManager());
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WINDSLASH.get(), WindslashRenderer::new);
    }

    @Override
    public void submit(WindslashRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        Trail trail = trails.computeIfAbsent(renderState.entityId, ignored -> new Trail());
        trail.capture(renderState, gameTime);
        trails.entrySet().removeIf(entry -> gameTime - entry.getValue().lastSeenGameTime > 2L);
        for (Sample sample : trail.createAfterimages(renderState)) {
            float ageFade = 1.0F - (renderState.ageInTicks - sample.age) / AFTERIMAGE_LIFETIME;
            float lifeFade = 1.0F - WindslashProjectile.getFadeProgress(sample.life);
            int alpha = Mth.clamp(Math.round(112.0F * ageFade * sample.sequenceFade * lifeFade), 0, 255);
            submitModel(sample, renderState, poseStack, submitNodeCollector, AFTERIMAGE_RENDER_TYPE, alpha);
        }
        float fade = 1.0F - WindslashProjectile.getFadeProgress(renderState.life);
        int alpha = Mth.clamp(Math.round(196.0F * fade), 0, 255);
        submitModel(Sample.current(renderState), renderState, poseStack, submitNodeCollector, RENDER_TYPE, alpha);
        super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
    }

    private void submitModel(Sample sample, WindslashRenderState current, PoseStack poseStack,
                             SubmitNodeCollector submitNodeCollector, RenderType renderType, int alpha) {
        poseStack.pushPose();
        poseStack.translate(sample.x - current.x, sample.y - current.y, sample.z - current.z);
        poseStack.translate(0.0F, sample.visualHeight * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sample.yRotation - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(sample.xRotation));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(sample.tilt));
        submitNodeCollector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> drawModel(pose, buffer, sample.visualWidth, sample.visualHeight,
                        sample.visualThickness, sample.lightCoords, alpha));
        poseStack.popPose();
    }

    private void drawModel(PoseStack.Pose pose, VertexConsumer buffer, float width, float height, float thickness, int light, int alpha) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        float halfThickness = thickness * 0.5F;
        vertex(pose, buffer, -halfWidth, -halfHeight, halfThickness,
                0.0F, 0.0F, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(pose, buffer, halfWidth, -halfHeight, halfThickness,
                1.0F, 0.0F, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(pose, buffer, halfWidth, halfHeight, halfThickness,
                1.0F, 1.0F, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(pose, buffer, -halfWidth, halfHeight, halfThickness,
                0.0F, 1.0F, light, alpha, 0.0F, 0.0F, 1.0F);

        vertex(pose, buffer, -halfWidth, halfHeight, -halfThickness,
                0.0F, 1.0F, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(pose, buffer, halfWidth, halfHeight, -halfThickness,
                1.0F, 1.0F, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(pose, buffer, halfWidth, -halfHeight, -halfThickness,
                1.0F, 0.0F, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(pose, buffer, -halfWidth, -halfHeight, -halfThickness,
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
            vertex(pose, buffer, x0, y0, halfThickness,
                    edge.u0(), edge.v0(), light, alpha, nx, ny, 0.0F);
            vertex(pose, buffer, x0, y0, -halfThickness,
                    edge.u0(), edge.v0(), light, alpha, nx, ny, 0.0F);
            vertex(pose, buffer, x1, y1, -halfThickness,
                    edge.u1(), edge.v1(), light, alpha, nx, ny, 0.0F);
            vertex(pose, buffer, x1, y1, halfThickness,
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
            NoblePhantasms.LOGGER.error("Unable to build the Kusanagi windslash model", exception);
            return List.of();
        }
    }

    private static boolean isOpaque(NativeImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight() && ARGB.alpha(image.getPixel(x, y)) > 0;
    }

    @Override
    public WindslashRenderState createRenderState() {
        return new WindslashRenderState();
    }

    @Override
    public void extractRenderState(WindslashProjectile projectileEntity, WindslashRenderState renderState, float partialTick) {
        super.extractRenderState(projectileEntity, renderState, partialTick);
        renderState.entityId = projectileEntity.getId();
        renderState.xRotation = projectileEntity.getXRot(partialTick);
        renderState.yRotation = projectileEntity.getYRot(partialTick);
        renderState.tilt = projectileEntity.getTilt();
        renderState.life = projectileEntity.getLifeProgress(partialTick);
        float growthScale = projectileEntity.getGrowthScale(partialTick);
        renderState.visualWidth = WindslashProjectile.getVisualWidth(growthScale);
        renderState.visualHeight = WindslashProjectile.getVisualHeight(growthScale);
        renderState.visualThickness = WindslashProjectile.getVisualThickness(growthScale);
    }

    private static final class Trail {
        private final Deque<Sample> samples = new ArrayDeque<>();
        private long lastSeenGameTime;

        private void capture(WindslashRenderState state, long gameTime) {
            Sample latest = samples.peekLast();
            if (latest != null && state.ageInTicks < latest.age) {
                samples.clear();
                latest = null;
            }
            if (latest == null || state.ageInTicks - latest.age >= AFTERIMAGE_SAMPLE_INTERVAL) {
                samples.addLast(Sample.current(state));
                while (samples.size() > MAX_AFTERIMAGES) {
                    samples.removeFirst();
                }
            }
            while (!samples.isEmpty() && state.ageInTicks - samples.peekFirst().age > AFTERIMAGE_LIFETIME) {
                samples.removeFirst();
            }
            lastSeenGameTime = gameTime;
        }

        private List<Sample> createAfterimages(WindslashRenderState current) {
            List<Sample> afterimages = new ArrayList<>();
            Vec3 currentPosition = new Vec3(current.x, current.y, current.z);
            int sampleCount = samples.size();
            int index = 0;
            for (Sample sample : samples) {
                float age = current.ageInTicks - sample.age;
                if (age > 0.0F && age <= AFTERIMAGE_LIFETIME
                        && sample.position().distanceToSqr(currentPosition) >= MIN_AFTERIMAGE_DISTANCE_SQUARED) {
                    afterimages.add(sample.withSequenceFade((index + 1.0F) / Math.max(sampleCount, 1)));
                }
                index++;
            }
            return afterimages;
        }
    }

    private record Sample(double x, double y, double z, float age, float life,
                          float xRotation, float yRotation, float tilt,
                          float visualWidth, float visualHeight, float visualThickness,
                          int lightCoords, float sequenceFade) {
        private static Sample current(WindslashRenderState state) {
            return new Sample(state.x, state.y, state.z, state.ageInTicks, state.life,
                    state.xRotation, state.yRotation, state.tilt,
                    state.visualWidth, state.visualHeight, state.visualThickness,
                    state.lightCoords, 1.0F);
        }

        private Sample withSequenceFade(float fade) {
            return new Sample(x, y, z, age, life, xRotation, yRotation, tilt,
                    visualWidth, visualHeight, visualThickness, lightCoords, fade);
        }

        private Vec3 position() {
            return new Vec3(x, y, z);
        }
    }

    private record Edge(float x0, float y0, float x1, float y1,
                        float u0, float v0, float u1, float v1) {
    }
}
