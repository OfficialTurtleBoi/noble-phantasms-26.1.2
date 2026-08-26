package net.turtleboi.noblephantasms.client.renderer;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.AfterimageEffect;
import org.joml.Vector3f;

public final class AfterimageRenderer {
    private static final int MAX_SAMPLES = 7;
    private static final float SAMPLE_LIFETIME = 6.0F;
    private static final double MIN_SAMPLE_DISTANCE_SQUARED = 0.04;
    private static final ContextKey<Integer> ENTITY_ID_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "afterimage_entity_id"));
    private static final ContextKey<Vec3> POSITION_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "afterimage_position"));
    private static final Map<Integer, Trail> TRAILS = new HashMap<>();
    private static final List<AfterimageSubmit<?>> SUBMITS = new ArrayList<>();
    private static int frame;

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                AfterimageRenderer::extractState);
    }

    public static void beginFrame() {
        frame++;
        SUBMITS.clear();
        TRAILS.entrySet().removeIf(entry -> frame - entry.getValue().lastSeenFrame > 40);
    }

    public static <S extends LivingEntityRenderState> void submit(
            S state, EntityModel<? super S> model, PoseStack poseStack,
            Identifier texture, int modelTint) {
        Integer entityId = state.getRenderData(ENTITY_ID_KEY);
        Vec3 currentPosition = state.getRenderData(POSITION_KEY);
        Trail trail = entityId == null ? null : TRAILS.get(entityId);
        if (trail == null || currentPosition == null
                || state.isInvisible && state.isInvisibleToPlayer) {
            return;
        }
        List<Ghost> ghosts = trail.createGhosts(currentPosition, state.ageInTicks, modelTint);
        if (ghosts.isEmpty()) {
            return;
        }
        PoseStack.Pose pose = poseStack.last().copy();
        float distanceSquared = pose.pose().transformPosition(new Vector3f()).lengthSquared();
        SUBMITS.add(new AfterimageSubmit<>(model, state, pose, texture, ghosts, distanceSquared));
    }

    public static void render(RenderLevelStageEvent.AfterLevel event) {
        if (SUBMITS.isEmpty()) {
            return;
        }
        var modelViewStack = RenderSystem.getModelViewStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(event.getModelViewMatrix());
        try {
            SUBMITS.sort(Comparator.comparingDouble(
                    (AfterimageSubmit<?> submit) -> submit.distanceSquared()).reversed());
            for (AfterimageSubmit<?> submit : SUBMITS) {
                submit.draw(poseStack, bufferSource);
            }
            bufferSource.endLastBatch();
        } finally {
            modelViewStack.popMatrix();
            SUBMITS.clear();
        }
    }

    private static void extractState(LivingEntity entity, LivingEntityRenderState state) {
        int entityId = entity.getId();
        Vec3 position = entity.getPosition(state.partialTick);
        state.setRenderData(ENTITY_ID_KEY, entityId);
        state.setRenderData(POSITION_KEY, position);
        Trail trail = TRAILS.computeIfAbsent(entityId, ignored -> new Trail());
        trail.lastSeenFrame = frame;
        trail.prune(state.ageInTicks);
        if (AfterimageEffect.isActive(entity)) {
            trail.sample(position, state.ageInTicks);
        }
    }

    private static final class Trail {
        private final Deque<Sample> samples = new ArrayDeque<>();
        private int lastSeenFrame;

        private void sample(Vec3 position, float age) {
            Sample latest = samples.peekLast();
            if (latest != null && latest.position.distanceToSqr(position) < MIN_SAMPLE_DISTANCE_SQUARED) {
                return;
            }
            samples.addLast(new Sample(position, age));
            while (samples.size() > MAX_SAMPLES) {
                samples.removeFirst();
            }
        }

        private void prune(float age) {
            while (!samples.isEmpty() && age - samples.peekFirst().age > SAMPLE_LIFETIME) {
                samples.removeFirst();
            }
        }

        private List<Ghost> createGhosts(Vec3 currentPosition, float age, int modelTint) {
            List<Ghost> ghosts = new ArrayList<>();
            int sampleCount = samples.size();
            int index = 0;
            for (Sample sample : samples) {
                double distanceSquared = sample.position.distanceToSqr(currentPosition);
                if (distanceSquared >= MIN_SAMPLE_DISTANCE_SQUARED) {
                    float lifetimeFade = Math.max(0.0F, 1.0F - (age - sample.age) / SAMPLE_LIFETIME);
                    float sequenceFade = (index + 1.0F) / Math.max(sampleCount, 1);
                    int baseAlpha = ARGB.alpha(modelTint);
                    int alpha = Math.clamp(Math.round(baseAlpha * 0.55F * lifetimeFade * sequenceFade), 0, 255);
                    if (alpha > 0) {
                        ghosts.add(new Ghost(sample.position.subtract(currentPosition),
                                ARGB.color(alpha, modelTint & 0xFFFFFF)));
                    }
                }
                index++;
            }
            return ghosts;
        }
    }

    private record Sample(Vec3 position, float age) {
    }

    private record Ghost(Vec3 offset, int color) {
    }

    private record AfterimageSubmit<S extends LivingEntityRenderState>(
            EntityModel<? super S> model, S state, PoseStack.Pose pose,
            Identifier texture, List<Ghost> ghosts, float distanceSquared) {
        private void draw(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.entityTranslucent(texture));
            model.setupAnim(state);
            for (Ghost ghost : ghosts) {
                poseStack.pushPose();
                try {
                    poseStack.last().set(pose);
                    poseStack.last().pose().translateLocal(
                            (float) ghost.offset.x, (float) ghost.offset.y, (float) ghost.offset.z);
                    model.renderToBuffer(poseStack, vertexConsumer, LightCoordsUtil.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, ghost.color);
                } finally {
                    poseStack.popPose();
                }
            }
        }
    }
}
