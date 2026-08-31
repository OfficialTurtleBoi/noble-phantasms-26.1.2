package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;

public final class EntityTranslucencyRenderer {
    private static final Identifier COMPOSITE_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/entity_translucency_composite");
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                    NoblePhantasms.MOD_ID, "pipeline/entity_translucency_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(COMPOSITE_SHADER)
            .withSampler("EntitySampler")
            .withSampler("EntityDepthSampler")
            .withSampler("SceneDepthSampler")
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();
    private static TextureTarget TRANSLUCENCY_TARGET;
    private static TextureTarget OCCLUSION_DEPTH_TARGET;
    private static final OutputTarget TRANSLUCENCY_OUTPUT = new OutputTarget(
            NoblePhantasms.MOD_ID + "_entity_translucency", () -> TRANSLUCENCY_TARGET);
    private static final Function<Identifier, RenderType> ENTITY_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_entity_translucent",
            RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(TRANSLUCENCY_OUTPUT)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> ARMOR_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_armor_translucent",
            RenderSetup.builder(RenderPipelines.ARMOR_TRANSLUCENT)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(TRANSLUCENCY_OUTPUT)
                    .useLightmap()
                    .useOverlay()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> ITEM_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_item_translucent",
            RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(TRANSLUCENCY_OUTPUT)
                    .useLightmap()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> EYE_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_translucent_emissive_eyes",
            RenderSetup.builder(RenderPipelines.EYES)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(TRANSLUCENCY_OUTPUT)
                    .createRenderSetup()));
    private static final ContextKey<Float> TRANSLUCENCY_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "entity_translucency_progress"));
    private static final ContextKey<Float> VISIBILITY_ALPHA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "entity_visibility_alpha"));
    private static final ThreadLocal<Float> ACTIVE_PROGRESS = new ThreadLocal<>();
    private static final ThreadLocal<Float> ACTIVE_ALPHA = new ThreadLocal<>();
    private static final ThreadLocal<Float> ACTIVE_ITEM_ALPHA = new ThreadLocal<>();
    private static final Map<SubmitNodeStorage.ItemSubmit, Float> ITEM_ALPHAS = new IdentityHashMap<>();
    private static boolean hasContent;
    private static boolean hasOcclusionDepth;

    public static void initialize() {
        destroyTarget();
        ITEM_ALPHAS.clear();
        hasContent = false;
        hasOcclusionDepth = false;
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(COMPOSITE_PIPELINE);
    }

    public static void beginFrame() {
        ensureTarget();
        hasOcclusionDepth = false;
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(TRANSLUCENCY_TARGET.getColorTexture(), 0);
        TRANSLUCENCY_TARGET.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
    }

    public static void captureOcclusionDepth() {
        ensureTarget();
        OCCLUSION_DEPTH_TARGET.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        hasOcclusionDepth = true;
    }

    public static void composite(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (!hasContent || TRANSLUCENCY_TARGET == null) {
            hasContent = false;
            return;
        }
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        var sceneDepthTarget = hasOcclusionDepth ? OCCLUSION_DEPTH_TARGET : mainTarget;
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST);
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Noble Phantasms entity translucency composite",
                mainTarget.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("EntitySampler", TRANSLUCENCY_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("EntityDepthSampler", TRANSLUCENCY_TARGET.getDepthTextureView(), sampler);
            renderPass.bindTexture("SceneDepthSampler", sceneDepthTarget.getDepthTextureView(), sampler);
            renderPass.draw(0, 3);
        }
        hasContent = false;
        hasOcclusionDepth = false;
    }

    public static float getProgress(LivingEntityRenderState state) {
        Float progress = state.getRenderData(TRANSLUCENCY_KEY);
        return progress == null ? 0.0F : Mth.clamp(progress, 0.0F, 1.0F);
    }

    public static float getVisibilityAlpha(LivingEntityRenderState state) {
        Float alpha = state.getRenderData(VISIBILITY_ALPHA_KEY);
        return alpha == null ? 1.0F : Mth.clamp(alpha, 0.0F, 1.0F);
    }

    public static int applyAlpha(int color, float alphaScale) {
        int alpha = Mth.clamp(Math.round(ARGB.alpha(color) * Mth.clamp(alphaScale, 0.0F, 1.0F)), 0, 255);
        return ARGB.color(alpha, color);
    }

    public static void beginRendering(LivingEntityRenderState state) {
        float progress = getProgress(state);
        if (progress > 0.0F) {
            ACTIVE_PROGRESS.set(progress);
            ACTIVE_ALPHA.set(getVisibilityAlpha(state));
            if (getVisibilityAlpha(state) > 0.0F) {
                hasContent = true;
            }
        }
    }

    public static void endRendering() {
        ACTIVE_PROGRESS.remove();
        ACTIVE_ALPHA.remove();
    }

    public static float getActiveProgress() {
        Float progress = ACTIVE_PROGRESS.get();
        return progress == null ? 0.0F : progress;
    }

    public static float getActiveAlpha() {
        Float alpha = ACTIVE_ALPHA.get();
        return alpha == null ? 1.0F : alpha;
    }

    public static RenderType entityRenderType(Identifier texture) {
        return ENTITY_TYPES.apply(texture);
    }

    public static RenderType armorRenderType(Identifier texture) {
        return ARMOR_TYPES.apply(texture);
    }

    public static RenderType itemRenderType(Identifier texture) {
        return ITEM_TYPES.apply(texture);
    }

    public static RenderType eyeRenderType(Identifier texture) {
        return EYE_TYPES.apply(texture);
    }

    public static void captureItemSubmit(SubmitNodeStorage.ItemSubmit submit) {
        if (getActiveProgress() > 0.0F) {
            ITEM_ALPHAS.put(submit, getActiveAlpha());
        }
    }

    public static boolean hasCapturedItemAlpha(SubmitNodeStorage.ItemSubmit submit) {
        return ITEM_ALPHAS.containsKey(submit);
    }

    public static void beginItemRendering(SubmitNodeStorage.ItemSubmit submit) {
        Float alpha = ITEM_ALPHAS.get(submit);
        if (alpha != null) {
            ACTIVE_ITEM_ALPHA.set(alpha);
        }
    }

    public static void endItemRendering() {
        ACTIVE_ITEM_ALPHA.remove();
    }

    public static float getActiveItemAlpha() {
        Float alpha = ACTIVE_ITEM_ALPHA.get();
        return alpha == null ? 1.0F : alpha;
    }

    public static void clearItemSubmits() {
        ITEM_ALPHAS.clear();
    }

    public static void setTranslucencyState(
            LivingEntityRenderState state, float progress, float minimumAlpha) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        state.setRenderData(TRANSLUCENCY_KEY, clampedProgress);
        state.setRenderData(VISIBILITY_ALPHA_KEY,
                Mth.lerp(clampedProgress, 1.0F, Mth.clamp(minimumAlpha, 0.0F, 1.0F)));
        state.shadowRadius *= 1.0F - clampedProgress;
        if (clampedProgress >= 1.0F) {
            state.nameTag = null;
            state.outlineColor = 0;
        }
    }

    private static void ensureTarget() {
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        if (TRANSLUCENCY_TARGET != null && TRANSLUCENCY_TARGET.useStencil != mainTarget.useStencil) {
            destroyTarget();
        }
        if (TRANSLUCENCY_TARGET == null) {
            TRANSLUCENCY_TARGET = new TextureTarget(
                    "Noble Phantasms entity translucency",
                    mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
        }
        if (OCCLUSION_DEPTH_TARGET == null) {
            OCCLUSION_DEPTH_TARGET = new TextureTarget(
                    "Noble Phantasms entity translucency occlusion depth",
                    mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
        }
        if (TRANSLUCENCY_TARGET.width != mainTarget.width
                || TRANSLUCENCY_TARGET.height != mainTarget.height) {
            TRANSLUCENCY_TARGET.resize(mainTarget.width, mainTarget.height);
        }
        if (OCCLUSION_DEPTH_TARGET.width != mainTarget.width
                || OCCLUSION_DEPTH_TARGET.height != mainTarget.height) {
            OCCLUSION_DEPTH_TARGET.resize(mainTarget.width, mainTarget.height);
        }
    }

    private static void destroyTarget() {
        if (TRANSLUCENCY_TARGET != null) {
            TRANSLUCENCY_TARGET.destroyBuffers();
            TRANSLUCENCY_TARGET = null;
        }
        if (OCCLUSION_DEPTH_TARGET != null) {
            OCCLUSION_DEPTH_TARGET.destroyBuffers();
            OCCLUSION_DEPTH_TARGET = null;
        }
        hasOcclusionDepth = false;
    }

}
