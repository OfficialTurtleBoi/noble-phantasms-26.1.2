package net.turtleboi.noblephantasms.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.turtleboi.noblephantasms.client.renderer.ItemOutlineRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectionMatrixBuffer.class)
public class ProjectionMatrixBufferMixin {
    @Inject(method = "getBuffer(Lnet/minecraft/client/renderer/Projection;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At("HEAD"))
    private void captureProjection(Projection projection, CallbackInfoReturnable<GpuBufferSlice> callbackInfo) {
        ItemOutlineRenderer.setProjection(projection.getMatrix(new Matrix4f()));
    }

    @Inject(method = "getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At("HEAD"))
    private void captureProjection(Matrix4f projection, CallbackInfoReturnable<GpuBufferSlice> callbackInfo) {
        ItemOutlineRenderer.setProjection(projection);
    }
}
