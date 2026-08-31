package net.turtleboi.noblephantasms.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.datagen.ModDatagen;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.AnubiteEntity;
import net.turtleboi.noblephantasms.entity.custom.EcclesiasticEntity;
import net.turtleboi.noblephantasms.entity.custom.DraugrEntity;
import net.turtleboi.noblephantasms.entity.custom.JaguarMicquiEntity;
import net.turtleboi.noblephantasms.entity.custom.OniEntity;
import net.turtleboi.noblephantasms.entity.custom.YasakaniGuardianEntity;
import net.turtleboi.noblephantasms.network.EyeAssemblyPayload;
import net.turtleboi.noblephantasms.network.KusanagiDashPayload;
import net.turtleboi.noblephantasms.network.ReliquaryStationCompletePayload;
import net.turtleboi.noblephantasms.network.MythicalReliquarySelectPayload;
import net.turtleboi.noblephantasms.network.TrophySupportPayload;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID)
public final class ModBusEvents {
    @SubscribeEvent
    static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.YASAKANI_GUARDIAN.get(), YasakaniGuardianEntity.createAttributes().build());
        event.put(ModEntities.ANUBITE.get(), AnubiteEntity.createAttributes().build());
        event.put(ModEntities.ECCLESIASTIC.get(), EcclesiasticEntity.createAttributes().build());
        event.put(ModEntities.DRAUGR.get(), DraugrEntity.createAttributes().build());
        event.put(ModEntities.ONI.get(), OniEntity.createAttributes().build());
        event.put(ModEntities.JAGUAR_MICQUI.get(), JaguarMicquiEntity.createAttributes().build());
    }

    @SubscribeEvent
    static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        KusanagiDashPayload.register(event);
        TrophySupportPayload.register(event);
        ReliquaryStationCompletePayload.register(event);
        MythicalReliquarySelectPayload.register(event);
        EyeAssemblyPayload.register(event);
    }

    @SubscribeEvent
    static void gatherServerData(GatherDataEvent.Server event) {
        ModDatagen.gatherServerData(event);
    }
}
