package net.turtleboi.noblephantasms.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.turtleboi.noblephantasms.attachment.ModAttachments;

public final class AfterimageEffect {
    private static final int MOTION_EXTENSION_TICKS = 10;
    private static final double MIN_EXTENSION_SPEED_SQUARED = 0.0225;

    public static void activate(Entity entity, int durationTicks) {
        long expiresAt = entity.level().getGameTime() + Math.max(durationTicks, 1);
        if (expiresAt > entity.getData(ModAttachments.AFTERIMAGE_EXPIRES_AT)) {
            entity.setData(ModAttachments.AFTERIMAGE_EXPIRES_AT, expiresAt);
        }
    }

    public static boolean isActive(Entity entity) {
        long expiresAt = entity.getData(ModAttachments.AFTERIMAGE_EXPIRES_AT);
        long gameTime = entity.level().getGameTime();
        boolean active = expiresAt > gameTime
                || expiresAt > 0L && gameTime - expiresAt <= MOTION_EXTENSION_TICKS
                && entity.getDeltaMovement().lengthSqr() > MIN_EXTENSION_SPEED_SQUARED;
        if (!active || !(entity instanceof Player)) {
            return active;
        }
        double horizontalSpeedSquared = entity.getDeltaMovement().horizontalDistanceSqr();
        return horizontalSpeedSquared > MIN_EXTENSION_SPEED_SQUARED;
    }
}
