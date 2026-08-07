package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.chicken.BabyChickenModel;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.guardian.GuardianModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.TrophyHeadShapeCache;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem.TrophyData;
import net.turtleboi.noblephantasms.mixin.client.ModelPartAccessor;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class TrophyHeadRenderer implements SpecialModelRenderer<TrophyData> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<ModelCapture> MODEL_CAPTURE = new ThreadLocal<>();
    private static final Map<TrophyData, Optional<HeadRenderData>> HEADS = new HashMap<>();
    private static final Map<LivingEntityRenderState, TrophyData> WORN_HEADS = new WeakHashMap<>();

    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "trophy_head"), Unbaked.MAP_CODEC);
    }

    public static boolean hasRenderableHead(LivingEntity entity) {
        return createHead(Minecraft.getInstance().getEntityRenderDispatcher(), entity).isPresent();
    }

    public static boolean captureSelectedModel(EntityModel<?> model, List<?> layers) {
        ModelCapture capture = MODEL_CAPTURE.get();
        if (capture == null) {
            return false;
        }
        capture.model = model;
        capture.layers = List.copyOf(layers);
        return true;
    }

    public static void setWornHead(LivingEntityRenderState state, @Nullable TrophyData trophyData) {
        if (trophyData == null) {
            WORN_HEADS.remove(state);
        } else {
            WORN_HEADS.put(state, trophyData);
        }
    }

    public static @Nullable TrophyData getWornHead(LivingEntityRenderState state) {
        return WORN_HEADS.get(state);
    }

    @Override
    public void submit(@Nullable TrophyData trophyData, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (trophyData == null) {
            return;
        }

        submitHead(trophyData, poseStack, submitNodeCollector, lightCoords,
                hasFoil, outlineColor, null, false, null);
    }

    public void submitHead(TrophyData trophyData, PoseStack poseStack,
                           SubmitNodeCollector submitNodeCollector,
                           int lightCoords, boolean hasFoil, int outlineColor,
                           ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, boolean groundAligned,
                           @Nullable Direction wallFacing) {
        HeadRenderData data = HEADS.computeIfAbsent(trophyData, TrophyHeadRenderer::createHead).orElse(null);
        if (data == null) {
            return;
        }

        poseStack.pushPose();
        if (wallFacing != null) {
            poseStack.translate(0.0F, 0.0F, 0.25F - data.anchorDepth() * 0.5F);
        }
        poseStack.translate(0.0F, groundAligned ? -data.anchorHeight() * 0.5F : -0.25F, 0.0F);
        poseStack.translate(-data.centerX(), -data.centerY(), -data.centerZ());
        submitLayers(data, poseStack, submitNodeCollector, lightCoords, hasFoil,
                outlineColor, breakProgress);
        poseStack.popPose();
    }

    public static void submitWorn(TrophyData trophyData, PoseStack poseStack,
                                  SubmitNodeCollector submitNodeCollector,
                                  int lightCoords, int outlineColor) {
        HeadRenderData data = HEADS.computeIfAbsent(trophyData, TrophyHeadRenderer::createHead).orElse(null);
        if (data == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-data.centerX(),
                -(data.centerY() + data.anchorHeight() * 0.5F), -data.centerZ());
        submitLayers(data, poseStack, submitNodeCollector, lightCoords,
                false, outlineColor, null);
        poseStack.popPose();
    }

    private static void submitLayers(HeadRenderData data, PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector,
                                     int lightCoords, boolean hasFoil, int outlineColor,
                                     ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        for (HeadLayerData layer : data.layers()) {
            OrderedSubmitNodeCollector collector = layer.order() == 0
                    ? submitNodeCollector
                    : submitNodeCollector.order(layer.order());
            collector.submitModelPart(layer.head(), poseStack, layer.renderType(), lightCoords,
                    OverlayTexture.NO_OVERLAY, null, false, layer.base() && hasFoil,
                    layer.color(), breakProgress, outlineColor);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.25F, -0.5F, -0.25F));
        output.accept(new Vector3f(0.25F, 0.0F, 0.25F));
    }

    @Override
    public @Nullable TrophyData extractArgument(ItemStack stack) {
        return TrophyHeadItem.getTrophyData(stack);
    }

    private static Optional<HeadRenderData> createHead(TrophyData trophyData) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.empty();
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(trophyData.entityTypeId());
        if (entityType == null) {
            return Optional.empty();
        }

        Entity entity = entityType.create(minecraft.level, EntitySpawnReason.LOAD);
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }

        loadAppearanceData(livingEntity, trophyData);
        if (trophyData.hasBabyMarker() && livingEntity instanceof Mob mob) {
            mob.setBaby(trophyData.isBaby());
        }
        Optional<HeadRenderData> result =
                createHead(minecraft.getEntityRenderDispatcher(), livingEntity);
        result.ifPresent(data -> TrophyHeadShapeCache.register(trophyData,
                data.width(), data.height(), data.depth()));
        return result;
    }

    private static void loadAppearanceData(LivingEntity livingEntity, TrophyData trophyData) {
        if (trophyData.entityData().isEmpty()) {
            return;
        }
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(livingEntity.problemPath(), LOGGER)) {
            livingEntity.load(TagValueInput.create(
                    reporter, livingEntity.registryAccess(), trophyData.entityData()));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<HeadRenderData> createHead(EntityRenderDispatcher dispatcher, LivingEntity entity) {
        EntityRenderer renderer = dispatcher.getRenderer(entity);
        if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
            return Optional.empty();
        }

        LivingEntityRenderState state = (LivingEntityRenderState) livingRenderer.createRenderState(entity, 0.0F);
        ModelSelection selection = selectModel(livingRenderer, state);
        EntityModel model = selection.model();
        Identifier texture = livingRenderer.getTextureLocation(state);
        HeadCopy baseHead = copyAnimatedHead(model, state);
        if (baseHead == null) {
            return Optional.empty();
        }
        List<HeadLayerData> layers = new ArrayList<>();
        layers.add(new HeadLayerData(baseHead.model(), model.renderType(texture), -1, 0, true));
        captureLayerModels(selection.layers(), state).stream()
                .map(submission -> createHeadLayer(submission, state))
                .filter(java.util.Objects::nonNull)
                .forEach(layers::add);

        Bounds bounds = Bounds.measure(layers.stream().map(HeadLayerData::head).toList());
        float largestDimension = Math.max(bounds.width(), Math.max(bounds.height(), bounds.depth()));
        if (largestDimension <= 0.0F) {
            return Optional.empty();
        }

        return Optional.of(new HeadRenderData(List.copyOf(layers),
                bounds.width(), bounds.height(), bounds.depth(),
                baseHead.anchor().centerX(), baseHead.anchor().centerY(), baseHead.anchor().centerZ(),
                baseHead.anchor().height(), baseHead.anchor().depth()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ModelSelection selectModel(LivingEntityRenderer renderer, LivingEntityRenderState state) {
        ModelCapture capture = new ModelCapture();
        MODEL_CAPTURE.set(capture);
        try {
            renderer.submit(state, new PoseStack(), null, null);
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not run entity renderer model selection for a trophy head", exception);
        } finally {
            MODEL_CAPTURE.remove();
        }
        return new ModelSelection(capture.model != null ? capture.model : renderer.getModel(), capture.layers);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @Nullable HeadCopy copyAnimatedHead(Model model, LivingEntityRenderState state) {
        List<ModelPartState> originalModelState = model.root().getAllParts().stream()
                .map(ModelPartState::capture)
                .toList();
        try {
            model.setupAnim(state);
            ModelPart primaryHead = model instanceof HeadedModel headedModel
                    ? headedModel.getHead()
                    : findExactPart(model.root(), "head");
            if (primaryHead == null && model instanceof BabyChickenModel) {
                primaryHead = findExactPart(model.root(), "body");
            }
            if (primaryHead == null) {
                return null;
            }
            Set<ModelPart> selected = new HashSet<>();
            collectSubtree(primaryHead, selected);
            ModelPart rebaseRoot = primaryHead;
            if (model instanceof AbstractEquineModel) {
                ModelPart upperMouth = findExactPart(model.root(), "upper_mouth");
                ModelPart headParent = findParent(model.root(), primaryHead);
                if (upperMouth != null && headParent != null
                        && findParent(model.root(), upperMouth) == headParent) {
                    collectSubtree(upperMouth, selected);
                    rebaseRoot = headParent;
                }
            }
            if (model instanceof GuardianModel) {
                ModelPart tail = findExactPart(primaryHead, "tail0");
                if (tail != null) {
                    removeSubtree(tail, selected);
                }
            }
            ModelPart copied = copySelectedTree(rebaseRoot, selected);
            ModelPart anchorModel = copySelectedTree(rebaseRoot, Set.of(primaryHead));
            if (anchorModel == null) {
                Set<ModelPart> anchorParts = new HashSet<>();
                collectSubtree(primaryHead, anchorParts);
                anchorModel = copySelectedTree(rebaseRoot, anchorParts);
            }
            if (copied == null || anchorModel == null) {
                return null;
            }
            resetTransform(copied);
            resetTransform(anchorModel);
            Bounds anchor = Bounds.measure(List.of(anchorModel));
            return new HeadCopy(copied, anchor);
        } finally {
            originalModelState.forEach(ModelPartState::restore);
        }
    }

    private static @Nullable HeadLayerData createHeadLayer(ModelSubmission submission,
                                                           LivingEntityRenderState state) {
        HeadCopy head = copyAnimatedHead(submission.model(), state);
        return head == null ? null : new HeadLayerData(head.model(), submission.renderType(),
                submission.color(), submission.order(), false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<ModelSubmission> captureLayerModels(List<?> layers, LivingEntityRenderState state) {
        List<ModelSubmission> submissions = new ArrayList<>();
        for (Object value : layers) {
            if (!(value instanceof RenderLayer layer)) {
                continue;
            }
            try {
                layer.submit(new PoseStack(), layerCollector(submissions, 0), state.lightCoords,
                        state, state.yRot, state.xRot);
            } catch (RuntimeException exception) {
                LOGGER.debug("Could not capture a trophy head render layer", exception);
            }
        }
        return submissions;
    }

    private static SubmitNodeCollector layerCollector(List<ModelSubmission> submissions, int order) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("order")) {
                        return layerCollector(submissions, (int) arguments[0]);
                    }
                    if (method.getName().equals("submitModel") && arguments != null
                            && arguments.length >= 4 && arguments[0] instanceof Model<?> model) {
                        RenderType renderType = arguments[3] instanceof RenderType value
                                ? value
                                : arguments[3] instanceof Identifier texture
                                ? model.renderType(texture)
                                : null;
                        if (renderType != null) {
                            int color = arguments.length >= 10 && arguments[6] instanceof Integer value
                                    ? value
                                    : -1;
                            submissions.add(new ModelSubmission(model, renderType, color, order));
                        }
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static @Nullable Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private static ModelPart copyModelPart(ModelPart source, List<ModelPart.Cube> cubes,
                                           Map<String, ModelPart> children) {
        ModelPart copy = new ModelPart(cubes, children);
        copy.x = source.x;
        copy.y = source.y;
        copy.z = source.z;
        copy.xRot = source.xRot;
        copy.yRot = source.yRot;
        copy.zRot = source.zRot;
        copy.xScale = source.xScale;
        copy.yScale = source.yScale;
        copy.zScale = source.zScale;
        copy.visible = source.visible;
        copy.skipDraw = source.skipDraw;
        copy.setInitialPose(source.getInitialPose());
        return copy;
    }

    private static @Nullable ModelPart copySelectedTree(ModelPart source, Set<ModelPart> selected) {
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) source;
        Map<String, ModelPart> children = new HashMap<>();
        accessor.noblePhantasms$getChildren().forEach((name, child) -> {
            ModelPart copiedChild = copySelectedTree(child, selected);
            if (copiedChild != null) {
                children.put(name, copiedChild);
            }
        });
        List<ModelPart.Cube> cubes = selected.contains(source)
                ? accessor.noblePhantasms$getCubes()
                : List.of();
        if (cubes.isEmpty() && children.isEmpty()) {
            return null;
        }
        return copyModelPart(source, cubes, children);
    }

    private static @Nullable ModelPart findExactPart(ModelPart current, String targetName) {
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) current;
        ModelPart direct = accessor.noblePhantasms$getChildren().get(targetName);
        if (direct != null) {
            return direct;
        }
        for (ModelPart child : accessor.noblePhantasms$getChildren().values()) {
            ModelPart result = findExactPart(child, targetName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static void collectSubtree(ModelPart part, Set<ModelPart> selected) {
        selected.add(part);
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) part;
        accessor.noblePhantasms$getChildren().values().forEach(child -> collectSubtree(child, selected));
    }

    private static void removeSubtree(ModelPart part, Set<ModelPart> selected) {
        selected.remove(part);
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) part;
        accessor.noblePhantasms$getChildren().values().forEach(child -> removeSubtree(child, selected));
    }

    private static @Nullable ModelPart findParent(ModelPart current, ModelPart target) {
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) current;
        for (ModelPart child : accessor.noblePhantasms$getChildren().values()) {
            if (child == target) {
                return current;
            }
            ModelPart result = findParent(child, target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static void resetTransform(ModelPart part) {
        part.x = 0.0F;
        part.y = 0.0F;
        part.z = 0.0F;
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<TrophyData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public TrophyHeadRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new TrophyHeadRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }

    private record HeadRenderData(List<HeadLayerData> layers, float width, float height, float depth,
                                  float centerX, float centerY, float centerZ,
                                  float anchorHeight, float anchorDepth) {
    }

    private record HeadLayerData(ModelPart head, RenderType renderType, int color, int order, boolean base) {
    }

    private record HeadCopy(ModelPart model, Bounds anchor) {
    }

    private record ModelSubmission(Model<?> model, RenderType renderType, int color, int order) {
    }

    private record ModelSelection(EntityModel<?> model, List<?> layers) {
    }

    private record ModelPartState(ModelPart part, float x, float y, float z, float xRot, float yRot, float zRot,
                                  float xScale, float yScale, float zScale, boolean visible, boolean skipDraw) {
        private static ModelPartState capture(ModelPart part) {
            return new ModelPartState(part, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                    part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw);
        }

        private void restore() {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.skipDraw = skipDraw;
        }
    }

    private static final class ModelCapture {
        private EntityModel<?> model;
        private List<?> layers = List.of();
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        private static Bounds measure(List<ModelPart> parts) {
            float[] values = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (ModelPart part : parts) {
                part.getExtentsForGui(new PoseStack(), point -> {
                    values[0] = Math.min(values[0], point.x());
                    values[1] = Math.min(values[1], point.y());
                    values[2] = Math.min(values[2], point.z());
                    values[3] = Math.max(values[3], point.x());
                    values[4] = Math.max(values[4], point.y());
                    values[5] = Math.max(values[5], point.z());
                });
            }
            return new Bounds(values[0], values[1], values[2], values[3], values[4], values[5]);
        }

        private float width() {
            return maxX - minX;
        }

        private float height() {
            return maxY - minY;
        }

        private float depth() {
            return maxZ - minZ;
        }

        private boolean isValid() {
            return Float.isFinite(minX) && Float.isFinite(minY) && Float.isFinite(minZ)
                    && Float.isFinite(maxX) && Float.isFinite(maxY) && Float.isFinite(maxZ);
        }

        private float centerX() {
            return (minX + maxX) * 0.5F;
        }

        private float centerY() {
            return (minY + maxY) * 0.5F;
        }

        private float centerZ() {
            return (minZ + maxZ) * 0.5F;
        }
    }
}
