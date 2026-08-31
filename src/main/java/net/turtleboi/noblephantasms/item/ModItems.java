package net.turtleboi.noblephantasms.item;

import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.custom.AndvaranautItem;
import net.turtleboi.noblephantasms.item.custom.AnkhItem;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.BiaEnPetItem;
import net.turtleboi.noblephantasms.item.custom.ClydnoHalterItem;
import net.turtleboi.noblephantasms.item.custom.ChimalliItem;
import net.turtleboi.noblephantasms.item.custom.ClawsOfTepeyollotlItem;
import net.turtleboi.noblephantasms.item.custom.MedjuNetjerItem;
import net.turtleboi.noblephantasms.item.custom.MythicalReliquaryItem;
import net.turtleboi.noblephantasms.item.custom.CarnwennanItem;
import net.turtleboi.noblephantasms.item.custom.DraupnirItem;
import net.turtleboi.noblephantasms.item.custom.EagleKnightTalonsItem;
import net.turtleboi.noblephantasms.item.custom.ExcaliburItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;
import net.turtleboi.noblephantasms.item.custom.GjallarhornItem;
import net.turtleboi.noblephantasms.item.custom.GramItem;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import net.turtleboi.noblephantasms.item.custom.HekaItem;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;
import net.turtleboi.noblephantasms.item.custom.WebenItem;
import net.turtleboi.noblephantasms.item.custom.KazagurumaItem;
import net.turtleboi.noblephantasms.item.custom.KanaboItem;
import net.turtleboi.noblephantasms.item.custom.KusanagiNoTsurugiItem;
import net.turtleboi.noblephantasms.item.custom.MacuahuitlItem;
import net.turtleboi.noblephantasms.item.custom.MegingjordItem;
import net.turtleboi.noblephantasms.item.custom.NekhakhaItem;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import net.turtleboi.noblephantasms.item.custom.ScabbardItem;
import net.turtleboi.noblephantasms.item.custom.KheperScarabItem;
import net.turtleboi.noblephantasms.item.custom.ScalesOfMaatItem;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import net.turtleboi.noblephantasms.item.custom.UchideNoKozuchiItem;
import net.turtleboi.noblephantasms.item.custom.YamawariItem;
import net.turtleboi.noblephantasms.item.custom.HolyGrailItem;
import net.turtleboi.noblephantasms.item.custom.HofskorItem;
import net.turtleboi.noblephantasms.item.custom.PridwenItem;
import net.turtleboi.noblephantasms.item.custom.RaikoItem;
import net.turtleboi.noblephantasms.item.custom.RecallBellItem;
import net.turtleboi.noblephantasms.item.custom.SmokingMirrorItem;
import net.turtleboi.noblephantasms.item.custom.TyrfingItem;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;
import net.turtleboi.noblephantasms.item.custom.ApilolliItem;
import net.turtleboi.noblephantasms.item.custom.IwatoshiItem;
import net.turtleboi.noblephantasms.item.custom.TecpatlOfTheFifthSunItem;
import net.turtleboi.noblephantasms.item.custom.XiuhcoatlItem;
import net.turtleboi.noblephantasms.item.custom.YasakaniNoMagatamaItem;
import net.turtleboi.noblephantasms.item.custom.YataNoKagamiItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NoblePhantasms.MOD_ID);

    public static final DeferredItem<EyeOfHorusItem> EYE_OF_HORUS =
            registerRelic("eye_of_horus", EyeOfHorusItem::new);

    public static final DeferredItem<AnkhItem> ANKH =
            registerRelic("ankh", AnkhItem::new);

    public static final DeferredItem<KheperScarabItem> KHEPER_SCARAB =
            registerRelic("kheper_scarab", KheperScarabItem::new);

    public static final DeferredItem<ScalesOfMaatItem> SCALES_OF_MAAT =
            registerRelic("scales_of_maat", ScalesOfMaatItem::new);

    public static final DeferredItem<ScabbardItem> SCABBARD =
            registerRelic("scabbard", ScabbardItem::new);

    public static final DeferredItem<CarnwennanItem> CARNWENNAN =
            registerRelic("carnwennan", CarnwennanItem::new);

    public static final DeferredItem<BertilakItem> BERTILAK =
            registerRelic("bertilak", BertilakItem::new);

    public static final DeferredItem<TrophyHeadItem> TROPHY_HEAD =
            ITEMS.registerItem("trophy_head", properties -> new TrophyHeadItem(
                    ModBlocks.TROPHY_HEAD.get(), ModBlocks.TROPHY_WALL_HEAD.get(), properties));

    public static final DeferredItem<ExcaliburItem> EXCALIBUR =
            registerRelic("excalibur", ExcaliburItem::new);

    public static final DeferredItem<GungnirItem> GUNGNIR =
            registerRelic("gungnir", GungnirItem::new);

    public static final DeferredItem<GramItem> GRAM =
            registerRelic("gram", GramItem::new);

    public static final DeferredItem<TyrfingItem> TYRFING =
            registerRelic("tyrfing", TyrfingItem::new);

    public static final DeferredItem<GjallarhornItem> GJALLARHORN =
            registerRelic("gjallarhorn", GjallarhornItem::new);

    public static final DeferredItem<WebenItem> WEBEN =
            registerRelic("weben", WebenItem::new);

    public static final DeferredItem<BiaEnPetItem> BIA_EN_PET =
            registerRelic("bia_en_pet", BiaEnPetItem::new);

    public static final DeferredItem<KusanagiNoTsurugiItem> KUSANAGI_NO_TSURUGI =
            registerRelic("kusanagi_no_tsurugi", KusanagiNoTsurugiItem::new);

    public static final DeferredItem<YasakaniNoMagatamaItem> YASAKANI_NO_MAGATAMA =
            registerRelic("yasakani_no_magatama", YasakaniNoMagatamaItem::new);

    public static final DeferredItem<YataNoKagamiItem> YATA_NO_KAGAMI =
            registerRelic("yata_no_kagami", YataNoKagamiItem::new);

    public static final DeferredItem<IwatoshiItem> IWATOSHI =
            registerRelic("iwatoshi", IwatoshiItem::new);

    public static final DeferredItem<KazagurumaItem> KAZAGURUMA =
            registerRelic("kazaguruma", KazagurumaItem::new);

    public static final DeferredItem<KanaboItem> KANABO =
            registerRelic("kanabo", KanaboItem::new);

    public static final DeferredItem<RhongomyniadItem> RHONGOMYNIAD =
            registerRelic("rhongomyniad", RhongomyniadItem::new);

    public static final DeferredItem<PridwenItem> PRIDWEN =
            registerRelic("pridwen", PridwenItem::new);

    public static final DeferredItem<ClydnoHalterItem> CLYDNO_HALTER =
            registerRelic("clydno_halter", ClydnoHalterItem::new);

    public static final DeferredItem<RecallBellItem> RECALL_BELL =
            ITEMS.registerItem("recall_bell", RecallBellItem::new);

    public static final DeferredItem<HulioshjalmrItem> HULIOSHJALMR =
            registerRelic("hulioshjalmr", HulioshjalmrItem::new);

    public static final DeferredItem<UchideNoKozuchiItem> UCHIDE_NO_KOZUCHI =
            registerRelic("uchide_no_kozuchi", UchideNoKozuchiItem::new);

    public static final DeferredItem<YamawariItem> YAMAWARI =
            registerRelic("yamawari", YamawariItem::new);

    public static final DeferredItem<AndvaranautItem> ANDVARANAUT =
            registerRelic("andvaranaut", AndvaranautItem::new);

    public static final DeferredItem<DraupnirItem> DRAUPNIR =
            registerRelic("draupnir", DraupnirItem::new);

    public static final DeferredItem<EagleKnightTalonsItem> EAGLE_KNIGHT_TALONS =
            registerRelic("eagle_knight_talons", EagleKnightTalonsItem::new);

    public static final DeferredItem<MacuahuitlItem> MACUAHUITL =
            registerRelic("macuahuitl", MacuahuitlItem::new);

    public static final DeferredItem<MegingjordItem> MEGINGJORD =
            registerRelic("megingjord", MegingjordItem::new);

    public static final DeferredItem<HofskorItem> HOFSKOR =
            registerRelic("hofskor", HofskorItem::new);

    public static final DeferredItem<HekaItem> HEKA =
            registerRelic("heka", HekaItem::new);

    public static final DeferredItem<NekhakhaItem> NEKHAKHA =
            registerRelic("nekhakha", NekhakhaItem::new);

    public static final DeferredItem<MedjuNetjerItem> MEDJU_NETJER =
            registerRelic("medju_netjer", MedjuNetjerItem::new);

    public static final DeferredItem<RelicFragmentItem> RELIC_FRAGMENT =
            ITEMS.registerItem("relic_fragment", RelicFragmentItem::new);

    public static final DeferredItem<BlockItem> RELIQUARY_STATION =
            ITEMS.registerSimpleBlockItem(ModBlocks.RELIQUARY_STATION);

    public static final DeferredItem<BlockItem> BRAZIER =
            ITEMS.registerSimpleBlockItem(ModBlocks.BRAZIER);

    public static final DeferredItem<MythicalReliquaryItem> MYTHICAL_RELIQUARY =
            ITEMS.registerItem("mythical_reliquary", MythicalReliquaryItem::new);

    public static final DeferredItem<HolyGrailItem> HOLY_GRAIL =
            registerRelic("holy_grail", HolyGrailItem::new);

    public static final DeferredItem<SmokingMirrorItem> SMOKING_MIRROR =
            registerRelic("smoking_mirror", SmokingMirrorItem::new);

    public static final DeferredItem<RaikoItem> RAIKO =
            registerRelic("raiko", RaikoItem::new);

    public static final DeferredItem<ApilolliItem> APILOLLI =
            registerRelic("apilolli", ApilolliItem::new);

    public static final DeferredItem<XiuhcoatlItem> XIUHCOATL =
            registerRelic("xiuhcoatl", XiuhcoatlItem::new);

    public static final DeferredItem<ChimalliItem> CHIMALLI =
            registerRelic("chimalli", ChimalliItem::new);

    public static final DeferredItem<TecpatlOfTheFifthSunItem> TECPATL_OF_THE_FIFTH_SUN =
            registerRelic("tecpatl_of_the_fifth_sun", TecpatlOfTheFifthSunItem::new);

    public static final DeferredItem<ClawsOfTepeyollotlItem> CLAWS_OF_TEPEYOLLOTL =
            registerRelic("claws_of_tepeyollotl", ClawsOfTepeyollotlItem::new);

    public static final DeferredItem<AxeItem> NORTHERN_AXE =
            ITEMS.registerItem("northern_axe",
                    properties -> new AxeItem(ToolMaterial.IRON, 6.0F, -3.1F, properties));

    public static final DeferredItem<SpawnEggItem> ANUBITE_SPAWN_EGG =
            ITEMS.registerItem("anubite_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.ANUBITE.get()));

    public static final DeferredItem<SpawnEggItem> ECCLESIASTIC_SPAWN_EGG =
            ITEMS.registerItem("ecclesiastic_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.ECCLESIASTIC.get()));

    public static final DeferredItem<SpawnEggItem> DRAUGR_SPAWN_EGG =
            ITEMS.registerItem("draugr_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.DRAUGR.get()));

    public static final DeferredItem<SpawnEggItem> ONI_SPAWN_EGG =
            ITEMS.registerItem("oni_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.ONI.get()));

    public static final DeferredItem<SpawnEggItem> JAGUAR_MICQUI_SPAWN_EGG =
            ITEMS.registerItem("jaguar_micqui_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.JAGUAR_MICQUI.get()));

    static {
        ITEMS.addAlias(
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "claw_of_tepeyollotl"),
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "claws_of_tepeyollotl"));
    }

    private static <T extends Item> DeferredItem<T> registerRelic(
            String name, Function<Item.Properties, ? extends T> factory) {
        return ITEMS.registerItem(name, factory, properties -> properties.component(
                DataComponents.LORE,
                new ItemLore(List.of(Component.translatable("tooltip.noblephantasms." + name + ".flavor")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)))));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
