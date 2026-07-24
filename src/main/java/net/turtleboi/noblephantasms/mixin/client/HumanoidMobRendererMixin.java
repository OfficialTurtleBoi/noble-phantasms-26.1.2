package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public class HumanoidMobRendererMixin {
    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void applyRhongomyniadRecoveryTime(LivingEntity entity, HumanoidRenderState state, float partialTick,
                                                       ItemModelResolver itemModelResolver, CallbackInfo callbackInfo) {
        if (!(entity.getUseItem().getItem() instanceof RhongomyniadItem)) {
            return;
        }

        state.ticksUsingItem = RhongomyniadSpinState.getVisualUseTime(
                entity.getUseItem(), entity, state.ticksUsingItem, partialTick);
    }
}
