package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class TrophyHeadRenderer implements SpecialModelRenderer<Identifier> {
    private final Map<Identifier, Optional<HeadRenderData>> heads = new HashMap<>();

    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "trophy_head"), Unbaked.MAP_CODEC);
    }

    @Override
    public void submit(@Nullable Identifier entityTypeId, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (entityTypeId == null) {
            return;
        }

        HeadRenderData data = heads.computeIfAbsent(entityTypeId, this::createHead).orElse(null);
        if (data == null) {
            return;
        }

        data.setupAnimation();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(-data.scale(), -data.scale(), data.scale());
        poseStack.translate(-data.centerX(), -data.centerY(), -data.centerZ());
        submitNodeCollector.submitModelPart(data.head(), poseStack, data.renderType(), lightCoords, OverlayTexture.NO_OVERLAY,
                null, false, hasFoil, -1, null, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    @Override
    public @Nullable Identifier extractArgument(ItemStack stack) {
        return TrophyHeadItem.getEntityType(stack);
    }

    private Optional<HeadRenderData> createHead(Identifier entityTypeId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.empty();
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityTypeId);
        if (entityType == null) {
            return Optional.empty();
        }

        Entity entity = entityType.create(minecraft.level, EntitySpawnReason.LOAD);
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }

        return createHead(minecraft.getEntityRenderDispatcher(), livingEntity);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<HeadRenderData> createHead(EntityRenderDispatcher dispatcher, LivingEntity entity) {
        EntityRenderer renderer = dispatcher.getRenderer(entity);
        if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
            return Optional.empty();
        }

        EntityModel model = livingRenderer.getModel();
        LivingEntityRenderState state = (LivingEntityRenderState) livingRenderer.createRenderState(entity, 0.0F);
        model.setupAnim(state);
        ModelPart head = findHead(model);
        if (head == null) {
            return Optional.empty();
        }

        Identifier texture = livingRenderer.getTextureLocation(state);
        Bounds bounds = Bounds.measure(head);
        float largestDimension = Math.max(bounds.width(), Math.max(bounds.height(), bounds.depth()));
        if (largestDimension <= 0.0F) {
            return Optional.empty();
        }

        return Optional.of(new HeadRenderData(model, state, head, model.renderType(texture), 0.8F / largestDimension,
                bounds.centerX(), bounds.centerY(), bounds.centerZ()));
    }

    private static @Nullable ModelPart findHead(EntityModel<?> model) {
        if (model instanceof HeadedModel headedModel) {
            return headedModel.getHead();
        }
        return model.root().createPartLookup().apply("head");
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Identifier> {
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private record HeadRenderData(EntityModel model, LivingEntityRenderState state, ModelPart head, RenderType renderType,
                                  float scale, float centerX, float centerY, float centerZ) {
        private void setupAnimation() {
            model.setupAnim(state);
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
