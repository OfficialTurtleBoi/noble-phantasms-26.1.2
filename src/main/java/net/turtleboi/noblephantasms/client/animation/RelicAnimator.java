package net.turtleboi.noblephantasms.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public final class RelicAnimator {
    private static final Map<Identifier, Map<String, RelicAnimation>> ANIMATIONS = new HashMap<>();
    private static final Map<Identifier, Map<String, EditorPose>> EDITOR_POSES = new HashMap<>();

    public static void register(Identifier itemId, String animationId, RelicAnimationClip animation) {
        register(itemId, animationId,
                new RelicAnimation().channel(RelicAnimation.Channel.ITEM, animation));
    }

    public static void register(Identifier itemId, String animationId, RelicAnimation animation) {
        ANIMATIONS.computeIfAbsent(itemId, ignored -> new HashMap<>()).put(animationId, animation);
    }

    public static void registerEditorPose(Identifier itemId, String pose,
                                          String firstPersonAnimation, String thirdPersonAnimation) {
        EDITOR_POSES.computeIfAbsent(itemId, ignored -> new LinkedHashMap<>())
                .put(pose, new EditorPose(firstPersonAnimation, thirdPersonAnimation));
    }

    public static List<String> getEditorPoses(ItemStack itemStack) {
        Map<String, EditorPose> poses = EDITOR_POSES.get(
                BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
        return poses == null ? List.of() : List.copyOf(poses.keySet());
    }

    public static String getEditorAnimationId(ItemStack itemStack, CameraType cameraType, String pose) {
        return getEditorAnimationId(BuiltInRegistries.ITEM.getKey(itemStack.getItem()), cameraType, pose);
    }

    public static String getEditorAnimationId(Identifier itemId, CameraType cameraType, String pose) {
        Map<String, EditorPose> poses = EDITOR_POSES.get(itemId);
        EditorPose editorPose = poses == null ? null : poses.get(pose);
        if (editorPose == null) {
            return null;
        }
        return cameraType.isFirstPerson()
                ? editorPose.firstPersonAnimation() : editorPose.thirdPersonAnimation();
    }

    public static boolean applyEditorAnimation(Identifier itemId, CameraType cameraType,
                                               String pose, RelicAnimation editedAnimation) {
        String animationId = getEditorAnimationId(itemId, cameraType, pose);
        Map<String, RelicAnimation> animations = ANIMATIONS.get(itemId);
        RelicAnimation animation = animationId == null || animations == null
                ? null : animations.get(animationId);
        if (animation == null) {
            return false;
        }

        for (RelicAnimation.Channel channel : editedAnimation.channels()) {
            animation.channel(channel, editedAnimation.channel(channel).copy(),
                    editedAnimation.blendMode(channel));
        }
        return true;
    }

    public static RelicAnimation getAnimation(ItemStack itemStack, String animationId) {
        return getAnimation(BuiltInRegistries.ITEM.getKey(itemStack.getItem()), animationId);
    }

    static RelicAnimation getAnimation(Identifier itemId, String animationId) {
        Map<String, RelicAnimation> animations = ANIMATIONS.get(itemId);
        return animations == null ? null : animations.get(animationId);
    }

    static boolean applyStoredAnimation(Identifier itemId, String animationId,
                                        RelicAnimation storedAnimation) {
        RelicAnimation animation = getAnimation(itemId, animationId);
        if (animation == null) {
            return false;
        }
        for (RelicAnimation.Channel channel : storedAnimation.channels()) {
            animation.channel(channel, storedAnimation.channel(channel).copy(),
                    storedAnimation.blendMode(channel));
        }
        return true;
    }

    public static RelicAnimationClip get(ItemStack itemStack, String animationId) {
        RelicAnimation animation = getAnimation(itemStack, animationId);
        return animation == null ? null : animation.channel(RelicAnimation.Channel.ITEM);
    }

    public static RelicTransform sample(ItemStack itemStack, String animationId, float tick) {
        return sample(itemStack, animationId, RelicAnimation.Channel.ITEM, tick);
    }

    public static RelicTransform sample(ItemStack itemStack, String animationId,
                                        RelicAnimation.Channel channel, float tick) {
        RelicTransform editorTransform = ItemPoseEditor.getAnimationOverride(
                itemStack, animationId, channel);
        if (editorTransform != null) {
            return editorTransform;
        }
        RelicAnimation animationDefinition = getAnimation(itemStack, animationId);
        RelicAnimationClip animation = animationDefinition == null
                ? null : animationDefinition.channel(channel);
        return animation == null ? null : animation.sample(tick);
    }

    public static boolean apply(ItemStack itemStack, String animationId, float tick,
                                PoseStack poseStack, HumanoidArm arm) {
        RelicTransform transform = sample(itemStack, animationId, tick);
        if (transform == null) {
            return false;
        }
        transform.apply(poseStack, arm);
        return true;
    }

    public static boolean apply(ItemStack itemStack, String animationId,
                                RelicAnimation.Channel channel, float tick,
                                ModelPart modelPart, HumanoidArm arm) {
        RelicAnimation animation = getAnimation(itemStack, animationId);
        if (animation == null) {
            return false;
        }
        RelicTransform transform = sample(itemStack, animationId, channel, tick);
        if (transform == null) {
            return false;
        }
        transform.apply(modelPart, arm, animation.blendMode(channel));
        return true;
    }

    private record EditorPose(String firstPersonAnimation, String thirdPersonAnimation) {
    }
}
