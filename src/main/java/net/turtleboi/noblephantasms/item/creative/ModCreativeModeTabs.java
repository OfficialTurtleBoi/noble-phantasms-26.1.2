package net.turtleboi.noblephantasms.item.creative;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NoblePhantasms.MOD_ID);

    public static final Supplier<CreativeModeTab> NOBLE_PHANTASMS_TAB =
            CREATIVE_MODE_TABS.register("noble_phantasms_tab",
                    () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.EYE_OF_HORUS.get()))
                            .title(Component.translatable("creativetab.noblephantasms.title"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept(ModItems.WEBEN);
                                output.accept(ModItems.BIA_EN_PET);
                                output.accept(ModItems.HEKA);
                                output.accept(ModItems.NEKHAKHA);
                                output.accept(ModItems.EYE_OF_HORUS);
                                output.accept(ModItems.ANKH);
                                output.accept(ModItems.KHEPER_SCARAB);
                                output.accept(ModItems.SCALES_OF_MAAT);
                                output.accept(ModItems.MEDJU_NETJER);

                                output.accept(ModItems.EAGLE_KNIGHT_TALONS);
                                output.accept(ModItems.MACUAHUITL);
                                output.accept(ModItems.SMOKING_MIRROR);

                                output.accept(ModItems.EXCALIBUR);
                                output.accept(ModItems.SCABBARD);
                                output.accept(ModItems.CARNWENNAN);
                                output.accept(ModItems.RHONGOMYNIAD);
                                output.accept(ModItems.PRIDWEN);
                                output.accept(ModItems.CLYDNO_HALTER);
                                output.accept(ModItems.BERTILAK);
                                output.accept(ModItems.HOLY_GRAIL);

                                output.accept(ModItems.GUNGNIR);
                                output.accept(ModItems.GRAM);
                                output.accept(ModItems.TYRFING);
                                output.accept(ModItems.GJALLARHORN);
                                output.accept(ModItems.HULIOSHJALMR);
                                output.accept(ModItems.ANDVARANAUT);
                                output.accept(ModItems.DRAUPNIR);
                                output.accept(ModItems.MEGINGJORD);
                                output.accept(ModItems.HOFSKOR);

                                output.accept(ModItems.KUSANAGI_NO_TSURUGI);
                                output.accept(ModItems.KAZAGURUMA);
                                output.accept(ModItems.UCHIDE_NO_KOZUCHI);
                                output.accept(ModItems.YAMAWARI);
                                output.accept(ModItems.RAIKO);
                                output.accept(ModItems.RELIQUARY_STATION);
                                RelicFragmentDefinitions.relicIds().forEach(relicId -> output.accept(
                                        RelicFragmentItem.create(ModItems.RELIC_FRAGMENT.get(),
                                                RelicFragmentData.forgePiece(relicId),
                                                1)));
                            })
                            .build());

    public static final Supplier<CreativeModeTab> TROPHY_HEADS_TAB =
            CREATIVE_MODE_TABS.register("trophy_heads_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> TrophyHeadItem.create(
                                    BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW)))
                            .title(Component.translatable("creativetab.noblephantasms.trophy_heads"))
                            .displayItems((itemDisplayParameters, output) ->
                                    TrophyHeadItem.createCreativeTabHeads().forEach(output::accept))
                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
