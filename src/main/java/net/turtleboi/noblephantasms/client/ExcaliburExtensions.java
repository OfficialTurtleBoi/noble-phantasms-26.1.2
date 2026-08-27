package net.turtleboi.noblephantasms.client;

import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.item.ModItems;

public final class ExcaliburExtensions {
    private static final int OUTLINE_COLOR = 0x66C4FF;

    public static void register() {
        ItemOutlineRenderer.registerHeld(ModItems.EXCALIBUR.get(), (stack, context, owner) ->
                ItemOutlineRenderer.glow(OUTLINE_COLOR, 1.0F, 1.0F).mask(stack.getItem()));
    }
}
