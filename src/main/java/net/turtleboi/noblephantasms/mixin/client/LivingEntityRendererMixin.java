package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    protected EntityModel<?> model;

    @Final
    @Shadow
    protected List<?> layers;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void noblePhantasms$extractTrophyHead(LivingEntity entity, LivingEntityRenderState state,
                                                   float partialTick, CallbackInfo callbackInfo) {
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        TrophyHeadItem.TrophyData trophyData = TrophyHeadItem.getTrophyData(headItem);
        TrophyHeadRenderer.setWornHead(state, trophyData);
        if (trophyData == null) {
            return;
        }
        state.wornHeadType = null;
        state.wornHeadProfile = null;
        state.headItem.clear();
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$captureTrophyModel(LivingEntityRenderState state, PoseStack poseStack,
                                                   SubmitNodeCollector submitNodeCollector,
                                                   CameraRenderState camera, CallbackInfo callbackInfo) {
        if (TrophyHeadRenderer.captureSelectedModel(model, layers)) {
            callbackInfo.cancel();
        }
    }
}
