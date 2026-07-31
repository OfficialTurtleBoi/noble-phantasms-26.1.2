package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.client.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.GungnirExtensions;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    private final ThrustSwingState gungnirMainHandState = new ThrustSwingState();
    private final ThrustSwingState gungnirOffHandState = new ThrustSwingState();

    private static final float RHONGOMYNIAD_INWARD_ROTATION = 15.0F;
    private static final float RHONGOMYNIAD_DOWNWARD_ROTATION = -15.0F;
    private static final float RHONGOMYNIAD_MAX_EXTENSION = 0.15F;
    private static final float RHONGOMYNIAD_MIN_EXTENSION = -0.75F;
    private static final float RHONGOMYNIAD_JOUST_INWARD_ROTATION = 15.0F;
    private static final float RHONGOMYNIAD_JOUST_DOWNWARD_ROTATION = -15.0F;
    private static final float RHONGOMYNIAD_JOUST_EXTENSION = 0.15F;
    private final ThrustSwingState rhongomyniadMainHandState = new ThrustSwingState();
    private final ThrustSwingState rhongomyniadOffHandState = new ThrustSwingState();
    @Unique
    private AbstractClientPlayer rhongomyniadRenderPlayer;
    @Unique
    private ItemStack rhongomyniadRenderStack = ItemStack.EMPTY;
    @Unique
    private float rhongomyniadFrameInterp;

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
        RhongomyniadSpinState.begin(itemStack, timeHeld, player);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void endRhongomyniadSpin(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                     float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        RhongomyniadSpinState.end();
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
    private void adjustGungnirStab(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                   float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof GungnirItem)) {
            return;
        }

        ThrustSwingState swingState = hand == InteractionHand.MAIN_HAND ? gungnirMainHandState : gungnirOffHandState;
        applyThrustAdjustment(player, hand, attack, itemStack, poseStack, swingState,
                GungnirExtensions.STAB_ROTATION_Z, GungnirExtensions.STAB_ROTATION_X,
                GungnirExtensions.STAB_TRANSLATION_Y, GungnirExtensions.STAB_MIN_EXTENSION);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            shift = At.Shift.AFTER))
    private void adjustRhongomyniadStab(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                        float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof RhongomyniadItem)) {
            return;
        }

        ThrustSwingState swingState = hand == InteractionHand.MAIN_HAND ? rhongomyniadMainHandState : rhongomyniadOffHandState;
        applyThrustAdjustment(player, hand, attack, itemStack, poseStack, swingState,
                RHONGOMYNIAD_INWARD_ROTATION, RHONGOMYNIAD_DOWNWARD_ROTATION,
                RHONGOMYNIAD_MAX_EXTENSION, RHONGOMYNIAD_MIN_EXTENSION);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            shift = At.Shift.AFTER))
    private void previewEditedStab(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                   float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        ItemPoseEditor.applyFirstPersonAttackPreview(poseStack, hand, itemStack, arm);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void previewEditedUse(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                  float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                  SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        ItemPoseEditor.applyFirstPersonUsePreview(poseStack, hand, itemStack, arm);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonUse(FLcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER))
    private void adjustRhongomyniadJoust(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                         float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                         SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof RhongomyniadItem)) {
            return;
        }

        float timeHeld = itemStack.getUseDuration(player) - (player.getUseItemRemainingTicks() - frameInterp + 1.0F);
        float visualUseTime = RhongomyniadSpinState.getVisualUseTime(itemStack, player, timeHeld, frameInterp);
        float joustProgress = calculateJoustProgress(itemStack, visualUseTime)
                * RhongomyniadSpinState.getPoseWeight(itemStack, visualUseTime);
        float lowerProgressCorrection =
                RhongomyniadSpinState.getLowerProgressCorrection(itemStack, visualUseTime);
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F * lowerProgressCorrection));
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * RHONGOMYNIAD_JOUST_INWARD_ROTATION * joustProgress));
        poseStack.mulPose(Axis.XP.rotationDegrees(RHONGOMYNIAD_JOUST_DOWNWARD_ROTATION * joustProgress));
        poseStack.translate(0.0F, RHONGOMYNIAD_JOUST_EXTENSION * joustProgress, 0.0F);
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
    private static float calculateJoustProgress(ItemStack itemStack, float timeHeld) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return 0.0F;
        }

        float raiseProgress = Mth.clamp(timeHeld / Math.max(kineticWeapon.delayTicks(), 1), 0.0F, 1.0F);
        return Ease.inOutSine(raiseProgress);
    }

    @Unique
    private static float progress(float time, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
    }

    private static final class ThrustSwingState {
        private float extensionScale = 1.0F;
        private boolean wasSwinging;
        private int lastSwingTime;
    }
}
