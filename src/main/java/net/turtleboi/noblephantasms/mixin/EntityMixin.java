package net.turtleboi.noblephantasms.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.CurioRelicItem;
import net.minecraft.world.entity.player.Player;
import net.turtleboi.noblephantasms.entity.custom.ApilolliCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isInWaterOrRain", at = @At("HEAD"), cancellable = true)
    private void apilolliCountsAsRain(CallbackInfoReturnable<Boolean> callbackInfo) {
        if ((Object) this instanceof Player player && ApilolliCloudEntity.hasActiveCloud(player)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void ignoreBlockMovementPenalty(BlockState state, Vec3 multiplier, CallbackInfo callbackInfo) {
        if ((Object) this instanceof LivingEntity living
                && CurioRelicItem.isEquipped(living, ModItems.MEGINGJORD.get())) {
            callbackInfo.cancel();
        }
    }
}
