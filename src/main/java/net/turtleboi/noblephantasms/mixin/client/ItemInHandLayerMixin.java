package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void beginRhongomyniadSpin(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack itemStack,
                                       HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                       int lightCoords, CallbackInfo callbackInfo) {
        LivingEntity entity = null;
        if (state instanceof AvatarRenderState avatarRenderState
                && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(avatarRenderState.id) instanceof LivingEntity livingEntity) {
            entity = livingEntity;
        }
        RhongomyniadSpinState.begin(itemStack, state.ticksUsingItem(arm), entity);
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void endRhongomyniadSpin(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack itemStack,
                                     HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                     int lightCoords, CallbackInfo callbackInfo) {
        RhongomyniadSpinState.end();
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void applyGenericItemPose(ArmedEntityRenderState state, ItemStackRenderState item,
                                      ItemStack itemStack, HumanoidArm arm, PoseStack poseStack,
                                      SubmitNodeCollector submitNodeCollector, int lightCoords,
                                      CallbackInfo callbackInfo) {
        ItemPoseEditor.applyThirdPersonGenericPose(state, poseStack, arm, itemStack);
    }
}
