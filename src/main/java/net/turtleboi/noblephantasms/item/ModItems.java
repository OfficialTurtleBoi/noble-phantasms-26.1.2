package net.turtleboi.noblephantasms.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.item.custom.AndvaranautItem;
import net.turtleboi.noblephantasms.item.custom.AnkhItem;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.MedjuNetjerItem;
import net.turtleboi.noblephantasms.item.custom.CarnwennanItem;
import net.turtleboi.noblephantasms.item.custom.DraupnirItem;
import net.turtleboi.noblephantasms.item.custom.EagleKnightTalonsItem;
import net.turtleboi.noblephantasms.item.custom.ExcaliburItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;
import net.turtleboi.noblephantasms.item.custom.GjallarhornItem;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import net.turtleboi.noblephantasms.item.custom.HekaItem;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;
import net.turtleboi.noblephantasms.item.custom.WebenItem;
import net.turtleboi.noblephantasms.item.custom.KazagurumaItem;
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
import net.turtleboi.noblephantasms.item.custom.SmokingMirrorItem;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NoblePhantasms.MOD_ID);

    public static final DeferredItem<EyeOfHorusItem> EYE_OF_HORUS =
            ITEMS.registerItem("eye_of_horus", EyeOfHorusItem::new);

    public static final DeferredItem<AnkhItem> ANKH =
            ITEMS.registerItem("ankh", AnkhItem::new);

    public static final DeferredItem<KheperScarabItem> KHEPER_SCARAB =
            ITEMS.registerItem("kheper_scarab", KheperScarabItem::new);

    public static final DeferredItem<ScalesOfMaatItem> SCALES_OF_MAAT =
            ITEMS.registerItem("scales_of_maat", ScalesOfMaatItem::new);

    public static final DeferredItem<ScabbardItem> SCABBARD =
            ITEMS.registerItem("scabbard", ScabbardItem::new);

    public static final DeferredItem<CarnwennanItem> CARNWENNAN =
            ITEMS.registerItem("carnwennan", CarnwennanItem::new);

    public static final DeferredItem<BertilakItem> BERTILAK =
            ITEMS.registerItem("bertilak", BertilakItem::new);

    public static final DeferredItem<TrophyHeadItem> TROPHY_HEAD =
            ITEMS.registerItem("trophy_head", properties -> new TrophyHeadItem(
                    ModBlocks.TROPHY_HEAD.get(), ModBlocks.TROPHY_WALL_HEAD.get(), properties));

    public static final DeferredItem<ExcaliburItem> EXCALIBUR =
            ITEMS.registerItem("excalibur", ExcaliburItem::new);

    public static final DeferredItem<GungnirItem> GUNGNIR =
            ITEMS.registerItem("gungnir", GungnirItem::new);

    public static final DeferredItem<GjallarhornItem> GJALLARHORN =
            ITEMS.registerItem("gjallarhorn", GjallarhornItem::new);

    public static final DeferredItem<WebenItem> WEBEN =
            ITEMS.registerItem("weben", WebenItem::new);

    public static final DeferredItem<KusanagiNoTsurugiItem> KUSANAGI_NO_TSURUGI =
            ITEMS.registerItem("kusanagi_no_tsurugi", KusanagiNoTsurugiItem::new);

    public static final DeferredItem<KazagurumaItem> KAZAGURUMA =
            ITEMS.registerItem("kazaguruma", KazagurumaItem::new);

    public static final DeferredItem<RhongomyniadItem> RHONGOMYNIAD =
            ITEMS.registerItem("rhongomyniad", RhongomyniadItem::new);

    public static final DeferredItem<HulioshjalmrItem> HULIOSHJALMR =
            ITEMS.registerItem("hulioshjalmr", HulioshjalmrItem::new);

    public static final DeferredItem<UchideNoKozuchiItem> UCHIDE_NO_KOZUCHI =
            ITEMS.registerItem("uchide_no_kozuchi", UchideNoKozuchiItem::new);

    public static final DeferredItem<YamawariItem> YAMAWARI =
            ITEMS.registerItem("yamawari", YamawariItem::new);

    public static final DeferredItem<AndvaranautItem> ANDVARANAUT =
            ITEMS.registerItem("andvaranaut", AndvaranautItem::new);

    public static final DeferredItem<DraupnirItem> DRAUPNIR =
            ITEMS.registerItem("draupnir", DraupnirItem::new);

    public static final DeferredItem<EagleKnightTalonsItem> EAGLE_KNIGHT_TALONS =
            ITEMS.registerItem("eagle_knight_talons", EagleKnightTalonsItem::new);

    public static final DeferredItem<MacuahuitlItem> MACUAHUITL =
            ITEMS.registerItem("macuahuitl", MacuahuitlItem::new);

    public static final DeferredItem<MegingjordItem> MEGINGJORD =
            ITEMS.registerItem("megingjord", MegingjordItem::new);

    public static final DeferredItem<HekaItem> HEKA =
            ITEMS.registerItem("heka", HekaItem::new);

    public static final DeferredItem<NekhakhaItem> NEKHAKHA =
            ITEMS.registerItem("nekhakha", NekhakhaItem::new);

    public static final DeferredItem<MedjuNetjerItem> MEDJU_NETJER =
            ITEMS.registerItem("medju_netjer", MedjuNetjerItem::new);

    public static final DeferredItem<RelicFragmentItem> RELIC_FRAGMENT =
            ITEMS.registerItem("relic_fragment", RelicFragmentItem::new);

    public static final DeferredItem<BlockItem> RELIQUARY_STATION =
            ITEMS.registerSimpleBlockItem(ModBlocks.RELIQUARY_STATION);

    public static final DeferredItem<HolyGrailItem> HOLY_GRAIL =
            ITEMS.registerItem("holy_grail", HolyGrailItem::new);

    public static final DeferredItem<SmokingMirrorItem> SMOKING_MIRROR =
            ITEMS.registerItem("smoking_mirror", SmokingMirrorItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
