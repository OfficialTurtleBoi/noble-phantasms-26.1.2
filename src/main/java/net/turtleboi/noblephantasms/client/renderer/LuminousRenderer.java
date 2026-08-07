package net.turtleboi.noblephantasms.client.renderer;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.stencil.StencilOperation;
import net.neoforged.neoforge.client.stencil.StencilPerFaceTest;
import net.neoforged.neoforge.client.stencil.StencilTest;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.client.BertilakClientUtil;
import net.turtleboi.noblephantasms.client.EyeOfHorusClientState;
import org.joml.Vector3f;

public final class LuminousRenderer {
    private static final float OUTLINE_WIDTH = 0.05F;
    private static final int EYE_OF_HORUS_COLOR = 0xEFBF04;
    private static final int EYE_OF_HORUS_LIGHT_GOLD = 0xFFC766;
    private static final int EYE_OF_HORUS_YELLOW = 0xFFDD00;
    private static final int EYE_OF_HORUS_ORANGE = 0xF07200;
    public static final int BERTILAK_GREEN = 0x2C5F34;
    public static final int BERTILAK_BRIGHT_GREEN = 0x66DE78;
    public static final int BERTILAK_LIGHT_GREEN = 0x418C4C;
    public static final int BERTILAK_DARK_GREEN = 0x1B3B20;
    private static final float BERTILAK_COLOR_CYCLE_SPEED = 0.005F;
    private static final ContextKey<Integer> COLOR_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "luminous_color"));
    private static final ContextKey<Float> OUTLINE_WIDTH_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "luminous_outline_width"));
    private static final ContextKey<Integer> SECONDARY_COLOR_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "luminous_secondary_color"));
    private static final ContextKey<Float> SECONDARY_OUTLINE_WIDTH_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "luminous_secondary_outline_width"));
    private static final Identifier SHADER =
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "core/luminous");
    private static final ThreadLocal<Float> ACTIVE_OUTLINE_WIDTH = ThreadLocal.withInitial(() -> 0.0F);
    private static final List<GlowSubmit> OUTLINE_SUBMITS = new ArrayList<>();
    private static final Map<LivingEntityRenderState, LivingSubmit<?>> LIVING_SUBMITS = new IdentityHashMap<>();
    private static final StencilPerFaceTest MASK_STENCIL = new StencilPerFaceTest(
            StencilOperation.KEEP,
            StencilOperation.KEEP,
            StencilOperation.REPLACE,
            CompareOp.ALWAYS_PASS);
    private static final StencilPerFaceTest DISCARD_FRONT = new StencilPerFaceTest(
            StencilOperation.KEEP,
            StencilOperation.KEEP,
            StencilOperation.KEEP,
            CompareOp.NEVER_PASS);
    private static final StencilPerFaceTest OUTLINE_BACK = new StencilPerFaceTest(
            StencilOperation.KEEP,
            StencilOperation.KEEP,
            StencilOperation.KEEP,
            CompareOp.EQUAL);

    private static final RenderPipeline VISIBLE_SELF_MASK_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            NoblePhantasms.MOD_ID, "pipeline/luminous_visible_self_mask"))
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withSampler("Sampler0")
                    .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
                    .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
                    .withStencilTest(new StencilTest(MASK_STENCIL, 0x01, 0x01, 1))
                    .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
                    .build();

    private static final RenderPipeline SELF_MASK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/luminous_self_mask"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
            .withStencilTest(new StencilTest(MASK_STENCIL, 0x02, 0x02, 2))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();

    private static final RenderPipeline SELF_CLEAR_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/luminous_self_clear"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
            .withStencilTest(new StencilTest(MASK_STENCIL, 0x03, 0x03, 0))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();

    private static final RenderPipeline OCCLUDED_FILL_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            NoblePhantasms.MOD_ID, "pipeline/luminous_occluded_fill"))
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withSampler("Sampler0")
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withStencilTest(new StencilTest(OUTLINE_BACK, 0x01, 0xFF, 0))
                    .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
                    .build();

    private static final RenderPipeline OUTLINE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            NoblePhantasms.MOD_ID, "pipeline/luminous_outline"))
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withSampler("Sampler0")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withStencilTest(new StencilTest(DISCARD_FRONT, OUTLINE_BACK, 0x03, 0xFF, 0))
                    .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
                    .build();

    private static final Function<Identifier, RenderType> VISIBLE_SELF_MASK_TYPES = Util.memoize(texture ->
            createRenderType("luminous_visible_self_mask", VISIBLE_SELF_MASK_PIPELINE, texture));
    private static final Function<Identifier, RenderType> SELF_MASK_TYPES = Util.memoize(texture ->
            createRenderType("luminous_self_mask", SELF_MASK_PIPELINE, texture));
    private static final Function<Identifier, RenderType> SELF_CLEAR_TYPES = Util.memoize(texture ->
            createRenderType("luminous_self_clear", SELF_CLEAR_PIPELINE, texture));
    private static final Function<Identifier, RenderType> OCCLUDED_FILL_TYPES = Util.memoize(texture ->
            createRenderType("luminous_occluded_fill", OCCLUDED_FILL_PIPELINE, texture));
    private static final Function<Identifier, RenderType> OUTLINE_TYPES = Util.memoize(texture ->
            createRenderType("luminous_outline", OUTLINE_PIPELINE, texture));

    public static void enableStencil(ConfigureMainRenderTargetEvent event) {
        event.enableStencil();
    }

    public static void beginFrame() {
        OUTLINE_SUBMITS.clear();
        LIVING_SUBMITS.clear();
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(VISIBLE_SELF_MASK_PIPELINE);
        event.registerPipeline(SELF_MASK_PIPELINE);
        event.registerPipeline(SELF_CLEAR_PIPELINE);
        event.registerPipeline(OCCLUDED_FILL_PIPELINE);
        event.registerPipeline(OUTLINE_PIPELINE);
    }

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                LuminousRenderer::extractGlowState);
    }

    public static <S extends LivingEntityRenderState> void submit(S state, EntityModel<? super S> model, PoseStack poseStack,
                                                                  Identifier texture) {
        Integer color = state.getRenderData(COLOR_KEY);
        Float outlineWidth = state.getRenderData(OUTLINE_WIDTH_KEY);
        Integer secondaryColor = state.getRenderData(SECONDARY_COLOR_KEY);
        Float secondaryOutlineWidth = state.getRenderData(SECONDARY_OUTLINE_WIDTH_KEY);
        if (color == null || outlineWidth == null || ARGB.alpha(color) == 0
                || state.isInvisible && state.isInvisibleToPlayer) {
            return;
        }

        PoseStack.Pose pose = poseStack.last().copy();
        float distanceSquared = pose.pose().transformPosition(new Vector3f()).lengthSquared();
        LivingSubmit<S> submit = new LivingSubmit<>(
                new ArrayList<>(List.of(new LivingPart<>(model, state, pose, texture))),
                color,
                outlineWidth,
                secondaryColor == null ? 0 : secondaryColor,
                secondaryOutlineWidth == null ? 0.0F : secondaryOutlineWidth,
                distanceSquared);
        OUTLINE_SUBMITS.add(submit);
        LIVING_SUBMITS.put(state, submit);
    }

    @SuppressWarnings("unchecked")
    public static <S extends LivingEntityRenderState> void submitOccluder(
            S state, EntityModel<? super S> model, PoseStack poseStack, Identifier texture) {
        LivingSubmit<?> submit = LIVING_SUBMITS.get(state);
        if (submit != null) {
            ((LivingSubmit<S>) submit).parts().add(
                    new LivingPart<>(model, state, poseStack.last().copy(), texture));
        }
    }

    public static void submitJudgementGeometry(PoseStack poseStack, Identifier texture,
                                                float ageInTicks, float phase,
                                                LuminousGeometry geometry) {
        float flicker = Mth.sin(ageInTicks * 1.1F + phase) * 0.18F
                + Mth.sin(ageInTicks * 2.9F + phase * 1.7F) * 0.08F;
        int color = getJudgementColor(ageInTicks, phase);
        float secondaryWidthScale = Mth.clamp(1.9F + flicker * 1.4F, 1.45F, 2.35F);
        PoseStack.Pose pose = poseStack.last().copy();
        float distanceSquared = pose.pose().transformPosition(new Vector3f()).lengthSquared();
        OUTLINE_SUBMITS.add(new GeometrySubmit(
                pose,
                texture,
                ARGB.opaque(color),
                OUTLINE_WIDTH,
                ARGB.color(0.35F, color),
                OUTLINE_WIDTH * secondaryWidthScale,
                distanceSquared,
                geometry));
    }

    public static void renderOutlines(RenderLevelStageEvent.AfterLevel event) {
        if (OUTLINE_SUBMITS.isEmpty()) {
            return;
        }

        var modelViewStack = RenderSystem.getModelViewStack();
        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(event.getModelViewMatrix());
        try {
            clearStencil();
            OUTLINE_SUBMITS.sort(Comparator.comparingDouble(GlowSubmit::distanceSquared));
            for (int index = 0; index < OUTLINE_SUBMITS.size(); index++) {
                GlowSubmit submit = OUTLINE_SUBMITS.get(index);
                renderSubmit(submit, VISIBLE_SELF_MASK_TYPES, poseStack, bufferSource, -1, 0.0F);
                renderSubmit(
                        submit,
                        OCCLUDED_FILL_TYPES,
                        poseStack,
                        bufferSource,
                        submit.color(),
                        0.0F);
                renderSubmit(submit, SELF_MASK_TYPES, poseStack, bufferSource, -1, 0.0F);
                renderOutline(submit, poseStack, bufferSource);
                if (index < OUTLINE_SUBMITS.size() - 1) {
                    renderSubmit(submit, SELF_CLEAR_TYPES, poseStack, bufferSource, -1, 0.0F);
                }
            }
            bufferSource.endLastBatch();
        } finally {
            ACTIVE_OUTLINE_WIDTH.remove();
            modelViewStack.popMatrix();
            OUTLINE_SUBMITS.clear();
            LIVING_SUBMITS.clear();
        }
    }

    public static void expandCubeVertex(Vector3f position, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float outlineWidth = ACTIVE_OUTLINE_WIDTH.get();
        if (outlineWidth <= 0.0F) {
            return;
        }

        float centerX = (minX + maxX) / 32.0F;
        float centerY = (minY + maxY) / 32.0F;
        float centerZ = (minZ + maxZ) / 32.0F;
        position.add(
                directionFromCenter(position.x(), centerX) * outlineWidth,
                directionFromCenter(position.y(), centerY) * outlineWidth,
                directionFromCenter(position.z(), centerZ) * outlineWidth);
    }

    private static RenderType createRenderType(String name, RenderPipeline pipeline, Identifier texture) {
        return RenderType.create(
                NoblePhantasms.MOD_ID + "_" + name,
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", texture)
                        .createRenderSetup());
    }

    private static float directionFromCenter(float coordinate, float center) {
        return coordinate < center ? -1.0F : coordinate > center ? 1.0F : 0.0F;
    }

    private static void extractGlowState(LivingEntity entity, LivingEntityRenderState renderState) {
        EyeOfHorusClientState.GlowProgress eyeGlowProgress =
                EyeOfHorusClientState.getProgress(entity, renderState.partialTick);
        BertilakClientUtil.GlowProgress covenantGlowProgress =
                BertilakClientUtil.getGlowProgress(entity, renderState.partialTick);
        if (eyeGlowProgress.total() > 0.0F) {
            applyEyeOfHorusGlow(entity, renderState, eyeGlowProgress);
            return;
        }
        if (covenantGlowProgress.total() > 0.0F) {
            applyCovenantGlow(entity, renderState, covenantGlowProgress);
            return;
        }

        Integer luminousColor = entity.getExistingDataOrNull(ModAttachments.LUMINOUS_COLOR);
        if (luminousColor == null) {
            clearGlowState(renderState);
            return;
        }
        renderState.setRenderData(COLOR_KEY, ARGB.opaque(luminousColor));
        renderState.setRenderData(OUTLINE_WIDTH_KEY, OUTLINE_WIDTH);
        renderState.setRenderData(SECONDARY_COLOR_KEY, null);
        renderState.setRenderData(SECONDARY_OUTLINE_WIDTH_KEY, null);
    }

    private static void applyEyeOfHorusGlow(LivingEntity entity, LivingEntityRenderState renderState,
                                             EyeOfHorusClientState.GlowProgress glowProgress) {
        float progress = glowProgress.total();
        float judgementProgress = glowProgress.judgement();
        float activation = progress * progress * (3.0F - 2.0F * progress);
        float phase = entity.getId() * 0.7548777F;
        float flicker = Mth.sin(renderState.ageInTicks * 1.1F + phase) * 0.18F
                + Mth.sin(renderState.ageInTicks * 2.9F + phase * 1.7F) * 0.08F;
        float primaryWidthScale = activation;
        if (judgementProgress <= 0.0F) {
            primaryWidthScale *= Mth.clamp(1.0F + flicker, 0.65F, 1.35F);
        }
        int color = judgementProgress > 0.0F
                ? getJudgementColor(renderState.ageInTicks, phase)
                : EYE_OF_HORUS_COLOR;
        renderState.setRenderData(COLOR_KEY, ARGB.color(activation, color));
        renderState.setRenderData(OUTLINE_WIDTH_KEY, OUTLINE_WIDTH * primaryWidthScale);
        if (judgementProgress > 0.0F) {
            float judgementActivation = judgementProgress * judgementProgress * (3.0F - 2.0F * judgementProgress);
            float secondaryWidthScale = judgementActivation
                    * Mth.clamp(1.9F + flicker * 1.4F, 1.45F, 2.35F);
            renderState.setRenderData(
                    SECONDARY_COLOR_KEY, ARGB.color(judgementActivation * 0.35F, color));
            renderState.setRenderData(
                    SECONDARY_OUTLINE_WIDTH_KEY, OUTLINE_WIDTH * secondaryWidthScale);
        } else {
            renderState.setRenderData(SECONDARY_COLOR_KEY, null);
            renderState.setRenderData(SECONDARY_OUTLINE_WIDTH_KEY, null);
        }
    }

    private static void applyCovenantGlow(LivingEntity entity, LivingEntityRenderState renderState,
                                           BertilakClientUtil.GlowProgress glowProgress) {
        float progress = glowProgress.total();
        float activation = progress * progress * (3.0F - 2.0F * progress);
        float phase = entity.getId() * 0.7548777F;
        float pulse = Mth.sin(renderState.ageInTicks * 0.12F + phase);
        float primaryWidthScale = activation;
        if (glowProgress.readinessFlashing()) {
            primaryWidthScale *= 1.0F + glowProgress.readinessFlash() * 0.75F;
        } else if (glowProgress.ready()) {
            primaryWidthScale *= 1.0F + pulse * 0.08F;
        }
        int covenantColor = glowProgress.ready()
                ? getCovenantColor(renderState.ageInTicks, phase)
                : BERTILAK_GREEN;
        int color = ARGB.srgbLerp(glowProgress.readinessFlash(), covenantColor, 0xFFFFFF);
        renderState.setRenderData(COLOR_KEY, ARGB.color(activation, color));
        renderState.setRenderData(OUTLINE_WIDTH_KEY, OUTLINE_WIDTH * primaryWidthScale);
        renderState.setRenderData(SECONDARY_COLOR_KEY, null);
        renderState.setRenderData(SECONDARY_OUTLINE_WIDTH_KEY, null);
    }

    private static void clearGlowState(LivingEntityRenderState renderState) {
        renderState.setRenderData(COLOR_KEY, null);
        renderState.setRenderData(OUTLINE_WIDTH_KEY, null);
        renderState.setRenderData(SECONDARY_COLOR_KEY, null);
        renderState.setRenderData(SECONDARY_OUTLINE_WIDTH_KEY, null);
    }

    private static int getJudgementColor(float ageInTicks, float phase) {
        float colorCycle = Mth.frac(ageInTicks * 0.04F + phase) * 4.0F;
        if (colorCycle < 1.0F) {
            return ARGB.srgbLerp(colorCycle, EYE_OF_HORUS_COLOR, EYE_OF_HORUS_LIGHT_GOLD);
        }
        if (colorCycle < 2.0F) {
            return ARGB.srgbLerp(colorCycle - 1.0F, EYE_OF_HORUS_LIGHT_GOLD, EYE_OF_HORUS_YELLOW);
        }
        if (colorCycle < 3.0F) {
            return ARGB.srgbLerp(colorCycle - 2.0F, EYE_OF_HORUS_YELLOW, EYE_OF_HORUS_ORANGE);
        }
        return ARGB.srgbLerp(colorCycle - 3.0F, EYE_OF_HORUS_ORANGE, EYE_OF_HORUS_COLOR);
    }

    private static int getCovenantColor(float ageInTicks, float phase) {
        float colorCycle = Mth.frac(ageInTicks * BERTILAK_COLOR_CYCLE_SPEED + phase) * 4.0F;
        if (colorCycle < 1.0F) {
            return ARGB.srgbLerp(colorCycle, BERTILAK_GREEN, BERTILAK_BRIGHT_GREEN);
        }
        if (colorCycle < 2.0F) {
            return ARGB.srgbLerp(colorCycle - 1.0F, BERTILAK_BRIGHT_GREEN, BERTILAK_LIGHT_GREEN);
        }
        if (colorCycle < 3.0F) {
            return ARGB.srgbLerp(colorCycle - 2.0F, BERTILAK_LIGHT_GREEN, BERTILAK_DARK_GREEN);
        }
        return ARGB.srgbLerp(colorCycle - 3.0F, BERTILAK_DARK_GREEN, BERTILAK_GREEN);
    }

    private static void clearStencil() {
        var mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.useStencil && mainRenderTarget.getDepthTexture() != null) {
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearStencilTexture(mainRenderTarget.getDepthTexture(), 0);
        }
    }

    private static void renderOutline(GlowSubmit submit, PoseStack poseStack,
                                      MultiBufferSource.BufferSource bufferSource) {
        if (ARGB.alpha(submit.secondaryColor()) > 0 && submit.secondaryOutlineWidth() > 0.0F) {
            renderSubmit(
                    submit,
                    OUTLINE_TYPES,
                    poseStack,
                    bufferSource,
                    submit.secondaryColor(),
                    submit.secondaryOutlineWidth());
        }
        renderSubmit(submit, OUTLINE_TYPES, poseStack, bufferSource, submit.color(), submit.outlineWidth());
    }

    private static void renderSubmit(GlowSubmit submit, Function<Identifier, RenderType> renderTypes,
                                     PoseStack poseStack,
                                     MultiBufferSource.BufferSource bufferSource,
                                     int color,
                                     float outlineWidth) {
        ACTIVE_OUTLINE_WIDTH.set(outlineWidth);
        try {
            submit.draw(poseStack, bufferSource, renderTypes, color, outlineWidth);
        } finally {
            ACTIVE_OUTLINE_WIDTH.remove();
        }
    }

    @FunctionalInterface
    public interface LuminousGeometry {
        void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer, int color, float outlineWidth);
    }

    private interface GlowSubmit {
        int color();

        float outlineWidth();

        int secondaryColor();

        float secondaryOutlineWidth();

        float distanceSquared();

        void draw(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                  Function<Identifier, RenderType> renderTypes, int color, float outlineWidth);
    }

    private record LivingSubmit<S extends LivingEntityRenderState>(
            List<LivingPart<S>> parts, int color, float outlineWidth,
            int secondaryColor, float secondaryOutlineWidth, float distanceSquared) implements GlowSubmit {
        @Override
        public void draw(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                         Function<Identifier, RenderType> renderTypes, int color, float outlineWidth) {
            for (LivingPart<S> part : parts) {
                VertexConsumer vertexConsumer = bufferSource.getBuffer(renderTypes.apply(part.texture()));
                poseStack.pushPose();
                try {
                    poseStack.last().set(part.pose());
                    part.model().setupAnim(part.state());
                    part.model().renderToBuffer(
                            poseStack,
                            vertexConsumer,
                            LightCoordsUtil.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            color);
                } finally {
                    poseStack.popPose();
                }
            }
        }
    }

    private record LivingPart<S extends LivingEntityRenderState>(
            EntityModel<? super S> model, S state, PoseStack.Pose pose, Identifier texture) {
    }

    private record GeometrySubmit(
            PoseStack.Pose pose, Identifier texture, int color, float outlineWidth,
            int secondaryColor, float secondaryOutlineWidth, float distanceSquared,
            LuminousGeometry geometry) implements GlowSubmit {
        @Override
        public void draw(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                         Function<Identifier, RenderType> renderTypes, int color, float outlineWidth) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderTypes.apply(texture));
            poseStack.pushPose();
            try {
                poseStack.last().set(pose);
                geometry.draw(poseStack.last(), vertexConsumer, color, outlineWidth);
            } finally {
                poseStack.popPose();
            }
        }
    }
}
