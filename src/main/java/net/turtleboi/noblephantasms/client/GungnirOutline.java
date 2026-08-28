package net.turtleboi.noblephantasms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import org.jspecify.annotations.Nullable;

public final class GungnirOutline {
    private static final int GOLD = 0xEFBF04;
    private static final int LIGHT_GOLD = 0xFFE58A;
    private static final float OUTLINE_WIDTH = 0.75F;
    private static final float MAX_TOTAL_WIDTH = 2.0F;
    private static final float CHARGING_PEAK_WIDTH = MAX_TOTAL_WIDTH;
    private static final float FULL_CHARGE_CORE_START_WIDTH = MAX_TOTAL_WIDTH;
    private static final float FULL_CHARGE_TRANSITION_TICKS = 12.0F;
    private static final float CORE_SHRINK_PROGRESS = 0.35F;
    private static final float FLICKER_TOTAL_BASE_WIDTH = 1.85F;
    private static final float FLICKER_TOTAL_MIN_WIDTH = 1.55F;
    private static final float FLICKER_TOTAL_SCALE = 1.35F;

    public static void register() {
        ItemOutlineRenderer.register(ModItems.GUNGNIR.get(), GungnirOutline::supports,
                GungnirOutline::create);
    }

    private static boolean supports(ItemDisplayContext context) {
        return context.firstPerson()
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIXED;
    }

    private static ItemOutlineRenderer.@Nullable Outline create(ItemStack stack, ItemDisplayContext context,
                                                                 @Nullable ItemOwner owner) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float animationTime = minecraft.level == null ? partialTick : minecraft.level.getGameTime() + partialTick;
        if (owner instanceof GungnirProjectile projectile) {
            if (!projectile.isChargedThrow()) {
                return null;
            }
            Entity thrower = projectile.getOwner();
            float phase = (thrower == null ? projectile.getId() : thrower.getId()) * 0.7548777F;
            float chargedTicks = projectile.getChargedTicks() + projectile.tickCount + partialTick;
            return masked(fullyCharged(chargedTicks, animationTime, phase));
        }

        LivingEntity wielder = owner == null ? null : owner.asLivingEntity();
        if (wielder == null || !wielder.isUsingItem() || wielder.getUseItem() != stack) {
            return null;
        }

        float timeHeld = stack.getUseDuration(wielder)
                - (wielder.getUseItemRemainingTicks() - partialTick + 1.0F);
        float chargeProgress = Mth.clamp(timeHeld / GungnirItem.FULL_CHARGE_TICKS, 0.0F, 1.0F);
        if (chargeProgress <= 0.0F) {
            return null;
        }
        if (chargeProgress < 1.0F) {
            float chargingWidth = Mth.lerp(smooth(chargeProgress), OUTLINE_WIDTH, CHARGING_PEAK_WIDTH);
            return masked(ItemOutlineRenderer.glow(GOLD, chargeProgress, chargingWidth));
        }
        float phase = wielder.getId() * 0.7548777F;
        return masked(fullyCharged(timeHeld - GungnirItem.FULL_CHARGE_TICKS, animationTime, phase));
    }

    private static ItemOutlineRenderer.Outline fullyCharged(float chargedTicks, float animationTime, float phase) {
        int color = animatedGold(animationTime);
        float transition = Mth.clamp(chargedTicks / FULL_CHARGE_TRANSITION_TICKS, 0.0F, 1.0F);
        if (transition < 1.0F) {
            float settled = smooth(transition);
            float outerAlpha = smooth(Mth.clamp(transition / 0.35F, 0.0F, 1.0F)) * 0.35F;
            float coreShrink = smooth(Mth.clamp(transition / CORE_SHRINK_PROGRESS, 0.0F, 1.0F));
            float coreWidth = Mth.lerp(coreShrink, FULL_CHARGE_CORE_START_WIDTH, OUTLINE_WIDTH);
            float totalWidth = Mth.lerp(settled, MAX_TOTAL_WIDTH,
                    flickeringTotalWidth(animationTime, phase));
            float outerWidth = Math.max(0.0F, totalWidth - coreWidth);
            int coreColor = ARGB.srgbLerp(settled, 0xFFFFFF, color);
            return ItemOutlineRenderer.multiGlow(
                    ItemOutlineRenderer.glow(color, outerAlpha, outerWidth),
                    ItemOutlineRenderer.glow(coreColor, 1.0F, coreWidth));
        }
        return settledGlow(color, animationTime, phase);
    }

    private static ItemOutlineRenderer.Outline settledGlow(int color, float animationTime, float phase) {
        float outerWidth = flickeringTotalWidth(animationTime, phase) - OUTLINE_WIDTH;
        return ItemOutlineRenderer.multiGlow(
                ItemOutlineRenderer.glow(color, 0.35F, outerWidth),
                ItemOutlineRenderer.glow(color, 1.0F, OUTLINE_WIDTH));
    }

    private static float flickeringTotalWidth(float animationTime, float phase) {
        float flicker = Mth.sin(animationTime * 1.1F + phase) * 0.18F
                + Mth.sin(animationTime * 2.9F + phase * 1.7F) * 0.08F;
        return Mth.clamp(FLICKER_TOTAL_BASE_WIDTH + flicker * FLICKER_TOTAL_SCALE,
                FLICKER_TOTAL_MIN_WIDTH, MAX_TOTAL_WIDTH);
    }

    private static int animatedGold(float animationTime) {
        float time = animationTime * 0.65F;
        float flicker = Mth.clamp(
                0.82F + Mth.sin(time) * 0.11F + Mth.sin(time * 2.73F) * 0.07F,
                0.62F, 1.0F);
        return ARGB.srgbLerp((flicker - 0.62F) / 0.38F, GOLD, LIGHT_GOLD);
    }

    private static float smooth(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static ItemOutlineRenderer.Outline masked(ItemOutlineRenderer.Outline outline) {
        return outline.mask(ModItems.GUNGNIR.get());
    }
}
