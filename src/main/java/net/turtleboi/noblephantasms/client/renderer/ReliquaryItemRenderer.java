package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import org.joml.Vector3f;

/** Renders the held relic model into the Mythical Reliquary's model-viewer pane. */
public final class ReliquaryItemRenderer extends PictureInPictureRenderer<ReliquaryItemRenderState> {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/reliquary_sepia");
    private static final RenderPipeline SEPIA_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(
                    NoblePhantasms.MOD_ID, "pipeline/reliquary_sepia"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final Function<Identifier, RenderType> SEPIA_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_reliquary_sepia",
            RenderSetup.builder(SEPIA_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload()
                    .createRenderSetup()));
    private static final ThreadLocal<Boolean> RESOLVING_PREVIEW = new ThreadLocal<>();

    public ReliquaryItemRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SEPIA_PIPELINE);
    }

    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(ReliquaryItemRenderState.class, ReliquaryItemRenderer::new);
    }

    public static TrackingItemStackRenderState resolveHeldModel(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        TrackingItemStackRenderState state = new TrackingItemStackRenderState();
        RESOLVING_PREVIEW.set(true);
        try {
            minecraft.getItemModelResolver().updateForTopItem(
                    state, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    minecraft.level, null, stack.getItem().hashCode());
        } finally {
            RESOLVING_PREVIEW.remove();
        }
        return state;
    }

    public static boolean isResolvingPreview() {
        return Boolean.TRUE.equals(RESOLVING_PREVIEW.get());
    }

    @Override
    public Class<ReliquaryItemRenderState> getRenderStateClass() {
        return ReliquaryItemRenderState.class;
    }

    @Override
    protected void renderToTexture(ReliquaryItemRenderState state, PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(
                state.item().usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        // Match Minecraft's normal GUI item orientation before applying the model-viewer camera.
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.mulPose(state.rotation());
        Vector3f center = state.modelCenter();
        poseStack.translate(-center.x, -center.y, -center.z);

        FeatureRenderDispatcher dispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage storage = dispatcher.getSubmitNodeStorage();
        state.item().submit(poseStack, sepiaCollector(storage), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
    }

    private static SubmitNodeCollector sepiaCollector(SubmitNodeCollector delegate) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("submitItem") && arguments != null && arguments.length >= 8) {
                        PoseStack itemPose = (PoseStack) arguments[0];
                        int overlay = (int) arguments[3];
                        int[] tintLayers = (int[]) arguments[5];
                        @SuppressWarnings("unchecked")
                        List<BakedQuad> quads = (List<BakedQuad>) arguments[6];
                        Map<Identifier, List<BakedQuad>> quadsByAtlas = new LinkedHashMap<>();
                        for (BakedQuad quad : quads) {
                            quadsByAtlas.computeIfAbsent(
                                    quad.materialInfo().sprite().atlasLocation(), ignored -> new ArrayList<>())
                                    .add(quad);
                        }
                        for (Map.Entry<Identifier, List<BakedQuad>> entry : quadsByAtlas.entrySet()) {
                            List<BakedQuad> atlasQuads = List.copyOf(entry.getValue());
                            delegate.submitCustomGeometry(itemPose, SEPIA_TYPES.apply(entry.getKey()),
                                    (pose, buffer) -> renderSepiaQuads(
                                            pose, buffer, atlasQuads, tintLayers, overlay));
                        }
                        return null;
                    }
                    return invoke(method, delegate, arguments);
                });
    }

    private static void renderSepiaQuads(PoseStack.Pose pose, VertexConsumer buffer,
                                          List<BakedQuad> quads, int[] tintLayers, int overlay) {
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
        instance.setOverlayCoords(overlay);
        for (BakedQuad quad : quads) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            int color = material.isTinted()
                    && material.tintIndex() >= 0
                    && material.tintIndex() < tintLayers.length
                    ? tintLayers[material.tintIndex()] : -1;
            instance.setColor(color);
            buffer.putBakedQuad(pose, quad, instance);
        }
    }

    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "mythical_reliquary_item";
    }
}
