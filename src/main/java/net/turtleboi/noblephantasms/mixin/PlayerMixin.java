package net.turtleboi.noblephantasms.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.effect.custom.FearedEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public class PlayerMixin {
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 forceFearedMovement(Vec3 input) {
        return FearedEffect.forcePlayerTravel((Player) (Object) this, input);
    }
}
