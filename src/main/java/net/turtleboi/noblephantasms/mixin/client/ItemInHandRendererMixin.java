package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.GungnirExtensions;
import net.turtleboi.noblephantasms.client.animation.RelicWeaponAnimationContext;
import net.turtleboi.noblephantasms.client.animation.RelicWeaponAnimations;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import net.turtleboi.noblephantasms.item.custom.SpearRelicItem;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    private final ThrustSwingState spearMainHandState = new ThrustSwingState();
    private final ThrustSwingState spearOffHandState = new ThrustSwingState();
    private static final float RHONGOMYNIAD_INWARD_ROTATION = 15.0F;
    private static final float RHONGOMYNIAD_DOWNWARD_ROTATION = -15.0F;
    private static final float RHONGOMYNIAD_MAX_EXTENSION = 0.15F;
    private static final float RHONGOMYNIAD_MIN_EXTENSION = -0.75F;
    private final ThrustSwingState rhongomyniadMainHandState = new ThrustSwingState();
    private final ThrustSwingState rhongomyniadOffHandState = new ThrustSwingState();

    @Unique
    private AbstractClientPlayer rhongomyniadRenderPlayer;
    @Unique
    private ItemStack rhongomyniadRenderStack = ItemStack.EMPTY;
    @Unique
    private float rhongomyniadFrameInterp;

    @Shadow
    private void swingArm(float attack, PoseStack poseStack, int direction, HumanoidArm arm) {
        throw new AssertionError();
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"))
    private boolean keepPridwenRaised(boolean original,
                                      @Local(argsOnly = true) AbstractClientPlayer player,
                                      @Local(argsOnly = true) InteractionHand hand,
                                      @Local(argsOnly = true) ItemStack itemStack) {
        return original || PridwenBarrierEntity.shouldKeepRaised(player, hand, itemStack)
                || ItemPoseEditor.isPreviewingUse(hand, itemStack);
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I"))
    private int keepPridwenUseTime(int original,
                                   @Local(argsOnly = true) AbstractClientPlayer player,
                                   @Local(argsOnly = true) InteractionHand hand,
                                   @Local(argsOnly = true) ItemStack itemStack) {
        int previewTicks = ItemPoseEditor.getPreviewUseRemainingTicks(hand, itemStack, player);
        if (previewTicks > 0) {
            return previewTicks;
        }
        return PridwenBarrierEntity.shouldKeepRaised(player, hand, itemStack)
                ? Math.max(1, original) : original;
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;"))
    private InteractionHand keepPridwenHand(InteractionHand original,
                                            @Local(argsOnly = true) AbstractClientPlayer player,
                                            @Local(argsOnly = true) InteractionHand hand,
                                            @Local(argsOnly = true) ItemStack itemStack) {
        return PridwenBarrierEntity.shouldKeepRaised(player, hand, itemStack)
                || ItemPoseEditor.isPreviewingUse(hand, itemStack) ? hand : original;
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void beginRhongomyniadSpin(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                       float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                       SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        float timeHeld = 0.0F;
        if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            timeHeld = itemStack.getUseDuration(player)
                    - (player.getUseItemRemainingTicks() - frameInterp + 1.0F);
        }
        rhongomyniadRenderPlayer = player;
        rhongomyniadRenderStack = itemStack;
        rhongomyniadFrameInterp = frameInterp;
        ItemDisplayContext displayContext = armFor(player, hand) == HumanoidArm.RIGHT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        RelicWeaponAnimationContext.begin(player, itemStack, frameInterp, hand, displayContext);
        RhongomyniadSpinState.begin(itemStack, timeHeld, player);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void endRhongomyniadSpin(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                     float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        RhongomyniadSpinState.end();
        RelicWeaponAnimationContext.end();
        rhongomyniadRenderPlayer = null;
        rhongomyniadRenderStack = ItemStack.EMPTY;
        rhongomyniadFrameInterp = 0.0F;
    }

    @ModifyArg(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonUse(FLcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;)V"),
            index = 2)
    private float useRhongomyniadRecoveryTime(float timeHeld) {
        if (!(rhongomyniadRenderStack.getItem() instanceof RhongomyniadItem)
                || rhongomyniadRenderPlayer == null) {
            return timeHeld;
        }

        return RhongomyniadSpinState.getVisualUseTime(
                rhongomyniadRenderStack, rhongomyniadRenderPlayer, timeHeld, rhongomyniadFrameInterp);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            shift = At.Shift.AFTER))
    private void adjustSpearStab(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                 float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                 SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof SpearRelicItem)
                || ItemPoseEditor.isPreviewingUse(hand, itemStack)
                || RelicWeaponAnimations.isIwatoshiRightClickAttack(player, itemStack)) {
            return;
        }

        ThrustSwingState swingState = hand == InteractionHand.MAIN_HAND
                ? spearMainHandState : spearOffHandState;
        applyThrustAdjustment(player, hand, attack, itemStack, poseStack, swingState,
                GungnirExtensions.STAB_ROTATION_Z, GungnirExtensions.STAB_ROTATION_X,
                GungnirExtensions.STAB_TRANSLATION_Y, GungnirExtensions.STAB_MIN_EXTENSION);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            shift = At.Shift.AFTER))
    private void adjustRhongomyniadStab(AbstractClientPlayer player, float frameInterp, float xRot,
                                        InteractionHand hand, float attack, ItemStack itemStack,
                                        float inverseArmHeight, PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector, int lightCoords,
                                        CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof RhongomyniadItem)
                || ItemPoseEditor.isPreviewingUse(hand, itemStack)) {
            return;
        }

        ThrustSwingState swingState = hand == InteractionHand.MAIN_HAND
                ? rhongomyniadMainHandState : rhongomyniadOffHandState;
        applyThrustAdjustment(player, hand, attack, itemStack, poseStack, swingState,
                RHONGOMYNIAD_INWARD_ROTATION, RHONGOMYNIAD_DOWNWARD_ROTATION,
                RHONGOMYNIAD_MAX_EXTENSION, RHONGOMYNIAD_MIN_EXTENSION);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void applyGenericItemPose(AbstractClientPlayer player, float frameInterp, float xRot,
                                      InteractionHand hand, float attack, ItemStack itemStack,
                                      float inverseArmHeight, PoseStack poseStack,
                                      SubmitNodeCollector submitNodeCollector, int lightCoords,
                                      CallbackInfo callbackInfo) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        if (ItemPoseEditor.isPreviewingGenericAttack(hand, itemStack)) {
            float previewAttack = ItemPoseEditor.getPreviewAttackProgress(hand, itemStack);
            int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
            switch (itemStack.getSwingAnimation().type()) {
                case WHACK -> swingArm(previewAttack, poseStack, direction, arm);
                case STAB -> net.minecraft.client.model.effects.SpearAnimations.firstPersonAttack(
                        previewAttack, poseStack, direction, arm);
            }
        }
        ItemPoseEditor.applyFirstPersonGenericPose(
                poseStack, player, hand, itemStack, arm, attack, frameInterp);
    }

    @Unique
    private static void applyThrustAdjustment(AbstractClientPlayer player, InteractionHand hand, float attack, ItemStack itemStack,
                                              PoseStack poseStack, ThrustSwingState swingState, float inwardRotation,
                                              float downwardRotation, float maxExtension, float minExtension) {
        boolean isSwinging = player.swinging && player.swingingArm == hand;
        if (isSwinging && (!swingState.wasSwinging || player.swingTime < swingState.lastSwingTime)) {
            swingState.extensionScale = calculateExtensionScale(player, itemStack, minExtension);
        }
        swingState.wasSwinging = isSwinging;
        swingState.lastSwingTime = player.swingTime;

        float thrust = Ease.outBack(progress(attack, 0.05F, 0.2F)) - Ease.inOutExpo(progress(attack, 0.4F, 1.0F));
        float thrustAdjust = thrust * swingState.extensionScale;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * inwardRotation * thrustAdjust));
        poseStack.mulPose(Axis.XP.rotationDegrees(downwardRotation * thrust));
        poseStack.translate(0.0F, maxExtension * thrustAdjust, 0.0F);
    }

    @Unique
    private static float calculateExtensionScale(AbstractClientPlayer player, ItemStack itemStack, float minExtension) {
        AttackRange attackRange = player.getAttackRangeWith(itemStack);
        var hitResults = ProjectileUtil.getHitEntitiesAlong(player, attackRange,
                target -> PiercingWeapon.canHitEntity(player, target), ClipContext.Block.COLLIDER);

        Vec3 eyePosition = player.getEyePosition();
        double closestDistanceSquared = Double.MAX_VALUE;
        var blockHitResult = hitResults.left();
        if (blockHitResult.isPresent() && blockHitResult.get().getType() == HitResult.Type.BLOCK) {
            closestDistanceSquared = eyePosition.distanceToSqr(blockHitResult.get().getLocation());
        }

        var entityHitResults = hitResults.right();
        if (entityHitResults.isPresent()) {
            for (EntityHitResult hitResult : entityHitResults.get()) {
                closestDistanceSquared = Math.min(closestDistanceSquared,
                        eyePosition.distanceToSqr(hitResult.getLocation()));
            }
        }

        if (closestDistanceSquared == Double.MAX_VALUE) {
            return 1.0F;
        }

        float minimumReach = attackRange.effectiveMinRange(player);
        float maximumReach = attackRange.effectiveMaxRange(player);
        if (maximumReach <= minimumReach) {
            return 1.0F;
        }

        float targetDistance = (float) Math.sqrt(closestDistanceSquared);
        float distanceProgress = Mth.clamp(
                Mth.inverseLerp(targetDistance, minimumReach, maximumReach), 0.0F, 1.0F);
        return Mth.lerp(distanceProgress, minExtension, 1.0F);
    }

    @Unique
    private static float progress(float time, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
    }

    @Unique
    private static HumanoidArm armFor(AbstractClientPlayer player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private static final class ThrustSwingState {
        private float extensionScale = 1.0F;
        private boolean wasSwinging;
        private int lastSwingTime;
    }
}
