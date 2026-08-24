package net.turtleboi.noblephantasms.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.turtleboi.noblephantasms.entity.custom.ApilolliCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow
    private int timeUntilLured;

    @Shadow
    private int timeUntilHooked;

    @Inject(method = "catchingFish", at = @At("TAIL"))
    private void accelerateApilolliFishing(BlockPos pos, CallbackInfo callbackInfo) {
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        if (owner == null || !ApilolliCloudEntity.hasActiveCloud(owner)) {
            return;
        }

        if (timeUntilLured > 1) {
            timeUntilLured--;
        }
        if (timeUntilHooked > 1) {
            timeUntilHooked--;
        }
    }
}
