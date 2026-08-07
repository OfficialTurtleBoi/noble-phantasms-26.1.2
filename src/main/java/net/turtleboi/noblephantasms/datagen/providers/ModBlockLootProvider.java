package net.turtleboi.noblephantasms.datagen.providers;

import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.item.ModItems;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(ModItems.TROPHY_HEAD.get()), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        Block trophyHead = ModBlocks.TROPHY_HEAD.get();
        add(trophyHead, LootTable.lootTable().withPool(
                applyExplosionCondition(trophyHead, LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(trophyHead)
                                .apply(CopyComponentsFunction
                                        .copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                        .include(DataComponents.CUSTOM_DATA))))));
        dropSelf(ModBlocks.RELIQUARY_STATION.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(entry -> (Block) entry.get()).toList();
    }
}
