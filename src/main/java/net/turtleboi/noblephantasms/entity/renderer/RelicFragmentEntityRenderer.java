package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Quaternionf;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.RelicFragmentEntity;
import net.turtleboi.noblephantasms.entity.renderer.states.EyeShardRenderState;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class RelicFragmentEntityRenderer
        extends EntityRenderer<RelicFragmentEntity, EyeShardRenderState> {
    public RelicFragmentEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RELIC_FRAGMENT.get(),
                RelicFragmentEntityRenderer::new);
    }

    @Override
    public void submit(EyeShardRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fragment == null) {
            return;
        }
        RelicFragmentDefinitions.Definition definition = RelicFragmentDefinitions.get(
                state.fragment.relicId());
        RelicFragmenter.Layout layout = RelicFragmenter.createExact(
                state.fragment.relicId(), state.fragment.seed(), state.fragment.pieceCount());
        if (definition == null || layout == null || state.fragment.pieceIndex() < 0
                || state.fragment.pieceIndex() >= layout.pieceCount()) {
            return;
        }
        Identifier source = definition.textureId();
        Identifier texture = Identifier.fromNamespaceAndPath(source.getNamespace(),
                "textures/" + source.getPath() + ".png");
        RenderType renderType = RenderTypes.entityCutout(texture);
        RelicFragmenter.Piece piece = layout.pieces().get(state.fragment.pieceIndex());
        float reveal = 1.0F - (float) Math.pow(1.0F
                - Math.clamp(state.age / 8.0F, 0.0F, 1.0F), 3.0);
        float handoff = state.absorbProgress * state.absorbProgress
                * (3.0F - 2.0F * state.absorbProgress);
        float worldScale = 1.0F + (0.18F - 1.0F) * handoff;
        float hover = (0.38F + (float) Math.sin(state.age * 0.13F) * 0.035F)
                * (1.0F - handoff);
        Quaternionf rotation = Axis.YP.rotationDegrees(state.age * 2.4F)
                .slerp(new Quaternionf(camera.orientation), handoff);
        poseStack.pushPose();
        poseStack.translate(0.0F, hover, 0.0F);
        poseStack.mulPose(rotation);
        poseStack.scale(reveal * worldScale, reveal * worldScale, reveal * worldScale);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> EyeShardRenderer.drawRelicPiece(
                        pose, buffer, layout, piece, 15728880));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public EyeShardRenderState createRenderState() {
        return new EyeShardRenderState();
    }

    @Override
    public void extractRenderState(RelicFragmentEntity entity, EyeShardRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.fragment = entity.getItem().get(ModDataComponents.RELIC_FRAGMENT.get());
        state.age = entity.tickCount + partialTick;
        state.absorbProgress = entity.getAbsorbProgress();
    }
}
