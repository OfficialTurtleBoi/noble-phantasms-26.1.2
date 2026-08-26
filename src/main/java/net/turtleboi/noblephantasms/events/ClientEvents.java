package net.turtleboi.noblephantasms.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
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
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import net.turtleboi.noblephantasms.client.renderer.AfterimageRenderer;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.client.ui.EyeOfHorusHud;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.KheperScarabItem;
import net.turtleboi.noblephantasms.item.custom.TyrfingItem;

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
    static void onClientResourcesLoaded(ClientResourceLoadFinishedEvent event) {
        ColoredGlintRenderer.register(ModItems.KHEPER_SCARAB.get(), 0xFFD700, KheperScarabItem::isActive);
        ColoredGlintRenderer.registerTransitioningFromEnchantment(ModItems.TYRFING.get(), 0xFF0905, stack -> TyrfingItem.getCurseGlintTint(stack, getClientGameTime()));
        ColoredGlintRenderer.initialize();
        ItemOutlineRenderer.initialize();
    }

    private static double getClientGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0;
        }
        return minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != InputConstants.PRESS
                || minecraft.screen != null
                || minecraft.player == null
                || !minecraft.options.keyDrop.matches(event.getKeyEvent())
                || !TyrfingItem.isCurseActive(minecraft.player.getMainHandItem())) {
            return;
        }
        while (minecraft.options.keyDrop.consumeClick()) {
        }
        minecraft.options.keyDrop.setDown(false);
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
        AfterimageRenderer.beginFrame();
        LuminousRenderer.beginFrame();
        ItemOutlineRenderer.beginFrame();
    }

    @SubscribeEvent
    static void captureItemOutlineOcclusion(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        ItemOutlineRenderer.captureOcclusionDepth();
    }

    @SubscribeEvent
    static void renderLuminousOutlines(RenderLevelStageEvent.AfterLevel event) {
        AfterimageRenderer.render(event);
        LuminousRenderer.renderOutlines(event);
        ItemOutlineRenderer.renderOutlines(event);
    }

    @SubscribeEvent
    static void registerClientCommands(RegisterClientCommandsEvent event) {
        ItemPoseEditor.register(event);
    }
}
