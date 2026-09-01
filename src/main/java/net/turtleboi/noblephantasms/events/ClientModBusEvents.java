package net.turtleboi.noblephantasms.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
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
import net.turtleboi.noblephantasms.client.renderer.outline.GramOutline;
import net.turtleboi.noblephantasms.client.HulioshjalmrExtensions;
import net.turtleboi.noblephantasms.client.HolyGrailChargeProperty;
import net.turtleboi.noblephantasms.client.PridwenEnergyDecorator;
import net.turtleboi.noblephantasms.client.model.EagleKnightTalonsModel;
import net.turtleboi.noblephantasms.client.model.HulioshjalmrModel;
import net.turtleboi.noblephantasms.client.model.AnubiteModel;
import net.turtleboi.noblephantasms.client.model.EcclesiasticModel;
import net.turtleboi.noblephantasms.client.model.DraugrModel;
import net.turtleboi.noblephantasms.client.model.OniModel;
import net.turtleboi.noblephantasms.client.model.JaguarMicquiModel;
import net.turtleboi.noblephantasms.client.model.YasakaniGuardianModel;
import net.turtleboi.noblephantasms.entity.model.XiuhcoatlModel;
import net.turtleboi.noblephantasms.client.renderer.ClydnoHalterLayer;
import net.turtleboi.noblephantasms.client.renderer.AfterimageRenderer;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.client.renderer.FrozenRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.client.renderer.EntityTranslucencyRenderer;
import net.turtleboi.noblephantasms.client.renderer.HulioshjalmrRenderer;
import net.turtleboi.noblephantasms.client.renderer.EnergyProjectionRenderer;
import net.turtleboi.noblephantasms.client.renderer.TecpatlRebuildingRenderer;
import net.turtleboi.noblephantasms.client.renderer.RelicFragmentRenderer;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadBlockEntityRenderer;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.client.renderer.ReliquaryItemRenderer;
import net.turtleboi.noblephantasms.datagen.ModDatagen;
import net.turtleboi.noblephantasms.entity.renderer.EyeShardRenderer;
import net.turtleboi.noblephantasms.entity.renderer.AnubiteRenderer;
import net.turtleboi.noblephantasms.entity.renderer.EcclesiasticRenderer;
import net.turtleboi.noblephantasms.entity.renderer.DraugrRenderer;
import net.turtleboi.noblephantasms.entity.renderer.OniRenderer;
import net.turtleboi.noblephantasms.entity.renderer.JaguarMicquiRenderer;
import net.turtleboi.noblephantasms.entity.renderer.ExcaliburProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.GungnirProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.KazagurumaProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.PridwenBarrierRenderer;
import net.turtleboi.noblephantasms.entity.renderer.RelicFragmentEntityRenderer;
import net.turtleboi.noblephantasms.entity.renderer.SimpleEntityRenderers;
import net.turtleboi.noblephantasms.entity.renderer.TecpatlShardRenderer;
import net.turtleboi.noblephantasms.entity.renderer.WindslashRenderer;
import net.turtleboi.noblephantasms.entity.renderer.XiuhcoatlProjectileRenderer;
import net.turtleboi.noblephantasms.particle.custom.CovenantLeafParticle;
import net.turtleboi.noblephantasms.particle.custom.FireFangsParticle;
import net.turtleboi.noblephantasms.particle.custom.GungnirRuneParticle;
import net.turtleboi.noblephantasms.particle.custom.ApilolliCloudParticle;
import net.turtleboi.noblephantasms.particle.custom.ChilledParticle;
import net.turtleboi.noblephantasms.particle.custom.StunnedParticle;
import net.turtleboi.noblephantasms.screens.ReliquaryStationScreen;
import net.turtleboi.noblephantasms.screens.MythicalReliquaryScreen;
import net.turtleboi.noblephantasms.screens.menus.ModMenus;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.client.ui.RelicFragmentRevealHud;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID, value = Dist.CLIENT)
public final class ClientModBusEvents {
    @SubscribeEvent
    static void registerRangeSelectItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        NoblePhantasms.MOD_ID, "holy_grail_charge"),
                HolyGrailChargeProperty.MAP_CODEC);
    }

    @SubscribeEvent
    static void configureMainRenderTarget(ConfigureMainRenderTargetEvent event) {
        LuminousRenderer.enableStencil(event);
    }

    @SubscribeEvent
    static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminousRenderer.registerPipelines(event);
        ItemOutlineRenderer.registerPipelines(event);
        EntityTranslucencyRenderer.registerPipelines(event);
        ExcaliburProjectileRenderer.registerPipelines(event);
        EnergyProjectionRenderer.registerPipelines(event);
        ReliquaryItemRenderer.registerPipelines(event);
        CovenantLeafParticle.registerPipeline(event);
        FireFangsParticle.registerPipeline(event);
    }

    @SubscribeEvent
    static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        LuminousRenderer.registerRenderStateModifiers(event);
        FrozenRenderer.registerRenderStateModifiers(event);
        AfterimageRenderer.registerRenderStateModifiers(event);
        ClydnoHalterLayer.registerRenderStateModifier(event);
        HulioshjalmrRenderer.registerRenderStateModifiers(event);
    }

    @SubscribeEvent
    static void gatherClientData(GatherDataEvent.Client event) {
        ModDatagen.gatherClientData(event);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        GramOutline.register();
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
        AnubiteModel.registerLayerDefinition(event);
        EcclesiasticModel.registerLayerDefinition(event);
        DraugrModel.registerLayerDefinition(event);
        OniModel.registerLayerDefinition(event);
        JaguarMicquiModel.registerLayerDefinition(event);
    }

    @SubscribeEvent
    static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        ClydnoHalterLayer.addLayer(event);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ExcaliburProjectileRenderer.register(event);
        PridwenBarrierRenderer.register(event);
        GungnirProjectileRenderer.register(event);
        KazagurumaProjectileRenderer.register(event);
        WindslashRenderer.register(event);
        EyeShardRenderer.register(event);
        RelicFragmentEntityRenderer.register(event);
        TecpatlShardRenderer.register(event);
        XiuhcoatlProjectileRenderer.register(event);
        TrophyHeadBlockEntityRenderer.register(event);
        SimpleEntityRenderers.register(event);
        AnubiteRenderer.register(event);
        EcclesiasticRenderer.register(event);
        DraugrRenderer.register(event);
        OniRenderer.register(event);
        JaguarMicquiRenderer.register(event);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        FrozenRenderer.registerGuiLayers(event);
        RelicFragmentRevealHud.register(event);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RELIQUARY_STATION.get(), ReliquaryStationScreen::new);
        event.register(ModMenus.MYTHICAL_RELIQUARY.get(), MythicalReliquaryScreen::new);
    }

    @SubscribeEvent
    static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
        ReliquaryItemRenderer.register(event);
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        GungnirRuneParticle.registerProvider(event);
        CovenantLeafParticle.registerProvider(event);
        FireFangsParticle.registerProvider(event);
        ApilolliCloudParticle.registerProvider(event);
        ChilledParticle.registerProvider(event);
        StunnedParticle.registerProvider(event);
    }

    @SubscribeEvent
    static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        TecpatlRebuildingRenderer.register(event);
        RelicFragmentRenderer.register(event);
        TrophyHeadRenderer.register(event);
    }

    @SubscribeEvent
    static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.PRIDWEN.get(), new PridwenEnergyDecorator());
    }
}
