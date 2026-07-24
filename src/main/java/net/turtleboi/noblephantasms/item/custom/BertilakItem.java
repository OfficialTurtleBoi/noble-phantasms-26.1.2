package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.AABB;
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
    private static final float DAMAGE_MULTIPLIER = 1.5F;
    private static final float EXECUTION_THRESHOLD = 0.2F;
    private static final float RESISTANT_EXECUTION_THRESHOLD = 0.05F;
    private static final Map<UUID, UUID> PENDING_EXECUTIONS = new HashMap<>();

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

        Mob target = findLookTarget(player);
        if (target == null || !bindCovenant(player, target)) {
            return false;
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
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
        player.addEffect(new MobEffectInstance(ModEffects.COVENANT, (int) COVENANT_DURATION, 0, false, true, false));
        target.addEffect(new MobEffectInstance(ModEffects.COVENANT, (int) COVENANT_DURATION, 0, false, true, false));
        player.sendOverlayMessage(Component.translatable(
                "message.noblephantasms.bertilak.bound", target.getDisplayName()));
        return true;
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
        UUID playerId = PENDING_EXECUTIONS.remove(target.getUUID());
        if (playerId == null || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !player.getUUID().equals(playerId)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        BertilakCovenantState covenant = getActiveCovenant(player);
        if (!covenant.targets(target.getUUID())
                || !player.hasEffect(ModEffects.COVENANT)
                || !target.hasEffect(ModEffects.COVENANT)) {
            return;
        }

        fulfillCovenant(player, gameTime);
        if (!hasTag(target, ModTags.EntityTypes.BERTILAK_TROPHY_EXCLUDED)
                && target.level() instanceof ServerLevel serverLevel) {
            target.spawnAtLocation(serverLevel, TrophyHeadItem.create(target));
        }
        player.sendOverlayMessage(Component.translatable(
                "message.noblephantasms.bertilak.fulfilled", target.getDisplayName()));
    }

    private static void clearCovenant(ServerPlayer player, BertilakCovenantState covenant, boolean removeTargetEffect) {
        player.setData(ModAttachments.BERTILAK_COVENANT, covenant.clearTarget());
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
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
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

    private static Mob findLookTarget(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F).scale(COVENANT_RANGE);
        Vec3 endPosition = eyePosition.add(viewVector);
        HitResult blockHitResult = player.level().clipIncludingBorder(new ClipContext(
                eyePosition, endPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            endPosition = blockHitResult.getLocation();
        }

        AABB searchArea = player.getBoundingBox().expandTowards(viewVector).inflate(1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(player.level(), player, eyePosition,
                endPosition, searchArea, entity -> entity instanceof Mob mob && isValidTarget(player, mob), 0.0F);
        return entityHitResult != null ? (Mob) entityHitResult.getEntity() : null;
    }

    private static boolean isValidTarget(Player player, Mob target) {
        return target.isAlive()
                && !target.isSpectator()
                && target.isAttackable()
                && !player.isAlliedTo(target);
    }
}
