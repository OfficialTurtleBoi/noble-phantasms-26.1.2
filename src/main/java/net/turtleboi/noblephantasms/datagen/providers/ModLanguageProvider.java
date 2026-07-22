package net.turtleboi.noblephantasms.datagen.providers;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, NoblePhantasms.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addSimpleItem(ModItems.ANKH);
        addSimpleItem(ModItems.CARNWENNAN);
        addSimpleItem(ModItems.EXCALIBUR);
        addSimpleItem(ModItems.EYE_OF_HORUS, "Eye of Horus");
        addSimpleItem(ModItems.GUNGNIR);
        addSimpleItem(ModItems.KHOPESH_OF_RA, "Khopesh of Ra");

        add("creativetab.noblephantasms.title", "Noble Phantasms");
        add("noblephantasms.configuration.title", "Noble Phantasms Configs");
        add("noblephantasms.configuration.section.noblephantasms.common.toml", "Noble Phantasms Configs");
        add("noblephantasms.configuration.section.noblephantasms.common.toml.title", "Noble Phantasms Configs");
    }

    protected void addSimpleItem(DeferredItem<?> item) {
        addSimpleItem(item, toDisplayName(item.getId().getPath()));
    }

    protected void addSimpleItem(DeferredItem<?> item, String displayName) {
        add(item.get(), displayName);
    }

    private static String toDisplayName(String registryPath) {
        return Arrays.stream(registryPath.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
