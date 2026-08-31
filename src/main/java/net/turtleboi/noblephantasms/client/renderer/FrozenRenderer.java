package net.turtleboi.noblephantasms.client.renderer;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.ModEffects;

public final class FrozenRenderer {
    private static final Identifier ICE_TEXTURE = Identifier.withDefaultNamespace("textures/block/blue_ice.png");
    private static final Identifier FROZEN_OVERLAY_TEXTURE =
            Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static final Identifier FROZEN_SCREEN_LAYER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "frozen_screen_overlay");
    private static final Identifier FROZEN_ICE_LAYER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "frozen_ice_overlay");
    private static final ContextKey<Boolean> FROZEN_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "frozen_visual"));
    private static final Map<UUID, FrozenPoseSnapshot> FROZEN_POSES = new HashMap<>();

    private FrozenRenderer() {
    }

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> applyFrozenState(entity, state));
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(FROZEN_SCREEN_LAYER, (graphics, deltaTracker) -> {
            if (!isLocalPlayerFrozen()) {
                return;
            }
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            graphics.blit(RenderPipelines.GUI_TEXTURED, FROZEN_OVERLAY_TEXTURE,
                    0, 0, 0.0F, 0.0F, width, height, width, height, -1);
        });
        event.registerBelowAll(FROZEN_ICE_LAYER, (graphics, deltaTracker) -> {
            if (!isLocalPlayerFrozen()) {
                return;
            }
            int tileSize = 64;
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            int color = ARGB.white(0.5F);
            for (int x = 0; x < width; x += tileSize) {
                for (int y = 0; y < height; y += tileSize) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, ICE_TEXTURE,
                            x, y, 0.0F, 0.0F, tileSize, tileSize, tileSize, tileSize, color);
                }
            }
        });
    }

    public static <S extends LivingEntityRenderState> void submit(
            S state, EntityModel<? super S> model, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!Boolean.TRUE.equals(state.getRenderData(FROZEN_KEY))) {
            return;
        }
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(ICE_TEXTURE),
                (pose, consumer) -> {
                    PoseStack frozenPose = new PoseStack();
                    frozenPose.last().set(pose);
                    model.setupAnim(state);
                    model.renderToBuffer(frozenPose, new RepeatingVertexConsumer(consumer, 8.0F, 8.0F),
                            state.lightCoords, OverlayTexture.NO_OVERLAY, 0x80FFFFFF);
                });
    }

    public static void clear(UUID entityId) {
        FROZEN_POSES.remove(entityId);
    }

    public static void clearAll() {
        FROZEN_POSES.clear();
    }

    private static void applyFrozenState(LivingEntity entity, LivingEntityRenderState state) {
        boolean frozen = entity.hasEffect(ModEffects.FROZEN);
        state.setRenderData(FROZEN_KEY, frozen);
        if (!frozen) {
            FROZEN_POSES.remove(entity.getUUID());
            return;
        }
        FROZEN_POSES.computeIfAbsent(entity.getUUID(), ignored -> FrozenPoseSnapshot.capture(state)).apply(state);
    }

    private static boolean isLocalPlayerFrozen() {
        return Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.hasEffect(ModEffects.FROZEN);
    }

    private record FrozenPoseSnapshot(float bodyRot, float yRot, float xRot, float walkAnimationPos,
                                      float walkAnimationSpeed, float ageInTicks, boolean armed,
                                      float attackTime, net.minecraft.client.model.HumanoidModel.ArmPose leftArmPose,
                                      net.minecraft.client.model.HumanoidModel.ArmPose rightArmPose,
                                      boolean humanoid, boolean usingItem, float ticksUsingItem,
                                      net.minecraft.world.InteractionHand useItemHand) {
        private static FrozenPoseSnapshot capture(LivingEntityRenderState state) {
            ArmedEntityRenderState armedState = state instanceof ArmedEntityRenderState value ? value : null;
            HumanoidRenderState humanoidState = state instanceof HumanoidRenderState value ? value : null;
            return new FrozenPoseSnapshot(state.bodyRot, state.yRot, state.xRot,
                    state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks,
                    armedState != null, armedState == null ? 0.0F : armedState.attackTime,
                    armedState == null ? net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY : armedState.leftArmPose,
                    armedState == null ? net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY : armedState.rightArmPose,
                    humanoidState != null, humanoidState != null && humanoidState.isUsingItem,
                    humanoidState == null ? 0.0F : humanoidState.ticksUsingItem,
                    humanoidState == null ? net.minecraft.world.InteractionHand.MAIN_HAND : humanoidState.useItemHand);
        }

        private void apply(LivingEntityRenderState state) {
            state.bodyRot = bodyRot;
            state.yRot = yRot;
            state.xRot = xRot;
            state.walkAnimationPos = walkAnimationPos;
            state.walkAnimationSpeed = walkAnimationSpeed;
            state.ageInTicks = ageInTicks;
            if (armed && state instanceof ArmedEntityRenderState armedState) {
                armedState.attackTime = attackTime;
                armedState.leftArmPose = leftArmPose;
                armedState.rightArmPose = rightArmPose;
            }
            if (humanoid && state instanceof HumanoidRenderState humanoidState) {
                humanoidState.isUsingItem = usingItem;
                humanoidState.ticksUsingItem = ticksUsingItem;
                humanoidState.useItemHand = useItemHand;
            }
        }
    }
}
