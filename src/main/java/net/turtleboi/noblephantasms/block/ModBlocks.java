package net.turtleboi.noblephantasms.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.custom.TrophyHeadBlock;
import net.turtleboi.noblephantasms.block.custom.TrophyWallHeadBlock;
import net.turtleboi.noblephantasms.block.custom.ReliquaryStationBlock;
import net.minecraft.world.level.block.Blocks;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NoblePhantasms.MOD_ID);

    public static final DeferredBlock<TrophyHeadBlock> TROPHY_HEAD =
            BLOCKS.registerBlock("trophy_head", TrophyHeadBlock::new, properties -> properties
                    .instrument(NoteBlockInstrument.CUSTOM_HEAD)
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

    public static final DeferredBlock<ReliquaryStationBlock> RELIQUARY_STATION =
            BLOCKS.registerBlock("reliquary_station", ReliquaryStationBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
