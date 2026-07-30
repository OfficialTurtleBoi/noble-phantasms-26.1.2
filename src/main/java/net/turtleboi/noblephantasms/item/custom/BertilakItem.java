package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.attachment.BertilakCovenantState;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;
import net.turtleboi.noblephantasms.tags.ModTags;

public class BertilakItem extends AxeItem {
    private static final double COVENANT_RANGE = 16.0;
    private static final long COVENANT_DURATION = 20L * 60L * 5L;
    private static final long BIND_COOLDOWN = 20L * 60L;
    private static final int COVENANT_CHARGE_TICKS = 40;
    private static final float DAMAGE_MULTIPLIER = 1.5F;
    private static final float EXECUTION_THRESHOLD = 0.2F;
    private static final float RESISTANT_EXECUTION_THRESHOLD = 0.05F;
    private static final Map<UUID, UUID> PENDING_EXECUTIONS = new HashMap<>();
    private static final Map<UUID, TrophySupport> TROPHY_SUPPORT = new HashMap<>();

    public BertilakItem(Properties properties) {
        super(ToolMaterial.NETHERITE, 7.0F, -2.8F,
                properties.rarity(ModRarities.LEGENDARY.getValue()).fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                breakCovenant(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        int chargeTicks = getUseDuration(itemStack, entity) - remainingTime;
        if (chargeTicks < COVENANT_CHARGE_TICKS) {
            return false;
        }

        Mob target = findLookTarget(player);
        if (target == null || !bindCovenant(player, target)) {
            return false;
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int ticksRemaining) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        int chargeTicks = getUseDuration(itemStack, entity) - ticksRemaining;
        if (chargeTicks != COVENANT_CHARGE_TICKS) {
            return;
        }

        Mob target = findLookTarget(player);
        if (target != null && canBindCovenant(player, target)) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.SPEAR;
    }

    private static boolean bindCovenant(ServerPlayer player, Mob target) {
        long gameTime = player.level().getGameTime();
        BertilakCovenantState covenant = getActiveCovenant(player);
        if (covenant.targetId().isPresent()) {
            player.sendOverlayMessage(Component.translatable("message.noblephantasms.bertilak.already_bound"));
            return false;
        }
        if (target.hasEffect(ModEffects.COVENANT)) {
            player.sendOverlayMessage(Component.translatable("message.noblephantasms.bertilak.target_bound"));
            return false;
        }

        long remaining = covenant.nextBindAt() - gameTime;
        if (remaining > 0L) {
            player.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.bertilak.cooldown", (remaining + 19L) / 20L));
            return false;
        }

        player.setData(ModAttachments.BERTILAK_COVENANT,
                new BertilakCovenantState(Optional.of(target.getUUID()), gameTime + BIND_COOLDOWN));
        player.addEffect(new MobEffectInstance(ModEffects.COVENANT, (int) COVENANT_DURATION, 0, false, true, true));
        target.addEffect(new MobEffectInstance(ModEffects.COVENANT, (int) COVENANT_DURATION, 0, false, true, false));
        player.sendOverlayMessage(Component.translatable(
                "message.noblephantasms.bertilak.bound", target.getDisplayName()));
        return true;
    }

    private static boolean canBindCovenant(ServerPlayer player, Mob target) {
        BertilakCovenantState covenant = getActiveCovenant(player);
        return covenant.targetId().isEmpty()
                && !target.hasEffect(ModEffects.COVENANT)
                && covenant.nextBindAt() <= player.level().getGameTime();
    }

    public static void reportTrophySupport(ServerPlayer player, UUID targetId, boolean supported) {
        LivingEntity target = findEntity(player, targetId);
        double reportRange = COVENANT_RANGE + 2.0;
        if (!(target instanceof Mob)
                || !target.isAlive()
                || target.level() != player.level()
                || player.distanceToSqr(target) > reportRange * reportRange
                || !player.hasLineOfSight(target)) {
            return;
        }
        TROPHY_SUPPORT.put(player.getUUID(), new TrophySupport(targetId, supported));
    }

    private static void breakCovenant(ServerPlayer player) {
        BertilakCovenantState covenant = getActiveCovenant(player);
        if (covenant.targetId().isEmpty()) {
            return;
        }
        clearCovenant(player, covenant, true);
        player.sendOverlayMessage(Component.translatable("message.noblephantasms.bertilak.broken"));
    }

    public static BertilakCovenantState getActiveCovenant(ServerPlayer player) {
        BertilakCovenantState covenant = player.getData(ModAttachments.BERTILAK_COVENANT);
        if (covenant.targetId().isEmpty()) {
            return covenant;
        }
        LivingEntity target = findBoundTarget(player, covenant);
        if (!player.hasEffect(ModEffects.COVENANT)
                || target == null
                || !target.hasEffect(ModEffects.COVENANT)) {
            clearCovenant(player, covenant, false);
            return covenant.clearTarget();
        }
        return covenant;
    }

    public static void fulfillCovenant(ServerPlayer player, long gameTime) {
        BertilakCovenantState covenant = player.getData(ModAttachments.BERTILAK_COVENANT);
        clearCovenant(player, covenant, true);
        player.setData(ModAttachments.BERTILAK_COVENANT, new BertilakCovenantState(Optional.empty(), gameTime));
    }

    public static void handleCovenantEffectRemoved(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            BertilakCovenantState covenant = player.getData(ModAttachments.BERTILAK_COVENANT);
            if (covenant.targetId().isEmpty()) {
                return;
            }

            player.setData(ModAttachments.BERTILAK_COVENANT, covenant.clearTarget());
            LivingEntity target = findBoundTarget(player, covenant);
            if (target != null) {
                target.removeEffect(ModEffects.COVENANT);
            }
            return;
        }

        if (entity.level().getServer() == null) {
            return;
        }

        for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
            BertilakCovenantState covenant = player.getData(ModAttachments.BERTILAK_COVENANT);
            if (!covenant.targets(entity.getUUID())) {
                continue;
            }

            player.setData(ModAttachments.BERTILAK_COVENANT, covenant.clearTarget());
            player.removeEffect(ModEffects.COVENANT);
            return;
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player
                || !player.getMainHandItem().is(ModItems.BERTILAK)) {
            return;
        }

        BertilakCovenantState covenant = getActiveCovenant(player);
        if (covenant.targetId().isEmpty()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!covenant.targets(target.getUUID())) {
            event.setCanceled(true);
            return;
        }
        if (!player.hasEffect(ModEffects.COVENANT) || !target.hasEffect(ModEffects.COVENANT)) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
        float threshold = isExecutionResistant(target) ? RESISTANT_EXECUTION_THRESHOLD : EXECUTION_THRESHOLD;
        if (target.getHealth() / target.getMaxHealth() < threshold) {
            PENDING_EXECUTIONS.put(target.getUUID(), player.getUUID());
        }
    }

