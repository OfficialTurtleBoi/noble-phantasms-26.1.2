package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.BertilakExtensions;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.effects.SpearAnimations")
public class SpearAnimationsMixin {
    @Inject(method = "thirdPersonUseItem", at = @At("TAIL"))
    private static void adjustBertilakCovenant(ArmedEntityRenderState state, PoseStack poseStack, float timeHeld,
                                               HumanoidArm arm, ItemStack itemStack, CallbackInfo callbackInfo) {
        if (itemStack.getItem() instanceof BertilakItem) {
            BertilakExtensions.applyThirdPersonCovenantTransform(poseStack, arm, itemStack, timeHeld);
        }
    }
}
