package net.turtleboi.noblephantasms.datagen.providers;

import com.mojang.math.Transformation;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.client.renderer.TecpatlRebuildingRenderer;
import net.turtleboi.noblephantasms.client.renderer.RelicFragmentRenderer;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.client.HolyGrailChargeProperty;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.item.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModModelProvider extends ModelProvider {
    private static final Transformation SHIELD_HANDLE_TRANSFORMATION = new Transformation(
            new Vector3f(-0.5F, 0.6875F, 0.4375F), null, new Vector3f(1.0F, -1.0F, -1.0F), null);
    private static final Transformation SHIELD_PLATE_NINETY_TRANSFORMATION = new Transformation(
            new Vector3f(0.6875F, 0.5F, 0.4375F), null, new Vector3f(1.0F, -1.0F, -1.0F),
            new Quaternionf().rotationZ((float) (Math.PI / 2.0)));
    private static final Transformation SHIELD_PLATE_FLIPPED_TRANSFORMATION = new Transformation(
            new Vector3f(0.5F, -0.6875F, 0.4375F), null, new Vector3f(1.0F, -1.0F, -1.0F),
            new Quaternionf().rotationZ((float) Math.PI));

    public ModModelProvider(PackOutput output) {
        super(output, NoblePhantasms.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.ANKH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EYE_OF_HORUS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.KHEPER_SCARAB.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCALES_OF_MAAT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BIA_EN_PET.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
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
        itemModels.generateFlatItem(ModItems.TYRFING.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.UCHIDE_NO_KOZUCHI.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.HEKA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.NEKHAKHA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.ANDVARANAUT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAUPNIR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.MEGINGJORD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.HOFSKOR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CLYDNO_HALTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RECALL_BELL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.NORTHERN_AXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generatePridwenItem(itemModels);
        itemModels.generateFlatItem(ModItems.MEDJU_NETJER.get(), ModelTemplates.FLAT_ITEM);
        generateRelicFragmentItem(itemModels);
        itemModels.generateFlatItem(ModItems.RELIC_FRAGMENTS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RELIC_FRAGMENTS_ARTHURIAN.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RELIC_FRAGMENTS_AZTEC.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RELIC_FRAGMENTS_EGYPT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RELIC_FRAGMENTS_JAPANESE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RELIC_FRAGMENTS_NORSE.get(), ModelTemplates.FLAT_ITEM);
        generateMappedFlatItem(itemModels, ModItems.MYTHICAL_RELIQUARY.get(),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/mythic_reliquary"));
        generateMappedFlatItem(itemModels, ModItems.ANUBITE_SPAWN_EGG.get(),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/anubite_egg"));
        generateMappedFlatItem(itemModels, ModItems.ECCLESIASTIC_SPAWN_EGG.get(),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/ecclesiastic_egg"));
        generateMappedFlatItem(itemModels, ModItems.DRAUGR_SPAWN_EGG.get(),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/draugr_egg"));
        generateMappedFlatItem(itemModels, ModItems.ONI_SPAWN_EGG.get(),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/oni_egg"));
        generateMappedFlatItem(itemModels, ModItems.JAGUAR_MICQUI_SPAWN_EGG.get(),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/jaguar_micqui_egg"));
        itemModels.generateFlatItem(ModItems.HOLY_GRAIL.get(), ModelTemplates.FLAT_ITEM);
        generateHolyGrailItem(itemModels);
        itemModels.generateFlatItem(ModItems.SMOKING_MIRROR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.YASAKANI_NO_MAGATAMA.get(), ModelTemplates.FLAT_ITEM);
        generateYataNoKagamiItem(itemModels);
        itemModels.generateFlatItem(ModItems.APILOLLI.get(), ModelTemplates.FLAT_ITEM);
        generateChimalliItem(itemModels);
        itemModels.generateFlatItem(ModItems.CLAWS_OF_TEPEYOLLOTL.get(), ModelTemplates.FLAT_ITEM);
        generateTecpatlItem(itemModels);
        generateKazagurumaItem(itemModels, ModItems.KAZAGURUMA.get());
        generateRaikoItem(itemModels);
        var skullModel = BlockModelGenerators.plainVariant(
                ModelLocationUtils.decorateBlockModelLocation("skull"));
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(ModBlocks.TROPHY_HEAD.get(), skullModel));
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(ModBlocks.TROPHY_WALL_HEAD.get(), skullModel));
        Identifier brazierModel = ModelLocationUtils.getModelLocation(ModBlocks.BRAZIER.get());
        Identifier brazierFireModel = ModelLocationUtils.getModelLocation(ModBlocks.BRAZIER.get(), "_fire");
        blockModels.blockStateOutput.accept(MultiPartGenerator.multiPart(ModBlocks.BRAZIER.get())
                .with(BlockModelGenerators.plainVariant(brazierModel))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.LIT, true),
                        BlockModelGenerators.plainVariant(brazierFireModel)));
        blockModels.registerSimpleItemModel(ModBlocks.BRAZIER.get(), brazierModel);
        generateTrophyHeadItem(itemModels);
        generateBigItem(itemModels, ModItems.BERTILAK.get(), BigItemType.AXE);
        generateBigItem(itemModels, ModItems.EXCALIBUR.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.GRAM.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.GUNGNIR.get(), BigItemType.THROWING_SPEAR);
        generateBigItem(itemModels, ModItems.WEBEN.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.KUSANAGI_NO_TSURUGI.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.RHONGOMYNIAD.get(), BigItemType.LANCE);
        generateBigItem(itemModels, ModItems.YAMAWARI.get(), BigItemType.AXE);
        generateBigItem(itemModels, ModItems.MACUAHUITL.get(), BigItemType.SWORD);
        generateBigItem(itemModels, ModItems.IWATOSHI.get(), BigItemType.SPEAR);
        generateBigItem(itemModels, ModItems.XIUHCOATL.get(), BigItemType.STAFF);
        generateBigItem(itemModels, ModItems.KANABO.get(), BigItemType.SWORD);
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

    private static void generateHolyGrailItem(ItemModelGenerators itemModels) {
        Item item = ModItems.HOLY_GRAIL.get();
        Identifier full = createFlatItemModel(itemModels, item, "", "holy_grail");
        Identifier fillTwo = createFlatItemModel(itemModels, item, "_2", "holy_grail2");
        Identifier fillThree = createFlatItemModel(itemModels, item, "_3", "holy_grail3");
        Identifier fillFour = createFlatItemModel(itemModels, item, "_4", "holy_grail4");
        Identifier empty = createFlatItemModel(itemModels, item, "_empty", "holy_grail_empty");
        itemModels.itemModelOutput.accept(item, ItemModelUtils.rangeSelect(
                new HolyGrailChargeProperty(), ItemModelUtils.plainModel(full), List.of(
                        ItemModelUtils.override(ItemModelUtils.plainModel(empty), 0.0F),
                        ItemModelUtils.override(ItemModelUtils.plainModel(fillFour), 0.0001F),
                        ItemModelUtils.override(ItemModelUtils.plainModel(fillThree), 0.25F),
                        ItemModelUtils.override(ItemModelUtils.plainModel(fillTwo), 0.5F),
                        ItemModelUtils.override(ItemModelUtils.plainModel(full), 0.75F))));
    }

    private static Identifier createFlatItemModel(
            ItemModelGenerators itemModels, Item item, String suffix, String textureName) {
        return ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(item, suffix),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "item/" + textureName))), itemModels.modelOutput);
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

    private static void generateRaikoItem(ItemModelGenerators itemModels) {
        Identifier standardModel = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(ModItems.RAIKO.get(), "_item"),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "item/raiko_item"))), itemModels.modelOutput);
        Identifier drumModel = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/raiko_drum");
        itemModels.itemModelOutput.accept(ModItems.RAIKO.get(), ItemModelUtils.select(
                new DisplayContext(), ItemModelUtils.plainModel(standardModel),
                ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND), ItemModelUtils.plainModel(drumModel))));
    }

    private static void generateChimalliItem(ItemModelGenerators itemModels) {
        Item item = ModItems.CHIMALLI.get();
        Identifier spriteModel = itemModels.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        Identifier plateModel = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/chimalli_shield");
        Identifier blockingPlateModel = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/chimalli_shield_blocking");
        Identifier handleModel = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/chimalli_shield_handle");
        Identifier blockingHandleModel = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/chimalli_shield_handle_blocking");
        var handle = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingHandleModel, SHIELD_HANDLE_TRANSFORMATION),
                transformedModel(handleModel, SHIELD_HANDLE_TRANSFORMATION));
        var thirdPersonPlate = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingPlateModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION),
                transformedModel(plateModel, SHIELD_PLATE_NINETY_TRANSFORMATION));
        var firstPersonPlate = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingPlateModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION),
                transformedModel(plateModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION));
        var previewShield = ItemModelUtils.composite(
                ItemModelUtils.plainModel(plateModel), ItemModelUtils.plainModel(handleModel));
        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(),
                ItemModelUtils.plainModel(spriteModel), List.of(
                        ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND), ItemModelUtils.composite(thirdPersonPlate, handle)),
                        ItemModelUtils.when(List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                                ItemDisplayContext.FIRST_PERSON_LEFT_HAND), ItemModelUtils.composite(firstPersonPlate, handle)),
                        ItemModelUtils.when(ItemDisplayContext.FIXED, previewShield))));
    }

    private static void generateYataNoKagamiItem(ItemModelGenerators itemModels) {
        Item item = ModItems.YATA_NO_KAGAMI.get();
        Identifier spriteModel = itemModels.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        Identifier shieldModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/yata_no_kagami_shield");
        Identifier blockingShieldModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/yata_no_kagami_shield_blocking");
        Identifier handleModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/yata_no_kagami_shield_handle");
        Identifier blockingHandleModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/yata_no_kagami_shield_handle_blocking");
        var handle = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingHandleModel, SHIELD_HANDLE_TRANSFORMATION),
                transformedModel(handleModel, SHIELD_HANDLE_TRANSFORMATION));
        var thirdPersonShield = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingShieldModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION),
                transformedModel(shieldModel, SHIELD_PLATE_NINETY_TRANSFORMATION));
        var firstPersonShield = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingShieldModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION),
                transformedModel(shieldModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION));
        var previewShield = ItemModelUtils.composite(
                ItemModelUtils.plainModel(shieldModel), ItemModelUtils.plainModel(handleModel));
        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(),
                ItemModelUtils.plainModel(spriteModel), List.of(
                        ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
                                ItemModelUtils.composite(thirdPersonShield, handle)),
                        ItemModelUtils.when(List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                                ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
                                ItemModelUtils.composite(firstPersonShield, handle)),
                        ItemModelUtils.when(ItemDisplayContext.FIXED, previewShield))));
    }

    private static void generatePridwenItem(ItemModelGenerators itemModels) {
        Item item = ModItems.PRIDWEN.get();
        Identifier spriteModel = itemModels.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        Identifier plateModel = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/pridwen_shield");
        Identifier blockingPlateModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/pridwen_shield_blocking");
        Identifier handleModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/pridwen_shield_handle");
        Identifier blockingHandleModel = Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "item/pridwen_shield_handle_blocking");
        var handle = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingHandleModel, SHIELD_HANDLE_TRANSFORMATION),
                transformedModel(handleModel, SHIELD_HANDLE_TRANSFORMATION));
        var plate = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                transformedModel(blockingPlateModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION),
                transformedModel(plateModel, SHIELD_PLATE_FLIPPED_TRANSFORMATION));
        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(),
                ItemModelUtils.plainModel(spriteModel), List.of(
                        ItemModelUtils.when(List.of(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                                ItemDisplayContext.FIRST_PERSON_LEFT_HAND), ItemModelUtils.composite(plate, handle)))));
    }

    private static ItemModel.Unbaked transformedModel(Identifier model, Transformation transformation) {
        return new CuboidItemModelWrapper.Unbaked(model, Optional.of(transformation), List.of());
    }

    private static void generateTecpatlItem(ItemModelGenerators itemModels) {
        Item item = ModItems.TECPATL_OF_THE_FIFTH_SUN.get();
        Identifier model = ModelTemplates.FLAT_HANDHELD_ITEM.create(
                ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModDataComponents.TECPATL_DEPLOYMENT.get()),
                ItemModelUtils.specialModel(model, new TecpatlRebuildingRenderer.Unbaked()),
                ItemModelUtils.plainModel(model)));
    }

    private static void generateRelicFragmentItem(ItemModelGenerators itemModels) {
        Item item = ModItems.RELIC_FRAGMENT.get();
        Identifier model = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(item),
                TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "item/relic_fragments"))), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item,
                ItemModelUtils.specialModel(model, new RelicFragmentRenderer.Unbaked()));
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
        THROWING_SPEAR,
        LANCE,
        STAFF
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
            case SPEAR, THROWING_SPEAR -> BIG_HANDHELD_SPEAR;
            case LANCE -> BIG_HANDHELD_LANCE;
            case STAFF -> BIG_HANDHELD_STAFF;
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

        if (type == BigItemType.THROWING_SPEAR) {
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

    private static final ModelTemplate BIG_HANDHELD_STAFF = ModelTemplates.FLAT_HANDHELD_ITEM.extend()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 45.0F)
                    .translation(0.0F, 0.0F, 1.5F)
                    .scale(1.7F, 1.7F, 0.85F))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -45.0F)
                    .translation(0.0F, 0.0F, 1.5F)
                    .scale(1.7F, 1.7F, 0.85F))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, 35.0F)
                    .translation(1.26F, 0.0F, 1.13F)
                    .scale(1.36F, 1.36F, 0.68F))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                    .rotation(0.0F, -90.0F, -45.0F)
                    .translation(1.26F, 0.0F, 1.13F)
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
