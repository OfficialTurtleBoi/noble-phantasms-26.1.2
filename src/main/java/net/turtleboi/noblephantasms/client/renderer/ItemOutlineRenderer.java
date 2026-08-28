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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private static final String GLOW_TEXTURE_FOLDER = "item/glow/";
    private static final Identifier MASK_VERTEX_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_mask");
    private static final Identifier MASK_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_mask");
    private static final Identifier GEOMETRY_MASK_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_geometry_mask");
    private static final Identifier MODEL_COMPOSITE_SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/item_outline_model_composite");
    private static final int MASK_UV_SCALE = 65535;
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
    private static TextureTarget OCCLUSION_DEPTH_TARGET;
    private static TextureTarget EXPANDED_MASK_TARGET;
    private static final OutputTarget MASK_OUTPUT = new OutputTarget(
            NoblePhantasms.MOD_ID + "_item_outline_mask", () -> MASK_TARGET);
    private static final OutputTarget EXPANDED_MASK_OUTPUT = new OutputTarget(
            NoblePhantasms.MOD_ID + "_item_outline_expanded_mask", () -> EXPANDED_MASK_TARGET);
    private static final RenderPipeline VISIBLE_MASK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_visible_mask"))
            .withVertexShader(MASK_VERTEX_SHADER)
            .withFragmentShader(MASK_FRAGMENT_SHADER)
            .withSampler("Sampler0")
            .withSampler("MaskSampler")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, 0.0F, 0.0F))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline THROUGH_MASK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_through_mask"))
            .withVertexShader(MASK_VERTEX_SHADER)
            .withFragmentShader(MASK_FRAGMENT_SHADER)
            .withSampler("Sampler0")
            .withSampler("MaskSampler")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, 0.0F, 0.0F))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline GEOMETRY_MASK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_geometry_mask"))
            .withVertexShader(MASK_VERTEX_SHADER)
            .withFragmentShader(GEOMETRY_MASK_SHADER)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, 0.0F, 0.0F))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline MODEL_COMPOSITE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/item_outline_model_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(MODEL_COMPOSITE_SHADER)
            .withSampler("ExpandedSampler")
            .withSampler("MaskSampler")
            .withSampler("SceneDepthSampler")
            .withUniform("OutlineConfig", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();
    private static final RenderType EXPANDED_GEOMETRY_MASK_TYPE = RenderType.create(
            NoblePhantasms.MOD_ID + "_item_outline_expanded_texel_geometry_mask",
            RenderSetup.builder(GEOMETRY_MASK_PIPELINE)
                    .setOutputTarget(EXPANDED_MASK_OUTPUT)
                    .createRenderSetup());
    private static final RenderType INNER_GEOMETRY_MASK_TYPE = RenderType.create(
            NoblePhantasms.MOD_ID + "_item_outline_inner_texel_geometry_mask",
            RenderSetup.builder(GEOMETRY_MASK_PIPELINE)
                    .setOutputTarget(MASK_OUTPUT)
                    .createRenderSetup());
    private static final Map<MaskTextures, RenderType> VISIBLE_MASK_TYPES = new java.util.HashMap<>();
    private static final Map<MaskTextures, RenderType> THROUGH_MASK_TYPES = new java.util.HashMap<>();
    private static final Map<MaskTextures, RenderType> EXPANDED_MASK_TYPES = new java.util.HashMap<>();
    private static final Map<TexelModel, TexelShape> TEXEL_SHAPES = new java.util.HashMap<>();
    private static final Map<Item, Registration> REGISTRATIONS = new IdentityHashMap<>();
    private static final Map<ItemStackRenderState, Outline> RENDER_STATES = new WeakHashMap<>();
    private static final Map<SubmitNodeStorage.ItemSubmit, Outline> SUBMITS = new IdentityHashMap<>();
    private static final Map<Region, Map<BakedQuad, List<BakedQuad>>> REGION_QUADS = new java.util.HashMap<>();
    private static final List<PendingOutline> PENDING_OUTLINES = new ArrayList<>();
    private static final ThreadLocal<Outline> SUBMITTING = new ThreadLocal<>();
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

    public static Identifier glowTexture(Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        return Identifier.fromNamespaceAndPath(itemId.getNamespace(), GLOW_TEXTURE_FOLDER + itemId.getPath());
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
        EXPANDED_MASK_TYPES.clear();
        TEXEL_SHAPES.clear();
        hasProjection = false;
        hasOcclusionDepth = false;
        SUBMITTING.remove();
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(VISIBLE_MASK_PIPELINE);
        event.registerPipeline(THROUGH_MASK_PIPELINE);
        event.registerPipeline(GEOMETRY_MASK_PIPELINE);
        event.registerPipeline(MODEL_COMPOSITE_PIPELINE);
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
        LocalBounds modelBounds = localBounds(submit.quads());
        if (outline.previousMask() != null && outline.maskProgress() < 1.0F) {
            renderMask(bufferSource, submit, outline, outlined, modelBounds,
                    outline.previousMask(), 1.0F - outline.maskProgress());
        }
        if (outline.previousMask() == null || outline.maskProgress() > 0.0F) {
            renderMask(bufferSource, submit, outline, outlined, modelBounds,
                    outline.mask(), outline.previousMask() == null ? 1.0F : outline.maskProgress());
        }
    }

    private static void renderMask(MultiBufferSource.BufferSource bufferSource,
                                   SubmitNodeStorage.ItemSubmit submit, Outline outline,
                                   List<BakedQuad> outlined, LocalBounds modelBounds,
                                   @Nullable Identifier mask, float alphaScale) {
        if (alphaScale <= 0.0F) {
            return;
        }
        TextureAtlasSprite maskSprite = mask == null ? null : maskSprite(mask);
        TexelModel texelModel = maskSprite == null
                ? null
                : new TexelModel(maskSprite, spritePlane(submit.quads(), modelBounds), modelBounds, outline.region());
        renderLayers(bufferSource, submit, submit.quads(), outlined, maskSprite, texelModel,
                scaleAlpha(outline.layers(), alphaScale), outline.visibleThroughObjects());
    }

    private static List<GlowLayer> scaleAlpha(List<GlowLayer> layers, float alphaScale) {
        return layers.stream()
                .map(layer -> new GlowLayer(layer.color(), layer.alpha() * alphaScale, layer.thickness()))
                .toList();
    }

    private static void renderLayers(MultiBufferSource.BufferSource bufferSource, SubmitNodeStorage.ItemSubmit submit,
                                     List<BakedQuad> modelQuads, List<BakedQuad> outlined,
                                     @Nullable TextureAtlasSprite maskSprite,
                                     @Nullable TexelModel texelModel, List<GlowLayer> layers,
                                     boolean visibleThroughObjects) {
        if (outlined.isEmpty() || layers.isEmpty()) {
            return;
        }
        float[] innerBoundaries = new float[layers.size()];
        float[] outerBoundaries = new float[layers.size()];
        float maximumThickness = 0.0F;
        for (int layerIndex = layers.size() - 1; layerIndex >= 0; layerIndex--) {
            innerBoundaries[layerIndex] = maximumThickness;
            maximumThickness += layers.get(layerIndex).thickness();
            outerBoundaries[layerIndex] = maximumThickness;
        }
        TexelShape texelShape = texelModel == null
                ? null
                : texelShape(texelModel);
        if (texelShape != null && texelShape.texels().isEmpty()) {
            return;
        }
        TexelGeometry[] layerGeometries = texelShape == null
                ? null
                : new TexelGeometry[layers.size()];
        if (layerGeometries != null) {
            for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
                layerGeometries[layerIndex] = createTexelGeometry(
                        texelShape, outerBoundaries[layerIndex]);
            }
        }
        bufferSource.endBatch();
        ensureTargets();
        Minecraft minecraft = Minecraft.getInstance();
        var mainTarget = minecraft.getMainRenderTarget();
        var sceneDepthTarget = hasOcclusionDepth ? OCCLUSION_DEPTH_TARGET : mainTarget;
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        LocalBounds localBounds = localBounds(outlined);
        Vector3f center = localBounds.center();
        float texelSize = modelTexelSize(outlined, localBounds);
        float maximumExpansion = texelSize * maximumThickness;
        ScreenBounds compositeBounds = submit.displayContext().firstPerson()
                ? ScreenBounds.full()
                : layerGeometries == null
                ? expandedScreenBounds(submit, outlined, center, maximumExpansion, 2.0F)
                : expandedLocalBoundsScreenBounds(
                        submit, layerGeometries[0].bounds(), new Vector3f(), 2.0F);
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            GlowLayer layer = layers.get(layerIndex);
            encoder.clearColorTexture(MASK_TARGET.getColorTexture(), 0);
            encoder.clearDepthTexture(MASK_TARGET.getDepthTexture(), 1.0D);
            renderQuads(bufferSource, submit.pose(), modelQuads, null, visibleThroughObjects);
            float innerThickness = innerBoundaries[layerIndex];
            if (innerThickness > 0.0F) {
                if (layerGeometries == null) {
                    renderExpandedQuads(bufferSource, submit.pose(), outlined, maskSprite, center,
                            texelSize * innerThickness, true);
                } else {
                    renderTexelGeometry(bufferSource, submit.pose(), layerGeometries[layerIndex + 1],
                            INNER_GEOMETRY_MASK_TYPE);
                }
            }
            encoder.clearColorTexture(EXPANDED_MASK_TARGET.getColorTexture(), 0);
            encoder.clearDepthTexture(EXPANDED_MASK_TARGET.getDepthTexture(), 1.0D);
            float outerThickness = outerBoundaries[layerIndex];
            if (layerGeometries == null) {
                renderExpandedQuads(bufferSource, submit.pose(), outlined, maskSprite, center,
                        texelSize * outerThickness, false);
            } else {
                renderTexelGeometry(bufferSource, submit.pose(), layerGeometries[layerIndex],
                        EXPANDED_GEOMETRY_MASK_TYPE);
            }
            writeModelConfig(encoder, layer, visibleThroughObjects);
            renderModelComposite(encoder, mainTarget.getColorTextureView(),
                    sceneDepthTarget.getDepthTextureView(), compositeBounds);
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
            OCCLUSION_DEPTH_TARGET = new TextureTarget(
                    "Noble Phantasms item outline occlusion depth", mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
            EXPANDED_MASK_TARGET = new TextureTarget(
                    "Noble Phantasms item expanded outline mask", mainTarget.width, mainTarget.height, true, mainTarget.useStencil);
            configBuffer = new MappableRingBuffer(
                    () -> "Noble Phantasms item outline config",
                    com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_WRITE | com.mojang.blaze3d.buffers.GpuBuffer.USAGE_UNIFORM,
                    CONFIG_UBO_SIZE);
            return;
        }
        if (MASK_TARGET.width != mainTarget.width || MASK_TARGET.height != mainTarget.height) {
            MASK_TARGET.resize(mainTarget.width, mainTarget.height);
            OCCLUSION_DEPTH_TARGET.resize(mainTarget.width, mainTarget.height);
            EXPANDED_MASK_TARGET.resize(mainTarget.width, mainTarget.height);
        }
    }

    private static void renderQuads(MultiBufferSource.BufferSource bufferSource,
                                    com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                    List<BakedQuad> quads, @Nullable TextureAtlasSprite maskSprite,
                                    boolean visibleThroughObjects) {
        Map<RenderType, VertexConsumer> buffers = new IdentityHashMap<>();
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sourceSprite = quad.materialInfo().sprite();
            Identifier sourceAtlas = sourceSprite.atlasLocation();
            Identifier maskAtlas = maskSprite == null ? sourceAtlas : maskSprite.atlasLocation();
            RenderType renderType = renderType(sourceAtlas, maskAtlas, visibleThroughObjects);
            VertexConsumer vertexConsumer = buffers.computeIfAbsent(renderType, bufferSource::getBuffer);
            putMaskedQuad(vertexConsumer, pose, quad, sourceSprite, maskSprite);
        }
        buffers.keySet().forEach(bufferSource::endBatch);
    }

    private static void renderExpandedQuads(MultiBufferSource.BufferSource bufferSource,
                                            com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                            List<BakedQuad> quads,
                                            @Nullable TextureAtlasSprite maskSprite,
                                            Vector3f center,
                                            float expansion,
                                            boolean innerMask) {
        Map<RenderType, VertexConsumer> buffers = new IdentityHashMap<>();
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sourceSprite = quad.materialInfo().sprite();
            Identifier sourceAtlas = sourceSprite.atlasLocation();
            Identifier maskAtlas = maskSprite == null ? sourceAtlas : maskSprite.atlasLocation();
            RenderType renderType = innerMask
                    ? renderType(sourceAtlas, maskAtlas, false)
                    : expandedRenderType(sourceAtlas, maskAtlas);
            VertexConsumer vertexConsumer = buffers.computeIfAbsent(renderType, bufferSource::getBuffer);
            putExpandedMaskedQuad(vertexConsumer, pose, quad, sourceSprite, maskSprite, center, expansion);
        }
        buffers.keySet().forEach(bufferSource::endBatch);
    }

    private static void renderTexelGeometry(MultiBufferSource.BufferSource bufferSource,
                                            com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                            TexelGeometry geometry,
                                            RenderType renderType) {
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        for (TexelCuboid cuboid : geometry.cuboids()) {
            putTexelCuboid(consumer, pose, cuboid);
        }
        bufferSource.endBatch(renderType);
    }

    private static void putTexelCuboid(VertexConsumer consumer,
                                       com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                       TexelCuboid cuboid) {
        int color = ARGB.color(cuboid.alpha(), 0xFFFFFF);
        float minX = cuboid.minX();
        float minY = cuboid.minY();
        float minZ = cuboid.minZ();
        float maxX = cuboid.maxX();
        float maxY = cuboid.maxY();
        float maxZ = cuboid.maxZ();
        putGeometryFace(consumer, pose,
                minX, minY, maxZ, maxX, minY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, color);
        putGeometryFace(consumer, pose,
                maxX, minY, minZ, minX, minY, minZ,
                minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, color);
        if (cuboid.minXFace()) {
            putGeometryFace(consumer, pose,
                    minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, color);
        }
        if (cuboid.maxXFace()) {
            putGeometryFace(consumer, pose,
                    maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, color);
        }
        if (cuboid.minYFace()) {
            putGeometryFace(consumer, pose,
                    minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, color);
        }
        if (cuboid.maxYFace()) {
            putGeometryFace(consumer, pose,
                    minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, color);
        }
    }

    private static void putGeometryFace(VertexConsumer consumer,
                                        com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                        float x0, float y0, float z0,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float normalX, float normalY, float normalZ,
                                        int color) {
        Vector3f normal = pose.transformNormal(new Vector3f(normalX, normalY, normalZ), new Vector3f());
        putGeometryVertex(consumer, pose, x0, y0, z0, normal, color);
        putGeometryVertex(consumer, pose, x1, y1, z1, normal, color);
        putGeometryVertex(consumer, pose, x2, y2, z2, normal, color);
        putGeometryVertex(consumer, pose, x3, y3, z3, normal, color);
    }

    private static void putGeometryVertex(VertexConsumer consumer,
                                          com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                          float x, float y, float z,
                                          Vector3f normal,
                                          int color) {
        Vector3f position = pose.pose().transformPosition(x, y, z, new Vector3f());
        consumer.addVertex(position.x(), position.y(), position.z())
                .setColor(color)
                .setUv(0.0F, 0.0F)
                .setUv1(0, 0)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(normal.x(), normal.y(), normal.z());
    }

    private static void putExpandedMaskedQuad(VertexConsumer consumer,
                                              com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                              BakedQuad quad,
                                              TextureAtlasSprite sourceSprite,
                                              @Nullable TextureAtlasSprite maskSprite,
                                              Vector3f center,
                                              float expansion) {
        Vector3f normal = pose.transformNormal(quad.direction().getUnitVec3f(), new Vector3f());
        for (int index = 0; index < 4; index++) {
            var localPosition = quad.position(index);
            Vector3f expandedPosition = new Vector3f(
                    localPosition.x() + directionFromCenter(localPosition.x(), center.x()) * expansion,
                    localPosition.y() + directionFromCenter(localPosition.y(), center.y()) * expansion,
                    localPosition.z() + directionFromCenter(localPosition.z(), center.z()) * expansion);
            Vector3f position = pose.pose().transformPosition(expandedPosition, new Vector3f());
            float u = UVPair.unpackU(quad.packedUV(index));
            float v = UVPair.unpackV(quad.packedUV(index));
            float maskU = maskSprite == null ? u : moveUv(u, sourceSprite.getU0(), sourceSprite.getU1(),
                    maskSprite.getU0(), maskSprite.getU1());
            float maskV = maskSprite == null ? v : moveUv(v, sourceSprite.getV0(), sourceSprite.getV1(),
                    maskSprite.getV0(), maskSprite.getV1());
            int color = ARGB.multiply(-1, quad.bakedColors().color(index));
            consumer.addVertex(position.x, position.y, position.z)
                    .setColor(color)
                    .setUv(u, v)
                    .setUv1(packMaskUv(maskU), packMaskUv(maskV))
                    .setLight(LightCoordsUtil.FULL_BRIGHT)
                    .setNormal(normal.x, normal.y, normal.z);
        }
    }

    private static void putMaskedQuad(VertexConsumer consumer, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                                      BakedQuad quad, TextureAtlasSprite sourceSprite,
                                      @Nullable TextureAtlasSprite maskSprite) {
        Vector3f normal = pose.transformNormal(quad.direction().getUnitVec3f(), new Vector3f());
        for (int index = 0; index < 4; index++) {
            Vector3f position = pose.pose().transformPosition(quad.position(index), new Vector3f());
            float u = UVPair.unpackU(quad.packedUV(index));
            float v = UVPair.unpackV(quad.packedUV(index));
            float maskU = maskSprite == null ? u : moveUv(u, sourceSprite.getU0(), sourceSprite.getU1(),
                    maskSprite.getU0(), maskSprite.getU1());
            float maskV = maskSprite == null ? v : moveUv(v, sourceSprite.getV0(), sourceSprite.getV1(),
                    maskSprite.getV0(), maskSprite.getV1());
            int color = ARGB.multiply(-1, quad.bakedColors().color(index));
            consumer.addVertex(position.x, position.y, position.z)
                    .setColor(color)
                    .setUv(u, v)
                    .setUv1(packMaskUv(maskU), packMaskUv(maskV))
                    .setLight(LightCoordsUtil.FULL_BRIGHT)
                    .setNormal(normal.x, normal.y, normal.z);
        }
    }

    private static float moveUv(float value, float sourceMin, float sourceMax, float targetMin, float targetMax) {
        float sourceSize = sourceMax - sourceMin;
        float progress = Math.abs(sourceSize) < 1.0E-7F ? 0.0F : (value - sourceMin) / sourceSize;
        return Mth.lerp(progress, targetMin, targetMax);
    }

    private static float directionFromCenter(float coordinate, float center) {
        return coordinate < center ? -1.0F : coordinate > center ? 1.0F : 0.0F;
    }

    private static int packMaskUv(float value) {
        return Math.round(Mth.clamp(value, 0.0F, 1.0F) * MASK_UV_SCALE);
    }

    private static void writeModelConfig(CommandEncoder encoder, GlowLayer layer, boolean visibleThroughObjects) {
        try (var mapped = encoder.mapBuffer(configBuffer.currentBuffer(), false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(mapped.data());
            builder.putVec4(
                    ARGB.redFloat(layer.color()),
                    ARGB.greenFloat(layer.color()),
                    ARGB.blueFloat(layer.color()),
                    1.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(layer.alpha(), 0.0F, 0.0F, 0.0F);
            builder.putVec4(visibleThroughObjects ? 1.0F : 0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void renderModelComposite(CommandEncoder encoder,
                                             com.mojang.blaze3d.textures.GpuTextureView output,
                                             com.mojang.blaze3d.textures.GpuTextureView sceneDepth,
                                             ScreenBounds bounds) {
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST);
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Noble Phantasms item model outline composite", output, OptionalInt.empty())) {
            renderPass.setPipeline(MODEL_COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("ExpandedSampler", EXPANDED_MASK_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("MaskSampler", MASK_TARGET.getColorTextureView(), sampler);
            renderPass.bindTexture("SceneDepthSampler", sceneDepth, sampler);
            renderPass.setUniform("OutlineConfig", configBuffer.currentBuffer());
            bounds.enable(renderPass);
            renderPass.draw(0, 3);
        }
    }

    private static float modelTexelSize(List<BakedQuad> quads, LocalBounds bounds) {
        List<Float> candidates = new ArrayList<>();
        int maximumResolution = 1;
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            maximumResolution = Math.max(maximumResolution,
                    Math.max(sprite.contents().width(), sprite.contents().height()));
            float uSpan = Math.abs(sprite.getU1() - sprite.getU0());
            float vSpan = Math.abs(sprite.getV1() - sprite.getV0());
            if (uSpan <= 1.0E-7F || vSpan <= 1.0E-7F) {
                continue;
            }
            float uScale = sprite.contents().width() / uSpan;
            float vScale = sprite.contents().height() / vSpan;
            for (int index = 0; index < 4; index++) {
                int nextIndex = (index + 1) % 4;
                var first = quad.position(index);
                var second = quad.position(nextIndex);
                float modelDistance = first.distance(second);
                float deltaU = (UVPair.unpackU(quad.packedUV(nextIndex))
                        - UVPair.unpackU(quad.packedUV(index))) * uScale;
                float deltaV = (UVPair.unpackV(quad.packedUV(nextIndex))
                        - UVPair.unpackV(quad.packedUV(index))) * vScale;
                float textureDistance = Mth.sqrt(deltaU * deltaU + deltaV * deltaV);
                if (modelDistance > 1.0E-5F && textureDistance >= 0.5F) {
                    candidates.add(modelDistance / textureDistance);
                }
            }
        }
        if (!candidates.isEmpty()) {
            candidates.sort(Float::compare);
            return candidates.get(candidates.size() / 4);
        }
        float maximumSpan = Math.max(bounds.maxX() - bounds.minX(),
                Math.max(bounds.maxY() - bounds.minY(), bounds.maxZ() - bounds.minZ()));
        return maximumSpan / maximumResolution;
    }

    private static SpritePlane spritePlane(List<BakedQuad> quads, LocalBounds bounds) {
        BakedQuad selected = null;
        float selectedArea = 0.0F;
        for (BakedQuad quad : quads) {
            if (quad.direction() != Direction.SOUTH) {
                continue;
            }
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            float minU = Float.POSITIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY;
            float maxU = Float.NEGATIVE_INFINITY;
            float maxV = Float.NEGATIVE_INFINITY;
            for (int index = 0; index < 4; index++) {
                float u = normalizedU(sprite, UVPair.unpackU(quad.packedUV(index)));
                float v = normalizedV(sprite, UVPair.unpackV(quad.packedUV(index)));
                minU = Math.min(minU, u);
                minV = Math.min(minV, v);
                maxU = Math.max(maxU, u);
                maxV = Math.max(maxV, v);
            }
            float area = (maxU - minU) * (maxV - minV);
            if (area > selectedArea) {
                selected = quad;
                selectedArea = area;
            }
        }
        if (selected == null) {
            return new SpritePlane(
                    new Vector3f(bounds.minX(), bounds.maxY(), (bounds.minZ() + bounds.maxZ()) * 0.5F),
                    new Vector3f(bounds.maxX() - bounds.minX(), 0.0F, 0.0F),
                    new Vector3f(0.0F, bounds.minY() - bounds.maxY(), 0.0F));
        }
        TextureAtlasSprite sprite = selected.materialInfo().sprite();
        for (int firstIndex = 0; firstIndex < 4; firstIndex++) {
            int secondIndex = (firstIndex + 1) % 4;
            int thirdIndex = (firstIndex + 2) % 4;
            float firstU = normalizedU(sprite, UVPair.unpackU(selected.packedUV(firstIndex)));
            float firstV = normalizedV(sprite, UVPair.unpackV(selected.packedUV(firstIndex)));
            float secondU = normalizedU(sprite, UVPair.unpackU(selected.packedUV(secondIndex)));
            float secondV = normalizedV(sprite, UVPair.unpackV(selected.packedUV(secondIndex)));
            float thirdU = normalizedU(sprite, UVPair.unpackU(selected.packedUV(thirdIndex)));
            float thirdV = normalizedV(sprite, UVPair.unpackV(selected.packedUV(thirdIndex)));
            float deltaU1 = secondU - firstU;
            float deltaV1 = secondV - firstV;
            float deltaU2 = thirdU - firstU;
            float deltaV2 = thirdV - firstV;
            float determinant = deltaU1 * deltaV2 - deltaU2 * deltaV1;
            if (Math.abs(determinant) <= 1.0E-6F) {
                continue;
            }
            Vector3f firstPosition = new Vector3f(selected.position(firstIndex));
            Vector3f firstDelta = new Vector3f(selected.position(secondIndex)).sub(firstPosition);
            Vector3f secondDelta = new Vector3f(selected.position(thirdIndex)).sub(firstPosition);
            Vector3f uAxis = new Vector3f(firstDelta).mul(deltaV2)
                    .sub(new Vector3f(secondDelta).mul(deltaV1))
                    .div(determinant);
            Vector3f vAxis = new Vector3f(secondDelta).mul(deltaU1)
                    .sub(new Vector3f(firstDelta).mul(deltaU2))
                    .div(determinant);
            Vector3f origin = new Vector3f(firstPosition)
                    .sub(new Vector3f(uAxis).mul(firstU))
                    .sub(new Vector3f(vAxis).mul(firstV));
            return new SpritePlane(origin, uAxis, vAxis);
        }
        return new SpritePlane(
                new Vector3f(bounds.minX(), bounds.maxY(), (bounds.minZ() + bounds.maxZ()) * 0.5F),
                new Vector3f(bounds.maxX() - bounds.minX(), 0.0F, 0.0F),
                new Vector3f(0.0F, bounds.minY() - bounds.maxY(), 0.0F));
    }

    private static float normalizedU(TextureAtlasSprite sprite, float u) {
        return (u - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
    }

    private static float normalizedV(TextureAtlasSprite sprite, float v) {
        return (v - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
    }

    private static TexelShape texelShape(TexelModel model) {
        return TEXEL_SHAPES.computeIfAbsent(model, ItemOutlineRenderer::createTexelShape);
    }

    private static TexelShape createTexelShape(TexelModel model) {
        TextureAtlasSprite sprite = model.sprite();
        SpritePlane spritePlane = model.spritePlane();
        var contents = sprite.contents();
        int width = contents.width();
        int height = contents.height();
        List<SourceTexel> texels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Vector3f center = spritePlane.position((x + 0.5F) / width, (y + 0.5F) / height);
                if (model.region() == null || model.region().contains(center.x(), center.y(), center.z())) {
                    float alpha = texelAlpha(sprite, x, y);
                    if (alpha > 0.0F) {
                        texels.add(new SourceTexel(x, y, alpha));
                    }
                }
            }
        }
        float texelZ = Math.min(spritePlane.uAxis().length() / width,
                spritePlane.vAxis().length() / height);
        return new TexelShape(List.copyOf(texels), width, height, spritePlane, model.modelBounds(), texelZ);
    }

    private static TexelGeometry createTexelGeometry(TexelShape shape, float thickness) {
        float[] xCoordinates = geometryCoordinates(shape.texels(), thickness, true);
        float[] yCoordinates = geometryCoordinates(shape.texels(), thickness, false);
        int gridWidth = Math.max(0, xCoordinates.length - 1);
        int gridHeight = Math.max(0, yCoordinates.length - 1);
        float[] alpha = new float[gridWidth * gridHeight];
        for (SourceTexel texel : shape.texels()) {
            int minX = coordinateIndex(xCoordinates, texel.x() - thickness);
            int maxX = coordinateIndex(xCoordinates, texel.x() + 1.0F + thickness);
            int minY = coordinateIndex(yCoordinates, texel.y() - thickness);
            int maxY = coordinateIndex(yCoordinates, texel.y() + 1.0F + thickness);
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    int index = y * gridWidth + x;
                    alpha[index] = Math.max(alpha[index], texel.alpha());
                }
            }
        }

        List<TexelCuboid> cuboids = new ArrayList<>();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        boolean uIncreasesX = shape.spritePlane().uAxis().x() >= 0.0F;
        boolean vIncreasesY = shape.spritePlane().vAxis().y() >= 0.0F;
        float cuboidMinZ = shape.modelBounds().minZ() - shape.texelZ() * thickness;
        float cuboidMaxZ = shape.modelBounds().maxZ() + shape.texelZ() * thickness;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int index = y * gridWidth + x;
                float cellAlpha = alpha[index];
                if (cellAlpha <= 0.0F) {
                    continue;
                }
                boolean uMinusFace = x == 0 || alpha[index - 1] <= 0.0F;
                boolean uPlusFace = x == gridWidth - 1 || alpha[index + 1] <= 0.0F;
                boolean vMinusFace = y == 0 || alpha[index - gridWidth] <= 0.0F;
                boolean vPlusFace = y == gridHeight - 1 || alpha[index + gridWidth] <= 0.0F;
                Vector3f firstCorner = shape.spritePlane().position(
                        xCoordinates[x] / shape.width(), yCoordinates[y] / shape.height());
                Vector3f secondCorner = shape.spritePlane().position(
                        xCoordinates[x + 1] / shape.width(), yCoordinates[y + 1] / shape.height());
                float cellMinX = Math.min(firstCorner.x(), secondCorner.x());
                float cellMinY = Math.min(firstCorner.y(), secondCorner.y());
                float cellMaxX = Math.max(firstCorner.x(), secondCorner.x());
                float cellMaxY = Math.max(firstCorner.y(), secondCorner.y());
                if (cellMaxX - cellMinX <= 1.0E-6F
                        || cellMaxY - cellMinY <= 1.0E-6F
                        || cuboidMaxZ - cuboidMinZ <= 1.0E-6F) {
                    continue;
                }
                cuboids.add(new TexelCuboid(
                        cellMinX, cellMinY, cuboidMinZ,
                        cellMaxX, cellMaxY, cuboidMaxZ, cellAlpha,
                        uIncreasesX ? uMinusFace : uPlusFace,
                        uIncreasesX ? uPlusFace : uMinusFace,
                        vIncreasesY ? vMinusFace : vPlusFace,
                        vIncreasesY ? vPlusFace : vMinusFace));
                minX = Math.min(minX, cellMinX);
                minY = Math.min(minY, cellMinY);
                minZ = Math.min(minZ, cuboidMinZ);
                maxX = Math.max(maxX, cellMaxX);
                maxY = Math.max(maxY, cellMaxY);
                maxZ = Math.max(maxZ, cuboidMaxZ);
            }
        }
        cuboids.sort((first, second) -> Float.compare(first.alpha(), second.alpha()));
        LocalBounds bounds = cuboids.isEmpty()
                ? shape.modelBounds()
                : new LocalBounds(minX, minY, minZ, maxX, maxY, maxZ);
        return new TexelGeometry(List.copyOf(cuboids), bounds);
    }

    private static float[] geometryCoordinates(List<SourceTexel> texels, float thickness, boolean horizontal) {
        float[] coordinates = new float[texels.size() * 2];
        int coordinateIndex = 0;
        for (SourceTexel texel : texels) {
            float coordinate = horizontal ? texel.x() : texel.y();
            coordinates[coordinateIndex++] = coordinate - thickness;
            coordinates[coordinateIndex++] = coordinate + 1.0F + thickness;
        }
        java.util.Arrays.sort(coordinates);
        int uniqueCount = 0;
        for (float coordinate : coordinates) {
            if (uniqueCount == 0 || Math.abs(coordinate - coordinates[uniqueCount - 1]) > 1.0E-5F) {
                coordinates[uniqueCount++] = coordinate;
            }
        }
        return java.util.Arrays.copyOf(coordinates, uniqueCount);
    }

    private static int coordinateIndex(float[] coordinates, float coordinate) {
        int index = java.util.Arrays.binarySearch(coordinates, coordinate);
        if (index >= 0) {
            return index;
        }
        int insertion = -index - 1;
        if (insertion < coordinates.length
                && Math.abs(coordinates[insertion] - coordinate) <= 1.0E-5F) {
            return insertion;
        }
        return Math.max(0, insertion - 1);
    }

    private static float texelAlpha(TextureAtlasSprite sprite, int x, int y) {
        int alpha = 0;
        for (int frame : sprite.contents().getUniqueFrames()) {
            alpha = Math.max(alpha, ARGB.alpha(sprite.getPixelRGBA(frame, x, y)));
        }
        return alpha / 255.0F;
    }

    private static ScreenBounds expandedScreenBounds(SubmitNodeStorage.ItemSubmit submit, List<BakedQuad> quads,
                                                     Vector3f center, float expansion, float margin) {
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
                float x = position.x() + directionFromCenter(position.x(), center.x()) * expansion;
                float y = position.y() + directionFromCenter(position.y(), center.y()) * expansion;
                float z = position.z() + directionFromCenter(position.z(), center.z()) * expansion;
                Vector4f projected = project(modelToClip, x, y, z);
                if (projected == null) {
                    continue;
                }
                float screenX = (projected.x * 0.5F + 0.5F) * MASK_TARGET.width;
                float screenY = (projected.y * 0.5F + 0.5F) * MASK_TARGET.height;
                minX = Math.min(minX, screenX);
                minY = Math.min(minY, screenY);
                maxX = Math.max(maxX, screenX);
                maxY = Math.max(maxY, screenY);
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

    private static ScreenBounds expandedLocalBoundsScreenBounds(SubmitNodeStorage.ItemSubmit submit,
                                                                LocalBounds bounds,
                                                                Vector3f expansion,
                                                                float margin) {
        if (!hasProjection) {
            return ScreenBounds.full();
        }
        Matrix4f modelToClip = modelToClip(submit);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float[] xCoordinates = {bounds.minX() - expansion.x(), bounds.maxX() + expansion.x()};
        float[] yCoordinates = {bounds.minY() - expansion.y(), bounds.maxY() + expansion.y()};
        float[] zCoordinates = {bounds.minZ() - expansion.z(), bounds.maxZ() + expansion.z()};
        for (float x : xCoordinates) {
            for (float y : yCoordinates) {
                for (float z : zCoordinates) {
                    Vector4f projected = project(modelToClip, x, y, z);
                    if (projected == null) {
                        continue;
                    }
                    float screenX = (projected.x * 0.5F + 0.5F) * MASK_TARGET.width;
                    float screenY = (projected.y * 0.5F + 0.5F) * MASK_TARGET.height;
                    minX = Math.min(minX, screenX);
                    minY = Math.min(minY, screenY);
                    maxX = Math.max(maxX, screenX);
                    maxY = Math.max(maxY, screenY);
                }
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

    private static TextureAtlasSprite maskSprite(Identifier mask) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        return atlas.getSprite(mask);
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

    private static RenderType renderType(Identifier atlas, Identifier maskAtlas, boolean visibleThroughObjects) {
        Map<MaskTextures, RenderType> types = visibleThroughObjects ? THROUGH_MASK_TYPES : VISIBLE_MASK_TYPES;
        RenderPipeline pipeline = visibleThroughObjects ? THROUGH_MASK_PIPELINE : VISIBLE_MASK_PIPELINE;
        return types.computeIfAbsent(new MaskTextures(atlas, maskAtlas), textures -> RenderType.create(
                NoblePhantasms.MOD_ID + "_item_outline_mask_" + (visibleThroughObjects ? "through" : "visible"),
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", textures.sourceAtlas())
                        .withTexture("MaskSampler", textures.maskAtlas())
                        .setOutputTarget(MASK_OUTPUT)
                        .createRenderSetup()));
    }

    private static RenderType expandedRenderType(Identifier atlas, Identifier maskAtlas) {
        return EXPANDED_MASK_TYPES.computeIfAbsent(new MaskTextures(atlas, maskAtlas), textures -> RenderType.create(
                NoblePhantasms.MOD_ID + "_item_outline_expanded_mask",
                RenderSetup.builder(VISIBLE_MASK_PIPELINE)
                        .withTexture("Sampler0", textures.sourceAtlas())
                        .withTexture("MaskSampler", textures.maskAtlas())
                        .setOutputTarget(EXPANDED_MASK_OUTPUT)
                        .createRenderSetup()));
    }

    private static void destroyTargets() {
        if (MASK_TARGET != null) {
            MASK_TARGET.destroyBuffers();
            MASK_TARGET = null;
        }
        if (OCCLUSION_DEPTH_TARGET != null) {
            OCCLUSION_DEPTH_TARGET.destroyBuffers();
            OCCLUSION_DEPTH_TARGET = null;
        }
        if (EXPANDED_MASK_TARGET != null) {
            EXPANDED_MASK_TARGET.destroyBuffers();
            EXPANDED_MASK_TARGET = null;
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

    private record MaskTextures(Identifier sourceAtlas, Identifier maskAtlas) {
    }

    private record TexelModel(TextureAtlasSprite sprite, SpritePlane spritePlane,
                              LocalBounds modelBounds, @Nullable Region region) {
    }

    private record SourceTexel(int x, int y, float alpha) {
    }

    private record TexelShape(List<SourceTexel> texels, int width, int height,
                              SpritePlane spritePlane, LocalBounds modelBounds, float texelZ) {
    }

    private record TexelGeometry(List<TexelCuboid> cuboids, LocalBounds bounds) {
    }

    private record SpritePlane(Vector3f origin, Vector3f uAxis, Vector3f vAxis) {
        private Vector3f position(float u, float v) {
            return new Vector3f(origin)
                    .fma(u, uAxis)
                    .fma(v, vAxis);
        }
    }

    private record TexelCuboid(float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ, float alpha,
                               boolean minXFace, boolean maxXFace,
                               boolean minYFace, boolean maxYFace) {
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
            return contains(vertex.x(), vertex.y(), vertex.z());
        }

        private boolean contains(float x, float y, float z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
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
                          boolean visibleThroughObjects, @Nullable Identifier previousMask,
                          float maskProgress) {
        public Outline(@Nullable Region region, @Nullable Identifier mask, List<GlowLayer> layers,
                       boolean visibleThroughObjects) {
            this(region, mask, layers, visibleThroughObjects, null, 1.0F);
        }

        public Outline {
            layers = List.copyOf(layers);
            maskProgress = Mth.clamp(maskProgress, 0.0F, 1.0F);
        }

        public Outline mask(Identifier mask) {
            return new Outline(region, mask, layers, visibleThroughObjects, previousMask, maskProgress);
        }

        public Outline mask(Item item) {
            return mask(glowTexture(item));
        }

        public Outline region(Region region) {
            return new Outline(region, mask, layers, visibleThroughObjects, previousMask, maskProgress);
        }

        public Outline visibleThroughObjects(boolean visibleThroughObjects) {
            return new Outline(region, mask, layers, visibleThroughObjects, previousMask, maskProgress);
        }

        public Outline transitionFrom(Identifier previousMask, float progress) {
            if (previousMask.equals(mask)) {
                return this;
            }
            return new Outline(region, mask, layers, visibleThroughObjects, previousMask, progress);
        }

        private boolean hasVisibleLayers() {
            return layers.stream().anyMatch(GlowLayer::visible);
        }

        private Outline normalized() {
            return new Outline(region, mask, layers.stream()
                    .map(GlowLayer::normalized)
                    .filter(GlowLayer::visible)
                    .toList(), visibleThroughObjects, previousMask, maskProgress);
        }
    }
}
