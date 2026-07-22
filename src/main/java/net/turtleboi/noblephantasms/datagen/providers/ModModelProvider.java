package net.turtleboi.noblephantasms.datagen.providers;

import java.util.List;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, NoblePhantasms.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.ANKH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EYE_OF_HORUS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CARNWENNAN.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generateBigItem(itemModels, ModItems.EXCALIBUR.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.GUNGNIR.get(), BigItemType.SPEAR);
        generateBigItem(itemModels, ModItems.KHOPESH_OF_RA.get(), BigItemType.SWORD);
    }

    public static void generateBigItem(ItemModelGenerators itemModels, Item item, BigItemType type) {
        ModelTemplate template = switch (type) {
            case SWORD -> BIG_HANDHELD_SWORD;
            case SPEAR -> BIG_HANDHELD_SPEAR;
        };

        Identifier standardModel = itemModels.createFlatItemModel(item, "_item", ModelTemplates.FLAT_ITEM);
        Identifier heldModel = itemModels.createFlatItemModel(item, "_weapon", template);
        List<ItemDisplayContext> heldContexts = switch (type) {
            case SWORD -> List.of(
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                    ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            case SPEAR -> List.of(
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                    ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    ItemDisplayContext.FIXED);
        };

        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(
                new DisplayContext(),
                ItemModelUtils.plainModel(standardModel),
                ItemModelUtils.when(heldContexts, ItemModelUtils.plainModel(heldModel))
        ));
    }

    public enum BigItemType {
        SWORD,
        SPEAR
    }

    private static final ModelTemplate BIG_HANDHELD_SWORD = ModelTemplates.FLAT_HANDHELD_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 45.0F)
                    .translation(0.0F, 11.0F, 1.5F)
                    .scale(1.7F, 1.7F, 0.85F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -45.0F)
                    .translation(0.0F, 11.0F, 1.5F)
                    .scale(1.7F, 1.7F, 0.85F))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 35.0F)
                    .translation(1.26F, 7.5F, 1.13F)
                    .scale(1.36F, 1.36F, 0.68F))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -45.0F)
                    .translation(1.26F, 7.5F, 1.13F)
                    .scale(1.36F, 1.36F, 0.68F))
            .build();

    private static final ModelTemplate BIG_HANDHELD_SPEAR = ModelTemplates.FLAT_HANDHELD_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 45.0F)
                    .translation(0.0F, 1.25F, 1.5F)
                    .scale(2.55F, 2.55F, 0.85F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -45.0F)
                    .translation(0.0F, 1.25F, 1.5F)
                    .scale(2.55F, 2.55F, 0.85F))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 35.0F)
                    .translation(1.26F, 1.5F, 1.13F)
                    .scale(2.04F, 2.04F, 0.68F))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -45.0F)
                    .translation(1.26F, 1.5F, 1.13F)
                    .scale(2.04F, 2.04F, 0.68F))
            .transform(ItemDisplayContext.FIXED, transform -> transform
                    .rotation(0.0F, 0.0F, -135.0F)
                    .scale(3.0F, 3.0F, 1.0F))
            .build();
}
