package net.turtleboi.noblephantasms.entity.renderer.states;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class GungnirProjectileRenderState extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public float xRotation;
    public float yRotation;
}
