package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;
import top.theillusivec4.curios.api.SlotContext;

public final class EyeOfHorusItem extends CurioRelicItem {
    public static final int FOCUS_DURATION = 20 * 2;
    public static final int FOCUS_DECAY_PER_TICK = 4;
    private static final int JUDGEMENT_DURATION = 20 * 15;
    private static final double JUDGEMENT_RANGE = 64.0;
    private static final Map<Player, Map<LivingEntity, Integer>> GAZE_STATES = new WeakHashMap<>();

    public EyeOfHorusItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = getLookTarget(player);
        if (target != null && target.hasEffect(ModEffects.JUDGEMENT)) {
            target = null;
        }

        Map<LivingEntity, Integer> gazeStates = GAZE_STATES.get(player);
        if (target != null) {
            if (gazeStates == null) {
                gazeStates = new HashMap<>();
                GAZE_STATES.put(player, gazeStates);
            }
            gazeStates.putIfAbsent(target, 0);
        }
        if (gazeStates == null) {
            return;
        }

        Set<LivingEntity> changedTargets = new HashSet<>();
        var iterator = gazeStates.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            LivingEntity focusedEntity = entry.getKey();
            changedTargets.add(focusedEntity);
            if (!focusedEntity.isAlive() || focusedEntity.hasEffect(ModEffects.JUDGEMENT)) {
                iterator.remove();
                continue;
            }

            int focusTicks = entry.getValue()
                    + (focusedEntity == target ? 1 : -FOCUS_DECAY_PER_TICK);
            if (focusTicks >= FOCUS_DURATION) {
                applyJudgement(level, player, focusedEntity);
                iterator.remove();
            } else if (focusTicks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(focusTicks);
            }
        }

        if (gazeStates.isEmpty()) {
            GAZE_STATES.remove(player);
        }
        syncGlowProgress(changedTargets);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            Map<LivingEntity, Integer> removedStates = GAZE_STATES.remove(player);
            if (player.level() instanceof ServerLevel && removedStates != null) {
                syncGlowProgress(new HashSet<>(removedStates.keySet()));
            }
        }
    }

    public static LivingEntity getLookTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = start.add(view.scale(JUDGEMENT_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        double distance = start.distanceToSqr(end);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                player.getBoundingBox().expandTowards(view.scale(JUDGEMENT_RANGE)).inflate(1.0),
                EyeOfHorusItem::isValidTarget,
                distance);
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity target
                && player.hasLineOfSight(target) ? target : null;
    }

    private static boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator();
    }

    private static void applyJudgement(ServerLevel level, Player player, LivingEntity target) {
        target.addEffect(new MobEffectInstance(ModEffects.JUDGEMENT, JUDGEMENT_DURATION, 0, false, true, true), player);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void syncGlowProgress(Set<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            int focusTicks = GAZE_STATES.values().stream()
                    .map(states -> states.getOrDefault(target, 0))
                    .max(Integer::compareTo)
                    .orElse(0);
            Integer syncedFocusTicks = target.getExistingDataOrNull(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS);
            if (focusTicks <= 0) {
                if (syncedFocusTicks != null) {
                    target.removeData(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS);
                }
            } else if (syncedFocusTicks == null || syncedFocusTicks != focusTicks) {
                target.setData(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS, focusTicks);
            }
        }
    }
}
