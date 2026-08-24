package net.turtleboi.noblephantasms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.animation.RelicTransform;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import org.jspecify.annotations.Nullable;

public class GungnirExtensions implements IClientItemExtensions {
    public static final float STAB_TRANSLATION_Y = 0.75F;
    public static final float STAB_ROTATION_X = -15.0F;
    public static final float STAB_ROTATION_Z = 15.0F;
    public static final float STAB_MIN_EXTENSION = -0.5F;
    public static final float THROW_TRANSLATION_X = -0.15F;
    public static final float THROW_TRANSLATION_Y = 0.55F;
    public static final float THROW_TRANSLATION_Z = 0.55F;
    public static final float THROW_ROTATION_X = -110.0F;
    public static final float THROW_ROTATION_Y = 25.0F;
    public static final float THROW_ROTATION_Z = 5.0F;
    private static final float CHARGE_ROTATION_Y = 30.0F;
    private static final float CHARGE_FORWARD_EXTENSION = 0.15F;
    private static final float FULL_CHARGE_TRANSITION_TICKS = 12.0F;
    private static final float OUTER_GLOW_BASE_SCALE = 1.9F;
    private static final float OUTER_GLOW_PEAK_SCALE = 2.65F;
    private static final float OUTER_GLOW_PEAK_PROGRESS = 0.6F;
    private static final int GOLD = 0xEFBF04;
    private static final int LIGHT_GOLD = 0xFFE58A;
    private static final ItemOutlineRenderer.Region TIP_REGION = ItemOutlineRenderer.Region.xy(0.5F, 0.5F, 1.0F, 1.0F);
    private static final Identifier TIP_MASK = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "item/gungnir_glow_mask");

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new GungnirExtensions(), ModItems.GUNGNIR.get());
        ItemOutlineRenderer.register(ModItems.GUNGNIR.get(), GungnirExtensions::supportsOutline,
                GungnirExtensions::getOutline);
    }

    public static void applyEditorThrowTransform(PoseStack poseStack, HumanoidArm arm,
                                                 RelicTransform transform) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        transform.apply(poseStack, arm);
        poseStack.translate(0.0F, 0.0F, CHARGE_FORWARD_EXTENSION);
        poseStack.mulPose(Axis.YN.rotationDegrees(direction * CHARGE_ROTATION_Y));
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProgress, float swingProgress) {
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof GungnirItem) || arm != getUsedArm(player)) {
            return false;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        poseStack.translate(direction * THROW_TRANSLATION_X, THROW_TRANSLATION_Y, THROW_TRANSLATION_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(THROW_ROTATION_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * THROW_ROTATION_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * THROW_ROTATION_Z));

        float timeHeld = itemInHand.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
        float chargeTime = Mth.clamp(timeHeld / GungnirItem.FULL_CHARGE_TICKS, 0.0F, 1.0F);
        if (chargeTime > 0.1F) {
            float shake = Mth.sin((timeHeld - 0.1F) * 1.3F) * (chargeTime - 0.1F);
            poseStack.translate(0.0F, shake * 0.004F, 0.0F);
        }

        poseStack.translate(0.0F, 0.0F, chargeTime * CHARGE_FORWARD_EXTENSION);
        poseStack.mulPose(Axis.YN.rotationDegrees(direction * chargeTime * CHARGE_ROTATION_Y));
        return true;
    }

    private static HumanoidArm getUsedArm(LocalPlayer player) {
        return player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private static boolean supportsOutline(ItemDisplayContext context) {
        return context.firstPerson()
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIXED;
    }

    private static ItemOutlineRenderer.@Nullable Outline getOutline(ItemStack stack, ItemDisplayContext context,
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
            return tipOutline(fullChargeGlow(chargedTicks, animationTime, phase));
        }
        LivingEntity wielder = owner == null ? null : owner.asLivingEntity();
        if (wielder == null || !wielder.isUsingItem() || wielder.getUseItem() != stack) {
            return null;
        }
        float timeHeld = stack.getUseDuration(wielder) - (wielder.getUseItemRemainingTicks() - partialTick + 1.0F);
        float chargeProgress = Mth.clamp(timeHeld / GungnirItem.FULL_CHARGE_TICKS, 0.0F, 1.0F);
        if (chargeProgress < 1.0F) {
            return tipOutline(ItemOutlineRenderer.glow(GOLD, chargeProgress, 1.0F));
        }
        float phase = wielder.getId() * 0.7548777F;
        return tipOutline(fullChargeGlow(timeHeld - GungnirItem.FULL_CHARGE_TICKS, animationTime, phase));
    }

    private static ItemOutlineRenderer.Outline fullChargeGlow(float chargedTicks, float animationTime, float phase) {
        int color = goldColor(animationTime);
        float transition = Mth.clamp(chargedTicks / FULL_CHARGE_TRANSITION_TICKS, 0.0F, 1.0F);
        if (transition < 1.0F) {
            float settled = smooth(transition);
            float outerAlpha = smooth(Mth.clamp(transition / 0.35F, 0.0F, 1.0F)) * 0.35F;
            float outerScale = transition < OUTER_GLOW_PEAK_PROGRESS
                    ? Mth.lerp(smooth(transition / OUTER_GLOW_PEAK_PROGRESS), 1.0F, OUTER_GLOW_PEAK_SCALE)
                    : Mth.lerp(smooth((transition - OUTER_GLOW_PEAK_PROGRESS)
                    / (1.0F - OUTER_GLOW_PEAK_PROGRESS)), OUTER_GLOW_PEAK_SCALE, OUTER_GLOW_BASE_SCALE);
            int coreColor = ARGB.srgbLerp(settled, 0xFFFFFF, color);
            return ItemOutlineRenderer.multiGlow(
                    ItemOutlineRenderer.glow(color, outerAlpha, outerScale),
                    ItemOutlineRenderer.glow(coreColor, 1.0F, 1.0F));
        }
        return ItemOutlineRenderer.vibrantGlow(color, 1.0F, 1.0F, animationTime, phase);
    }

    private static int goldColor(float animationTime) {
        float gameTime = animationTime * 0.65F;
        float flicker = Mth.clamp(
                0.82F + Mth.sin(gameTime) * 0.11F + Mth.sin(gameTime * 2.73F) * 0.07F, 0.62F, 1.0F);
        return ARGB.srgbLerp((flicker - 0.62F) / 0.38F, GOLD, LIGHT_GOLD);
    }

    private static float smooth(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static ItemOutlineRenderer.Outline tipOutline(ItemOutlineRenderer.Outline outline) {
        return outline
                .region(TIP_REGION)
                .mask(TIP_MASK);
    }
}
