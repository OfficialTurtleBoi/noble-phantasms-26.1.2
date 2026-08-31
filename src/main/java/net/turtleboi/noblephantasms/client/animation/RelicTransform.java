package net.turtleboi.noblephantasms.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class RelicTransform {
    private final boolean modelDisplay;
    private boolean initialized;
    private float initialTranslationX;
    private float initialTranslationY;
    private float initialTranslationZ;
    private float initialRotationX;
    private float initialRotationY;
    private float initialRotationZ;
    private float initialScaleX = 1.0F;
    private float initialScaleY = 1.0F;
    private float initialScaleZ = 1.0F;
    private float initialAnchorX;
    private float initialAnchorY;
    private float initialAnchorZ;
    public float translationX;
    public float translationY;
    public float translationZ;
    public float rotationX;
    public float rotationY;
    public float rotationZ;
    public float scaleX = 1.0F;
    public float scaleY = 1.0F;
    public float scaleZ = 1.0F;
    public float anchorX;
    public float anchorY;
    public float anchorZ;

    private RelicTransform(boolean modelDisplay) {
        this.modelDisplay = modelDisplay;
        if (modelDisplay) {
            anchorX = initialAnchorX = 0.5F;
            anchorY = initialAnchorY = 0.5F;
            anchorZ = initialAnchorZ = 0.5F;
        }
    }

    public static RelicTransform modelDisplay() {
        return new RelicTransform(true);
    }

    public static RelicTransform poseStack(float translationX, float translationY, float translationZ,
                                           float rotationX, float rotationY, float rotationZ) {
        return poseStack(translationX, translationY, translationZ,
                rotationX, rotationY, rotationZ, 1.0F, 1.0F, 1.0F);
    }

    public static RelicTransform poseStack(float translationX, float translationY, float translationZ,
                                           float rotationX, float rotationY, float rotationZ,
                                           float scaleX, float scaleY, float scaleZ) {
        RelicTransform transform = new RelicTransform(false);
        transform.initialize(translationX, translationY, translationZ,
                rotationX, rotationY, rotationZ, scaleX, scaleY, scaleZ);
        return transform;
    }

    public RelicTransform anchor(float anchorX, float anchorY, float anchorZ) {
        this.anchorX = initialAnchorX = anchorX;
        this.anchorY = initialAnchorY = anchorY;
        this.anchorZ = initialAnchorZ = anchorZ;
        return this;
    }

    public RelicTransform copy() {
        RelicTransform copy = new RelicTransform(modelDisplay);
        copy.initialized = initialized;
        copy.initialTranslationX = initialTranslationX;
        copy.initialTranslationY = initialTranslationY;
        copy.initialTranslationZ = initialTranslationZ;
        copy.initialRotationX = initialRotationX;
        copy.initialRotationY = initialRotationY;
        copy.initialRotationZ = initialRotationZ;
        copy.initialScaleX = initialScaleX;
        copy.initialScaleY = initialScaleY;
        copy.initialScaleZ = initialScaleZ;
        copy.initialAnchorX = initialAnchorX;
        copy.initialAnchorY = initialAnchorY;
        copy.initialAnchorZ = initialAnchorZ;
        copy.translationX = translationX;
        copy.translationY = translationY;
        copy.translationZ = translationZ;
        copy.rotationX = rotationX;
        copy.rotationY = rotationY;
        copy.rotationZ = rotationZ;
        copy.scaleX = scaleX;
        copy.scaleY = scaleY;
        copy.scaleZ = scaleZ;
        copy.anchorX = anchorX;
        copy.anchorY = anchorY;
        copy.anchorZ = anchorZ;
        return copy;
    }

    public static RelicTransform interpolate(RelicTransform start, RelicTransform end, float progress) {
        RelicTransform transform = new RelicTransform(end.modelDisplay);
        transform.initialize(
                Mth.lerp(progress, start.translationX, end.translationX),
                Mth.lerp(progress, start.translationY, end.translationY),
                Mth.lerp(progress, start.translationZ, end.translationZ),
                Mth.lerp(progress, start.rotationX, end.rotationX),
                Mth.lerp(progress, start.rotationY, end.rotationY),
                Mth.lerp(progress, start.rotationZ, end.rotationZ),
                Mth.lerp(progress, start.scaleX, end.scaleX),
                Mth.lerp(progress, start.scaleY, end.scaleY),
                Mth.lerp(progress, start.scaleZ, end.scaleZ));
        transform.anchorX = transform.initialAnchorX = end.anchorX;
        transform.anchorY = transform.initialAnchorY = end.anchorY;
        transform.anchorZ = transform.initialAnchorZ = end.anchorZ;
        return transform;
    }

    public void apply(PoseStack poseStack, HumanoidArm arm) {
        apply(poseStack, arm, 1.0F);
    }

    public void apply(PoseStack poseStack, HumanoidArm arm, float weight) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float progress = Mth.clamp(weight, 0.0F, 1.0F);
        float pivotX = direction * anchorX;
        poseStack.translate(direction * translationX * progress,
                translationY * progress, translationZ * progress);
        poseStack.translate(pivotX, anchorY, anchorZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX * progress));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * rotationY * progress));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * rotationZ * progress));
        poseStack.scale(Mth.lerp(progress, 1.0F, scaleX),
                Mth.lerp(progress, 1.0F, scaleY), Mth.lerp(progress, 1.0F, scaleZ));
        poseStack.translate(-pivotX, -anchorY, -anchorZ);
    }

    public void apply(ModelPart modelPart, HumanoidArm arm, RelicAnimation.BlendMode blendMode) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float rotationScale = (float) Math.PI / 180.0F;
        if (blendMode == RelicAnimation.BlendMode.REPLACE) {
            modelPart.x = direction * translationX;
            modelPart.y = translationY;
            modelPart.z = translationZ;
            modelPart.xRot = rotationX * rotationScale;
            modelPart.yRot = direction * rotationY * rotationScale;
            modelPart.zRot = direction * rotationZ * rotationScale;
            modelPart.xScale = scaleX;
            modelPart.yScale = scaleY;
            modelPart.zScale = scaleZ;
            return;
        }

        modelPart.x += direction * translationX;
        modelPart.y += translationY;
        modelPart.z += translationZ;
        modelPart.xRot += rotationX * rotationScale;
        modelPart.yRot += direction * rotationY * rotationScale;
        modelPart.zRot += direction * rotationZ * rotationScale;
        modelPart.xScale *= scaleX;
        modelPart.yScale *= scaleY;
        modelPart.zScale *= scaleZ;
    }

    void initialize(float translationX, float translationY, float translationZ,
                    float rotationX, float rotationY, float rotationZ,
                    float scaleX, float scaleY, float scaleZ) {
        if (initialized) {
            return;
        }
        this.translationX = initialTranslationX = translationX;
        this.translationY = initialTranslationY = translationY;
        this.translationZ = initialTranslationZ = translationZ;
        this.rotationX = initialRotationX = rotationX;
        this.rotationY = initialRotationY = rotationY;
        this.rotationZ = initialRotationZ = rotationZ;
        this.scaleX = initialScaleX = scaleX;
        this.scaleY = initialScaleY = scaleY;
        this.scaleZ = initialScaleZ = scaleZ;
        initialized = true;
    }

    public boolean initialized() {
        return initialized;
    }

    public boolean usesModelDisplay() {
        return modelDisplay;
    }

    public String spaceName() {
        return modelDisplay ? "model_display" : "pose_stack";
    }

    public void reset() {
        translationX = initialTranslationX;
        translationY = initialTranslationY;
        translationZ = initialTranslationZ;
        rotationX = initialRotationX;
        rotationY = initialRotationY;
        rotationZ = initialRotationZ;
        scaleX = initialScaleX;
        scaleY = initialScaleY;
        scaleZ = initialScaleZ;
        anchorX = initialAnchorX;
        anchorY = initialAnchorY;
        anchorZ = initialAnchorZ;
    }
}
