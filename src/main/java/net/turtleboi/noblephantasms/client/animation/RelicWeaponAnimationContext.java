package net.turtleboi.noblephantasms.client.animation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class RelicWeaponAnimationContext {
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    public static void begin(LivingEntity entity, ItemStack itemStack, float partialTick,
                             InteractionHand hand, ItemDisplayContext displayContext) {
        CONTEXT.set(new Context(entity, itemStack, partialTick, hand, displayContext));
    }

    public static LivingEntity getEntity() {
        Context context = CONTEXT.get();
        return context == null ? null : context.entity();
    }

    public static ItemStack getItemStack() {
        Context context = CONTEXT.get();
        return context == null ? ItemStack.EMPTY : context.itemStack();
    }

    public static float getPartialTick() {
        Context context = CONTEXT.get();
        return context == null ? 0.0F : context.partialTick();
    }

    public static InteractionHand getHand() {
        Context context = CONTEXT.get();
        return context == null ? InteractionHand.MAIN_HAND : context.hand();
    }

    public static ItemDisplayContext getDisplayContext() {
        Context context = CONTEXT.get();
        return context == null ? ItemDisplayContext.NONE : context.displayContext();
    }

    public static void end() {
        CONTEXT.remove();
    }

    private record Context(LivingEntity entity, ItemStack itemStack, float partialTick,
                           InteractionHand hand, ItemDisplayContext displayContext) {
    }
}
