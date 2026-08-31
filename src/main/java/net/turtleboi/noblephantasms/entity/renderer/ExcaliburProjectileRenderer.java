package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.ExcaliburProjectile;
import net.turtleboi.noblephantasms.entity.renderer.states.ExcaliburProjectileRenderState;

public final class ExcaliburProjectileRenderer
        extends EntityRenderer<ExcaliburProjectile, ExcaliburProjectileRenderState> {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "core/excalibur_energy");
    private static final RenderPipeline ENERGY_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/excalibur_energy"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline HIDDEN_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/excalibur_energy_hidden"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .build();
    private static final Function<Identifier, RenderType> ENERGY_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_excalibur_energy",
            RenderSetup.builder(ENERGY_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload()
                    .createRenderSetup()));
    private static final Function<Identifier, RenderType> HIDDEN_TYPES = Util.memoize(texture -> RenderType.create(
            NoblePhantasms.MOD_ID + "_excalibur_energy_hidden",
            RenderSetup.builder(HIDDEN_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .createRenderSetup()));
    private static final float MODEL_SCALE = 1.7F;
    private final ItemModelResolver itemModelResolver;

    public ExcaliburProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ENERGY_PIPELINE);
        event.registerPipeline(HIDDEN_PIPELINE);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.EXCALIBUR_PROJECTILE.get(), ExcaliburProjectileRenderer::new);
    }

    @Override
    public void submit(ExcaliburProjectileRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(-renderState.xRotation));
        poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        renderState.item.submit(poseStack, energyCollector(submitNodeCollector), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
        super.submit(renderState, poseStack, submitNodeCollector, camera);
    }

    @Override
    public ExcaliburProjectileRenderState createRenderState() {
        return new ExcaliburProjectileRenderState();
    }

    @Override
    public void extractRenderState(ExcaliburProjectile entity,
                                   ExcaliburProjectileRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        renderState.xRotation = entity.getXRot(partialTick);
        renderState.yRotation = entity.getYRot(partialTick);
        itemModelResolver.updateForNonLiving(renderState.item, entity.getItem(), ItemDisplayContext.FIXED, entity);
    }

    public static SubmitNodeCollector energyCollector(SubmitNodeCollector delegate) {
        return energyCollector(delegate, 1.0F);
    }

    public static SubmitNodeCollector energyCollector(SubmitNodeCollector delegate,
                                                      float alphaMultiplier) {
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
                                    quad.materialInfo().sprite().atlasLocation(), ignored -> new java.util.ArrayList<>())
                                    .add(quad);
                        }
                        for (Map.Entry<Identifier, List<BakedQuad>> entry : quadsByAtlas.entrySet()) {
                            List<BakedQuad> atlasQuads = List.copyOf(entry.getValue());
                            delegate.submitCustomGeometry(itemPose, ENERGY_TYPES.apply(entry.getKey()),
                                    (pose, buffer) -> renderEnergyQuads(
                                            pose, buffer, atlasQuads, tintLayers, overlay, clampedAlpha));
                        }
                        List<BakedQuad> hiddenQuads = quads.stream()
                                .map(ExcaliburProjectileRenderer::hiddenQuad)
                                .toList();
                        delegate.submitItem(itemPose, context, LightCoordsUtil.FULL_BRIGHT, overlay,
                                outlineColor, tintLayers, hiddenQuads, ItemStackRenderState.FoilType.NONE);
                        return null;
                    }
                    return invoke(method, delegate, arguments);
                });
    }

    private static void renderEnergyQuads(PoseStack.Pose pose,
                                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                          List<BakedQuad> quads, int[] tintLayers, int overlay,
                                          float alphaMultiplier) {
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
}
