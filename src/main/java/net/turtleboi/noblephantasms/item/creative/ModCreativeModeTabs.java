package net.turtleboi.noblephantasms.item.creative;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NoblePhantasms.MOD_ID);

    public static final Supplier<CreativeModeTab> NOBLE_PHANTASMS_TAB =
            CREATIVE_MODE_TABS.register("noble_phantasms_tab",
                    () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.EYE_OF_HORUS.get()))
                            .title(Component.translatable("creativetab.noblephantasms.title"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept(ModItems.EYE_OF_HORUS);
                                output.accept(ModItems.ANKH);
                                output.accept(ModItems.BERTILAK);
                                output.accept(ModItems.CARNWENNAN);
                                output.accept(ModItems.EXCALIBUR);
                                output.accept(ModItems.GJALLARHORN);
                                output.accept(ModItems.GUNGNIR);
                                output.accept(ModItems.KHOPESH_OF_RA);
                                output.accept(ModItems.KUSANAGI_NO_TSURUGI);
                                output.accept(ModItems.RHONGOMYNIAD);
                                output.accept(ModItems.HULIOSHJALMR);
                                output.accept(ModItems.UCHIDE_NO_KOZUCHI);
                                output.accept(ModItems.YAMAWARI);
                                output.accept(ModItems.ANDVARANAUT);
                                output.accept(ModItems.DRAUPNIR);
                                output.accept(ModItems.MEGINGJORD);
                                output.accept(ModItems.HEKA);
                                output.accept(ModItems.NEKHAKHA);
                                output.accept(ModItems.BOOK_OF_THOTH);
                            })
                            .build());

    public static void register (IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
