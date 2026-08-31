package net.turtleboi.noblephantasms.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakClientUtil;
import net.turtleboi.noblephantasms.client.EyeOfHorusClientState;
import net.turtleboi.noblephantasms.client.FrozenClientState;
import net.turtleboi.noblephantasms.client.KusanagiDashInput;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import net.turtleboi.noblephantasms.client.renderer.AfterimageRenderer;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.client.renderer.HulioshjalmrRenderer;
import net.turtleboi.noblephantasms.client.renderer.EnergyProjectionRenderer;
import net.turtleboi.noblephantasms.client.renderer.FrozenRenderer;
import net.turtleboi.noblephantasms.client.ui.EyeOfHorusHud;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.KheperScarabItem;
import net.turtleboi.noblephantasms.item.custom.TyrfingItem;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.config.ModConfig;
import net.turtleboi.noblephantasms.mixin.client.ClientInputAccessor;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = NoblePhantasms.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NoblePhantasms.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    public ClientEvents(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        FrozenClientState.tick();
        KusanagiDashInput.tick();
        BertilakClientUtil.tick();
        EyeOfHorusClientState.tick();
        EyeOfHorusHud.tick();
    }

    @SubscribeEvent
    static void freezeMovementInput(MovementInputUpdateEvent event) {
        if (!event.getEntity().hasEffect(ModEffects.FROZEN)) {
            return;
        }
        event.getInput().keyPresses = Input.EMPTY;
        ((ClientInputAccessor) event.getInput()).noblePhantasms$setMoveVector(Vec2.ZERO);
    }

    @SubscribeEvent
    static void freezeInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.hasEffect(ModEffects.FROZEN)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void freezeMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null
                || !minecraft.player.hasEffect(ModEffects.FROZEN)
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        minecraft.options.keyUse.setDown(event.getAction() != GLFW.GLFW_RELEASE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void freezeScreenOpening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.hasEffect(ModEffects.FROZEN)
                || ModConfig.ALLOW_GUI_ACCESS_WHILE_FROZEN.get()) {
            return;
        }
        Screen screen = event.getNewScreen();
        if (screen != null && !(screen instanceof PauseScreen)
                && !screen.getClass().getPackageName().startsWith("net.minecraft.client.gui.screens.options")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void clearFrozenClientState(ClientPlayerNetworkEvent.LoggingOut event) {
        FrozenClientState.clear();
        FrozenRenderer.clearAll();
    }

    @SubscribeEvent
    static void clearFrozenEntityState(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            FrozenRenderer.clear(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    static void onClientResourcesLoaded(ClientResourceLoadFinishedEvent event) {
        ColoredGlintRenderer.register(ModItems.KHEPER_SCARAB.get(), 0xFFD700, KheperScarabItem::isActive);
        ColoredGlintRenderer.registerTransitioningFromEnchantment(ModItems.TYRFING.get(), 0xFF0905, stack -> TyrfingItem.getCurseGlintTint(stack, getClientGameTime()));
        ColoredGlintRenderer.initialize();
        ItemOutlineRenderer.initialize();
        HulioshjalmrRenderer.initialize();
    }

    private static double getClientGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0;
        }
        return minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
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
        HulioshjalmrRenderer.beginFrame();
        EnergyProjectionRenderer.beginFrame();
    }

    @SubscribeEvent
    static void compositeHulioshjalmrConcealment(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        HulioshjalmrRenderer.composite(event);
    }

    @SubscribeEvent
    static void captureItemOutlineOcclusion(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        ItemOutlineRenderer.captureOcclusionDepth();
    }

    @SubscribeEvent
    static void renderLuminousOutlines(RenderLevelStageEvent.AfterLevel event) {
        AfterimageRenderer.render(event);
        EnergyProjectionRenderer.renderDeferred(event);
        LuminousRenderer.renderOutlines(event);
        ItemOutlineRenderer.renderOutlines(event);
    }

    @SubscribeEvent
    static void registerClientCommands(RegisterClientCommandsEvent event) {
        ItemPoseEditor.register(event);
    }
}
