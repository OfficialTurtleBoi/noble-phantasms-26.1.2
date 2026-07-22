package net.turtleboi.noblephantasms.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;

public class ModItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider,
                item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow(), NoblePhantasms.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.GUNGNIR.get());
        tag(ItemTags.MELEE_WEAPON_ENCHANTABLE).add(ModItems.GUNGNIR.get());
        tag(LOYALTY_ENCHANTABLE)
                .addOptionalTag(ItemTags.TRIDENT_ENCHANTABLE)
                .add(ModItems.GUNGNIR.get());
        tag(PIERCING_ENCHANTABLE)
                .addOptionalTag(ItemTags.CROSSBOW_ENCHANTABLE)
                .add(ModItems.GUNGNIR.get());
    }

    private static final TagKey<Item> LOYALTY_ENCHANTABLE = TagKey.create(Registries.ITEM,
            Identifier.parse(NoblePhantasms.MOD_ID + ":enchantable/loyalty"));
    private static final TagKey<Item> PIERCING_ENCHANTABLE = TagKey.create(Registries.ITEM,
            Identifier.parse(NoblePhantasms.MOD_ID + ":enchantable/piercing"));
}
