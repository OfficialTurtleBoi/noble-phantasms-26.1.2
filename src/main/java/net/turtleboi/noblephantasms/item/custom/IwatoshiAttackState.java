package net.turtleboi.noblephantasms.item.custom;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;

public final class IwatoshiAttackState {
    private static final int FIRST_ATTACK_EVENT = 100;
    private static final int ATTACK_ANIMATION_TICKS = 23;
    private static final Map<LivingEntity, Attack> ATTACKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static byte getAttackEvent(int chargeLevel) {
        return (byte) (FIRST_ATTACK_EVENT + chargeLevel - 1);
    }

    public static int getAttackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    public static boolean handleAttackEvent(LivingEntity entity, byte eventId) {
        int chargeLevel = eventId - FIRST_ATTACK_EVENT + 1;
        if (chargeLevel < 1 || chargeLevel > IwatoshiItem.getMaxChargeLevel()
                || (!(entity.getMainHandItem().getItem() instanceof IwatoshiItem)
                && !(entity.getOffhandItem().getItem() instanceof IwatoshiItem))) {
            return false;
        }

        ATTACKS.put(entity, new Attack(
                chargeLevel, entity.tickCount, Math.max(entity.getCurrentSwingDuration(), 1)));
        return true;
    }

    public static int getChargeLevel(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }

        Attack attack = ATTACKS.get(entity);
        if (attack == null) {
            return 0;
        }

        if (entity.tickCount - attack.startTick() > attack.durationTicks()) {
            ATTACKS.remove(entity);
            return 0;
        }

        return attack.chargeLevel();
    }

    private record Attack(int chargeLevel, int startTick, int durationTicks) {
    }
}
