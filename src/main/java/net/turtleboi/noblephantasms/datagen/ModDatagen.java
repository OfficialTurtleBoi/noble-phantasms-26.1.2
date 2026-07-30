package net.turtleboi.noblephantasms.datagen;

import java.util.List;
import java.util.Set;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.turtleboi.noblephantasms.datagen.providers.ModEnchantmentProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModEntityTypeTagsProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModEquipmentAssetProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModItemTagsProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModLanguageProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModBlockLootProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModModelProvider;

public final class ModDatagen {
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(true, new ModModelProvider(packOutput));
        generator.addProvider(true, new ModLanguageProvider(packOutput));
        generator.addProvider(true, new ModEquipmentAssetProvider(packOutput));
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        event.addProvider(new ModEntityTypeTagsProvider(packOutput, event.getLookupProvider()));
        event.addProvider(new ModItemTagsProvider(packOutput, event.getLookupProvider()));
        event.addProvider(new ModEnchantmentProvider(packOutput, event.getResourceManager(PackType.SERVER_DATA)));
        event.addProvider(new LootTableProvider(packOutput, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(
                        ModBlockLootProvider::new, LootContextParamSets.BLOCK)), event.getLookupProvider()));
    }
}
