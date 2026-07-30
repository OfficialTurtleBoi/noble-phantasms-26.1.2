package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.block.entity.ModBlockEntities;
import net.turtleboi.noblephantasms.block.entity.TrophyHeadBlockEntity;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem.TrophyData;
import org.jspecify.annotations.Nullable;

public final class TrophyHeadBlockEntityRenderer
        implements BlockEntityRenderer<TrophyHeadBlockEntity, TrophyHeadBlockEntityRenderer.RenderState> {
    private final TrophyHeadRenderer headRenderer = new TrophyHeadRenderer();

    public TrophyHeadBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TROPHY_HEAD.get(), TrophyHeadBlockEntityRenderer::new);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(TrophyHeadBlockEntity blockEntity, RenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.trophyData = blockEntity.getTrophyData();
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof WallSkullBlock) {
            state.groundAligned = false;
            state.wallFacing = blockState.getValue(WallSkullBlock.FACING);
            state.transformation = SkullBlockRenderer.TRANSFORMATIONS.wallTransformation(
                    state.wallFacing);
        } else {
            state.groundAligned = true;
            state.wallFacing = null;
            state.transformation = SkullBlockRenderer.TRANSFORMATIONS.freeTransformations(
                    blockState.getValue(SkullBlock.ROTATION));
        }
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        if (state.trophyData == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(state.transformation);
        headRenderer.submitHead(state.trophyData, poseStack, submitNodeCollector, state.lightCoords,
                false, 0, state.breakProgress, state.groundAligned, state.wallFacing);
        poseStack.popPose();
    }

    public static final class RenderState extends BlockEntityRenderState {
        private @Nullable TrophyData trophyData;
        private Transformation transformation = Transformation.IDENTITY;
        private boolean groundAligned;
        private @Nullable Direction wallFacing;
    }
}
