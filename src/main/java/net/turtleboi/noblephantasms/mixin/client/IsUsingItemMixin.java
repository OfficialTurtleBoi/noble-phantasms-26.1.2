package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IsUsingItem.class)
public class IsUsingItemMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$previewUsingItem(ItemStack stack, @Nullable ClientLevel level,
                                                 @Nullable LivingEntity owner, int seed,
                                                 ItemDisplayContext displayContext,
                                                 CallbackInfoReturnable<Boolean> callbackInfo) {
        if (owner != null && ItemPoseEditor.previewIsUsing(stack, owner, displayContext)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
