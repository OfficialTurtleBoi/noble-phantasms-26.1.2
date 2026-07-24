package net.turtleboi.noblephantasms.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(
            method = "releaseUsingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;stopUsingItem()V"),
            cancellable = true)
    private void keepRhongomyniadReleaseEngaged(CallbackInfo callbackInfo) {
        if (RhongomyniadItem.shouldKeepJousting((LivingEntity) (Object) this)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void keepRhongomyniadEngaged(CallbackInfo callbackInfo) {
        if (RhongomyniadItem.shouldKeepJousting((LivingEntity) (Object) this)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "stopUsingItem", at = @At("RETURN"))
    private void clearRhongomyniadEngagement(CallbackInfo callbackInfo) {
        RhongomyniadItem.clearForcedJoust((LivingEntity) (Object) this);
    }
}
