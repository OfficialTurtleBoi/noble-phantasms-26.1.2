package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
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
    private final Map<TrophyData, Optional<HeadRenderData>> heads = new HashMap<>();

    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "trophy_head"), Unbaked.MAP_CODEC);
    }

    public static boolean hasRenderableHead(LivingEntity entity) {
        return createHead(Minecraft.getInstance().getEntityRenderDispatcher(), entity).isPresent();
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
        HeadRenderData data = heads.computeIfAbsent(trophyData, this::createHead).orElse(null);
        if (data == null) {
            return;
        }

        poseStack.pushPose();
        if (wallFacing != null) {
            poseStack.translate(0.0F, 0.0F, 0.25F - data.depth() * 0.5F);
        }
        poseStack.translate(0.0F, groundAligned ? -data.height() * 0.5F : -0.25F, 0.0F);
        poseStack.translate(-data.centerX(), -data.centerY(), -data.centerZ());
        submitNodeCollector.submitModelPart(data.head(), poseStack, data.renderType(), lightCoords, OverlayTexture.NO_OVERLAY,
                null, false, hasFoil, -1, breakProgress, outlineColor);
        poseStack.popPose();
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

    private Optional<HeadRenderData> createHead(TrophyData trophyData) {
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

        EntityModel model = livingRenderer.getModel();
        LivingEntityRenderState state = (LivingEntityRenderState) livingRenderer.createRenderState(entity, 0.0F);
        Identifier texture = livingRenderer.getTextureLocation(state);
        List<ModelPartState> originalModelState = model.root().getAllParts().stream()
                .map(ModelPartState::capture)
                .toList();
        ModelPart head;
        try {
            model.setupAnim(state);
            PartNode rendererHead = findHead(model);
            if (rendererHead == null) {
                return Optional.empty();
            }
            head = copySelection(model.root(), rendererHead.part());
            if (head == null) {
                return Optional.empty();
            }
        } finally {
            originalModelState.forEach(ModelPartState::restore);
        }

        Bounds bounds = Bounds.measure(head);
        float largestDimension = Math.max(bounds.width(), Math.max(bounds.height(), bounds.depth()));
        if (largestDimension <= 0.0F) {
            return Optional.empty();
        }

        return Optional.of(new HeadRenderData(head, model.renderType(texture),
                bounds.width(), bounds.height(), bounds.depth(),
                bounds.centerX(), bounds.centerY(), bounds.centerZ()));
    }

    private static ModelPart copyModelPart(ModelPart source) {
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) source;
        Map<String, ModelPart> children = new HashMap<>();
        accessor.noblePhantasms$getChildren().forEach(
                (name, child) -> children.put(name, copyModelPart(child)));

        return copyModelPart(source, accessor.noblePhantasms$getCubes(), children);
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

    private static @Nullable ModelPart copySelection(ModelPart root, ModelPart selected) {
        return copySelectedTree(root, selected);
    }

    private static @Nullable ModelPart copySelectedTree(ModelPart source, ModelPart selected) {
        if (source == selected) {
            return copyModelPart(source);
        }

        ModelPartAccessor accessor = (ModelPartAccessor) (Object) source;
        Map<String, ModelPart> children = new HashMap<>();
        accessor.noblePhantasms$getChildren().forEach((name, child) -> {
            ModelPart copiedChild = copySelectedTree(child, selected);
            if (copiedChild != null) {
                children.put(name, copiedChild);
            }
        });
        if (children.isEmpty()) {
            return null;
        }
        return copyModelPart(source, List.of(), children);
    }

    private static @Nullable PartNode findHead(EntityModel<?> model) {
        List<PartNode> parts = new ArrayList<>();
        collectParts(model.root(), "root", parts);

        PartNode head = null;
        if (model instanceof HeadedModel headedModel) {
            ModelPart declaredHead = headedModel.getHead();
            head = parts.stream()
                    .filter(node -> node.part() == declaredHead)
                    .findFirst()
                    .orElse(null);
        }
        if (head == null) {
            head = parts.stream()
                    .filter(node -> scoreHeadName(node.name()) >= 0)
                    .max(java.util.Comparator.comparingInt(node -> scoreHeadName(node.name())))
                    .orElse(null);
        }
        if (head == null) {
            return null;
        }
        return head;
    }

    private static void collectParts(ModelPart part, String name, List<PartNode> output) {
        output.add(new PartNode(name, part));
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) part;
        accessor.noblePhantasms$getChildren().forEach(
                (childName, child) -> collectParts(child, childName, output));
    }

    private static int scoreHeadName(String name) {
        String normalized = normalizePartName(name);
        if (isHeadOverlayName(normalized)) {
            return -1;
        }
        if (normalized.equals("head")) {
            return 100;
        }
        if (normalized.equals("skull") || normalized.equals("cranium")) {
            return 95;
        }
        if (normalized.equals("mainhead") || normalized.equals("headmain")) {
            return 90;
        }
        if (normalized.startsWith("head") || normalized.endsWith("head")) {
            return 80;
        }
        if (normalized.contains("head") || normalized.contains("skull")) {
            return 70;
        }
        if (normalized.equals("face")) {
            return 60;
        }
        return -1;
    }

    private static boolean isHeadOverlayName(String name) {
        String normalized = normalizePartName(name);
        return normalized.equals("hat")
                || normalized.equals("helmet")
                || normalized.startsWith("headwear")
                || normalized.startsWith("headlayer")
                || normalized.startsWith("headoverlay")
                || normalized.equals("outerhead")
                || normalized.equals("headouter");
    }

    private static String normalizePartName(String name) {
        StringBuilder normalized = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char character = Character.toLowerCase(name.charAt(i));
            if (Character.isLetterOrDigit(character)) {
                normalized.append(character);
            }
        }
        return normalized.toString();
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

    private record HeadRenderData(ModelPart head, RenderType renderType, float width, float height, float depth,
                                  float centerX, float centerY, float centerZ) {
    }

    private record PartNode(String name, ModelPart part) {
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

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        private static Bounds measure(ModelPart part) {
            float[] values = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            part.getExtentsForGui(new PoseStack(), point -> {
                values[0] = Math.min(values[0], point.x());
                values[1] = Math.min(values[1], point.y());
                values[2] = Math.min(values[2], point.z());
                values[3] = Math.max(values[3], point.x());
                values[4] = Math.max(values[4], point.y());
                values[5] = Math.max(values[5], point.z());
            });
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
