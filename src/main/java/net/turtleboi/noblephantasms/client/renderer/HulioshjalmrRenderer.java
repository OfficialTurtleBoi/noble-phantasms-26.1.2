package net.turtleboi.noblephantasms.client.renderer;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.turtleboi.noblephantasms.item.custom.HulioshjalmrItem;

public final class HulioshjalmrRenderer {
    private static final float MINIMUM_ALPHA = 0.2F;

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                HulioshjalmrRenderer::extractState);
    }

    private static void extractState(LivingEntity entity, LivingEntityRenderState state) {
        if (!(entity instanceof Player player)) {
            return;
        }
        float progress = HulioshjalmrItem.getConcealmentProgress(player);
        float minimumAlpha = isVisibleToViewer(player) ? MINIMUM_ALPHA : 0.0F;
        EntityTranslucencyRenderer.setTranslucencyState(state, progress, minimumAlpha);
    }

    private static boolean isVisibleToViewer(Player wearer) {
        Player viewer = Minecraft.getInstance().player;
        return viewer != null && (viewer == wearer || viewer.isAlliedTo(wearer));
    }
}
