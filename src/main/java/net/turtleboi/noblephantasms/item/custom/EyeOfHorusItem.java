package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.config.ModConfig;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.entity.custom.EyeShardEntity;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;
import net.minecraft.resources.Identifier;
import top.theillusivec4.curios.api.SlotContext;

public final class EyeOfHorusItem extends CurioRelicItem {
    public static final int BASE_FOCUS_DURATION = 20 * 2;
    public static final float FOCUS_PROGRESS_DECAY_PER_TICK = 4.0F / BASE_FOCUS_DURATION;
    private static final Identifier EYE_ID = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "eye_of_horus");
    private static final int FOCUS_REDUCTION_PER_PIECE = 5;
    private static final int JUDGEMENT_DURATION = 20 * 15;
    private static final double JUDGEMENT_RANGE = 64.0;
    private static final double OPEN_EYE_RADIUS = 8.0;
    private static final float DAMAGE_BONUS_PER_PIECE = 0.05F;
    private static final Map<Player, Map<LivingEntity, Float>> GAZE_STATES = new WeakHashMap<>();

    public EyeOfHorusItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        clearExpiredPieces(player);
        if (isAssemblyPending(player)) {
            clearGaze(player);
            return;
        }

        LivingEntity target = getLookTarget(player);
        if (target != null && target.hasEffect(ModEffects.JUDGEMENT)) {
            target = null;
        }

        Map<LivingEntity, Float> gazeStates = GAZE_STATES.get(player);
        if (target != null) {
            if (gazeStates == null) {
                gazeStates = new HashMap<>();
                GAZE_STATES.put(player, gazeStates);
            }
            gazeStates.putIfAbsent(target, 0.0F);
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

            float previousProgress = entry.getValue();
            float progressChange = focusedEntity == target
                    ? 1.0F / getFocusDuration(player)
                    : -FOCUS_PROGRESS_DECAY_PER_TICK;
            float progress = Math.clamp(previousProgress + progressChange, 0.0F, 1.0F);
            if (progress >= 1.0F) {
                completeJudgement(level, player, focusedEntity);
                iterator.remove();
            } else if (progress <= 0.0F) {
                iterator.remove();
            } else {
                entry.setValue(progress);
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
            clearGaze(player);
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

    public static int getFocusDuration(Player player) {
        return Math.max(10, BASE_FOCUS_DURATION
                - getActivePieces(player) * FOCUS_REDUCTION_PER_PIECE);
    }

    public static boolean collectShard(ServerPlayer player, RelicFragmentData fragment) {
        clearExpiredPieces(player);
        if (!fragment.relicId().equals(EYE_ID)) {
            return false;
        }
        int mask = player.getData(ModAttachments.EYE_OF_HORUS_PIECE_MASK);
        long seed = player.getData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED);
        int pieceCount = getPieceCount(fragment.seed());
        if (fragment.pieceCount() != pieceCount || fragment.pieceIndex() < 0
                || fragment.pieceIndex() >= pieceCount || (mask & 1 << fragment.pieceIndex()) != 0) {
            return false;
        }
        if (mask == 0) {
            player.setData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED, fragment.seed());
            long lifetime = ModConfig.EYE_PIECE_LIFETIME_SECONDS.getAsInt() * 20L;
            player.setData(ModAttachments.EYE_OF_HORUS_PIECES_EXPIRE_AT,
                    player.level().getGameTime() + lifetime);
        } else if (seed != fragment.seed()) {
            return false;
        }
        int newMask = mask | 1 << fragment.pieceIndex();
        player.setData(ModAttachments.EYE_OF_HORUS_PIECE_MASK, newMask);
        if (Integer.bitCount(newMask) >= pieceCount) {
            player.setData(ModAttachments.EYE_OF_HORUS_ASSEMBLED, false);
        }
        return true;
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !event.getEntity().hasEffect(ModEffects.JUDGEMENT)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !isEquipped(player, ModItems.EYE_OF_HORUS.get())
                || hasCompleteEye(player)
                || player.getRandom().nextDouble() >= ModConfig.EYE_SHARD_DROP_CHANCE.getAsDouble()) {
            return;
        }

        long seed = getOrCreateFragmentSeed(player);
        int pieceCount = getPieceCount(seed);
        int mask = player.getData(ModAttachments.EYE_OF_HORUS_PIECE_MASK);
        int missingMask = (1 << pieceCount) - 1 & ~mask;
        for (EyeShardEntity pending : level.getEntitiesOfClass(
                EyeShardEntity.class, player.getBoundingBox().inflate(128.0))) {
            RelicFragmentData pendingFragment = pending.getFragment();
            if (player.getUUID().equals(pending.getTarget()) && pendingFragment != null
                    && pendingFragment.seed() == seed && pendingFragment.pieceIndex() >= 0) {
                missingMask &= ~(1 << pendingFragment.pieceIndex());
            }
        }
        if (missingMask == 0) {
            return;
        }
        int selected = player.getRandom().nextInt(Integer.bitCount(missingMask));
        int pieceIndex = -1;
        for (int index = 0; index < pieceCount; index++) {
            if ((missingMask & 1 << index) != 0 && selected-- == 0) {
                pieceIndex = index;
                break;
            }
        }
        RelicFragmentData fragment = new RelicFragmentData(EYE_ID, seed, pieceIndex, pieceCount);

        EyeShardEntity shard = new EyeShardEntity(level, event.getEntity().getX(),
                event.getEntity().getY() + event.getEntity().getBbHeight() * 0.5,
                event.getEntity().getZ(), player.getUUID(), fragment);
        level.addFreshEntity(shard);
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || !isEquipped(player, ModItems.EYE_OF_HORUS.get())) {
            return;
        }

        int pieces = getActivePieces(player);
        if (pieces > 0) {
            event.setNewDamage(event.getNewDamage() * (1.0F + pieces * DAMAGE_BONUS_PER_PIECE));
        }
    }

    private static void completeJudgement(ServerLevel level, Player player, LivingEntity target) {
        if (!hasCompleteEye(player)) {
            applyJudgement(level, player, target, true);
            return;
        }

        applyJudgement(level, player, target, false);
        Vec3 burstCenter = target.getBoundingBox().getCenter();
        AABB area = new AABB(burstCenter, burstCenter).inflate(OPEN_EYE_RADIUS);
        double radiusSquared = OPEN_EYE_RADIUS * OPEN_EYE_RADIUS;
        for (Mob enemy : level.getEntitiesOfClass(Mob.class, area,
                candidate -> candidate != target
                        && candidate.isAlive()
                        && candidate.getBoundingBox().getCenter().distanceToSqr(burstCenter) <= radiusSquared)) {
            applyJudgement(level, player, enemy, false);
        }

        level.sendParticles(ParticleTypes.END_ROD, burstCenter.x(), burstCenter.y(),
                burstCenter.z(), 240, 4.0, 2.5, 4.0, 0.32);
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFD75A),
                burstCenter.x(), burstCenter.y(), burstCenter.z(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 2.5F, 0.75F);
        level.playSound(null, target.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 2.0F, 0.9F);
        level.playSound(null, target.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS, 1.4F, 1.25F);
        player.setData(ModAttachments.EYE_OF_HORUS_PIECE_MASK, 0);
        player.setData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED, 0L);
        player.setData(ModAttachments.EYE_OF_HORUS_PIECES_EXPIRE_AT, 0L);
        player.setData(ModAttachments.EYE_OF_HORUS_ASSEMBLED, false);
    }

    private static void applyJudgement(ServerLevel level, Player player, LivingEntity target, boolean playSound) {
        target.addEffect(new MobEffectInstance(ModEffects.JUDGEMENT, JUDGEMENT_DURATION, 0, false, true, true), player);
        target.setData(ModAttachments.EYE_OF_HORUS_JUDGEMENT_GLOW, true);
        if (playSound) {
            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    private static int getActivePieces(Player player) {
        clearExpiredPieces(player);
        return Integer.bitCount(player.getData(ModAttachments.EYE_OF_HORUS_PIECE_MASK));
    }

    private static void clearExpiredPieces(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        int pieces = Integer.bitCount(player.getData(ModAttachments.EYE_OF_HORUS_PIECE_MASK));
        long expireAt = player.getData(ModAttachments.EYE_OF_HORUS_PIECES_EXPIRE_AT);
        if (pieces > 0 && expireAt > 0L && player.level().getGameTime() >= expireAt) {
            player.setData(ModAttachments.EYE_OF_HORUS_PIECE_MASK, 0);
            player.setData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED, 0L);
            player.setData(ModAttachments.EYE_OF_HORUS_PIECES_EXPIRE_AT, 0L);
            player.setData(ModAttachments.EYE_OF_HORUS_ASSEMBLED, false);
        }
    }

    private static long getOrCreateFragmentSeed(ServerPlayer player) {
        long seed = player.getData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED);
        if (seed == 0L) {
            do {
                seed = player.getRandom().nextLong();
            } while (seed == 0L);
            player.setData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED, seed);
        }
        return seed;
    }

    private static int getPieceCount(long seed) {
        return RelicFragmenter.create(EYE_ID, seed).pieceCount();
    }

    public static int getCollectedPieceMask(Player player) {
        return player.getData(ModAttachments.EYE_OF_HORUS_PIECE_MASK);
    }

    public static long getFragmentSeed(Player player) {
        return player.getData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED);
    }

    public static RelicFragmenter.Layout createFragmentLayout(long seed) {
        return RelicFragmenter.create(EYE_ID, seed);
    }

    public static boolean isAssembled(Player player) {
        return player.getData(ModAttachments.EYE_OF_HORUS_ASSEMBLED);
    }

    public static boolean isAssemblyPending(Player player) {
        return hasCompleteEye(player) && !isAssembled(player);
    }

    public static void finishAssembly(ServerPlayer player, long seed) {
        if (isEquipped(player, ModItems.EYE_OF_HORUS.get())
                && seed == getFragmentSeed(player)
                && hasCompleteEye(player)) {
            player.setData(ModAttachments.EYE_OF_HORUS_ASSEMBLED, true);
            LivingEntity target = getLookTarget(player);
            if (target != null && !target.hasEffect(ModEffects.JUDGEMENT)) {
                GAZE_STATES.computeIfAbsent(player, ignored -> new HashMap<>())
                        .putIfAbsent(target, 1.0F / getFocusDuration(player));
                syncGlowProgress(Set.of(target));
            }
        }
    }

    private static boolean hasCompleteEye(Player player) {
        int mask = player.getData(ModAttachments.EYE_OF_HORUS_PIECE_MASK);
        long seed = player.getData(ModAttachments.EYE_OF_HORUS_FRAGMENT_SEED);
        return seed != 0L && Integer.bitCount(mask) >= getPieceCount(seed);
    }

    private static void clearGaze(Player player) {
        Map<LivingEntity, Float> removedStates = GAZE_STATES.remove(player);
        if (player.level() instanceof ServerLevel && removedStates != null) {
            syncGlowProgress(new HashSet<>(removedStates.keySet()));
        }
    }

    private static void syncGlowProgress(Set<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            float progress = GAZE_STATES.values().stream()
                    .map(states -> states.getOrDefault(target, 0.0F))
                    .max(Float::compareTo)
                    .orElse(0.0F);
            Float syncedProgress = target.getExistingDataOrNull(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS);
            if (progress <= 0.0F) {
                if (syncedProgress != null) {
                    target.removeData(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS);
                }
            } else if (syncedProgress == null || Float.compare(syncedProgress, progress) != 0) {
                target.setData(ModAttachments.EYE_OF_HORUS_GLOW_PROGRESS, progress);
            }
        }
    }
}
