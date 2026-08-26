package net.turtleboi.noblephantasms.entity;

import net.minecraft.world.entity.Entity;
import net.turtleboi.noblephantasms.attachment.ModAttachments;

public final class AfterimageEffect {
    public static void activate(Entity entity, int durationTicks) {
        long expiresAt = entity.level().getGameTime() + Math.max(durationTicks, 1);
        if (expiresAt > entity.getData(ModAttachments.AFTERIMAGE_EXPIRES_AT)) {
            entity.setData(ModAttachments.AFTERIMAGE_EXPIRES_AT, expiresAt);
        }
    }

    public static boolean isActive(Entity entity) {
        return entity.getData(ModAttachments.AFTERIMAGE_EXPIRES_AT) > entity.level().getGameTime();
    }
}
