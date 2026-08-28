package net.turtleboi.noblephantasms.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakExtensions;
import net.turtleboi.noblephantasms.client.EagleKnightTalonsExtensions;
import net.turtleboi.noblephantasms.client.ExcaliburExtensions;
import net.turtleboi.noblephantasms.client.GungnirExtensions;
import net.turtleboi.noblephantasms.client.HulioshjalmrExtensions;
import net.turtleboi.noblephantasms.client.model.EagleKnightTalonsModel;
import net.turtleboi.noblephantasms.client.model.HulioshjalmrModel;
import net.turtleboi.noblephantasms.client.model.YasakaniGuardianModel;
import net.turtleboi.noblephantasms.entity.model.XiuhcoatlModel;
import net.turtleboi.noblephantasms.client.renderer.ClydnoHalterLayer;
import net.turtleboi.noblephantasms.client.renderer.AfterimageRenderer;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.client.renderer.TecpatlRebuildingRenderer;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadBlockEntityRenderer;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.datagen.ModDatagen;
import net.turtleboi.noblephantasms.entity.renderer.EyeShardRenderer;
import net.turtleboi.noblephantasms.entity.renderer.ExcaliburProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.GungnirProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.KazagurumaProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.SimpleEntityRenderers;
import net.turtleboi.noblephantasms.entity.renderer.TecpatlShardRenderer;
import net.turtleboi.noblephantasms.entity.renderer.WindslashRenderer;
import net.turtleboi.noblephantasms.entity.renderer.XiuhcoatlProjectileRenderer;
import net.turtleboi.noblephantasms.particle.custom.CovenantLeafParticle;
import net.turtleboi.noblephantasms.particle.custom.FireFangsParticle;
import net.turtleboi.noblephantasms.particle.custom.GungnirRuneParticle;
import net.turtleboi.noblephantasms.screens.ReliquaryStationScreen;
import net.turtleboi.noblephantasms.screens.menus.ModMenus;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID, value = Dist.CLIENT)
public final class ClientModBusEvents {
    @SubscribeEvent
    static void configureMainRenderTarget(ConfigureMainRenderTargetEvent event) {
        LuminousRenderer.enableStencil(event);
    }

    @SubscribeEvent
    static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminousRenderer.registerPipelines(event);
        ItemOutlineRenderer.registerPipelines(event);
        ExcaliburProjectileRenderer.registerPipelines(event);
        CovenantLeafParticle.registerPipeline(event);
    }

    @SubscribeEvent
    static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        LuminousRenderer.registerRenderStateModifiers(event);
        AfterimageRenderer.registerRenderStateModifiers(event);
        ClydnoHalterLayer.registerRenderStateModifier(event);
    }

    @SubscribeEvent
    static void gatherClientData(GatherDataEvent.Client event) {
        ModDatagen.gatherClientData(event);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        BertilakExtensions.register(event);
        EagleKnightTalonsExtensions.register(event);
        ExcaliburExtensions.register(event);
        GungnirExtensions.register(event);
        HulioshjalmrExtensions.register(event);
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        HulioshjalmrModel.registerLayerDefinition(event);
        EagleKnightTalonsModel.registerLayerDefinition(event);
        XiuhcoatlModel.registerLayerDefinition(event);
        YasakaniGuardianModel.registerLayerDefinition(event);
    }

    @SubscribeEvent
    static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        ClydnoHalterLayer.addLayer(event);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ExcaliburProjectileRenderer.register(event);
        GungnirProjectileRenderer.register(event);
        KazagurumaProjectileRenderer.register(event);
        WindslashRenderer.register(event);
        EyeShardRenderer.register(event);
        TecpatlShardRenderer.register(event);
        XiuhcoatlProjectileRenderer.register(event);
        TrophyHeadBlockEntityRenderer.register(event);
        SimpleEntityRenderers.register(event);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RELIQUARY_STATION.get(), ReliquaryStationScreen::new);
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        GungnirRuneParticle.registerProvider(event);
        CovenantLeafParticle.registerProvider(event);
        FireFangsParticle.registerProvider(event);
    }

    @SubscribeEvent
    static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        TecpatlRebuildingRenderer.register(event);
        TrophyHeadRenderer.register(event);
    }
}
