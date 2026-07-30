package net.turtleboi.noblephantasms.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.turtleboi.noblephantasms.block.ModSkullTypes;
import net.turtleboi.noblephantasms.block.TrophyHeadShapeCache;
import net.turtleboi.noblephantasms.block.entity.TrophyHeadBlockEntity;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;

public class TrophyHeadBlock extends SkullBlock {
    public static final MapCodec<TrophyHeadBlock> CODEC = simpleCodec(TrophyHeadBlock::new);

    public TrophyHeadBlock(BlockBehaviour.Properties properties) {
        super(ModSkullTypes.TROPHY, properties);
    }

    @Override
    public MapCodec<TrophyHeadBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyHeadBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof TrophyHeadBlockEntity trophyHead) {
            return TrophyHeadShapeCache.getShape(state, trophyHead.getTrophyData());
        }
        return TrophyHeadShapeCache.getShape(state, null);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
                                       boolean includeData, Player player) {
        if (level.getBlockEntity(pos) instanceof TrophyHeadBlockEntity trophyHead) {
            TrophyHeadItem.TrophyData data = trophyHead.getTrophyData();
            if (data != null) {
                return TrophyHeadItem.create(data.customData());
            }
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }
}
