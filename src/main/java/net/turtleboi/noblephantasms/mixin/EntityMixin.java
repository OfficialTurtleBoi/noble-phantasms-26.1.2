package net.turtleboi.noblephantasms.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.CurioRelicItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void ignoreBlockMovementPenalty(BlockState state, Vec3 multiplier, CallbackInfo callbackInfo) {
        if ((Object) this instanceof LivingEntity living
                && CurioRelicItem.isEquipped(living, ModItems.MEGINGJORD.get())) {
            callbackInfo.cancel();
        }
    }
}
