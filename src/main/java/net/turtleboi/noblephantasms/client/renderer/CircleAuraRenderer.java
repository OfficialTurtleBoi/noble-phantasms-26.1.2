package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.WebenItem;
import org.jspecify.annotations.Nullable;

public final class CircleAuraRenderer {
    private static final float SOURCE_HALF_SIZE = 6.0F;
    private static final float TURTLECORE_WORLD_SCALE = 0.5F;
    private static final float INITIAL_SCALE = 0.25F;
    private static final float INITIAL_GROWTH_TICKS = 5.0F;
    private static final float WEBEN_RELEASE_TICKS = 30.0F;
    private static final ContextKey<List<AuraRenderState>> AURAS_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "circle_auras"));
    private static final Map<UUID, WebenAuraTracker> WEBEN_TRACKERS = new HashMap<>();

    private CircleAuraRenderer() {
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double renderTime = event.getLevel().getGameTime() + partialTick;
        Vec3 cameraPosition = event.getCamera().position();
        List<AuraRenderState> auras = new ArrayList<>();
        Set<UUID> presentEntities = new HashSet<>();

        for (Entity entity : event.getLevel().entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            presentEntities.add(livingEntity.getUUID());
            extractWebenAura(livingEntity, partialTick, renderTime, cameraPosition,
                    event, auras);
        }

        WEBEN_TRACKERS.keySet().removeIf(entityId -> !presentEntities.contains(entityId));
        auras.sort(Comparator.comparingDouble(AuraRenderState::distanceSquared).reversed());
        event.getRenderState().setRenderData(AURAS_KEY, List.copyOf(auras));
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        LevelRenderState levelRenderState = event.getLevelRenderState();
        List<AuraRenderState> auras = levelRenderState.getRenderData(AURAS_KEY);
        if (auras == null || auras.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (AuraRenderState aura : auras) {
            poseStack.pushPose();
            poseStack.translate(aura.position().x, aura.position().y, aura.position().z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-aura.rotationDegrees()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            float worldScale = TURTLECORE_WORLD_SCALE * aura.scale();
            poseStack.scale(worldScale, worldScale, worldScale);
            submitFireCircle(collector, poseStack, aura.variant(), aura.alpha());
            poseStack.popPose();
        }
    }

    public static void submitFireCircle(SubmitNodeCollector collector, PoseStack poseStack,
                                        FireVariant variant, int alpha) {
        int clampedAlpha = Mth.clamp(alpha, 0, 255);
        collector.submitCustomGeometry(poseStack, variant.renderType(),
                (pose, buffer) -> drawCircle(pose, buffer, clampedAlpha));
    }

    public static void clear(UUID entityId) {
        WEBEN_TRACKERS.remove(entityId);
    }

    public static void clearAll() {
        WEBEN_TRACKERS.clear();
    }

    private static void extractWebenAura(LivingEntity entity, float partialTick, double renderTime,
                                         Vec3 cameraPosition, ExtractLevelRenderStateEvent event,
                                         List<AuraRenderState> auras) {
        ItemStack heldWeben = findHeldWeben(entity);
        UUID entityId = entity.getUUID();
        if (heldWeben == null) {
            WEBEN_TRACKERS.remove(entityId);
            return;
        }

        WebenAuraTracker tracker = WEBEN_TRACKERS.computeIfAbsent(entityId,
                ignored -> new WebenAuraTracker());
        boolean fullyCharged = WebenItem.isFullyCharged(heldWeben);
        if (fullyCharged && !tracker.ready) {
            tracker.ready = true;
            tracker.readySince = renderTime;
            tracker.releaseSince = Double.NaN;
        } else if (!fullyCharged && tracker.ready) {
            tracker.ready = false;
            tracker.releaseSince = renderTime;
        }

        if (!event.getFrustum().isVisible(entity.getBoundingBox().inflate(2.0))) {
            return;
        }

        Vec3 position = entity.getPosition(partialTick)
                .add(0.0, entity.getBbHeight() * 0.5, 0.0)
                .subtract(cameraPosition);
        double distanceSquared = position.lengthSqr();
        if (tracker.ready) {
            float elapsed = (float) Math.max(0.0, renderTime - tracker.readySince);
            float growth = Mth.clamp(elapsed / INITIAL_GROWTH_TICKS, 0.0F, 1.0F);
            float scale = INITIAL_SCALE * growth;
            float rotationSpeed = elapsed < INITIAL_GROWTH_TICKS ? 3.0F : 5.0F;
            auras.add(new AuraRenderState(position, elapsed * rotationSpeed, scale,
                    255, FireVariant.STANDARD, distanceSquared));
        } else if (!Double.isNaN(tracker.releaseSince)) {
            float elapsed = (float) Math.max(0.0, renderTime - tracker.releaseSince);
            if (elapsed >= WEBEN_RELEASE_TICKS) {
                tracker.releaseSince = Double.NaN;
                return;
            }
            float scale = releaseScale(elapsed, WEBEN_RELEASE_TICKS, 1.0F);
            int alpha = Math.round(255.0F * releaseAlpha(elapsed, WEBEN_RELEASE_TICKS));
            float rotationSpeed = elapsed < INITIAL_GROWTH_TICKS ? 3.0F : 5.0F;
            auras.add(new AuraRenderState(position, elapsed * rotationSpeed, scale,
                    alpha, FireVariant.STANDARD, distanceSquared));
        }
    }

    private static float releaseScale(float elapsed, float duration, float amplifier) {
        if (elapsed < INITIAL_GROWTH_TICKS) {
            return INITIAL_SCALE * elapsed / INITIAL_GROWTH_TICKS;
        }
        float progress = (elapsed - INITIAL_GROWTH_TICKS)
                / Math.max(duration - INITIAL_GROWTH_TICKS, 1.0F);
        return INITIAL_SCALE + progress * (0.3F * amplifier);
    }

    private static float releaseAlpha(float elapsed, float duration) {
        float fadeStart = duration * 0.75F;
        if (elapsed <= fadeStart) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (elapsed - fadeStart) / (duration - fadeStart), 0.0F, 1.0F);
    }

    private static @Nullable ItemStack findHeldWeben(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        if (mainHand.is(ModItems.WEBEN)) {
            return mainHand;
        }
        ItemStack offHand = entity.getOffhandItem();
        return offHand.is(ModItems.WEBEN) ? offHand : null;
    }

    private static void drawCircle(PoseStack.Pose pose, VertexConsumer buffer, int alpha) {
        vertex(pose, buffer, -SOURCE_HALF_SIZE, -SOURCE_HALF_SIZE, 0.0F, 0.0F, 0.0F, alpha);
        vertex(pose, buffer, SOURCE_HALF_SIZE, -SOURCE_HALF_SIZE, 0.0F, 1.0F, 0.0F, alpha);
        vertex(pose, buffer, SOURCE_HALF_SIZE, SOURCE_HALF_SIZE, 0.0F, 1.0F, 1.0F, alpha);
        vertex(pose, buffer, -SOURCE_HALF_SIZE, SOURCE_HALF_SIZE, 0.0F, 0.0F, 1.0F, alpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer,
                               float x, float y, float z, float u, float v, int alpha) {
        buffer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    public enum FireVariant {
        STANDARD("fire_aura"),
        BLUE("ultra_fire_aura"),
        GREEN("green_fire_aura");

        private final RenderType renderType;

        FireVariant(String textureName) {
            Identifier texture = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                    "textures/spell_effects/" + textureName + ".png");
            this.renderType = RenderTypes.entityTranslucentEmissive(texture, false);
        }

        private RenderType renderType() {
            return renderType;
        }
    }

    private record AuraRenderState(Vec3 position, float rotationDegrees, float scale,
                                   int alpha, FireVariant variant, double distanceSquared) {
    }

    private static final class WebenAuraTracker {
        private boolean ready;
        private double readySince;
        private double releaseSince = Double.NaN;
    }
}
