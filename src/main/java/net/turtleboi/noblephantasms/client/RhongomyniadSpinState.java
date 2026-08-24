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

    public static void begin(ItemStack itemStack, float timeHeld) {
        begin(itemStack, timeHeld, null);
    }

    public static void begin(ItemStack itemStack, float timeHeld, LivingEntity entity) {
        if (itemStack.getItem() instanceof RhongomyniadItem) {
            float spinTime = entity == null
                    ? timeHeld : RhongomyniadItem.getSpinTime(entity, timeHeld);
            SPIN_DEGREES.set(RhongomyniadItem.getSpinDegrees(itemStack, spinTime));
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

    public static void end() {
        SPIN_DEGREES.remove();
    }
}
