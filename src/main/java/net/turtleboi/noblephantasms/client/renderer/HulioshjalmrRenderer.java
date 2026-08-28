package net.turtleboi.noblephantasms.client.renderer;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;

public final class HulioshjalmrRenderer {
    private static final float MINIMUM_ALPHA = 0.2F;
    private static final ContextKey<Float> CONCEALMENT_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "hulioshjalmr_concealment"));
    private static final ThreadLocal<Float> ACTIVE_PROGRESS = new ThreadLocal<>();

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                HulioshjalmrRenderer::extractState);
    }

    public static float getProgress(LivingEntityRenderState state) {
        Float progress = state.getRenderData(CONCEALMENT_KEY);
        return progress == null ? 0.0F : Mth.clamp(progress, 0.0F, 1.0F);
    }

    public static int applyFade(int color, float progress) {
        float alphaScale = Mth.lerp(Mth.clamp(progress, 0.0F, 1.0F), 1.0F, MINIMUM_ALPHA);
        int alpha = Mth.clamp(Math.round(ARGB.alpha(color) * alphaScale), 0, 255);
        return ARGB.color(alpha, color);
    }

    public static void beginRendering(LivingEntityRenderState state) {
        float progress = getProgress(state);
        if (progress > 0.0F) {
            ACTIVE_PROGRESS.set(progress);
        }
    }

    public static void endRendering() {
        ACTIVE_PROGRESS.remove();
    }

    public static float getActiveProgress() {
        Float progress = ACTIVE_PROGRESS.get();
        return progress == null ? 0.0F : progress;
    }

    private static void extractState(LivingEntity entity, LivingEntityRenderState state) {
        float progress = entity instanceof Player player
                ? HulioshjalmrItem.getConcealmentProgress(player)
                : 0.0F;
        state.setRenderData(CONCEALMENT_KEY, progress);
        state.shadowRadius *= 1.0F - progress;
        if (progress >= 1.0F) {
            state.nameTag = null;
            state.outlineColor = 0;
        }
    }
}
