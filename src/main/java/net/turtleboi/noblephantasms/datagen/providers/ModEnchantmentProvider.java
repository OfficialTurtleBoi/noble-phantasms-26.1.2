package net.turtleboi.noblephantasms.datagen.providers;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.turtleboi.noblephantasms.NoblePhantasms;

public class ModEnchantmentProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;
    private final ResourceManager resourceManager;

    public ModEnchantmentProvider(PackOutput output, ResourceManager resourceManager) {
        pathProvider = output.createRegistryElementsPathProvider(Registries.ENCHANTMENT);
        this.resourceManager = resourceManager;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        try {
            return CompletableFuture.allOf(
                    generate(output, Enchantments.LOYALTY,
                            "#" + NoblePhantasms.MOD_ID + ":enchantable/loyalty"),
                    generate(output, Enchantments.PIERCING,
                            "#" + NoblePhantasms.MOD_ID + ":enchantable/piercing"));
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<?> generate(CachedOutput output, ResourceKey<Enchantment> enchantment,
            String supportedItems) throws IOException {
        Identifier enchantmentId = enchantment.identifier();
        Identifier sourceId = Identifier.fromNamespaceAndPath(enchantmentId.getNamespace(),
                "enchantment/" + enchantmentId.getPath() + ".json");
        Resource source = resourceManager.getResource(sourceId).orElseThrow(
                () -> new IOException("Missing vanilla enchantment resource " + sourceId));
        JsonObject json;
        try (var reader = source.openAsReader()) {
            json = GsonHelper.parse(reader);
        }
        json.addProperty("supported_items", supportedItems);
        Path path = pathProvider.json(enchantment);
        return DataProvider.saveStable(output, json, path);
    }

    @Override
    public String getName() {
        return "Modified enchantments - " + NoblePhantasms.MOD_ID;
    }
}
