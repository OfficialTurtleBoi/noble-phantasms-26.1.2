package net.turtleboi.noblephantasms.entity.renderer.states;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.Vec3;

public class KazagurumaProjectileRenderState extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public Vec3 chainVector = Vec3.ZERO;
    public Vec3 chainOrigin = Vec3.ZERO;
    public float xRotation;
    public float yRotation;
}
