package net.turtleboi.noblephantasms.item.custom;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.turtleboi.noblephantasms.effect.ModEffects;
import top.theillusivec4.curios.api.SlotContext;

public final class EyeOfHorusItem extends CurioRelicItem {
    private static final int FOCUS_DURATION = 20 * 2;
    private static final int JUDGEMENT_DURATION = 20 * 15;
    private static final double JUDGEMENT_RANGE = 64.0;
    private static final Map<Player, GazeState> GAZE_STATES = new WeakHashMap<>();

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
        if (target == null || target.hasEffect(ModEffects.JUDGEMENT)) {
            GAZE_STATES.remove(player);
            return;
        }

        GazeState previous = GAZE_STATES.get(player);
        int focusTicks = previous != null && previous.target() == target ? previous.focusTicks() + 1 : 1;
        if (focusTicks < FOCUS_DURATION) {
            GAZE_STATES.put(player, new GazeState(target, focusTicks));
            return;
        }

        target.addEffect(new MobEffectInstance(ModEffects.JUDGEMENT, JUDGEMENT_DURATION, 0, false, true, true), player);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, JUDGEMENT_DURATION, 0, false, false, true), player);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.4F);
        GAZE_STATES.remove(player);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            GAZE_STATES.remove(player);
        }
    }

    private static LivingEntity getLookTarget(Player player) {
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

    private record GazeState(LivingEntity target, int focusTicks) {
    }
}
