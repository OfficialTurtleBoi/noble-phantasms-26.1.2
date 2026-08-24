package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.turtleboi.noblephantasms.client.animation.RelicTransform;
import org.joml.Vector3f;

public final class ItemPoseDebugRenderer {
    private static final int BOUNDS_COLOR = 0xBFFFFFFF;
    private static final int ANCHOR_COLOR = 0xFFFFFF00;
    private static final int X_COLOR = 0xFFFF5555;
    private static final int Y_COLOR = 0xFF55FF55;
    private static final int Z_COLOR = 0xFF5555FF;

    public static void submit(ItemStackRenderState renderState, PoseStack poseStack,
                              SubmitNodeCollector submitNodeCollector, RelicTransform transform,
                              boolean leftHand) {
        AABB bounds = renderState.getModelBoundingBox();
        if (bounds.getSize() <= 0.0) {
            return;
        }

        Vector3f anchor = getAnchor(transform, leftHand);
        float axisLength = (float) Math.max(bounds.getSize() * 0.35, 0.2);
        float markerSize = axisLength * 0.08F;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(),
                (pose, buffer) -> {
                    drawBounds(pose, buffer, bounds);
                    drawAnchor(pose, buffer, anchor, markerSize);
                    drawAxis(pose, buffer, anchor, axisLength, 1.0F, 0.0F, 0.0F, X_COLOR);
                    drawAxis(pose, buffer, anchor, axisLength, 0.0F, 1.0F, 0.0F, Y_COLOR);
                    drawAxis(pose, buffer, anchor, axisLength, 0.0F, 0.0F, 1.0F, Z_COLOR);
                });
    }

    private static Vector3f getAnchor(RelicTransform transform, boolean leftHand) {
        float direction = leftHand ? -1.0F : 1.0F;
        if (transform.usesModelDisplay()) {
            float anchorX = leftHand ? 1.0F - transform.anchorX : transform.anchorX;
            return new Vector3f(
                    direction * transform.translationX / 16.0F + anchorX - 0.5F,
                    transform.translationY / 16.0F + transform.anchorY - 0.5F,
                    transform.translationZ / 16.0F + transform.anchorZ - 0.5F);
        }
        return new Vector3f(
                direction * transform.anchorX,
                transform.anchorY,
                transform.anchorZ);
    }

    private static void drawBounds(PoseStack.Pose pose, VertexConsumer buffer, AABB bounds) {
        float minX = (float) bounds.minX;
        float minY = (float) bounds.minY;
        float minZ = (float) bounds.minZ;
        float maxX = (float) bounds.maxX;
        float maxY = (float) bounds.maxY;
        float maxZ = (float) bounds.maxZ;
        line(pose, buffer, minX, minY, minZ, maxX, minY, minZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, maxX, minY, minZ, maxX, maxY, minZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, maxX, maxY, minZ, minX, maxY, minZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, minX, maxY, minZ, minX, minY, minZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, minX, minY, maxZ, maxX, minY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, maxX, minY, maxZ, maxX, maxY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, maxX, maxY, maxZ, minX, maxY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, minX, maxY, maxZ, minX, minY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, minX, minY, minZ, minX, minY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, maxX, minY, minZ, maxX, minY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, maxX, maxY, minZ, maxX, maxY, maxZ, BOUNDS_COLOR, 1.0F);
        line(pose, buffer, minX, maxY, minZ, minX, maxY, maxZ, BOUNDS_COLOR, 1.0F);
    }

    private static void drawAnchor(PoseStack.Pose pose, VertexConsumer buffer,
                                   Vector3f anchor, float size) {
        line(pose, buffer, anchor.x - size, anchor.y, anchor.z,
                anchor.x + size, anchor.y, anchor.z, ANCHOR_COLOR, 3.0F);
        line(pose, buffer, anchor.x, anchor.y - size, anchor.z,
                anchor.x, anchor.y + size, anchor.z, ANCHOR_COLOR, 3.0F);
        line(pose, buffer, anchor.x, anchor.y, anchor.z - size,
                anchor.x, anchor.y, anchor.z + size, ANCHOR_COLOR, 3.0F);
    }

    private static void drawAxis(PoseStack.Pose pose, VertexConsumer buffer, Vector3f anchor,
                                 float length, float x, float y, float z, int color) {
        float endX = anchor.x + x * length;
        float endY = anchor.y + y * length;
        float endZ = anchor.z + z * length;
        line(pose, buffer, anchor.x, anchor.y, anchor.z, endX, endY, endZ, color, 2.5F);
        float arrowSize = length * 0.16F;
        if (x != 0.0F) {
            line(pose, buffer, endX, endY, endZ, endX - arrowSize, endY + arrowSize * 0.5F,
                    endZ, color, 2.5F);
            line(pose, buffer, endX, endY, endZ, endX - arrowSize, endY - arrowSize * 0.5F,
                    endZ, color, 2.5F);
        } else if (y != 0.0F) {
            line(pose, buffer, endX, endY, endZ, endX + arrowSize * 0.5F, endY - arrowSize,
                    endZ, color, 2.5F);
            line(pose, buffer, endX, endY, endZ, endX - arrowSize * 0.5F, endY - arrowSize,
                    endZ, color, 2.5F);
        } else {
            line(pose, buffer, endX, endY, endZ, endX + arrowSize * 0.5F, endY,
                    endZ - arrowSize, color, 2.5F);
            line(pose, buffer, endX, endY, endZ, endX - arrowSize * 0.5F, endY,
                    endZ - arrowSize, color, 2.5F);
        }
    }

    private static void line(PoseStack.Pose pose, VertexConsumer buffer,
                             float startX, float startY, float startZ,
                             float endX, float endY, float endZ, int color, float width) {
        Vector3f normal = new Vector3f(endX - startX, endY - startY, endZ - startZ).normalize();
        buffer.addVertex(pose, startX, startY, startZ)
                .setColor(color).setNormal(pose, normal).setLineWidth(width);
        buffer.addVertex(pose, endX, endY, endZ)
                .setColor(color).setNormal(pose, normal).setLineWidth(width);
    }
}
