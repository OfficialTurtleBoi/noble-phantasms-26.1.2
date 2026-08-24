package net.turtleboi.noblephantasms.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.TecpatlShardEntity;
import net.turtleboi.noblephantasms.entity.renderer.states.TecpatlShardRenderState;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class TecpatlShardRenderer extends EntityRenderer<TecpatlShardEntity, TecpatlShardRenderState> {
    private static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "tecpatl_of_the_fifth_sun");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/item/tecpatl_of_the_fifth_sun.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    public TecpatlShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TECPATL_SHARD.get(), TecpatlShardRenderer::new);
    }

    @Override
    public void submit(TecpatlShardRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        RelicFragmenter.Layout layout = RelicFragmenter.createExact(
                ITEM_ID, renderState.seed, renderState.pieceCount);
        if (layout == null || renderState.pieceIndex < 0
                || renderState.pieceIndex >= layout.pieceCount()) {
            return;
        }
        RelicFragmenter.Piece piece = layout.pieces().get(renderState.pieceIndex);
        float tremble = renderState.phase == TecpatlShardEntity.PREPARING
                ? (float) Math.sin(renderState.age * 2.8F) * 0.035F : 0.0F;
        poseStack.pushPose();
        poseStack.translate(tremble, 0.2F, -tremble);
        if (renderState.phase == TecpatlShardEntity.WAITING) {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    renderState.yRot + renderState.pieceIndex * 31F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    renderState.xRot + renderState.pieceIndex * 47F));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(renderState.age
                    * (renderState.phase == TecpatlShardEntity.RETURNING ? 28F : 11F)
                    + renderState.pieceIndex * 31F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    renderState.pieceIndex * 47F + renderState.age * 7F));
        }
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE,
                (pose, buffer) -> EyeShardRenderer.drawPiece(pose, buffer, layout, piece, -1, 0.0F));
        if (renderState.hasFoil) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityGlint(),
                    (pose, buffer) -> {
                        SheetedDecalTextureGenerator glintBuffer =
                                new SheetedDecalTextureGenerator(buffer, pose, 0.0078125F);
                        EyeShardRenderer.drawPiece(
                                pose, glintBuffer, layout, piece, -1, 0.0F);
                    });
        }
        poseStack.popPose();
        super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    public TecpatlShardRenderState createRenderState() {
        return new TecpatlShardRenderState();
    }

    @Override
    public void extractRenderState(TecpatlShardEntity projectileEntity,
                                   TecpatlShardRenderState renderState, float partialTick) {
        super.extractRenderState(projectileEntity, renderState, partialTick);
        renderState.pieceIndex = projectileEntity.getPieceIndex();
        renderState.pieceCount = projectileEntity.getPieceCount();
        renderState.phase = projectileEntity.getPhase();
        renderState.seed = projectileEntity.getShatterSeed();
        renderState.age = projectileEntity.tickCount + partialTick;
        renderState.xRot = projectileEntity.getViewXRot(partialTick);
        renderState.yRot = projectileEntity.getViewYRot(partialTick);
        renderState.hasFoil = projectileEntity.hasFoil();
    }
}