    public static void handleDamageFinalized(LivingDamageEvent.Pre event) {
        if (isPendingExecution(event.getEntity(), event.getSource())) {
            event.setNewDamage(event.getEntity().getHealth() + event.getEntity().getAbsorptionAmount() + 1.0F);
        }
    }

    public static void handleDamageComplete(LivingDamageEvent.Post event) {
        if (event.getEntity().isAlive()) {
            PENDING_EXECUTIONS.remove(event.getEntity().getUUID());
        }
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        PENDING_EXECUTIONS.remove(target.getUUID());
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !player.getMainHandItem().is(ModItems.BERTILAK)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        BertilakCovenantState covenant = player.getData(ModAttachments.BERTILAK_COVENANT);
        if (!covenant.targets(target.getUUID())) {
            return;
        }

        boolean trophySupported = isTrophySupported(player, target);
        fulfillCovenant(player, gameTime);
        if (trophySupported && target.level() instanceof ServerLevel serverLevel) {
            target.spawnAtLocation(serverLevel, TrophyHeadItem.create(target));
        }
        player.sendOverlayMessage(Component.translatable(
                "message.noblephantasms.bertilak.fulfilled", target.getDisplayName()));
    }

    private static void clearCovenant(ServerPlayer player, BertilakCovenantState covenant, boolean removeTargetEffect) {
        player.setData(ModAttachments.BERTILAK_COVENANT, covenant.clearTarget());
        TROPHY_SUPPORT.remove(player.getUUID());
        if (removeTargetEffect) {
            LivingEntity target = findBoundTarget(player, covenant);
            if (target != null) {
                target.removeEffect(ModEffects.COVENANT);
            }
        }
        player.removeEffect(ModEffects.COVENANT);
    }

