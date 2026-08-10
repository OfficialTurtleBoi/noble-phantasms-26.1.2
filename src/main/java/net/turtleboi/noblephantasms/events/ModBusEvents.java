package net.turtleboi.noblephantasms.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.datagen.ModDatagen;
import net.turtleboi.noblephantasms.network.TrophySupportPayload;
import net.turtleboi.noblephantasms.network.KusanagiDashPayload;
import net.turtleboi.noblephantasms.network.ReliquaryStationCompletePayload;
import net.turtleboi.noblephantasms.network.EyeAssemblyPayload;

@EventBusSubscriber(modid = NoblePhantasms.MOD_ID)
public final class ModBusEvents {
    @SubscribeEvent
    static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        KusanagiDashPayload.register(event);
        TrophySupportPayload.register(event);
        ReliquaryStationCompletePayload.register(event);
        EyeAssemblyPayload.register(event);
    }

    @SubscribeEvent
    static void gatherServerData(GatherDataEvent.Server event) {
        ModDatagen.gatherServerData(event);
    }
}
