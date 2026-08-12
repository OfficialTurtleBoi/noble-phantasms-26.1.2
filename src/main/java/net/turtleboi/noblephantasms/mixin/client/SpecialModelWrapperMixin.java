package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelWrapper.class)
public class SpecialModelWrapperMixin {
    @Inject(method = "update", at = @At("RETURN"))
    private void animateColoredGlint(ItemStackRenderState output, ItemStack item,
                                     ItemModelResolver resolver, ItemDisplayContext displayContext,
                                     ClientLevel level, ItemOwner owner, int seed,
                                     CallbackInfo callbackInfo) {
        if (ColoredGlintRenderer.hasColoredGlint(item)) {
            output.setAnimated();
        }
    }
}
