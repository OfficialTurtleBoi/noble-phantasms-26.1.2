package net.turtleboi.noblephantasms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.joml.Quaternionf;

public final class RhongomyniadSpinState {
    private static final float SHAFT_AXIS = 0.70710677F;
    private static final float RAISED_TRANSLATION_X = 3.7511F;
    private static final float RAISED_TRANSLATION_Y = 3.7511F;
    private static final float RAISED_TRANSLATION_Z = 1.0F;
    private static final float LOWERED_TRANSLATION_X = 3.7456F;
    private static final float LOWERED_TRANSLATION_Y = 3.7450F;
    private static final float LOWERED_TRANSLATION_Z = 1.0F;
    private static final float JOUST_ROTATION_X = 0.0F;
    private static final float JOUST_ROTATION_Y = 0.0F;
    private static final float JOUST_ROTATION_Z = 30.0F;
    private static final float JOUST_SCALE_X = 1.36F;
    private static final float JOUST_SCALE_Y = 1.36F;
    private static final float JOUST_SCALE_Z = 0.68F;
    private static final ThreadLocal<Float> SPIN_DEGREES = ThreadLocal.withInitial(() -> 0.0F);
    private static final ThreadLocal<Float> JOUST_PROGRESS = ThreadLocal.withInitial(() -> 0.0F);
    private static final ThreadLocal<Float> LOWER_PROGRESS = ThreadLocal.withInitial(() -> 0.0F);
    private static final ThreadLocal<ItemDisplayContext> DISPLAY_CONTEXT =
            ThreadLocal.withInitial(() -> ItemDisplayContext.NONE);

    public static void begin(ItemStack itemStack, float timeHeld) {
        begin(itemStack, timeHeld, null, 0.0F);
    }

    public static void begin(ItemStack itemStack, float timeHeld, LivingEntity entity) {
        begin(itemStack, timeHeld, entity, 0.0F);
    }

    public static void begin(ItemStack itemStack, float timeHeld, LivingEntity entity, float partialTick) {
        if (itemStack.getItem() instanceof RhongomyniadItem) {
            float spinTime = timeHeld;
            if (entity != null) {
                spinTime = RhongomyniadItem.getSpinTime(entity, timeHeld);
                timeHeld = getVisualUseTime(itemStack, entity, timeHeld, partialTick);
            }
            SPIN_DEGREES.set(RhongomyniadItem.getSpinDegrees(itemStack, spinTime));
            JOUST_PROGRESS.set(getJoustProgress(itemStack, timeHeld) * getPoseWeight(itemStack, timeHeld));
            LOWER_PROGRESS.set(getLowerProgressCorrection(itemStack, timeHeld));
        } else {
            SPIN_DEGREES.set(0.0F);
            JOUST_PROGRESS.set(0.0F);
            LOWER_PROGRESS.set(0.0F);
        }
    }

    public static void beginModelTransform(ItemDisplayContext displayContext) {
        DISPLAY_CONTEXT.set(displayContext);
    }

    public static boolean applyModelTransform(ItemTransform source, boolean applyLeftHandFix, PoseStack.Pose pose) {
        ItemDisplayContext displayContext = DISPLAY_CONTEXT.get();
        float progress = JOUST_PROGRESS.get();
        boolean firstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        if (progress <= 0.0F || !firstPerson) {
            return false;
        }

        float lowerProgress = LOWER_PROGRESS.get();
        float targetTranslationX = Mth.lerp(lowerProgress, RAISED_TRANSLATION_X, LOWERED_TRANSLATION_X);
        float targetTranslationY = Mth.lerp(lowerProgress, RAISED_TRANSLATION_Y, LOWERED_TRANSLATION_Y);
        float targetTranslationZ = Mth.lerp(lowerProgress, RAISED_TRANSLATION_Z, LOWERED_TRANSLATION_Z);
        float translationX = Mth.lerp(progress, source.translation().x() * 16.0F, targetTranslationX) / 16.0F;
        float translationY = Mth.lerp(progress, source.translation().y() * 16.0F, targetTranslationY) / 16.0F;
        float translationZ = Mth.lerp(progress, source.translation().z() * 16.0F, targetTranslationZ) / 16.0F;
        float rotationX = Mth.lerp(progress, source.rotation().x(), JOUST_ROTATION_X);
        float rotationY = Mth.lerp(progress, source.rotation().y(), JOUST_ROTATION_Y);
        float rotationZ = Mth.lerp(progress, source.rotation().z(), JOUST_ROTATION_Z);
        float scaleX = Mth.lerp(progress, source.scale().x(), JOUST_SCALE_X);
        float scaleY = Mth.lerp(progress, source.scale().y(), JOUST_SCALE_Y);
        float scaleZ = Mth.lerp(progress, source.scale().z(), JOUST_SCALE_Z);

        if (applyLeftHandFix) {
            translationX = -translationX;
            rotationY = -rotationY;
            rotationZ = -rotationZ;
        }

        pose.translate(translationX, translationY, translationZ);
        pose.rotate(TransformationHelper.quatFromXYZ(rotationX, rotationY, rotationZ, true));
        pose.scale(scaleX, scaleY, scaleZ);
        apply(pose);
        pose.rotate(TransformationHelper.quatFromXYZ(source.rightRotation().x(),
                source.rightRotation().y() * (applyLeftHandFix ? -1.0F : 1.0F),
                source.rightRotation().z() * (applyLeftHandFix ? -1.0F : 1.0F), true));
        pose.translate(-0.5F, -0.5F, -0.5F);
        return true;
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

    private static float getJoustProgress(ItemStack itemStack, float timeHeld) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return 0.0F;
        }

        float raiseProgress = Mth.clamp(timeHeld / Math.max(kineticWeapon.delayTicks(), 1), 0.0F, 1.0F);
        return Ease.inOutSine(raiseProgress);
    }

    public static void endModelTransform() {
        DISPLAY_CONTEXT.remove();
    }

    public static void end() {
        SPIN_DEGREES.remove();
        JOUST_PROGRESS.remove();
        LOWER_PROGRESS.remove();
    }
}
