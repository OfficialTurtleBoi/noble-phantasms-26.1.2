package net.turtleboi.noblephantasms.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.turtleboi.noblephantasms.block.entity.ReliquaryStationBlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;
import org.jspecify.annotations.Nullable;

public final class ReliquaryStationBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ReliquaryStationBlock> CODEC = simpleCodec(ReliquaryStationBlock::new);
    private static final VoxelShape NORTH_SHAPE = createShape(
            1.0, 3.0, 15.0, 13.0,
            9.75, 2.75, 14.25, 13.25);
    private static final VoxelShape EAST_SHAPE = createShape(
            3.0, 1.0, 13.0, 15.0,
            2.75, 9.75, 13.25, 14.25);
    private static final VoxelShape SOUTH_SHAPE = createShape(
            1.0, 3.0, 15.0, 13.0,
            1.75, 2.75, 6.25, 13.25);
    private static final VoxelShape WEST_SHAPE = createShape(
            3.0, 1.0, 13.0, 15.0,
            2.75, 1.75, 13.25, 6.25);

    public ReliquaryStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<ReliquaryStationBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReliquaryStationBlockEntity(pos, state);
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
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    private static VoxelShape createShape(double topMinX, double topMinZ,
                                          double topMaxX, double topMaxZ,
                                          double leatherMinX, double leatherMinZ,
                                          double leatherMaxX, double leatherMaxZ) {
        return Shapes.or(
                box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0),
                box(5.0, 4.0, 5.0, 11.0, 6.0, 11.0),
                box(topMinX, 6.0, topMinZ, topMaxX, 10.0, topMaxZ),
                box(leatherMinX, 5.75, leatherMinZ, leatherMaxX, 10.25, leatherMaxZ));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        return open(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return open(level, pos, player);
    }

    private static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof ReliquaryStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) ->
                new ReliquaryStationMenu(containerId, inventory, station),
                Component.translatable("block.noblephantasms.reliquary_station")), buffer -> {
            buffer.writeBlockPos(pos);
        });
        return InteractionResult.SUCCESS_SERVER;
    }

}
