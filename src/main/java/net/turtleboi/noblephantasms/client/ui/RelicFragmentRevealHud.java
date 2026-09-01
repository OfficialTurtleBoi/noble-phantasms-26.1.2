package net.turtleboi.noblephantasms.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public final class RelicFragmentRevealHud {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "relic_fragment_reveal");
    private static final long HANDOFF_DURATION = 180L;
    private static final long HOLD_DURATION = 760L;
    private static final long ABSORB_DURATION = 520L;
    private static ItemStack fragment = ItemStack.EMPTY;
    private static long started;

    private RelicFragmentRevealHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, (graphics, deltaTracker) -> render(graphics));
    }

    public static void show(RelicFragmentData data) {
        fragment = RelicFragmentItem.create(ModItems.RELIC_FRAGMENT.get(), data, 1);
        started = Util.getMillis();
    }

    public static void clear() {
        fragment = ItemStack.EMPTY;
        started = 0L;
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (fragment.isEmpty() || started == 0L || minecraft.options.hideGui) {
            return;
        }
        long elapsed = Util.getMillis() - started;
        long absorbStart = HANDOFF_DURATION + HOLD_DURATION;
        long total = absorbStart + ABSORB_DURATION;
        if (elapsed >= total) {
            clear();
            return;
        }

        float centerX = graphics.guiWidth() * 0.5F;
        float displayY = graphics.guiHeight() * 0.42F;
        float y;
        float scale;
        if (elapsed < HANDOFF_DURATION) {
            float progress = smoothStep(elapsed / (float) HANDOFF_DURATION);
            y = graphics.guiHeight() * 0.5F + (displayY - graphics.guiHeight() * 0.5F) * progress;
            scale = 4.0F;
        } else if (elapsed < absorbStart) {
            y = displayY;
            scale = 4.0F;
        } else {
            float progress = smoothStep((elapsed - absorbStart) / (float) ABSORB_DURATION);
            y = displayY + (graphics.guiHeight() - 14.0F - displayY) * progress;
            scale = 4.0F + (0.08F - 4.0F) * progress;
        }

        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.item(fragment, -8, -8);
        graphics.pose().popMatrix();
    }

    private static float smoothStep(float value) {
        float clamped = Math.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
