package net.turtleboi.noblephantasms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.custom.ExcaliburProjectile;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.ExcaliburItem;
import net.turtleboi.noblephantasms.NoblePhantasms;
import org.jspecify.annotations.Nullable;

public final class ExcaliburOutline {
    private static final int COLOR = 0x66C4FF;
    private static final float WIDTH = 0.5F;
    private static final float MAX_CHARGE_WIDTH = WIDTH * 3.0F;
    private static final float RELEASE_TRANSITION_TICKS = 12.0F;
    private static final float RELEASE_TOTAL_START_WIDTH = 2.0F;
    private static final float RELEASE_CORE_SHRINK_PROGRESS = 0.35F;
    private static final float RECHARGE_FADE_TICKS = 20.0F;
    private static final Identifier[] ENERGY_MASKS = {
            mask("excalibur_1"),
            mask("excalibur_2"),
            mask("excalibur_3"),
            mask("excalibur_4"),
            mask("excalibur_5"),
            mask("excalibur_6"),
            mask("excalibur_7")
    };
    private static final Identifier PROJECTILE_MASK = mask("excalibur_projectile");

    public static void register() {
        ItemOutlineRenderer.register(ModItems.EXCALIBUR.get(), ExcaliburOutline::supports,
                ExcaliburOutline::create);
    }

    private static boolean supports(ItemDisplayContext context) {
        return context.firstPerson()
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIXED;
    }

    private static ItemOutlineRenderer.@Nullable Outline create(
            ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner) {
        if (owner instanceof ExcaliburProjectile) {
            return glow(COLOR, WIDTH, PROJECTILE_MASK);
        }
        if (context == ItemDisplayContext.FIXED) {
            return null;
        }

        int energy = ExcaliburItem.getEnergy(stack);
        LivingEntity wielder = owner == null ? null : owner.asLivingEntity();
        if (wielder == null) {
            return energy <= 0 ? null : glow(COLOR, WIDTH, energyMask(energy));
        }

        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Long releaseTick = stack.get(ModDataComponents.EXCALIBUR_RELEASE_TICK.get());
        if (releaseTick != null && minecraft.level != null) {
            float releaseAge = minecraft.level.getGameTime() + partialTick - releaseTick;
            if (releaseAge >= 0.0F && releaseAge < RELEASE_TRANSITION_TICKS) {
                float transition = Mth.clamp(releaseAge / RELEASE_TRANSITION_TICKS, 0.0F, 1.0F);
                float settled = smooth(transition);
                float coreShrink = smooth(Mth.clamp(
                        transition / RELEASE_CORE_SHRINK_PROGRESS, 0.0F, 1.0F));
                float coreWidth = Mth.lerp(coreShrink, MAX_CHARGE_WIDTH, WIDTH);
                float totalWidth = Mth.lerp(settled, RELEASE_TOTAL_START_WIDTH, WIDTH);
                float outerWidth = Math.max(0.0F, totalWidth - coreWidth);
                float outerAlpha = smooth(Mth.clamp(transition / RELEASE_CORE_SHRINK_PROGRESS,
                        0.0F, 1.0F)) * (1.0F - settled) * 0.45F;
                int coreColor = ARGB.srgbLerp(coreShrink, 0xFFFFFF, COLOR);
                return ItemOutlineRenderer.multiGlow(
                                ItemOutlineRenderer.glow(COLOR, outerAlpha, outerWidth),
                                ItemOutlineRenderer.glow(coreColor, 1.0F, coreWidth))
                        .mask(energyMask(Math.max(1, energy)));
            }
        }

        if (energy <= 0) {
            return null;
        }
        float progress = smooth(ExcaliburItem.getChargeProgress(stack));
        float width = Mth.lerp(progress, WIDTH, MAX_CHARGE_WIDTH);
        ItemOutlineRenderer.Outline outline = glow(COLOR, width, energyMask(energy));
        Long rechargeTick = stack.get(ModDataComponents.EXCALIBUR_RECHARGE_TICK.get());
        if (rechargeTick == null || minecraft.level == null) {
            return outline;
        }
        float rechargeAge = minecraft.level.getGameTime() + partialTick - rechargeTick;
        if (rechargeAge < 0.0F || rechargeAge >= RECHARGE_FADE_TICKS) {
            return outline;
        }
        float fade = smooth(Mth.clamp(rechargeAge / RECHARGE_FADE_TICKS, 0.0F, 1.0F));
        if (energy == 1) {
            return glow(COLOR, fade, width, energyMask(energy));
        }
        return outline.transitionFrom(energyMask(energy - 1), fade);
    }

    private static ItemOutlineRenderer.Outline glow(int color, float width, Identifier mask) {
        return glow(color, 1.0F, width, mask);
    }

    private static ItemOutlineRenderer.Outline glow(int color, float alpha, float width, Identifier mask) {
        return ItemOutlineRenderer.glow(color, alpha, width).mask(mask);
    }

    private static Identifier energyMask(int energy) {
        return ENERGY_MASKS[Mth.clamp(energy, 1, ENERGY_MASKS.length) - 1];
    }

    private static Identifier mask(String name) {
        return Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/glow/" + name);
    }

    private static float smooth(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }
}
