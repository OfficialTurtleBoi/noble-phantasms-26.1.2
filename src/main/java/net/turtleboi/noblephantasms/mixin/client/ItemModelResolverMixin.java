package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.renderer.ColoredGlintRenderer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import net.turtleboi.noblephantasms.client.renderer.ReliquaryItemRenderer;
import net.turtleboi.noblephantasms.client.PridwenProjectionAnchor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {
    @Inject(method = "updateForTopItem", at = @At("TAIL"))
    private void noblePhantasms$trackPoseEditorItem(ItemStackRenderState output, ItemStack item,
                                                    ItemDisplayContext displayContext, @Nullable Level level,
                                                    @Nullable ItemOwner owner, int seed, CallbackInfo callbackInfo) {
        if (ReliquaryItemRenderer.isResolvingPreview()) {
            return;
        }
        ItemPoseEditor.track(output, item, displayContext, owner);
        PridwenProjectionAnchor.track(output, item, displayContext, owner);
        ColoredGlintRenderer.track(output, item);
        ItemOutlineRenderer.track(output, item, displayContext, owner);
    }
}
