package net.turtleboi.noblephantasms.client;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.network.TrophySupportPayload;

public final class BertilakClientUtil {
    private static final float READINESS_FLASH_TICKS = 10.0F;
    private static final Map<LivingEntity, GlowState> GLOW_STATES = new WeakHashMap<>();
    private static UUID lastTargetId;

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            lastTargetId = null;
            GLOW_STATES.clear();
            return;
        }

        Mob target = null;
        if (player.isUsingItem() && player.getUseItem().is(ModItems.BERTILAK)
                && !player.hasEffect(ModEffects.COVENANT)) {
            target = BertilakItem.findLookTarget(player);
            if (target != null && (target.hasEffect(ModEffects.COVENANT) || hasCovenantGlow(target))) {
                target = null;
            }
        } else {
            lastTargetId = null;
        }

        if (target != null) {
            GLOW_STATES.computeIfAbsent(target, ignored -> new GlowState());
            if (!target.getUUID().equals(lastTargetId)) {
                lastTargetId = target.getUUID();
                boolean supported = TrophyHeadRenderer.hasRenderableHead(target);
                ClientPacketDistributor.sendToServer(
                        new TrophySupportPayload(target.getUUID(), supported));
            }
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
            state.previousCovenantProgress = state.covenantProgress;
            state.previousReadinessFlashTicks = state.readinessFlashTicks;
            state.focusProgress = getSyncedFocusProgress(entity);
            state.covenantProgress = hasCovenantGlow(entity)
                    ? 1.0F
                    : Mth.clamp(
                            state.covenantProgress - BertilakItem.COVENANT_PROGRESS_DECAY_PER_TICK,
                            0.0F,
                            1.0F);
            if (state.readinessFlashing) {
                state.readinessFlashTicks = Math.min(state.readinessFlashTicks + 1.0F, READINESS_FLASH_TICKS);
                if (state.readinessFlashTicks >= READINESS_FLASH_TICKS) {
                    state.readinessFlashing = false;
                }
            }
            if (state.focusProgress == 0.0F && state.previousFocusProgress == 0.0F
                    && state.covenantProgress == 0.0F && state.previousCovenantProgress == 0.0F) {
                iterator.remove();
            }
        }
    }

    public static GlowProgress getGlowProgress(LivingEntity entity, float partialTick) {
        GlowState state = GLOW_STATES.get(entity);
        boolean covenantGlow = hasCovenantGlow(entity);
        if (covenantGlow) {
            if (state == null) {
                state = new GlowState();
                GLOW_STATES.put(entity, state);
            }
            if (!state.covenantActive) {
                startReadinessFlash(state);
            }
            state.covenantActive = true;
            state.previousCovenantProgress = 1.0F;
            state.covenantProgress = 1.0F;
        } else if (state != null) {
            state.covenantActive = false;
        }

        float syncedFocusProgress = getSyncedFocusProgress(entity);
        if (state == null && syncedFocusProgress > 0.0F) {
            state = new GlowState();
            state.previousFocusProgress = syncedFocusProgress;
            state.focusProgress = syncedFocusProgress;
            GLOW_STATES.put(entity, state);
        }
        float focusProgress;
        float covenantProgress;
        if (state == null) {
            focusProgress = syncedFocusProgress;
            covenantProgress = 0.0F;
        } else {
            focusProgress = Mth.lerp(
                    partialTick, state.previousFocusProgress, state.focusProgress);
            covenantProgress = Mth.lerp(
                    partialTick, state.previousCovenantProgress, state.covenantProgress);
        }

        float readinessFlash = 0.0F;
        boolean readinessFlashing = state != null
                && (state.readinessFlashing
                        || (state.previousReadinessFlashTicks < READINESS_FLASH_TICKS
                                && state.readinessFlashTicks >= READINESS_FLASH_TICKS));
        if (readinessFlashing) {
            float flashTicks = Mth.lerp(
                    partialTick, state.previousReadinessFlashTicks, state.readinessFlashTicks);
            readinessFlash = Mth.sin(Mth.clamp(flashTicks / READINESS_FLASH_TICKS, 0.0F, 1.0F) * Mth.PI);
        }
        return new GlowProgress(
                focusProgress,
                covenantProgress,
                readinessFlash,
                readinessFlashing,
                covenantProgress > 0.0F);
    }

    private static boolean hasCovenantGlow(LivingEntity entity) {
        return Boolean.TRUE.equals(entity.getExistingDataOrNull(ModAttachments.BERTILAK_COVENANT_GLOW));
    }

    private static float getSyncedFocusProgress(LivingEntity entity) {
        Float syncedProgress = entity.getExistingDataOrNull(ModAttachments.BERTILAK_GLOW_PROGRESS);
        return syncedProgress == null
                ? 0.0F
                : Mth.clamp(syncedProgress, 0.0F, 1.0F);
    }

    private static void startReadinessFlash(GlowState state) {
        state.readinessFlashing = true;
        state.previousReadinessFlashTicks = 0.0F;
        state.readinessFlashTicks = 0.0F;
    }

    public record GlowProgress(float focus, float covenant, float readinessFlash, boolean readinessFlashing,
                               boolean ready) {
        public float total() {
            return Math.max(focus, covenant);
        }
    }

    private static final class GlowState {
        private float previousFocusProgress;
        private float focusProgress;
        private float previousCovenantProgress;
        private float covenantProgress;
        private float previousReadinessFlashTicks;
        private float readinessFlashTicks;
        private boolean readinessFlashing;
        private boolean covenantActive;
    }
}
