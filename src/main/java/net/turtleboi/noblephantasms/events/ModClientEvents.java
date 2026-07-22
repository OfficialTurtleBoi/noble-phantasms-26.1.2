package net.turtleboi.noblephantasms.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.GungnirExtensions;
import net.turtleboi.noblephantasms.particle.custom.GungnirRuneParticle;
import net.turtleboi.noblephantasms.entity.renderer.GungnirProjectileRenderer;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.particle.ModParticles;

@Mod(value = NoblePhantasms.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NoblePhantasms.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {
    public ModClientEvents(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {

    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new GungnirExtensions(), ModItems.GUNGNIR.get());
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUNGNIR.get(), GungnirProjectileRenderer::new);
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GUNGNIR_RUNE.get(), GungnirRuneParticle.Provider::new);
    }
}
