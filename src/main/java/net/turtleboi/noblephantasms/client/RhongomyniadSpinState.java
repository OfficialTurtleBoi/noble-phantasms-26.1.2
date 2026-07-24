package net.turtleboi.noblephantasms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.joml.Quaternionf;

public final class RhongomyniadSpinState {
    private static final float SHAFT_AXIS = 0.70710677F;
    private static final ThreadLocal<Float> SPIN_DEGREES = ThreadLocal.withInitial(() -> 0.0F);

    private RhongomyniadSpinState() {
    }

    public static void begin(ItemStack itemStack, float timeHeld) {
        begin(itemStack, timeHeld, null);
    }

    public static void begin(ItemStack itemStack, float timeHeld, LivingEntity entity) {
        if (itemStack.getItem() instanceof RhongomyniadItem) {
            if (entity != null) {
                timeHeld = RhongomyniadItem.getSpinTime(entity, timeHeld);
            }
            SPIN_DEGREES.set(RhongomyniadItem.getSpinDegrees(itemStack, timeHeld));
        } else {
            SPIN_DEGREES.set(0.0F);
        }
    }

    public static void apply(PoseStack.Pose pose) {
        float spinDegrees = SPIN_DEGREES.get();
        if (spinDegrees != 0.0F) {
            pose.rotate(new Quaternionf().rotationAxis(spinDegrees * ((float) Math.PI / 180.0F),
                    SHAFT_AXIS, SHAFT_AXIS, 0.0F));
        }
    }

    public static float getLowerProgressCorrection(ItemStack itemStack, float timeHeld) {
        if (timeHeld < RhongomyniadItem.getChargeStartTick(itemStack)) {
            return 0.0F;
        }

        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return 0.0F;
        }

        int finishRaisingTick = kineticWeapon.delayTicks();
        int finishLoweringTick = kineticWeapon.knockbackConditions()
                .map(KineticWeapon.Condition::maxDurationTicks)
                .orElse(0) + finishRaisingTick;
        int startLoweringTick = finishLoweringTick - 40;
        float lowerProgress = Ease.outCubic(Ease.inOutElastic(Mth.clamp(
                Mth.inverseLerp(timeHeld - 20.0F, startLoweringTick, finishLoweringTick), 0.0F, 1.0F)));
        int finishRaisingBackTick = kineticWeapon.damageConditions()
                .map(KineticWeapon.Condition::maxDurationTicks)
                .orElse(0) + finishRaisingTick;
        float raiseBackProgress = Mth.clamp(
                Mth.inverseLerp(timeHeld, finishRaisingBackTick - 5.0F, finishRaisingBackTick), 0.0F, 1.0F);
        return lowerProgress * (1.0F - raiseBackProgress);
    }

    public static float getVisualUseTime(ItemStack itemStack, LivingEntity entity, float currentUseTime, float partialTick) {
        if (!RhongomyniadItem.isRecovering(entity)) {
            return currentUseTime;
        }

        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return currentUseTime;
        }

        int finishRaisingBackTick = kineticWeapon.delayTicks()
                + kineticWeapon.damageConditions()
                .map(KineticWeapon.Condition::maxDurationTicks)
                .orElse(0);
        float recoveryProgress = Ease.inOutSine(RhongomyniadItem.getRecoveryProgress(entity, partialTick));
        return Mth.lerp(recoveryProgress, finishRaisingBackTick - 5.0F, finishRaisingBackTick);
    }

    public static float getPoseWeight(ItemStack itemStack, float visualUseTime) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return 1.0F;
        }

        int finishRaisingBackTick = kineticWeapon.delayTicks()
                + kineticWeapon.damageConditions()
                .map(KineticWeapon.Condition::maxDurationTicks)
                .orElse(0);
        float raiseBackProgress = Mth.clamp(
                Mth.inverseLerp(visualUseTime, finishRaisingBackTick - 5.0F, finishRaisingBackTick), 0.0F, 1.0F);
        return 1.0F - raiseBackProgress;
    }

    public static void end() {
        SPIN_DEGREES.remove();
    }
}
