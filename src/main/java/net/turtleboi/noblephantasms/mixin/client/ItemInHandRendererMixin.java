package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    private static final float GUNGNIR_INWARD_ROTATION = 15.0F;
    private static final float GUNGNIR_DOWNWARD_ROTATION = -15.0F;
    private static final float GUNGNIR_MAX_EXTENSION = 0.75F;
    private static final float GUNGNIR_MIN_EXTENSION = -0.5F;
    private final GungnirSwingState gungnirMainHandState = new GungnirSwingState();
    private final GungnirSwingState gungnirOffHandState = new GungnirSwingState();

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            shift = At.Shift.AFTER))
    private void adjustGungnirStab(AbstractClientPlayer player, float frameInterp, float xRot,
                                   InteractionHand hand, float attack, ItemStack itemStack,
                                   float inverseArmHeight, PoseStack poseStack,
                                   SubmitNodeCollector submitNodeCollector, int lightCoords,
                                   CallbackInfo callbackInfo) {
        if (!(itemStack.getItem() instanceof GungnirItem)) {
            return;
        }

        GungnirSwingState swingState = hand == InteractionHand.MAIN_HAND
                ? gungnirMainHandState
                : gungnirOffHandState;
        boolean isSwinging = player.swinging && player.swingingArm == hand;
        if (isSwinging && (!swingState.wasSwinging || player.swingTime < swingState.lastSwingTime)) {
            swingState.extensionScale = calculateExtensionScale(player, itemStack);
        }
        swingState.wasSwinging = isSwinging;
        swingState.lastSwingTime = player.swingTime;

        float thrust = Ease.outBack(progress(attack, 0.05F, 0.2F)) - Ease.inOutExpo(progress(attack, 0.4F, 1.0F));
        float thrustAdjust = thrust * swingState.extensionScale;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * GUNGNIR_INWARD_ROTATION * thrustAdjust));
        poseStack.mulPose(Axis.XP.rotationDegrees(GUNGNIR_DOWNWARD_ROTATION * thrust));
        poseStack.translate(0.0F, GUNGNIR_MAX_EXTENSION * thrustAdjust, 0.0F);
    }

    @Unique
    private static float calculateExtensionScale(AbstractClientPlayer player, ItemStack itemStack) {
        AttackRange attackRange = player.getAttackRangeWith(itemStack);
        var hitResults = ProjectileUtil.getHitEntitiesAlong(player, attackRange,
                target -> PiercingWeapon.canHitEntity(player, target), ClipContext.Block.COLLIDER);

        Vec3 eyePosition = player.getEyePosition();
        double closestDistanceSquared = Double.MAX_VALUE;
        var blockHitResult = hitResults.left();
        if (blockHitResult.isPresent() && blockHitResult.get().getType() == HitResult.Type.BLOCK) {
            closestDistanceSquared = eyePosition.distanceToSqr(blockHitResult.get().getLocation());
        }

        var entityHitResults = hitResults.right();
        if (entityHitResults.isPresent()) {
            for (EntityHitResult hitResult : entityHitResults.get()) {
                closestDistanceSquared = Math.min(
                        closestDistanceSquared, eyePosition.distanceToSqr(hitResult.getLocation()));
            }
        }

        if (closestDistanceSquared == Double.MAX_VALUE) {
            return 1.0F;
        }

        float minimumReach = attackRange.effectiveMinRange(player);
        float maximumReach = attackRange.effectiveMaxRange(player);
        if (maximumReach <= minimumReach) {
            return 1.0F;
        }

        float targetDistance = (float) Math.sqrt(closestDistanceSquared);
        float distanceProgress = Mth.clamp(
                Mth.inverseLerp(targetDistance, minimumReach, maximumReach),
                0.0F,
                1.0F);
        return Mth.lerp(distanceProgress, GUNGNIR_MIN_EXTENSION, 1.0F);
    }

    @Unique
    private static float progress(float time, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
    }

    private static final class GungnirSwingState {
        private float extensionScale = 1.0F;
        private boolean wasSwinging;
        private int lastSwingTime;
    }
}
