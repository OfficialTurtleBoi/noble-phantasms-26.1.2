package net.turtleboi.noblephantasms.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, NoblePhantasms.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrophyHeadBlockEntity>> TROPHY_HEAD =
            BLOCK_ENTITIES.register("trophy_head", () -> new BlockEntityType<>(
                    TrophyHeadBlockEntity::new, ModBlocks.TROPHY_HEAD.get(), ModBlocks.TROPHY_WALL_HEAD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReliquaryStationBlockEntity>> RELIQUARY_STATION =
            BLOCK_ENTITIES.register("reliquary_station", () -> new BlockEntityType<>(
                    ReliquaryStationBlockEntity::new, ModBlocks.RELIQUARY_STATION.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
