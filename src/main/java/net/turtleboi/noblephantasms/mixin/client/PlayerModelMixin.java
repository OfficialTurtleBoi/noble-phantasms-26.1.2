package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void noblePhantasms$hideHeadUnderTrophy(AvatarRenderState state, CallbackInfo callbackInfo) {
        PlayerModel model = (PlayerModel) (Object) this;
        boolean wearingTrophy = TrophyHeadRenderer.getWornHead(state) != null;
        model.head.visible = !wearingTrophy;
        if (wearingTrophy) {
            model.hat.visible = false;
        }
    }
}
