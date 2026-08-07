package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.EyeShardEntity;
import net.turtleboi.noblephantasms.entity.renderer.states.EyeShardRenderState;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class EyeShardRenderer extends EntityRenderer<EyeShardEntity, EyeShardRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/item/eye_of_horus.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    public EyeShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.EYE_SHARD.get(), EyeShardRenderer::new);
    }

    @Override
    public void submit(EyeShardRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fragment == null) {
            return;
        }
        RelicFragmenter.Layout layout = RelicFragmenter.create(
                state.fragment.relicId(), state.fragment.seed());
        if (layout == null || state.fragment.pieceIndex() < 0
                || state.fragment.pieceIndex() >= layout.pieceCount()) {
            return;
        }
        RelicFragmenter.Piece piece = layout.pieces().get(state.fragment.pieceIndex());
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.42F + Math.sin(state.age * 0.12F) * 0.05F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 3.0F));
        collector.submitCustomGeometry(poseStack, RENDER_TYPE,
                (pose, buffer) -> drawPiece(pose, buffer, layout, piece, -1, 0.0F));
        LuminousRenderer.submitJudgementGeometry(
                poseStack,
                TEXTURE,
                state.age,
                state.glowPhase,
                (pose, buffer, color, outlineWidth) ->
                        drawPiece(pose, buffer, layout, piece, color, outlineWidth));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void drawPiece(PoseStack.Pose pose, VertexConsumer buffer,
                                  RelicFragmenter.Layout layout, RelicFragmenter.Piece piece,
                                  int color, float outlineWidth) {
        float unit = 0.65F / Math.max(layout.width(), layout.height());
        float halfThickness = 0.025F + outlineWidth;
        float centerX = (piece.minX() + piece.maxX() + 1) * 0.5F;
        float centerY = (piece.minY() + piece.maxY() + 1) * 0.5F;
        Set<Long> occupied = new HashSet<>();
        for (RelicFragmenter.Pixel pixel : piece.pixels()) {
            occupied.add(coordinate(pixel.x(), pixel.y()));
        }
        for (RelicFragmenter.Pixel pixel : piece.pixels()) {
            float x0 = (pixel.x() - centerX) * unit;
            float x1 = x0 + unit;
            float y1 = (centerY - pixel.y()) * unit;
            float y0 = y1 - unit;
            float u0 = pixel.x() / (float) layout.width();
            float u1 = (pixel.x() + 1) / (float) layout.width();
            float v0 = pixel.y() / (float) layout.height();
            float v1 = (pixel.y() + 1) / (float) layout.height();
            float centerU = (u0 + u1) * 0.5F;
            float centerV = (v0 + v1) * 0.5F;
            boolean exposedLeft = !occupied.contains(coordinate(pixel.x() - 1, pixel.y()));
            boolean exposedRight = !occupied.contains(coordinate(pixel.x() + 1, pixel.y()));
            boolean exposedTop = !occupied.contains(coordinate(pixel.x(), pixel.y() - 1));
            boolean exposedBottom = !occupied.contains(coordinate(pixel.x(), pixel.y() + 1));
            float renderedX0 = x0 - (exposedLeft ? outlineWidth : 0.0F);
            float renderedX1 = x1 + (exposedRight ? outlineWidth : 0.0F);
            float renderedY0 = y0 - (exposedBottom ? outlineWidth : 0.0F);
            float renderedY1 = y1 + (exposedTop ? outlineWidth : 0.0F);
            vertex(pose, buffer, renderedX0, renderedY0, halfThickness, u0, v1, color, 0.0F, 0.0F, 1.0F);
            vertex(pose, buffer, renderedX1, renderedY0, halfThickness, u1, v1, color, 0.0F, 0.0F, 1.0F);
            vertex(pose, buffer, renderedX1, renderedY1, halfThickness, u1, v0, color, 0.0F, 0.0F, 1.0F);
            vertex(pose, buffer, renderedX0, renderedY1, halfThickness, u0, v0, color, 0.0F, 0.0F, 1.0F);
            vertex(pose, buffer, renderedX0, renderedY1, -halfThickness, u0, v0, color, 0.0F, 0.0F, -1.0F);
            vertex(pose, buffer, renderedX1, renderedY1, -halfThickness, u1, v0, color, 0.0F, 0.0F, -1.0F);
            vertex(pose, buffer, renderedX1, renderedY0, -halfThickness, u1, v1, color, 0.0F, 0.0F, -1.0F);
            vertex(pose, buffer, renderedX0, renderedY0, -halfThickness, u0, v1, color, 0.0F, 0.0F, -1.0F);
            if (exposedLeft) {
                vertex(pose, buffer, renderedX0, renderedY0, -halfThickness, centerU, v1, color, -1.0F, 0.0F, 0.0F);
                vertex(pose, buffer, renderedX0, renderedY0, halfThickness, centerU, v1, color, -1.0F, 0.0F, 0.0F);
                vertex(pose, buffer, renderedX0, renderedY1, halfThickness, centerU, v0, color, -1.0F, 0.0F, 0.0F);
                vertex(pose, buffer, renderedX0, renderedY1, -halfThickness, centerU, v0, color, -1.0F, 0.0F, 0.0F);
            }
            if (exposedRight) {
                vertex(pose, buffer, renderedX1, renderedY1, -halfThickness, centerU, v0, color, 1.0F, 0.0F, 0.0F);
                vertex(pose, buffer, renderedX1, renderedY1, halfThickness, centerU, v0, color, 1.0F, 0.0F, 0.0F);
                vertex(pose, buffer, renderedX1, renderedY0, halfThickness, centerU, v1, color, 1.0F, 0.0F, 0.0F);
                vertex(pose, buffer, renderedX1, renderedY0, -halfThickness, centerU, v1, color, 1.0F, 0.0F, 0.0F);
            }
            if (exposedTop) {
                vertex(pose, buffer, renderedX0, renderedY1, -halfThickness, u0, centerV, color, 0.0F, 1.0F, 0.0F);
                vertex(pose, buffer, renderedX0, renderedY1, halfThickness, u0, centerV, color, 0.0F, 1.0F, 0.0F);
                vertex(pose, buffer, renderedX1, renderedY1, halfThickness, u1, centerV, color, 0.0F, 1.0F, 0.0F);
                vertex(pose, buffer, renderedX1, renderedY1, -halfThickness, u1, centerV, color, 0.0F, 1.0F, 0.0F);
            }
            if (exposedBottom) {
                vertex(pose, buffer, renderedX1, renderedY0, -halfThickness, u1, centerV, color, 0.0F, -1.0F, 0.0F);
                vertex(pose, buffer, renderedX1, renderedY0, halfThickness, u1, centerV, color, 0.0F, -1.0F, 0.0F);
                vertex(pose, buffer, renderedX0, renderedY0, halfThickness, u0, centerV, color, 0.0F, -1.0F, 0.0F);
                vertex(pose, buffer, renderedX0, renderedY0, -halfThickness, u0, centerV, color, 0.0F, -1.0F, 0.0F);
            }
        }
    }

    private static long coordinate(int x, int y) {
        return (long) x << 32 | y & 0xFFFFFFFFL;
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer,
                               float x, float y, float z, float u, float v,
                               int color,
                               float normalX, float normalY, float normalZ) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    @Override
    public EyeShardRenderState createRenderState() {
        return new EyeShardRenderState();
    }

    @Override
    public void extractRenderState(EyeShardEntity entity, EyeShardRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.fragment = entity.getItem().get(ModDataComponents.RELIC_FRAGMENT.get());
        state.age = entity.tickCount + partialTick;
        state.glowPhase = entity.getId() * 0.7548777F;
    }
}
