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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;

public final class ReliquaryStationBlock extends Block {
    public static final MapCodec<ReliquaryStationBlock> CODEC = simpleCodec(ReliquaryStationBlock::new);

    public ReliquaryStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ReliquaryStationBlock> codec() {
        return CODEC;
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
