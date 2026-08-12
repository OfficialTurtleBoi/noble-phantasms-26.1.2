package net.turtleboi.noblephantasms.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CuboidItemModelWrapper.class)
public class CuboidItemModelWrapperMixin {
    @ModifyExpressionValue(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"))
    private boolean forceColoredGlintFoil(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || ColoredGlintRenderer.hasColoredGlint(stack);
    }
}
