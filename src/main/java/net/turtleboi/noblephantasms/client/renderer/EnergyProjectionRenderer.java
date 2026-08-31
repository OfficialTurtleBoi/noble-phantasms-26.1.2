package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import org.joml.Vector3f;

public final class EnergyProjectionRenderer {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/excalibur_energy");
    private static final RenderPipeline ENERGY_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/energy_projection"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline HIDDEN_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/energy_projection_hidden"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final Function<Identifier, RenderType> ENERGY_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_energy_projection",
            RenderSetup.builder(ENERGY_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload()
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> HIDDEN_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_energy_projection_hidden",
            RenderSetup.builder(HIDDEN_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .createRenderSetup()));
    private static final List<EnergySubmit> DEFERRED_SUBMITS = new ArrayList<>();

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ENERGY_PIPELINE);
        event.registerPipeline(HIDDEN_PIPELINE);
    }

    public static void beginFrame() {
        DEFERRED_SUBMITS.clear();
    }

    public static SubmitNodeCollector collector(SubmitNodeCollector delegate) {
        return collector(delegate, 1.0F, false);
    }

    public static SubmitNodeCollector collector(SubmitNodeCollector delegate, float alphaMultiplier) {
        return collector(delegate, alphaMultiplier, false);
    }

    public static SubmitNodeCollector deferredCollector(SubmitNodeCollector delegate,
                                                        float alphaMultiplier) {
        return collector(delegate, alphaMultiplier, true);
    }

    public static void renderDeferred(RenderLevelStageEvent.AfterLevel event) {
        if (DEFERRED_SUBMITS.isEmpty()) {
            return;
        }
        var modelViewStack = RenderSystem.getModelViewStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        modelViewStack.pushMatrix();
        modelViewStack.mul(event.getModelViewMatrix());
        try {
            DEFERRED_SUBMITS.sort(Comparator.comparingDouble(EnergySubmit::distanceSquared).reversed());
            for (EnergySubmit submit : DEFERRED_SUBMITS) {
                VertexConsumer buffer = bufferSource.getBuffer(ENERGY_TYPES.apply(submit.texture()));
                renderEnergyQuads(submit.pose(), buffer, submit.quads(), submit.tintLayers(),
                        submit.overlay(), submit.alphaMultiplier());
            }
            bufferSource.endLastBatch();
        } finally {
            modelViewStack.popMatrix();
            DEFERRED_SUBMITS.clear();
        }
    }

    private static SubmitNodeCollector collector(SubmitNodeCollector delegate,
                                                 float alphaMultiplier, boolean deferred) {
        float clampedAlpha = Math.clamp(alphaMultiplier, 0.0F, 1.0F);
        return (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("submitItem") && arguments != null && arguments.length >= 8) {
                        PoseStack itemPose = (PoseStack) arguments[0];
                        ItemDisplayContext context = (ItemDisplayContext) arguments[1];
                        int overlay = (int) arguments[3];
                        int outlineColor = (int) arguments[4];
                        int[] tintLayers = (int[]) arguments[5];
                        @SuppressWarnings("unchecked")
                        List<BakedQuad> quads = (List<BakedQuad>) arguments[6];
                        Map<Identifier, List<BakedQuad>> quadsByAtlas = new LinkedHashMap<>();
                        for (BakedQuad quad : quads) {
                            quadsByAtlas.computeIfAbsent(
                                            quad.materialInfo().sprite().atlasLocation(), ignored -> new ArrayList<>())
                                    .add(quad);
                        }
                        if (deferred) {
                            PoseStack.Pose pose = itemPose.last().copy();
                            float distanceSquared = pose.pose()
                                    .transformPosition(new Vector3f()).lengthSquared();
                            for (Map.Entry<Identifier, List<BakedQuad>> entry : quadsByAtlas.entrySet()) {
                                DEFERRED_SUBMITS.add(new EnergySubmit(
                                        pose, entry.getKey(), List.copyOf(entry.getValue()),
                                        tintLayers.clone(), overlay, clampedAlpha, distanceSquared));
                            }
                        } else {
                            for (Map.Entry<Identifier, List<BakedQuad>> entry : quadsByAtlas.entrySet()) {
                                List<BakedQuad> atlasQuads = List.copyOf(entry.getValue());
                                delegate.submitCustomGeometry(itemPose, ENERGY_TYPES.apply(entry.getKey()),
                                        (pose, buffer) -> renderEnergyQuads(
                                                pose, buffer, atlasQuads, tintLayers, overlay, clampedAlpha));
                            }
                            List<BakedQuad> hiddenQuads = quads.stream()
                                    .map(EnergyProjectionRenderer::hiddenQuad)
                                    .toList();
                            delegate.submitItem(itemPose, context, LightCoordsUtil.FULL_BRIGHT, overlay,
                                    outlineColor, tintLayers, hiddenQuads, ItemStackRenderState.FoilType.NONE);
                        }
                        return null;
                    }
                    return invoke(method, delegate, arguments);
                });
    }

    private static void renderEnergyQuads(PoseStack.Pose pose, VertexConsumer buffer,
                                          List<BakedQuad> quads, int[] tintLayers,
                                          int overlay, float alphaMultiplier) {
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
        instance.setOverlayCoords(overlay);
        for (BakedQuad quad : quads) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            int color = material.isTinted()
                    && material.tintIndex() >= 0
                    && material.tintIndex() < tintLayers.length
                    ? tintLayers[material.tintIndex()]
                    : -1;
            int alpha = Math.round(((color >>> 24) & 0xFF) * alphaMultiplier);
            instance.setColor((color & 0x00FFFFFF) | alpha << 24);
            buffer.putBakedQuad(pose, quad, instance);
        }
    }

    private static BakedQuad hiddenQuad(BakedQuad quad) {
        BakedQuad.MaterialInfo material = quad.materialInfo();
        BakedQuad.MaterialInfo hiddenMaterial = new BakedQuad.MaterialInfo(
                material.sprite(), material.layer(), HIDDEN_TYPES.apply(material.sprite().atlasLocation()),
                material.tintIndex(), material.shade(), material.lightEmission(), material.ambientOcclusion());
        return new BakedQuad(
                quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                quad.direction(), hiddenMaterial, quad.bakedNormals(), quad.bakedColors());
    }

    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private record EnergySubmit(PoseStack.Pose pose, Identifier texture, List<BakedQuad> quads,
                                int[] tintLayers, int overlay, float alphaMultiplier,
                                float distanceSquared) {
    }
}