    private static LivingEntity findBoundTarget(ServerPlayer player, BertilakCovenantState covenant) {
        UUID targetId = covenant.targetId().orElse(null);
        if (targetId == null) {
            return null;
        }
        return findEntity(player, targetId);
    }

    private static LivingEntity findEntity(ServerPlayer player, UUID targetId) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }

    private static boolean isTrophySupported(ServerPlayer player, LivingEntity target) {
        TrophySupport support = TROPHY_SUPPORT.get(player.getUUID());
        return support == null
                || !support.targetId().equals(target.getUUID())
                || support.supported();
    }

    private static boolean isPendingExecution(LivingEntity target, DamageSource source) {
        Entity attacker = source.getEntity();
        UUID playerId = PENDING_EXECUTIONS.get(target.getUUID());
        if (!(attacker instanceof ServerPlayer player)
                || source.getDirectEntity() != player
                || playerId == null
                || !playerId.equals(player.getUUID())) {
            return false;
        }
        return player.getMainHandItem().is(ModItems.BERTILAK)
                && player.hasEffect(ModEffects.COVENANT)
                && target.hasEffect(ModEffects.COVENANT)
                && getActiveCovenant(player).targets(target.getUUID());
    }

    private static boolean isExecutionResistant(LivingEntity target) {
        return hasTag(target, ModTags.EntityTypes.BERTILAK_EXECUTION_RESISTANT);
    }

    private static boolean hasTag(LivingEntity target,
                                  net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tag) {
        return target.getType().getTags().anyMatch(tag::equals);
    }

    public static Mob findLookTarget(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F).scale(COVENANT_RANGE);
        Vec3 endPosition = clipSolidBlocks(player, eyePosition, eyePosition.add(viewVector));

        AABB searchArea = player.getBoundingBox().expandTowards(viewVector).inflate(1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(player.level(), player, eyePosition,
                endPosition, searchArea, entity -> entity instanceof Mob mob && isValidTarget(player, mob), 0.0F);
        return entityHitResult != null ? (Mob) entityHitResult.getEntity() : null;
    }

    @SuppressWarnings("deprecation")
    private static Vec3 clipSolidBlocks(Player player, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        double length = direction.length();
        if (length == 0.0) {
            return to;
        }

        Vec3 unitDirection = direction.scale(1.0 / length);
        Vec3 cursor = from;
        for (int skippedBlocks = 0; skippedBlocks < 64; skippedBlocks++) {
            BlockHitResult hit = player.level().clipIncludingBorder(new ClipContext(
                    cursor, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return to;
            }
            if (hit.isWorldBorderHit()) {
                return hit.getLocation();
            }

            BlockState state = player.level().getBlockState(hit.getBlockPos());
            if (state.isSolid()) {
                return hit.getLocation();
            }

            cursor = advancePastBlock(hit.getBlockPos(), hit.getLocation(), unitDirection);
            if (cursor.subtract(from).dot(direction) >= direction.lengthSqr()) {
                return to;
            }
        }
        return to;
    }

    private static Vec3 advancePastBlock(BlockPos pos, Vec3 hitLocation, Vec3 direction) {
        double xDistance = distanceToExit(pos.getX(), hitLocation.x, direction.x);
        double yDistance = distanceToExit(pos.getY(), hitLocation.y, direction.y);
        double zDistance = distanceToExit(pos.getZ(), hitLocation.z, direction.z);
        double exitDistance = Math.min(xDistance, Math.min(yDistance, zDistance));
        return hitLocation.add(direction.scale(exitDistance + 1.0E-5));
    }

    private static double distanceToExit(int blockCoordinate, double hitCoordinate, double direction) {
        if (direction > 0.0) {
            return Math.max(0.0, (blockCoordinate + 1.0 - hitCoordinate) / direction);
        }
        if (direction < 0.0) {
            return Math.max(0.0, (blockCoordinate - hitCoordinate) / direction);
        }
        return Double.POSITIVE_INFINITY;
    }

    private static boolean isValidTarget(Player player, Mob target) {
        return target.isAlive()
                && !target.isSpectator()
                && target.isAttackable()
                && !player.isAlliedTo(target);
    }

    private record TrophySupport(UUID targetId, boolean supported) {
    }
}
