package net.turtleboi.noblephantasms.client.renderer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public record ReliquaryItemRenderState(
        TrackingItemStackRenderState item,
        Vector3f modelCenter,
        Quaternionf rotation,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public ReliquaryItemRenderState(
            TrackingItemStackRenderState item,
            Vector3f modelCenter,
            Quaternionf rotation,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(item, modelCenter, rotation, x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
