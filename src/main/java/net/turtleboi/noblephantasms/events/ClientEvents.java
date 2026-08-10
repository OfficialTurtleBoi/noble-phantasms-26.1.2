package net.turtleboi.noblephantasms.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakClientUtil;
import net.turtleboi.noblephantasms.client.EyeOfHorusClientState;
import net.turtleboi.noblephantasms.client.KusanagiDashInput;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.client.ui.EyeOfHorusHud;
import net.turtleboi.noblephantasms.client.ItemPoseEditor;

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
        EyeOfHorusClientState.tick();
        EyeOfHorusHud.tick();
    }

    @SubscribeEvent
    static void renderEyeOfHorusCrosshair(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName())
                && EyeOfHorusHud.render(event.getGuiGraphics())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void beginLuminousFrame(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        LuminousRenderer.beginFrame();
    }

    @SubscribeEvent
    static void renderLuminousOutlines(RenderLevelStageEvent.AfterLevel event) {
        LuminousRenderer.renderOutlines(event);
    }

    @SubscribeEvent
    static void registerClientCommands(RegisterClientCommandsEvent event) {
        ItemPoseEditor.register(event);
    }
}
