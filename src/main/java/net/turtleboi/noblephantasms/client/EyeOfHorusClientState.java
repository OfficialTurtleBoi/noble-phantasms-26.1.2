package net.turtleboi.noblephantasms.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.CurioRelicItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;

public final class EyeOfHorusClientState {
    private static final Map<LivingEntity, GlowState> GLOW_STATES = new WeakHashMap<>();

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            GLOW_STATES.clear();
            return;
        }

        LivingEntity target = null;
        if (CurioRelicItem.isEquipped(minecraft.player, ModItems.EYE_OF_HORUS.get())) {
            target = EyeOfHorusItem.getLookTarget(minecraft.player);
            if (target != null && hasJudgementGlow(target)) {
                target = null;
            }
        }

        if (target != null) {
            GLOW_STATES.computeIfAbsent(target, ignored -> new GlowState());
        }

        var iterator = GLOW_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            LivingEntity entity = entry.getKey();
            GlowState state = entry.getValue();
            if (!entity.isAlive() || entity.level() != minecraft.level) {
                iterator.remove();
                continue;
            }

            state.previousFocusProgress = state.focusProgress;
            state.previousJudgementProgress = state.judgementProgress;
            int focusDuration = EyeOfHorusItem.getFocusDuration(minecraft.player);
            float focusChange = entity == target
                    ? 1.0F / focusDuration
                    : -((float) EyeOfHorusItem.FOCUS_DECAY_PER_TICK / focusDuration);
            state.focusProgress = Mth.clamp(state.focusProgress + focusChange, 0.0F, 1.0F);
            state.judgementProgress = hasJudgementGlow(entity)
                    ? 1.0F
                    : Mth.clamp(
                            state.judgementProgress
                                    - (float) EyeOfHorusItem.FOCUS_DECAY_PER_TICK / focusDuration,
                            0.0F,
                            1.0F);
            if (state.focusProgress == 0.0F && state.previousFocusProgress == 0.0F
                    && state.judgementProgress == 0.0F && state.previousJudgementProgress == 0.0F) {
                iterator.remove();
            }
        }
    }

    public static GlowProgress getProgress(LivingEntity entity, float partialTick) {
        GlowState state = GLOW_STATES.get(entity);
        if (hasJudgementGlow(entity)) {
            if (state == null) {
                state = new GlowState();
                GLOW_STATES.put(entity, state);
            }
            state.previousJudgementProgress = 1.0F;
            state.judgementProgress = 1.0F;
        }

        Integer syncedFocusTicks = entity.getExistingDataOrNull(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS);
        Minecraft minecraft = Minecraft.getInstance();
        int focusDuration = minecraft.player == null
                ? EyeOfHorusItem.BASE_FOCUS_DURATION
                : EyeOfHorusItem.getFocusDuration(minecraft.player);
        float syncedFocusProgress = syncedFocusTicks == null
                ? 0.0F
                : Mth.clamp((float) syncedFocusTicks / focusDuration, 0.0F, 1.0F);
        float focusProgress;
        float judgementProgress;
        if (state == null) {
            focusProgress = syncedFocusProgress;
            judgementProgress = 0.0F;
        } else {
            focusProgress = Math.max(
                    Mth.lerp(partialTick, state.previousFocusProgress, state.focusProgress),
                    syncedFocusProgress);
            judgementProgress = Mth.lerp(
                    partialTick, state.previousJudgementProgress, state.judgementProgress);
        }
        return new GlowProgress(focusProgress, judgementProgress);
    }

    private static boolean hasJudgementGlow(LivingEntity entity) {
        return entity.hasEffect(ModEffects.JUDGEMENT)
                || Boolean.TRUE.equals(entity.getExistingDataOrNull(ModAttachments.EYE_OF_HORUS_JUDGEMENT_GLOW));
    }

    public record GlowProgress(float focus, float judgement) {
        public float total() {
            return Math.max(focus, judgement);
        }
    }

    private static final class GlowState {
        private float previousFocusProgress;
        private float focusProgress;
        private float previousJudgementProgress;
        private float judgementProgress;
    }
}
