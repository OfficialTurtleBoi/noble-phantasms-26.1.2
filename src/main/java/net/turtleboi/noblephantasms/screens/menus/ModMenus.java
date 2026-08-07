package net.turtleboi.noblephantasms.screens.menus;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, NoblePhantasms.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReliquaryStationMenu>> RELIQUARY_STATION =
            MENUS.register("reliquary_station", () -> new MenuType<>((IContainerFactory<ReliquaryStationMenu>)
                    ReliquaryStationMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
