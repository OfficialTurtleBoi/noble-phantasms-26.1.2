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
import com.google.common.reflect.TypeToken;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;

public final class HulioshjalmrRenderer {
    private static final float MINIMUM_ALPHA = 0.2F;
    private static final Identifier COMPOSITE_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/hulioshjalmr_composite");
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/hulioshjalmr_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(COMPOSITE_SHADER)
            .withSampler("ConcealmentSampler")
            .withSampler("ConcealmentDepthSampler")
            .withSampler("SceneDepthSampler")
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();
    private static TextureTarget CONCEALMENT_TARGET;
    private static TextureTarget OCCLUSION_DEPTH_TARGET;
    private static final OutputTarget CONCEALMENT_OUTPUT = new OutputTarget(
            NoblePhantasms.MOD_ID + "_hulioshjalmr_concealment", () -> CONCEALMENT_TARGET);
    private static final Function<Identifier, RenderType> ENTITY_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_hulioshjalmr_entity",
            RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(CONCEALMENT_OUTPUT)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> ARMOR_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_hulioshjalmr_armor",
            RenderSetup.builder(RenderPipelines.ARMOR_TRANSLUCENT)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(CONCEALMENT_OUTPUT)
                    .useLightmap()
                    .useOverlay()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> ITEM_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_hulioshjalmr_item",
            RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
                    .withTexture("Sampler0", texture)
                    .setOutputTarget(CONCEALMENT_OUTPUT)
                    .useLightmap()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
    private static final ContextKey<Float> CONCEALMENT_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "hulioshjalmr_concealment"));
    private static final ContextKey<Float> VISIBILITY_ALPHA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "hulioshjalmr_visibility_alpha"));
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
        encoder.clearColorTexture(CONCEALMENT_TARGET.getColorTexture(), 0);
        CONCEALMENT_TARGET.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
    }

    public static void captureOcclusionDepth() {
        ensureTarget();
        OCCLUSION_DEPTH_TARGET.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        hasOcclusionDepth = true;
    }

    public static void composite(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (!hasContent || CONCEALMENT_TARGET == null) {
            hasContent = false;
            return;
        }
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        var sceneDepthTarget = hasOcclusionDepth ? OCCLUSION_DEPTH_TARGET : mainTarget;
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST);
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Noble Phantasms Hulioshjalmr concealment composite",
                mainTarget.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("ConcealmentSampler", CONCEALMENT_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("ConcealmentDepthSampler", CONCEALMENT_TARGET.getDepthTextureView(), sampler);
            renderPass.bindTexture("SceneDepthSampler", sceneDepthTarget.getDepthTextureView(), sampler);
            renderPass.draw(0, 3);
        }
        hasContent = false;
        hasOcclusionDepth = false;
    }

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                HulioshjalmrRenderer::extractState);
    }

    public static float getProgress(LivingEntityRenderState state) {
        Float progress = state.getRenderData(CONCEALMENT_KEY);
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

    private static void ensureTarget() {
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        if (CONCEALMENT_TARGET != null && CONCEALMENT_TARGET.useStencil != mainTarget.useStencil) {
            destroyTarget();
        }
        if (CONCEALMENT_TARGET == null) {
            CONCEALMENT_TARGET = new TextureTarget(
                    "Noble Phantasms Hulioshjalmr concealment",
                    mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
        }
        if (OCCLUSION_DEPTH_TARGET == null) {
            OCCLUSION_DEPTH_TARGET = new TextureTarget(
                    "Noble Phantasms Hulioshjalmr entity occlusion depth",
                    mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
        }
        if (CONCEALMENT_TARGET.width != mainTarget.width
                || CONCEALMENT_TARGET.height != mainTarget.height) {
            CONCEALMENT_TARGET.resize(mainTarget.width, mainTarget.height);
        }
        if (OCCLUSION_DEPTH_TARGET.width != mainTarget.width
                || OCCLUSION_DEPTH_TARGET.height != mainTarget.height) {
            OCCLUSION_DEPTH_TARGET.resize(mainTarget.width, mainTarget.height);
        }
    }

    private static void destroyTarget() {
        if (CONCEALMENT_TARGET != null) {
            CONCEALMENT_TARGET.destroyBuffers();
            CONCEALMENT_TARGET = null;
        }
        if (OCCLUSION_DEPTH_TARGET != null) {
            OCCLUSION_DEPTH_TARGET.destroyBuffers();
            OCCLUSION_DEPTH_TARGET = null;
        }
        hasOcclusionDepth = false;
    }

    private static void extractState(LivingEntity entity, LivingEntityRenderState state) {
        float progress = entity instanceof Player player
                ? HulioshjalmrItem.getConcealmentProgress(player)
                : 0.0F;
        state.setRenderData(CONCEALMENT_KEY, progress);
        float minimumAlpha = entity instanceof Player player && isVisibleToViewer(player)
                ? MINIMUM_ALPHA
                : 0.0F;
        state.setRenderData(VISIBILITY_ALPHA_KEY, Mth.lerp(progress, 1.0F, minimumAlpha));
        state.shadowRadius *= 1.0F - progress;
        if (progress >= 1.0F) {
            state.nameTag = null;
            state.outlineColor = 0;
        }
    }

    private static boolean isVisibleToViewer(Player wearer) {
        Player viewer = Minecraft.getInstance().player;
        return viewer != null && (viewer == wearer || viewer.isAlliedTo(wearer));
    }
}
