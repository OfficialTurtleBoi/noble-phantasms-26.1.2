package net.turtleboi.noblephantasms.entity.renderer.states;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PridwenBarrierRenderState extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public float xRotation;
    public float yRotation;
    public float modelScale;
    public float projectionProgress;
    public float opacityMultiplier;
    public double targetOffsetX;
    public double targetOffsetY;
    public double targetOffsetZ;
    public @Nullable Matrix4f anchorPose;
}
