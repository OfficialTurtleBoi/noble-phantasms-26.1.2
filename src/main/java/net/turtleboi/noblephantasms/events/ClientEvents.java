package net.turtleboi.noblephantasms.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakClientUtil;
import net.turtleboi.noblephantasms.client.KusanagiDashInput;

@Mod(value = NoblePhantasms.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NoblePhantasms.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    public ClientEvents(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        KusanagiDashInput.tick();
        BertilakClientUtil.tick();
    }
}
