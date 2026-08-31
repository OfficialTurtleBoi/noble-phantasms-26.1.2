package net.turtleboi.noblephantasms.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;

public final class ReliquaryStationBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ReliquaryStationBlock> CODEC = simpleCodec(ReliquaryStationBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0),
            box(1.0, 6.0, 3.0, 15.0, 10.25, 13.0));

    public ReliquaryStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<ReliquaryStationBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        RelicFragmentData data = stack.get(ModDataComponents.RELIC_FRAGMENT.get());
        if (data == null || data.pieceIndex() >= 0 || !RelicFragmentDefinitions.supports(data.relicId())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        long seed = serverPlayer.getRandom().nextLong();
        serverPlayer.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) ->
                new ReliquaryStationMenu(containerId, inventory, data.relicId(), seed),
                Component.translatable("block.noblephantasms.reliquary_station")), buffer -> {
            Identifier.STREAM_CODEC.encode(buffer, data.relicId());
            buffer.writeLong(seed);
        });
        return InteractionResult.SUCCESS_SERVER;
    }
}
