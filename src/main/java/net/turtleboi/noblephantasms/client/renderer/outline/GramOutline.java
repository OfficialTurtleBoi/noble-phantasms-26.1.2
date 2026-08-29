package net.turtleboi.noblephantasms.client.renderer.outline;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.GramItem;
import org.jspecify.annotations.Nullable;

public final class GramOutline {
    private static final Identifier MASK = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "item/glow/gram");
    private static final Identifier MASK_RESOURCE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/item/glow/gram.png");
    private static final float FADE_IN_TICKS = 30.0F;
    private static final float MIN_OUTLINE_WIDTH = 0.36F;
    private static final float MAX_OUTLINE_WIDTH = 0.72F;

    public static void register() {
        ItemOutlineRenderer.register(ModItems.GRAM.get(), GramOutline::supports, GramOutline::create);
    }

    private static boolean supports(ItemDisplayContext context) {
        return context.firstPerson()
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static ItemOutlineRenderer.@Nullable Outline create(
            ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner) {
        if (!GramItem.isBiteReady(stack)) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || minecraft.getResourceManager().getResource(MASK_RESOURCE).isEmpty()) {
            return null;
        }

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = minecraft.level.getGameTime() + partialTick;
        long readyTick = stack.getOrDefault(ModDataComponents.GRAM_READY_TICK.get(),
                minecraft.level.getGameTime());
        float fade = smooth(Mth.clamp((time - readyTick) / FADE_IN_TICKS, 0.0F, 1.0F));
        float flame = Mth.clamp(0.5F
                + Mth.sin(time * 0.11F) * 0.32F
                + Mth.sin(time * 0.29F + 1.4F) * 0.18F, 0.0F, 1.0F);
        float flicker = Mth.clamp(0.5F
                + Mth.sin(time * 0.47F + 0.8F) * 0.32F
                + Mth.sin(time * 1.13F + 2.1F) * 0.18F, 0.0F, 1.0F);
        int color = flameColor(flame);
        float alpha = fade * Mth.lerp(flicker, 0.72F, 1.0F);
        float width = Mth.lerp(flicker, MIN_OUTLINE_WIDTH, MAX_OUTLINE_WIDTH);
        return ItemOutlineRenderer.glow(color, alpha, width).mask(MASK);
    }

    private static int flameColor(float flame) {
        if (flame < 0.55F) {
            return ARGB.srgbLerp(flame / 0.55F, 0xD43A16, 0xFF7900);
        }
        return ARGB.srgbLerp((flame - 0.55F) / 0.45F, 0xFF7900, 0xFFD76A);
    }

    private static float smooth(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }
}
