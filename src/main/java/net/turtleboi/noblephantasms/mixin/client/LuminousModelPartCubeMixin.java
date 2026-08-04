package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import net.turtleboi.noblephantasms.client.renderer.LuminousRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelPart.Cube.class)
public class LuminousModelPartCubeMixin {
    @Redirect(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix4f;transformPosition"
                            + "(FFFLorg/joml/Vector3f;)Lorg/joml/Vector3f;"))
    private Vector3f expandLuminousOutline(Matrix4f matrix, float x, float y, float z, Vector3f destination) {
        ModelPart.Cube cube = (ModelPart.Cube) (Object) this;
        destination.set(x, y, z);
        LuminousRenderer.expandCubeVertex(
                destination,
                cube.minX,
                cube.minY,
                cube.minZ,
                cube.maxX,
                cube.maxY,
                cube.maxZ);
        return matrix.transformPosition(destination);
    }
}
