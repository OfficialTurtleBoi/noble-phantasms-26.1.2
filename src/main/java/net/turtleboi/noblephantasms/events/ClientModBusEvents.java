package net.turtleboi.noblephantasms.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakExtensions;
import net.turtleboi.noblephantasms.client.GungnirExtensions;
import net.turtleboi.noblephantasms.client.HulioshjalmrExtensions;
import net.turtleboi.noblephantasms.client.model.HulioshjalmrModel;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadBlockEntityRenderer;
import net.turtleboi.noblephantasms.datagen.ModDatagen;
import net.turtleboi.noblephantasms.entity.renderer.GungnirProjectileRenderer;
import net.turtleboi.noblephantasms.entity.renderer.WindCutterRenderer;
import net.turtleboi.noblephantasms.particle.custom.GungnirRuneParticle;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID, value = Dist.CLIENT)
public final class ClientModBusEvents {
    @SubscribeEvent
    static void gatherClientData(GatherDataEvent.Client event) {
        ModDatagen.gatherClientData(event);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        BertilakExtensions.register(event);
        GungnirExtensions.register(event);
        HulioshjalmrExtensions.register(event);
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        HulioshjalmrModel.registerLayerDefinition(event);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        GungnirProjectileRenderer.register(event);
        WindCutterRenderer.register(event);
        TrophyHeadBlockEntityRenderer.register(event);
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        GungnirRuneParticle.registerProvider(event);
    }

    @SubscribeEvent
    static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        TrophyHeadRenderer.register(event);
    }
}
