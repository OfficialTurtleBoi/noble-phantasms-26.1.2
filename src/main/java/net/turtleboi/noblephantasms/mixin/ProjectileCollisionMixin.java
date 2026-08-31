package net.turtleboi.noblephantasms.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileCollisionMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$filterProjectionCollision(
            Entity target, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (target instanceof PridwenBarrierPart part
                && !part.canBlockProjectile((Projectile) (Object) this)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
