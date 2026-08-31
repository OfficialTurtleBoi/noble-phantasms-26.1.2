package net.turtleboi.noblephantasms.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.turtleboi.noblephantasms.item.custom.PridwenItem;

public final class PridwenEnergyDecorator implements IItemDecorator {
    @Override
    public boolean render(GuiGraphicsExtractor guiGraphics, Font font, ItemStack stack,
                          int xOffset, int yOffset) {
        int width = Math.clamp(Math.round(
                PridwenItem.getBarrierHealthProgress(stack) * 13.0F), 0, 13);
        int x = xOffset + 2;
        boolean durabilityVisible = stack.isBarVisible();
        int y = yOffset + (durabilityVisible ? 12 : 13);
        if (!durabilityVisible) {
            guiGraphics.fill(x, y, x + 13, y + 2, 0xFF000000);
        }
        if (width > 0) {
            guiGraphics.fill(x, y, x + width, y + 1, 0xFF66C4FF);
        }
        return false;
    }
}
