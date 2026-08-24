package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.BertilakExtensions;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.animation.RelicWeaponAnimations;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.effects.SpearAnimations")
public class SpearAnimationsMixin {
    @Inject(method = "firstPersonUse", at = @At("HEAD"), cancellable = true)
    private static void animateRelicFirstPersonUse(float hitFeedbackTime, PoseStack poseStack, float timeHeld,
                                                    HumanoidArm arm, ItemStack itemStack,
                                                    CallbackInfo callbackInfo) {
        if (RelicWeaponAnimations.firstPersonUse(hitFeedbackTime, poseStack, timeHeld, arm, itemStack)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "thirdPersonHandUse", at = @At("HEAD"), cancellable = true)
    private static void animateRelicThirdPersonHandUse(ModelPart armPart, ModelPart headPart, boolean rightArm,
                                                        ItemStack itemStack, HumanoidRenderState state,
                                                        CallbackInfo callbackInfo) {
        if (RelicWeaponAnimations.thirdPersonHandUse(armPart, headPart, rightArm, itemStack, state)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "thirdPersonUseItem", at = @At("HEAD"), cancellable = true)
    private static void animateRelicThirdPersonUseItem(ArmedEntityRenderState state, PoseStack poseStack,
                                                        float timeHeld, HumanoidArm arm, ItemStack itemStack,
                                                        CallbackInfo callbackInfo) {
        if (RelicWeaponAnimations.thirdPersonUseItem(state, poseStack, timeHeld, arm, itemStack)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "firstPersonAttack", at = @At("HEAD"), cancellable = true)
    private static void animateIwatoshiFirstPersonAttack(float attackTime, PoseStack poseStack, int direction,
                                                          HumanoidArm arm, CallbackInfo callbackInfo) {
        if (RelicWeaponAnimations.firstPersonAttack(attackTime, poseStack, direction, arm)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "thirdPersonAttackHand", at = @At("HEAD"), cancellable = true)
    private static void animateIwatoshiThirdPersonAttackHand(HumanoidModel<?> model, HumanoidRenderState state,
                                                              CallbackInfo callbackInfo) {
        if (RelicWeaponAnimations.thirdPersonAttackHand(model, state)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "thirdPersonAttackItem", at = @At("HEAD"), cancellable = true)
    private static void animateIwatoshiThirdPersonAttackItem(ArmedEntityRenderState state, PoseStack poseStack,
                                                              CallbackInfo callbackInfo) {
        if (RelicWeaponAnimations.thirdPersonAttackItem(state, poseStack)
                || ItemPoseEditor.applyThirdPersonAttackItemPreview(state, poseStack)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "thirdPersonUseItem", at = @At("TAIL"))
    private static void adjustBertilakCovenant(ArmedEntityRenderState state, PoseStack poseStack, float timeHeld,
                                               HumanoidArm arm, ItemStack itemStack, CallbackInfo callbackInfo) {
        if (itemStack.getItem() instanceof BertilakItem) {
            BertilakExtensions.applyThirdPersonCovenantTransform(
                    state, poseStack, arm, itemStack, timeHeld);
        }
    }
}
