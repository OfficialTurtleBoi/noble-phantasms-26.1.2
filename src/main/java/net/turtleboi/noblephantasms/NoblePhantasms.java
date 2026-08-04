package net.turtleboi.noblephantasms;

import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.block.entity.ModBlockEntities;
import net.turtleboi.noblephantasms.config.ModConfig;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.creative.ModCreativeModeTabs;
import net.turtleboi.noblephantasms.screens.menus.ModMenus;
import net.turtleboi.noblephantasms.particle.ModParticles;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.*;


@Mod(NoblePhantasms.MOD_ID)
public class NoblePhantasms {
    public static final String MOD_ID = "noblephantasms";
    public static final Logger LOGGER = LogUtils.getLogger();
    public NoblePhantasms(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenus.register(modEventBus);
        ModItems.register(modEventBus);

        modContainer.registerConfig(Type.COMMON, ModConfig.SPEC);
    }
}
