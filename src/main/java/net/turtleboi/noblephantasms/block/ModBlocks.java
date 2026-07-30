package net.turtleboi.noblephantasms.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.custom.TrophyHeadBlock;
import net.turtleboi.noblephantasms.block.custom.TrophyWallHeadBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NoblePhantasms.MOD_ID);

    public static final DeferredBlock<TrophyHeadBlock> TROPHY_HEAD =
            BLOCKS.registerBlock("trophy_head", TrophyHeadBlock::new, properties -> properties
                    .strength(1.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noOcclusion()
                    .dynamicShape());

    public static final DeferredBlock<TrophyWallHeadBlock> TROPHY_WALL_HEAD =
            BLOCKS.registerBlock("trophy_wall_head", TrophyWallHeadBlock::new, () ->
                    BlockBehaviour.Properties.ofFullCopy(TROPHY_HEAD.get())
                            .overrideLootTable(TROPHY_HEAD.get().getLootTable())
                            .overrideDescription(TROPHY_HEAD.get().getDescriptionId())
                            .dynamicShape());

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
