package net.turtleboi.noblephantasms.client.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;

public final class RelicAnimationClip {
    private float durationTicks;
    private final List<Keyframe> keyframes = new ArrayList<>();

    public RelicAnimationClip(float durationTicks) {
        this.durationTicks = Math.max(durationTicks, 1.0F);
    }

    public RelicAnimationClip keyframe(float tick, RelicTransform transform) {
        return keyframe(tick, transform, Easing.IN_OUT_SINE);
    }

    public RelicAnimationClip keyframe(float tick, RelicTransform transform, Easing easing) {
        float keyframeTick = Mth.clamp(tick, 0.0F, durationTicks);
        keyframes.removeIf(keyframe -> Math.abs(keyframe.tick() - keyframeTick) < 0.0001F);
        keyframes.add(new Keyframe(keyframeTick, transform, easing));
        keyframes.sort(Comparator.comparingDouble(Keyframe::tick));
        return this;
    }

    public void removeKeyframe(int index) {
        if (keyframes.size() > 1 && index >= 0 && index < keyframes.size()) {
            keyframes.remove(index);
        }
    }

    public void setEasing(int index, Easing easing) {
        if (index < 0 || index >= keyframes.size()) {
            return;
        }
        Keyframe keyframe = keyframes.get(index);
        keyframes.set(index, new Keyframe(keyframe.tick(), keyframe.transform(), easing));
    }

    public void setDurationTicks(float durationTicks) {
        float updatedDuration = Math.max(durationTicks, 1.0F);
        if (Math.abs(updatedDuration - this.durationTicks) < 0.0001F) {
            return;
        }
        float scale = updatedDuration / this.durationTicks;
        this.durationTicks = updatedDuration;
        for (int index = 0; index < keyframes.size(); index++) {
            Keyframe keyframe = keyframes.get(index);
            keyframes.set(index, new Keyframe(
                    Mth.clamp(keyframe.tick() * scale, 0.0F, updatedDuration),
                    keyframe.transform(), keyframe.easing()));
        }
    }

    public int moveKeyframe(int index, float tick) {
        if (index < 0 || index >= keyframes.size()) {
            return closestKeyframe(tick);
        }
        Keyframe moved = keyframes.remove(index);
        float updatedTick = Mth.clamp(tick, 0.0F, durationTicks);
        keyframes.removeIf(keyframe -> Math.abs(keyframe.tick() - updatedTick) < 0.0001F);
        keyframes.add(new Keyframe(updatedTick, moved.transform(), moved.easing()));
        keyframes.sort(Comparator.comparingDouble(Keyframe::tick));
        return closestKeyframe(updatedTick);
    }

    public RelicTransform sample(float tick) {
        if (keyframes.isEmpty()) {
            return RelicTransform.poseStack(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        }
        if (keyframes.size() == 1 || tick <= keyframes.getFirst().tick()) {
            return keyframes.getFirst().transform();
        }
        if (tick >= keyframes.getLast().tick()) {
            return keyframes.getLast().transform();
        }

        for (int index = 1; index < keyframes.size(); index++) {
            Keyframe end = keyframes.get(index);
            if (tick > end.tick()) {
                continue;
            }
            Keyframe start = keyframes.get(index - 1);
            float progress = Mth.clamp(Mth.inverseLerp(tick, start.tick(), end.tick()), 0.0F, 1.0F);
            return RelicTransform.interpolate(start.transform(), end.transform(), end.easing().apply(progress));
        }
        return keyframes.getLast().transform();
    }

    public RelicAnimationClip copy() {
        RelicAnimationClip copy = new RelicAnimationClip(durationTicks);
        for (Keyframe keyframe : keyframes) {
            copy.keyframe(keyframe.tick(), keyframe.transform().copy(), keyframe.easing());
        }
        return copy;
    }

    public int closestKeyframe(float tick) {
        int closestIndex = 0;
        float closestDistance = Float.MAX_VALUE;
        for (int index = 0; index < keyframes.size(); index++) {
            float distance = Math.abs(keyframes.get(index).tick() - tick);
            if (distance < closestDistance) {
                closestIndex = index;
                closestDistance = distance;
            }
        }
        return closestIndex;
    }

    public float durationTicks() {
        return durationTicks;
    }

    public List<Keyframe> keyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    public enum Easing {
        LINEAR,
        IN_OUT_SINE,
        OUT_BACK,
        IN_OUT_EXPO;

        private float apply(float progress) {
            return switch (this) {
                case LINEAR -> progress;
                case IN_OUT_SINE -> Ease.inOutSine(progress);
                case OUT_BACK -> Ease.outBack(progress);
                case IN_OUT_EXPO -> Ease.inOutExpo(progress);
            };
        }
    }

    public record Keyframe(float tick, RelicTransform transform, Easing easing) {
    }
}
