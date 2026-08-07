package net.turtleboi.noblephantasms.datagen.providers;

import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.item.ModItems;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, NoblePhantasms.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.ANKH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EYE_OF_HORUS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.KHEPER_SCARAB.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCALES_OF_MAAT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCABBARD.get(), ModelTemplates.FLAT_ITEM);
        generateHornItem(itemModels, ModItems.GJALLARHORN.get());
        itemModels.generateFlatItem(ModItems.HULIOSHJALMR.get(), ModelTemplates.FLAT_ITEM);
        Identifier talonsModel = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(ModItems.EAGLE_KNIGHT_TALONS.get()),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "item/eagle_knight_talons"))), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(ModItems.EAGLE_KNIGHT_TALONS.get(),
                ItemModelUtils.plainModel(talonsModel));
        itemModels.generateFlatItem(ModItems.CARNWENNAN.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.UCHIDE_NO_KOZUCHI.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.HEKA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.NEKHAKHA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.ANDVARANAUT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAUPNIR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.MEGINGJORD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.MEDJU_NETJER.get(), ModelTemplates.FLAT_ITEM);
        generateMappedFlatItem(itemModels, ModItems.RELIC_FRAGMENT.get(),
                Identifier.withDefaultNamespace("item/gold_nugget"));
        itemModels.generateFlatItem(ModItems.HOLY_GRAIL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SMOKING_MIRROR.get(), ModelTemplates.FLAT_ITEM);
        generateKazagurumaItem(itemModels, ModItems.KAZAGURUMA.get());
        var skullModel = BlockModelGenerators.plainVariant(
                ModelLocationUtils.decorateBlockModelLocation("skull"));
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(ModBlocks.TROPHY_HEAD.get(), skullModel));
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(ModBlocks.TROPHY_WALL_HEAD.get(), skullModel));
        Identifier reliquaryStationModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "block/reliquary_station");
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                ModBlocks.RELIQUARY_STATION.get(), BlockModelGenerators.plainVariant(reliquaryStationModel)));
        blockModels.registerSimpleItemModel(ModBlocks.RELIQUARY_STATION.get(), reliquaryStationModel);
        generateTrophyHeadItem(itemModels);
        generateBigItem(itemModels, ModItems.BERTILAK.get(), BigItemType.AXE);
        generateBigItem(itemModels, ModItems.EXCALIBUR.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.GUNGNIR.get(), BigItemType.SPEAR);
        generateBigItem(itemModels, ModItems.WEBEN.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.KUSANAGI_NO_TSURUGI.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.RHONGOMYNIAD.get(), BigItemType.LANCE);
        generateBigItem(itemModels, ModItems.YAMAWARI.get(), BigItemType.AXE);
        generateBigItem(itemModels, ModItems.MACUAHUITL.get(), BigItemType.SWORD,
                "macuahuitl", "macuahuitl");
    }

    private static void generateHornItem(ItemModelGenerators itemModels, Item item) {
        Identifier standardModel = HORN.create(
                ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), itemModels.modelOutput);
        Identifier tootingModel = HORN_TOOTING.create(
                ModelLocationUtils.getModelLocation(item, "_tooting"), TextureMapping.layer0(item), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.conditional(
                ItemModelUtils.isUsingItem(), ItemModelUtils.plainModel(tootingModel),
                ItemModelUtils.plainModel(standardModel)));
    }

    private static void generateMappedFlatItem(
            ItemModelGenerators itemModels, Item item, Identifier texture) {
        Identifier model = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(item),
                TextureMapping.layer0(new Material(texture)), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
    }

    private static void generateKazagurumaItem(ItemModelGenerators itemModels, Item item) {
        Identifier standardModel = itemModels.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        Identifier heldModel = itemModels.createFlatItemModel(item, "_held", ModelTemplates.FLAT_HANDHELD_ITEM);
        Identifier thrownModel = itemModels.createFlatItemModel(item, "_thrown", ModelTemplates.FLAT_HANDHELD_ITEM);
        var standardItemModel = ItemModelUtils.plainModel(standardModel);
        var heldItemModel = ItemModelUtils.plainModel(heldModel);
        var deployedHeldItemModel = ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModDataComponents.KAZAGURUMA_DEPLOYMENT.get()),
                ItemModelUtils.plainModel(thrownModel), heldItemModel);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(), standardItemModel,
                ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND), deployedHeldItemModel),
                ItemModelUtils.when(ItemDisplayContext.FIXED, heldItemModel)));
    }

    private static void generateTrophyHeadItem(ItemModelGenerators itemModels) {
        Identifier baseModel = Identifier.withDefaultNamespace("item/template_skull");
        itemModels.itemModelOutput.accept(ModItems.TROPHY_HEAD.get(), ItemModelUtils.specialModel(
                baseModel, BlockModelGenerators.SKULL_TRANSFORM, new TrophyHeadRenderer.Unbaked()));
    }

    public enum BigItemType {
        AXE,
        SWORD,
        SPEAR,
        LANCE
    }

    public static void generateBigItem(ItemModelGenerators itemModels, Item item, BigItemType type) {
        generateBigItem(itemModels, item, type, ModelLocationUtils.getModelLocation(item).getPath().substring(5));
    }

    public static void generateBigItem(
            ItemModelGenerators itemModels, Item item, BigItemType type, String textureBase) {
        generateBigItem(itemModels, item, type, textureBase + "_item", textureBase + "_weapon");
    }

    public static void generateBigItem(ItemModelGenerators itemModels, Item item, BigItemType type,
                                       String standardTexture, String heldTexture) {
        ModelTemplate template = switch (type) {
            case AXE -> BIG_HANDHELD_AXE;
            case SWORD -> BIG_HANDHELD_SWORD;
            case SPEAR -> BIG_HANDHELD_SPEAR;
            case LANCE -> BIG_HANDHELD_LANCE;
        };

        Identifier standardModel = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(item, "_item"),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "item/" + standardTexture))), itemModels.modelOutput);
        Identifier heldModel = template.create(
                ModelLocationUtils.getModelLocation(item, "_weapon"),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "item/" + heldTexture))), itemModels.modelOutput);
        var standardItemModel = ItemModelUtils.plainModel(standardModel);
        var heldItemModel = ItemModelUtils.plainModel(heldModel);

        if (type == BigItemType.SPEAR) {
            Identifier throwingModel = BIG_HANDHELD_SPEAR_THROWING.create(
                    ModelLocationUtils.getModelLocation(item, "_weapon_throwing"),
                    TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                            NoblePhantasms.MOD_ID, "item/" + heldTexture))), itemModels.modelOutput);
            var thirdPersonModel = ItemModelUtils.conditional(
                    ItemModelUtils.isUsingItem(), ItemModelUtils.plainModel(throwingModel), heldItemModel);

            itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(), standardItemModel,
                    ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                            ItemDisplayContext.THIRD_PERSON_LEFT_HAND), thirdPersonModel),
                    ItemModelUtils.when(List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                            ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIXED), heldItemModel)));
            return;
        }

        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(), standardItemModel,
                ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND), heldItemModel)));
    }

    private static final ModelTemplate HORN = ModelTemplates.FLAT_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, 180.0F, 0.0F)
                    .translation(0.0F, 3.0F, 1.0F)
                    .scale(0.825F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, 0.0F, 0.0F)
                    .translation(0.0F, 3.0F, 1.0F)
                    .scale(0.825F))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 25.0F)
                    .translation(1.13F, 3.2F, 1.13F)
                    .scale(1.02F))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, 90.0F, -25.0F)
                    .translation(1.13F, 3.2F, 1.13F)
                    .scale(1.02F))
            .build();

    private static final ModelTemplate HORN_TOOTING = ModelTemplates.FLAT_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -125.0F, 0.0F)
                    .translation(-1.0F, 2.0F, 2.0F)
                    .scale(0.75F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, 55.0F, 0.0F)
                    .translation(-1.0F, 2.0F, 2.0F)
                    .scale(0.75F))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -55.0F, -5.0F)
                    .translation(-1.0F, -2.5F, -7.5F)
                    .scale(1.5F))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, 115.0F, 5.0F)
                    .translation(0.0F, -2.5F, -7.5F)
                    .scale(1.5F))
            .build();

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

    private static final ModelTemplate BIG_HANDHELD_AXE = ModelTemplates.FLAT_HANDHELD_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(-15.0F, -90.0F, 45.0F)
                    .translation(0.0F, -4.5F, 0.0F)
                    .scale(1.7F, 1.7F, 0.85F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(-105.0F, 90.0F, 45.0F)
                    .translation(0.0F, -4.5F, 0.0F)
                    .scale(1.7F, 1.7F, 0.85F))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(-20.0F, -90.0F, 15.0F)
                    .translation(1.5F, 3.5F, -0.75F)
                    .scale(1.36F, 1.36F, 0.68F))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                    .rotation(-50.0F, 90.0F, 15.0F)
                    .translation(1.5F, 3.5F, -0.75F)
                    .scale(1.36F, 1.36F, 0.68F))
            .build();

    private static final ModelTemplate BIG_HANDHELD_LANCE = ModelTemplates.FLAT_HANDHELD_ITEM.extend()
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

    private static final ModelTemplate BIG_HANDHELD_SPEAR_THROWING = ModelTemplates.FLAT_HANDHELD_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -135.0F)
                    .translation(0.0F, 1.25F, 1.5F)
                    .scale(2.55F, 2.55F, 0.85F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 135.0F)
                    .translation(0.0F, 1.25F, 1.5F)
                    .scale(2.55F, 2.55F, 0.85F))
            .build();
}
