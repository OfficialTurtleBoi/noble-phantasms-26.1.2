package net.turtleboi.noblephantasms.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem.TrophyData;
import org.jspecify.annotations.Nullable;

public final class TrophyHeadShapeCache {
    private static final Map<TrophyData, Dimensions> DIMENSIONS = new ConcurrentHashMap<>();

    public static void register(TrophyData trophyData, float width, float height, float depth) {
        if (Float.isFinite(width) && Float.isFinite(height) && Float.isFinite(depth)
                && width > 0.0F && height > 0.0F && depth > 0.0F) {
            DIMENSIONS.put(trophyData, new Dimensions(width, height, depth));
        }
    }

    public static VoxelShape getShape(BlockState state, @Nullable TrophyData trophyData) {
        Dimensions dimensions = trophyData != null ? DIMENSIONS.get(trophyData) : null;
        if (dimensions == null) {
            return Shapes.block();
        }

        if (state.getBlock() instanceof WallSkullBlock) {
            return createWallShape(state, dimensions);
        }
        return createStandingShape(state, dimensions);
    }

    private static VoxelShape createStandingShape(BlockState state, Dimensions dimensions) {
        float angle = RotationSegment.convertToDegrees(state.getValue(SkullBlock.ROTATION))
                * ((float) Math.PI / 180.0F);
        float cosine = Math.abs((float) Math.cos(angle));
        float sine = Math.abs((float) Math.sin(angle));
        float rotatedWidth = cosine * dimensions.width() + sine * dimensions.depth();
        float rotatedDepth = sine * dimensions.width() + cosine * dimensions.depth();
        return box(0.5F - rotatedWidth * 0.5F, 0.0F, 0.5F - rotatedDepth * 0.5F,
                0.5F + rotatedWidth * 0.5F, dimensions.height(), 0.5F + rotatedDepth * 0.5F);
    }

    private static VoxelShape createWallShape(BlockState state, Dimensions dimensions) {
        Direction facing = state.getValue(WallSkullBlock.FACING);
        boolean rotated = facing.getAxis() == Direction.Axis.X;
        float width = rotated ? dimensions.depth() : dimensions.width();
        float depth = rotated ? dimensions.width() : dimensions.depth();
        float centerX = facing.getAxis() == Direction.Axis.X
                ? 0.5F - facing.getStepX() * (0.5F - width * 0.5F)
                : 0.5F;
        float centerZ = facing.getAxis() == Direction.Axis.Z
                ? 0.5F - facing.getStepZ() * (0.5F - depth * 0.5F)
                : 0.5F;
        return box(centerX - width * 0.5F, 0.5F - dimensions.height() * 0.5F,
                centerZ - depth * 0.5F, centerX + width * 0.5F,
                0.5F + dimensions.height() * 0.5F, centerZ + depth * 0.5F);
    }

    private static VoxelShape box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        double clampedMinX = clamp(minX);
        double clampedMinY = clamp(minY);
        double clampedMinZ = clamp(minZ);
        double clampedMaxX = clamp(maxX);
        double clampedMaxY = clamp(maxY);
        double clampedMaxZ = clamp(maxZ);
        if (clampedMinX >= clampedMaxX || clampedMinY >= clampedMaxY || clampedMinZ >= clampedMaxZ) {
            return Shapes.block();
        }
        return Shapes.box(clampedMinX, clampedMinY, clampedMinZ,
                clampedMaxX, clampedMaxY, clampedMaxZ);
    }

    private static double clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private record Dimensions(float width, float height, float depth) {
    }
}
