package net.turtleboi.noblephantasms.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.server.packs.PackType;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.datagen.providers.ModEnchantmentProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModItemTagsProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModLanguageProvider;
import net.turtleboi.noblephantasms.datagen.providers.ModModelProvider;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID)
public class ModDatagen {
    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(true, new ModModelProvider(packOutput));
        generator.addProvider(true, new ModLanguageProvider(packOutput));
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        event.addProvider(new ModItemTagsProvider(packOutput, event.getLookupProvider()));
        event.addProvider(new ModEnchantmentProvider(
                packOutput,
                event.getResourceManager(PackType.SERVER_DATA)));
    }
}
