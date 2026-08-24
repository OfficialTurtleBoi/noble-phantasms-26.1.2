package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import net.turtleboi.noblephantasms.NoblePhantasms;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class ItemOutlineRenderer {
    private static final Identifier MASK_VERTEX_SHADER = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "core/luminous");
    private static final Identifier MASK_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_mask");
    private static final Identifier HORIZONTAL_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_horizontal");
    private static final Identifier DEPTH_HORIZONTAL_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_depth_horizontal");
    private static final Identifier COMPOSITE_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_composite");
    private static final int MAX_RADIUS = 48;
    private static final int CONFIG_UBO_SIZE = new Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();
    private static TextureTarget MASK_TARGET;
    private static TextureTarget DILATION_TARGET;
    private static TextureTarget DEPTH_DILATION_TARGET;
    private static TextureTarget OCCLUSION_DEPTH_TARGET;
    private static final OutputTarget MASK_OUTPUT = new OutputTarget(
            NoblePhantasms.MOD_ID + "_item_outline_mask", () -> MASK_TARGET);
    private static final RenderPipeline VISIBLE_MASK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_visible_mask"))
            .withVertexShader(MASK_VERTEX_SHADER)
            .withFragmentShader(MASK_FRAGMENT_SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, 0.0F, 0.0F))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline THROUGH_MASK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_through_mask"))
            .withVertexShader(MASK_VERTEX_SHADER)
            .withFragmentShader(MASK_FRAGMENT_SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline HORIZONTAL_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_horizontal"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(HORIZONTAL_SHADER)
            .withSampler("InSampler")
            .withUniform("OutlineConfig", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();
    private static final RenderPipeline DEPTH_HORIZONTAL_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_depth_horizontal"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(DEPTH_HORIZONTAL_SHADER)
            .withSampler("InSampler")
            .withUniform("OutlineConfig", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(COMPOSITE_SHADER)
            .withSampler("DilatedSampler")
            .withSampler("DepthDilatedSampler")
            .withSampler("MaskSampler")
            .withSampler("SceneDepthSampler")
            .withUniform("OutlineConfig", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();
    private static final Map<Identifier, RenderType> VISIBLE_MASK_TYPES = new java.util.HashMap<>();
    private static final Map<Identifier, RenderType> THROUGH_MASK_TYPES = new java.util.HashMap<>();
    private static final Map<Identifier, Map<BakedQuad, BakedQuad>> MASKED_QUADS = new java.util.HashMap<>();
    private static final Map<Item, Registration> REGISTRATIONS = new IdentityHashMap<>();
    private static final Map<ItemStackRenderState, Outline> RENDER_STATES = new WeakHashMap<>();
    private static final Map<SubmitNodeStorage.ItemSubmit, Outline> SUBMITS = new IdentityHashMap<>();
    private static final Map<Region, Map<BakedQuad, List<BakedQuad>>> REGION_QUADS = new java.util.HashMap<>();
    private static final List<PendingOutline> PENDING_OUTLINES = new ArrayList<>();
    private static final ThreadLocal<Outline> SUBMITTING = new ThreadLocal<>();
    private static final QuadInstance QUAD_INSTANCE = new QuadInstance();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static boolean hasProjection;
    private static boolean hasOcclusionDepth;
    private static MappableRingBuffer configBuffer;

    public static Outline glow(int color, float alpha, float thickness) {
        return new Outline(null, null, List.of(new GlowLayer(color, alpha, thickness)), false);
    }

    public static Outline vibrantGlow(int color, float thickness) {
        return vibrantGlow(color, 1.0F, thickness);
    }

    public static Outline vibrantGlow(int color, float alpha, float thickness) {
        return multiGlow(
                glow(color, alpha * 0.35F, thickness * 1.9F),
                glow(color, alpha, thickness));
    }

    public static Outline vibrantGlow(int color, float alpha, float thickness, float animationTime, float phase) {
        float flicker = Mth.sin(animationTime * 1.1F + phase) * 0.18F
                + Mth.sin(animationTime * 2.9F + phase * 1.7F) * 0.08F;
        float outerThickness = thickness * Mth.clamp(1.9F + flicker * 1.4F, 1.45F, 2.35F);
        return multiGlow(
                glow(color, alpha * 0.35F, outerThickness),
                glow(color, alpha, thickness));
    }

    public static Outline multiGlow(Outline... glows) {
        List<GlowLayer> layers = new ArrayList<>();
        for (Outline glow : glows) {
            layers.addAll(glow.layers());
        }
        return new Outline(null, null, layers, false);
    }

    public static void registerHeld(Item item, int color, float width, Predicate<ItemStack> condition) {
        registerHeld(item, color, 1.0F, width, condition);
    }

    public static void registerHeld(Item item, int color, float alpha, float thickness,
                                    Predicate<ItemStack> condition) {
        registerHeld(item, (stack, context, owner) -> condition.test(stack)
                ? glow(color, alpha, thickness)
                : null);
    }

    public static void registerHeld(Item item, OutlineProvider provider) {
        register(item, ItemOutlineRenderer::isHeld, provider);
    }

    public static void register(Item item, Predicate<ItemDisplayContext> contexts, OutlineProvider provider) {
        REGISTRATIONS.put(item, new Registration(contexts, provider));
    }

    public static void initialize() {
        destroyTargets();
        RENDER_STATES.clear();
        SUBMITS.clear();
        PENDING_OUTLINES.clear();
        REGION_QUADS.clear();
        VISIBLE_MASK_TYPES.clear();
        THROUGH_MASK_TYPES.clear();
        MASKED_QUADS.clear();
        hasProjection = false;
        hasOcclusionDepth = false;
        SUBMITTING.remove();
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(VISIBLE_MASK_PIPELINE);
        event.registerPipeline(THROUGH_MASK_PIPELINE);
        event.registerPipeline(HORIZONTAL_PIPELINE);
        event.registerPipeline(DEPTH_HORIZONTAL_PIPELINE);
        event.registerPipeline(COMPOSITE_PIPELINE);
    }

    public static void setProjection(Matrix4fc projection) {
        PROJECTION.set(projection);
        hasProjection = true;
    }

    public static void beginFrame() {
        PENDING_OUTLINES.clear();
        hasOcclusionDepth = false;
    }

    public static void captureOcclusionDepth() {
        ensureTargets();
        OCCLUSION_DEPTH_TARGET.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        hasOcclusionDepth = true;
    }

    public static void track(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context,
                             @Nullable ItemOwner owner) {
        Registration registration = REGISTRATIONS.get(stack.getItem());
        Outline outline = registration == null || !registration.contexts().test(context)
                ? null
                : registration.provider().get(stack, context, owner);
        if (outline == null || !outline.hasVisibleLayers()) {
            RENDER_STATES.remove(state);
        } else {
            RENDER_STATES.put(state, outline.normalized());
        }
    }

    public static void beginSubmit(ItemStackRenderState state) {
        Outline outline = RENDER_STATES.get(state);
        if (outline == null) {
            SUBMITTING.remove();
        } else {
            SUBMITTING.set(outline);
        }
    }

    public static void endSubmit() {
        SUBMITTING.remove();
    }

    public static void capture(SubmitNodeStorage.ItemSubmit submit) {
        Outline outline = SUBMITTING.get();
        if (outline != null) {
            SUBMITS.put(submit, outline);
        }
    }

    public static void render(MultiBufferSource.BufferSource bufferSource, SubmitNodeStorage.ItemSubmit submit) {
        Outline outline = SUBMITS.remove(submit);
        if (outline == null || submit.quads().isEmpty()) {
            return;
        }
        if (!submit.displayContext().firstPerson()) {
            PENDING_OUTLINES.add(new PendingOutline(submit, outline));
            return;
        }
        render(bufferSource, submit, outline);
    }

    public static void renderOutlines(RenderLevelStageEvent.AfterLevel event) {
        if (PENDING_OUTLINES.isEmpty()) {
            return;
        }
        var modelViewStack = RenderSystem.getModelViewStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        modelViewStack.pushMatrix();
        modelViewStack.mul(event.getModelViewMatrix());
        try {
            for (PendingOutline pending : PENDING_OUTLINES) {
                render(bufferSource, pending.submit(), pending.outline());
            }
            bufferSource.endLastBatch();
        } finally {
            modelViewStack.popMatrix();
            PENDING_OUTLINES.clear();
        }
    }

    private static void render(MultiBufferSource.BufferSource bufferSource, SubmitNodeStorage.ItemSubmit submit,
                               Outline outline) {
        List<BakedQuad> outlined = outline.region() == null
                ? submit.quads()
                : clipToRegion(submit.quads(), outline.region());
        if (outline.mask() != null) {
            outlined = remapToMask(outlined, outline.mask());
        }
        renderLayers(bufferSource, submit, outlined, outline.layers(), outline.visibleThroughObjects());
    }

    private static void renderLayers(MultiBufferSource.BufferSource bufferSource, SubmitNodeStorage.ItemSubmit submit,
                                     List<BakedQuad> outlined, List<GlowLayer> layers,
                                     boolean visibleThroughObjects) {
        if (outlined.isEmpty() || layers.isEmpty()) {
            return;
        }
        bufferSource.endBatch();
        ensureTargets();
        Minecraft minecraft = Minecraft.getInstance();
        var mainTarget = minecraft.getMainRenderTarget();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(MASK_TARGET.getColorTexture(), 0);
        encoder.clearColorTexture(DILATION_TARGET.getColorTexture(), 0);
        encoder.clearColorTexture(DEPTH_DILATION_TARGET.getColorTexture(), -1);
        if (!visibleThroughObjects) {
            MASK_TARGET.copyDepthFrom(hasOcclusionDepth ? OCCLUSION_DEPTH_TARGET : mainTarget);
        }
        renderQuads(bufferSource, submit.pose(), outlined, -1, visibleThroughObjects);
        float[] radii = screenRadii(submit, outlined, layers);
        float maximumRadius = radii.length == 0 ? 0.0F : radii[0];
        ScreenBounds bounds = screenBounds(submit, outlined, maximumRadius + 2.0F);
        for (int start = 0; start < layers.size(); start += 4) {
            writeConfig(encoder, layers, radii, start, visibleThroughObjects);
            renderHorizontal(encoder, bounds);
            if (!visibleThroughObjects) {
                renderDepthHorizontal(encoder, bounds);
            }
            renderComposite(encoder, mainTarget.getColorTextureView(), bounds);
            configBuffer.rotate();
        }
    }

    private static void ensureTargets() {
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        if (MASK_TARGET != null && MASK_TARGET.useStencil != mainTarget.useStencil) {
            destroyTargets();
        }
        if (MASK_TARGET == null) {
            MASK_TARGET = new TextureTarget(
                    "Noble Phantasms item outline mask", mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
            DILATION_TARGET = new TextureTarget(
                    "Noble Phantasms item outline dilation", mainTarget.width, mainTarget.height, false);
            DEPTH_DILATION_TARGET = new TextureTarget(
                    "Noble Phantasms item outline depth dilation", mainTarget.width, mainTarget.height, false);
            OCCLUSION_DEPTH_TARGET = new TextureTarget(
                    "Noble Phantasms item outline occlusion depth", mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
            configBuffer = new MappableRingBuffer(
                    () -> "Noble Phantasms item outline config",
                    com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_WRITE | com.mojang.blaze3d.buffers.GpuBuffer.USAGE_UNIFORM,
                    CONFIG_UBO_SIZE);
            return;
        }
        if (MASK_TARGET.width != mainTarget.width || MASK_TARGET.height != mainTarget.height) {
            MASK_TARGET.resize(mainTarget.width, mainTarget.height);
            DILATION_TARGET.resize(mainTarget.width, mainTarget.height);
            DEPTH_DILATION_TARGET.resize(mainTarget.width, mainTarget.height);
            OCCLUSION_DEPTH_TARGET.resize(mainTarget.width, mainTarget.height);
        }
    }

    private static void renderQuads(MultiBufferSource.BufferSource bufferSource,
                                    com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                    List<BakedQuad> quads, int color, boolean visibleThroughObjects) {
        QUAD_INSTANCE.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
        QUAD_INSTANCE.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        Map<RenderType, VertexConsumer> buffers = new IdentityHashMap<>();
        for (BakedQuad quad : quads) {
            QUAD_INSTANCE.setColor(color);
            Identifier atlas = quad.materialInfo().sprite().atlasLocation();
            RenderType renderType = renderType(atlas, visibleThroughObjects);
            VertexConsumer vertexConsumer = buffers.computeIfAbsent(renderType, bufferSource::getBuffer);
            vertexConsumer.putBakedQuad(pose, quad, QUAD_INSTANCE);
        }
        buffers.keySet().forEach(bufferSource::endBatch);
    }

    private static void writeConfig(CommandEncoder encoder, List<GlowLayer> layers, float[] screenRadii, int start,
                                    boolean visibleThroughObjects) {
        float[] radii = new float[4];
        float[] alphas = new float[4];
        try (var mapped = encoder.mapBuffer(configBuffer.currentBuffer(), false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(mapped.data());
            for (int index = 0; index < 4; index++) {
                int layerIndex = start + index;
                if (layerIndex >= layers.size()) {
                    builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
                    continue;
                }
                GlowLayer layer = layers.get(layerIndex);
                builder.putVec4(
                        ARGB.redFloat(layer.color()),
                        ARGB.greenFloat(layer.color()),
                        ARGB.blueFloat(layer.color()),
                        1.0F);
                radii[index] = screenRadii[layerIndex];
                alphas[index] = layer.alpha();
            }
            builder.putVec4(radii[0], radii[1], radii[2], radii[3]);
            builder.putVec4(alphas[0], alphas[1], alphas[2], alphas[3]);
            builder.putVec4(visibleThroughObjects ? 1.0F : 0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void renderHorizontal(CommandEncoder encoder, ScreenBounds bounds) {
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Noble Phantasms item outline horizontal dilation",
                DILATION_TARGET.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(HORIZONTAL_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", MASK_TARGET.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST));
            renderPass.setUniform("OutlineConfig", configBuffer.currentBuffer());
            bounds.enable(renderPass);
            renderPass.draw(0, 3);
        }
    }

    private static void renderDepthHorizontal(CommandEncoder encoder, ScreenBounds bounds) {
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Noble Phantasms item outline horizontal depth dilation",
                DEPTH_DILATION_TARGET.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(DEPTH_HORIZONTAL_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", MASK_TARGET.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST));
            renderPass.setUniform("OutlineConfig", configBuffer.currentBuffer());
            bounds.enable(renderPass);
            renderPass.draw(0, 3);
        }
    }

    private static void renderComposite(CommandEncoder encoder, com.mojang.blaze3d.textures.GpuTextureView output,
                                        ScreenBounds bounds) {
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST);
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Noble Phantasms item outline composite", output, OptionalInt.empty())) {
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("DilatedSampler", DILATION_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("DepthDilatedSampler", DEPTH_DILATION_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("MaskSampler", MASK_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("SceneDepthSampler", MASK_TARGET.getDepthTextureView(), sampler);
            renderPass.setUniform("OutlineConfig", configBuffer.currentBuffer());
            bounds.enable(renderPass);
            renderPass.draw(0, 3);
        }
    }

    private static Vector3f axisCompensation(com.mojang.blaze3d.vertex.PoseStack.Pose pose) {
        Vector3f scale = pose.pose().getScale(new Vector3f());
        float target = Math.max(scale.x, Math.max(scale.y, scale.z));
        scale.x = scale.x > 1.0E-5F ? target / scale.x : 1.0F;
        scale.y = scale.y > 1.0E-5F ? target / scale.y : 1.0F;
        scale.z = scale.z > 1.0E-5F ? target / scale.z : 1.0F;
        return scale;
    }

    private static float[] screenRadii(SubmitNodeStorage.ItemSubmit submit, List<BakedQuad> quads,
                                       List<GlowLayer> layers) {
        float[] radii = new float[layers.size()];
        float maximumRadius = 0.0F;
        float texelRadius = screenTexelRadius(submit, quads);
        for (int index = 0; index < layers.size(); index++) {
            radii[index] = texelRadius * layers.get(index).thickness();
            maximumRadius = Math.max(maximumRadius, radii[index]);
        }
        float scale = maximumRadius > MAX_RADIUS ? MAX_RADIUS / maximumRadius : 1.0F;
        for (int index = 0; index < radii.length; index++) {
            radii[index] = Math.max(1.0F, radii[index] * scale);
        }
        return radii;
    }

    private static float screenTexelRadius(SubmitNodeStorage.ItemSubmit submit, List<BakedQuad> quads) {
        if (!hasProjection) {
            return fallbackTexelRadius(submit, quads);
        }
        Matrix4f modelToClip = modelToClip(submit);
        float radius = 0.0F;
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            float uScale = sprite.contents().width() / (sprite.getU1() - sprite.getU0());
            float vScale = sprite.contents().height() / (sprite.getV1() - sprite.getV0());
            for (int index = 0; index < 4; index++) {
                int nextIndex = (index + 1) % 4;
                Vector4f first = project(modelToClip, quad.position(index).x(), quad.position(index).y(), quad.position(index).z());
                Vector4f second = project(
                        modelToClip, quad.position(nextIndex).x(), quad.position(nextIndex).y(), quad.position(nextIndex).z());
                if (first == null || second == null) {
                    continue;
                }
                float deltaU = (UVPair.unpackU(quad.packedUV(nextIndex)) - UVPair.unpackU(quad.packedUV(index))) * uScale;
                float deltaV = (UVPair.unpackV(quad.packedUV(nextIndex)) - UVPair.unpackV(quad.packedUV(index))) * vScale;
                float textureDistance = Mth.sqrt(deltaU * deltaU + deltaV * deltaV);
                if (textureDistance <= 1.0E-5F) {
                    continue;
                }
                float deltaX = (second.x - first.x) * MASK_TARGET.width * 0.5F;
                float deltaY = (second.y - first.y) * MASK_TARGET.height * 0.5F;
                radius = Math.max(radius, Mth.sqrt(deltaX * deltaX + deltaY * deltaY) / textureDistance);
            }
        }
        return radius > 0.0F ? Math.max(1.0F, radius) : fallbackTexelRadius(submit, quads);
    }

    private static float fallbackTexelRadius(SubmitNodeStorage.ItemSubmit submit, List<BakedQuad> quads) {
        int resolution = 1;
        for (BakedQuad quad : quads) {
            var contents = quad.materialInfo().sprite().contents();
            resolution = Math.max(resolution, Math.max(contents.width(), contents.height()));
        }
        return screenModelRadius(submit, quads, 1.0F / resolution);
    }

    private static float screenModelRadius(SubmitNodeStorage.ItemSubmit submit, List<BakedQuad> quads, float width) {
        if (!hasProjection) {
            return Math.max(1.0F, width * MASK_TARGET.height * 0.4F);
        }
        LocalBounds bounds = localBounds(quads);
        Vector3f center = bounds.center();
        Matrix4f modelToClip = modelToClip(submit);
        Vector4f projectedCenter = project(modelToClip, center.x, center.y, center.z);
        if (projectedCenter == null) {
            return Math.max(1.0F, width * MASK_TARGET.height * 0.4F);
        }
        Vector3f compensation = axisCompensation(submit.pose());
        float radius = 0.0F;
        radius = Math.max(radius, projectedDistance(modelToClip, projectedCenter, center.x + width * compensation.x, center.y, center.z));
        radius = Math.max(radius, projectedDistance(modelToClip, projectedCenter, center.x, center.y + width * compensation.y, center.z));
        radius = Math.max(radius, projectedDistance(modelToClip, projectedCenter, center.x, center.y, center.z + width * compensation.z));
        return Math.max(1.0F, radius);
    }

    private static float projectedDistance(Matrix4f modelToClip, Vector4f center, float x, float y, float z) {
        Vector4f projected = project(modelToClip, x, y, z);
        if (projected == null) {
            return 0.0F;
        }
        float deltaX = (projected.x - center.x) * MASK_TARGET.width * 0.5F;
        float deltaY = (projected.y - center.y) * MASK_TARGET.height * 0.5F;
        return Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static ScreenBounds screenBounds(SubmitNodeStorage.ItemSubmit submit, List<BakedQuad> quads, float margin) {
        if (!hasProjection) {
            return ScreenBounds.full();
        }
        Matrix4f modelToClip = modelToClip(submit);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (BakedQuad quad : quads) {
            for (int index = 0; index < 4; index++) {
                var position = quad.position(index);
                Vector4f projected = project(modelToClip, position.x(), position.y(), position.z());
                if (projected == null) {
                    continue;
                }
                float x = (projected.x * 0.5F + 0.5F) * MASK_TARGET.width;
                float y = (projected.y * 0.5F + 0.5F) * MASK_TARGET.height;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (!Float.isFinite(minX)) {
            return ScreenBounds.full();
        }
        int left = Mth.clamp(Mth.floor(minX - margin), 0, MASK_TARGET.width);
        int bottom = Mth.clamp(Mth.floor(minY - margin), 0, MASK_TARGET.height);
        int right = Mth.clamp(Mth.ceil(maxX + margin), 0, MASK_TARGET.width);
        int top = Mth.clamp(Mth.ceil(maxY + margin), 0, MASK_TARGET.height);
        return new ScreenBounds(left, bottom, Math.max(1, right - left), Math.max(1, top - bottom));
    }

    private static Matrix4f modelToClip(SubmitNodeStorage.ItemSubmit submit) {
        return new Matrix4f(PROJECTION)
                .mul(RenderSystem.getModelViewMatrix())
                .mul(submit.pose().pose());
    }

    private static @Nullable Vector4f project(Matrix4f matrix, float x, float y, float z) {
        Vector4f projected = matrix.transform(new Vector4f(x, y, z, 1.0F));
        if (projected.w <= 1.0E-5F) {
            return null;
        }
        projected.div(projected.w);
        return projected;
    }

    private static LocalBounds localBounds(List<BakedQuad> quads) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (BakedQuad quad : quads) {
            for (int index = 0; index < 4; index++) {
                var position = quad.position(index);
                minX = Math.min(minX, position.x());
                minY = Math.min(minY, position.y());
                minZ = Math.min(minZ, position.z());
                maxX = Math.max(maxX, position.x());
                maxY = Math.max(maxY, position.y());
                maxZ = Math.max(maxZ, position.z());
            }
        }
        return new LocalBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static List<BakedQuad> clipToRegion(List<BakedQuad> quads, Region region) {
        Map<BakedQuad, List<BakedQuad>> cache = REGION_QUADS.computeIfAbsent(region, ignored -> new IdentityHashMap<>());
        List<BakedQuad> clipped = new ArrayList<>();
        for (BakedQuad quad : quads) {
            clipped.addAll(cache.computeIfAbsent(quad, source -> clipQuad(source, region)));
        }
        return clipped;
    }

    private static List<BakedQuad> remapToMask(List<BakedQuad> quads, Identifier mask) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        TextureAtlasSprite maskSprite = atlas.getSprite(mask);
        Map<BakedQuad, BakedQuad> cache = MASKED_QUADS.computeIfAbsent(mask, ignored -> new IdentityHashMap<>());
        List<BakedQuad> masked = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            masked.add(cache.computeIfAbsent(quad, source -> new MutableQuad().setFrom(source)
                    .setSpriteAndMoveUv(maskSprite, source.materialInfo().layer(), source.materialInfo().itemRenderType())
                    .toBakedQuad()));
        }
        return masked;
    }

    private static List<BakedQuad> clipQuad(BakedQuad source, Region region) {
        List<Vertex> vertices = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            vertices.add(new Vertex(
                    source.position(index).x(), source.position(index).y(), source.position(index).z(),
                    UVPair.unpackU(source.packedUV(index)), UVPair.unpackV(source.packedUV(index))));
        }
        if (vertices.stream().allMatch(region::contains)) {
            return List.of(source);
        }
        vertices = clip(vertices, 0, region.minX(), true);
        vertices = clip(vertices, 0, region.maxX(), false);
        vertices = clip(vertices, 1, region.minY(), true);
        vertices = clip(vertices, 1, region.maxY(), false);
        vertices = clip(vertices, 2, region.minZ(), true);
        vertices = clip(vertices, 2, region.maxZ(), false);
        if (vertices.size() < 3) {
            return List.of();
        }
        if (vertices.size() == 4) {
            return List.of(createQuad(source, vertices.get(0), vertices.get(1), vertices.get(2), vertices.get(3)));
        }
        List<BakedQuad> triangles = new ArrayList<>(vertices.size() - 2);
        for (int index = 1; index < vertices.size() - 1; index++) {
            Vertex first = vertices.get(0);
            Vertex second = vertices.get(index);
            Vertex third = vertices.get(index + 1);
            triangles.add(createQuad(source, first, second, third, third));
        }
        return triangles;
    }

    private static List<Vertex> clip(List<Vertex> input, int axis, float limit, boolean minimum) {
        if (input.isEmpty() || Float.isInfinite(limit)) {
            return input;
        }
        List<Vertex> output = new ArrayList<>();
        Vertex previous = input.getLast();
        boolean previousInside = inside(previous.component(axis), limit, minimum);
        for (Vertex current : input) {
            boolean currentInside = inside(current.component(axis), limit, minimum);
            if (currentInside != previousInside) {
                float denominator = current.component(axis) - previous.component(axis);
                float progress = denominator == 0.0F ? 0.0F : (limit - previous.component(axis)) / denominator;
                output.add(previous.lerp(current, Mth.clamp(progress, 0.0F, 1.0F)));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean inside(float coordinate, float limit, boolean minimum) {
        return minimum ? coordinate >= limit - 1.0E-5F : coordinate <= limit + 1.0E-5F;
    }

    private static BakedQuad createQuad(BakedQuad source, Vertex first, Vertex second, Vertex third, Vertex fourth) {
        MutableQuad quad = new MutableQuad().setFrom(source);
        setVertex(quad, 0, first);
        setVertex(quad, 1, second);
        setVertex(quad, 2, third);
        setVertex(quad, 3, fourth);
        quad.recomputeNormals(false);
        return quad.toBakedQuad();
    }

    private static void setVertex(MutableQuad quad, int index, Vertex vertex) {
        quad.setPosition(index, vertex.x(), vertex.y(), vertex.z());
        quad.setUv(index, vertex.u(), vertex.v());
    }

    private static RenderType renderType(Identifier atlas, boolean visibleThroughObjects) {
        Map<Identifier, RenderType> types = visibleThroughObjects ? THROUGH_MASK_TYPES : VISIBLE_MASK_TYPES;
        RenderPipeline pipeline = visibleThroughObjects ? THROUGH_MASK_PIPELINE : VISIBLE_MASK_PIPELINE;
        return types.computeIfAbsent(atlas, texture -> RenderType.create(
                NoblePhantasms.MOD_ID + "_item_outline_mask_" + (visibleThroughObjects ? "through" : "visible"),
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", texture)
                        .setOutputTarget(MASK_OUTPUT)
                        .createRenderSetup()));
    }

    private static void destroyTargets() {
        if (MASK_TARGET != null) {
            MASK_TARGET.destroyBuffers();
            MASK_TARGET = null;
        }
        if (DILATION_TARGET != null) {
            DILATION_TARGET.destroyBuffers();
            DILATION_TARGET = null;
        }
        if (DEPTH_DILATION_TARGET != null) {
            DEPTH_DILATION_TARGET.destroyBuffers();
            DEPTH_DILATION_TARGET = null;
        }
        if (OCCLUSION_DEPTH_TARGET != null) {
            OCCLUSION_DEPTH_TARGET.destroyBuffers();
            OCCLUSION_DEPTH_TARGET = null;
        }
        hasOcclusionDepth = false;
        if (configBuffer != null) {
            configBuffer.close();
            configBuffer = null;
        }
    }

    private static boolean isHeld(ItemDisplayContext context) {
        return context.firstPerson()
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @FunctionalInterface
    public interface OutlineProvider {
        @Nullable Outline get(ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner);
    }

    private record Registration(Predicate<ItemDisplayContext> contexts, OutlineProvider provider) {
    }

    private record PendingOutline(SubmitNodeStorage.ItemSubmit submit, Outline outline) {
    }

    private record ScreenBounds(int x, int y, int width, int height) {
        private static ScreenBounds full() {
            return new ScreenBounds(0, 0, MASK_TARGET.width, MASK_TARGET.height);
        }

        private void enable(RenderPass renderPass) {
            renderPass.enableScissor(x, y, width, height);
        }
    }

    private record LocalBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        private Vector3f center() {
            return new Vector3f(
                    (minX + maxX) * 0.5F,
                    (minY + maxY) * 0.5F,
                    (minZ + maxZ) * 0.5F);
        }
    }

    private record Vertex(float x, float y, float z, float u, float v) {
        private float component(int axis) {
            return switch (axis) {
                case 0 -> x;
                case 1 -> y;
                default -> z;
            };
        }

        private Vertex lerp(Vertex other, float progress) {
            return new Vertex(
                    Mth.lerp(progress, x, other.x),
                    Mth.lerp(progress, y, other.y),
                    Mth.lerp(progress, z, other.z),
                    Mth.lerp(progress, u, other.u),
                    Mth.lerp(progress, v, other.v));
        }
    }

    public record Region(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        public static Region xy(float minX, float minY, float maxX, float maxY) {
            return new Region(minX, minY, Float.NEGATIVE_INFINITY, maxX, maxY, Float.POSITIVE_INFINITY);
        }

        private boolean contains(Vertex vertex) {
            return vertex.x() >= minX && vertex.x() <= maxX
                    && vertex.y() >= minY && vertex.y() <= maxY
                    && vertex.z() >= minZ && vertex.z() <= maxZ;
        }
    }

    public record GlowLayer(int color, float alpha, float thickness) {
        private GlowLayer normalized() {
            return new GlowLayer(ARGB.opaque(color), Math.clamp(alpha, 0.0F, 1.0F), Math.max(0.0F, thickness));
        }

        private boolean visible() {
            return alpha > 0.0F && thickness > 0.0F;
        }
    }

    public record Outline(@Nullable Region region, @Nullable Identifier mask, List<GlowLayer> layers,
                          boolean visibleThroughObjects) {
        public Outline {
            layers = List.copyOf(layers);
        }

        public Outline mask(Identifier mask) {
            return new Outline(region, mask, layers, visibleThroughObjects);
        }

        public Outline region(Region region) {
            return new Outline(region, mask, layers, visibleThroughObjects);
        }

        public Outline visibleThroughObjects(boolean visibleThroughObjects) {
            return new Outline(region, mask, layers, visibleThroughObjects);
        }

        private boolean hasVisibleLayers() {
            return layers.stream().anyMatch(GlowLayer::visible);
        }

        private Outline normalized() {
            return new Outline(region, mask, layers.stream()
                    .map(GlowLayer::normalized)
                    .filter(GlowLayer::visible)
                    .sorted((first, second) -> Float.compare(second.thickness(), first.thickness()))
                    .toList(), visibleThroughObjects);
        }
    }
}
