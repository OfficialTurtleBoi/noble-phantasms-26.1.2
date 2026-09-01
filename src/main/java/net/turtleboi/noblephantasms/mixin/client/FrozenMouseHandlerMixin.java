package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.SmoothDouble;
import net.turtleboi.noblephantasms.client.FrozenMouseHandlerAccess;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.effect.custom.StunnedEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class FrozenMouseHandlerMixin implements FrozenMouseHandlerAccess {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    @Final
    private SmoothDouble smoothTurnX;

    @Shadow
    @Final
    private SmoothDouble smoothTurnY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$freezeView(double deltaTime, CallbackInfo callbackInfo) {
        LocalPlayer player = minecraft.player;
        if (StunnedEffect.isImmobilized(player)) {
            callbackInfo.cancel();
        }
    }

    @Override
    public void noblePhantasms$resetLookState() {
        accumulatedDX = 0.0;
        accumulatedDY = 0.0;
        smoothTurnX.reset();
        smoothTurnY.reset();
    }
}
