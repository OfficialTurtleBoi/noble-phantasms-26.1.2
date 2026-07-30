package net.turtleboi.noblephantasms.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.item.custom.AndvaranautItem;
import net.turtleboi.noblephantasms.item.custom.AnkhItem;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.BookOfThothItem;
import net.turtleboi.noblephantasms.item.custom.CarnwennanItem;
import net.turtleboi.noblephantasms.item.custom.DraupnirItem;
import net.turtleboi.noblephantasms.item.custom.ExcaliburItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;
import net.turtleboi.noblephantasms.item.custom.GjallarhornItem;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import net.turtleboi.noblephantasms.item.custom.HekaItem;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;
import net.turtleboi.noblephantasms.item.custom.KhopeshOfRaItem;
import net.turtleboi.noblephantasms.item.custom.KusanagiNoTsurugiItem;
import net.turtleboi.noblephantasms.item.custom.MegingjordItem;
import net.turtleboi.noblephantasms.item.custom.NekhakhaItem;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import net.turtleboi.noblephantasms.item.custom.ScabbardItem;
import net.turtleboi.noblephantasms.item.custom.ScarabOfKhepriItem;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import net.turtleboi.noblephantasms.item.custom.UchideNoKozuchiItem;
import net.turtleboi.noblephantasms.item.custom.YamawariItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NoblePhantasms.MOD_ID);

    public static final DeferredItem<EyeOfHorusItem> EYE_OF_HORUS =
            ITEMS.registerItem("eye_of_horus", EyeOfHorusItem::new);

    public static final DeferredItem<AnkhItem> ANKH =
            ITEMS.registerItem("ankh", AnkhItem::new);

    public static final DeferredItem<ScarabOfKhepriItem> SCARAB_OF_KHEPRI =
            ITEMS.registerItem("scarab_of_khepri", ScarabOfKhepriItem::new);

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

    public static final DeferredItem<KhopeshOfRaItem> KHOPESH_OF_RA =
            ITEMS.registerItem("khopesh", KhopeshOfRaItem::new);

    public static final DeferredItem<KusanagiNoTsurugiItem> KUSANAGI_NO_TSURUGI =
            ITEMS.registerItem("kusanagi_no_tsurugi", KusanagiNoTsurugiItem::new);

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

    public static final DeferredItem<MegingjordItem> MEGINGJORD =
            ITEMS.registerItem("megingjord", MegingjordItem::new);

    public static final DeferredItem<HekaItem> HEKA =
            ITEMS.registerItem("heka", HekaItem::new);

    public static final DeferredItem<NekhakhaItem> NEKHAKHA =
            ITEMS.registerItem("nekhakha", NekhakhaItem::new);

    public static final DeferredItem<BookOfThothItem> BOOK_OF_THOTH =
            ITEMS.registerItem("book_of_thoth", BookOfThothItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
