package net.turtleboi.noblephantasms.item.custom;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.attachment.TecpatlDeploymentState;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.TecpatlShardEntity;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class TecpatlOfTheFifthSunItem extends Item {
    public static final int SHARD_COUNT = 10;
    public static final long SHATTER_SEED = 0x5EC0A71F5EEDL;
    public static final float FULL_ATTACK_DAMAGE = 7.0F;
    public static final float ATTACK_DAMAGE_PER_SHARD = FULL_ATTACK_DAMAGE / SHARD_COUNT;
    public static final float SHARD_DAMAGE = ATTACK_DAMAGE_PER_SHARD * 2.0F;
    private static final int COMPLETE_SHARD_MASK = (1 << SHARD_COUNT) - 1;
    private static final int SHARD_INTERVAL_TICKS = 4;
    private static final int RECALL_STAGGER_TICKS = 2;
    private static final int RECALL_COOLDOWN_TICKS = 6000;
    private static final int RECOVERY_TIMEOUT_TICKS = 20 * 15;
    private static final double HORIZONTAL_INACCURACY = 0.045;
    private static final double VERTICAL_INACCURACY = 0.03;
    private static final double SHOTGUN_HORIZONTAL_INACCURACY = 0.18;
    private static final double SHOTGUN_VERTICAL_INACCURACY = 0.12;
    private static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "tecpatl_of_the_fifth_sun");

    public TecpatlOfTheFifthSunItem(Properties properties) {
        super(properties.sword(ToolMaterial.NETHERITE, 2.0F, -1.6F)
                .rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean shouldCauseReequipAnimation(
            ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        UUID batch = stack.get(ModDataComponents.TECPATL_DEPLOYMENT.get());
        int launchedShards = stack.getOrDefault(
                ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), 0);
        if (batch != null && launchedShards == COMPLETE_SHARD_MASK) {
            return InteractionResult.FAIL;
        }
        boolean shotgun = player.isShiftKeyDown();
        if (!shotgun) {
            player.startUsingItem(hand);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return shotgun ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (batch == null) {
            batch = UUID.randomUUID();
            beginDeployment(player, stack, hand, batch, serverLevel.getGameTime());
            stack.set(ModDataComponents.TECPATL_DEPLOYMENT.get(), batch);
            stack.set(ModDataComponents.TECPATL_RETURNED_SHARDS.get(), COMPLETE_SHARD_MASK);
            stack.set(ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), 0);
        } else {
            interruptReconstruction(serverLevel, player, stack, hand, batch);
        }
        if (shotgun) {
            launchShotgun(serverLevel, player, stack, hand);
            return InteractionResult.SUCCESS_SERVER;
        }
        launchNextShard(serverLevel, player, stack, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity,
                          ItemStack itemStack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }

        int timeHeld = getUseDuration(itemStack, entity) - remainingUseDuration;
        if (timeHeld <= 0 || timeHeld % SHARD_INTERVAL_TICKS != 0) {
            return;
        }
        launchNextShard(serverLevel, player, itemStack, player.getUsedItemHand());
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level,
                                LivingEntity entity, int remainingUseDuration) {
        return level instanceof ServerLevel serverLevel && entity instanceof Player player
                && finishShooting(serverLevel, player, itemStack);
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 72000;
    }

    private static void launchNextShard(ServerLevel level, Player player,
                                        ItemStack itemStack, InteractionHand hand) {
        launchNextShard(level, player, itemStack, hand,
                HORIZONTAL_INACCURACY, VERTICAL_INACCURACY, true);
    }

    private static void launchShotgun(ServerLevel level, Player player,
                                      ItemStack itemStack, InteractionHand hand) {
        int launchedShards = itemStack.getOrDefault(
                ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), 0);
        int shardsToLaunch = SHARD_COUNT
                - Integer.bitCount(launchedShards & COMPLETE_SHARD_MASK);
        for (int shard = 0; shard < shardsToLaunch; shard++) {
            launchNextShard(level, player, itemStack, hand,
                    SHOTGUN_HORIZONTAL_INACCURACY,
                    SHOTGUN_VERTICAL_INACCURACY, false);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1.25F, 0.7F);
    }

    private static void launchNextShard(ServerLevel level, Player player,
                                        ItemStack itemStack, InteractionHand hand,
                                        double horizontalInaccuracy,
                                        double verticalInaccuracy, boolean playSound) {
        UUID batch = itemStack.get(ModDataComponents.TECPATL_DEPLOYMENT.get());
        if (batch == null) {
            player.stopUsingItem();
            return;
        }

        int launchedShards = itemStack.getOrDefault(
                ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), 0);
        int pieceIndex = getNextPieceIndex(launchedShards);
        if (pieceIndex < 0) {
            player.stopUsingItem();
            return;
        }

        int shardBit = 1 << pieceIndex;
        ItemStack dagger = itemStack.copyWithCount(1);
        finishRebuilding(dagger);
        Vec3 look = player.getLookAngle();
        Vec3 spread = look.add(
                player.getRandom().triangle(0.0, horizontalInaccuracy),
                player.getRandom().triangle(0.0, verticalInaccuracy),
                player.getRandom().triangle(0.0, horizontalInaccuracy)).normalize();
        TecpatlShardEntity shard = new TecpatlShardEntity(
                level, player, batch, pieceIndex, SHARD_DAMAGE, dagger, hand);
        shard.setPos(player.getX() + spread.x * 0.8,
                player.getEyeY() - 0.2 + spread.y * 0.5,
                player.getZ() + spread.z * 0.8);
        shard.shoot(spread.x, spread.y, spread.z,
                1.55F + player.getRandom().nextFloat() * 0.25F, 0.0F);
        level.addFreshEntity(shard);
        refreshDeployment(player, batch, level.getGameTime());

        launchedShards |= shardBit;
        int attachedShards = itemStack.getOrDefault(
                ModDataComponents.TECPATL_RETURNED_SHARDS.get(), COMPLETE_SHARD_MASK) & ~shardBit;
        itemStack.set(ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), launchedShards);
        itemStack.set(ModDataComponents.TECPATL_RETURNED_SHARDS.get(), attachedShards);
        updateAttackDamage(itemStack, attachedShards);
        if (playSound) {
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 0.45F,
                    0.75F + Integer.bitCount(launchedShards) * 0.045F);
        }

        if (launchedShards == COMPLETE_SHARD_MASK) {
            finishShooting(level, player, itemStack);
            if (attachedShards == 0) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            player.stopUsingItem();
        }
    }

    private static boolean finishShooting(ServerLevel level, Player player,
                                          ItemStack itemStack) {
        UUID batch = itemStack.get(ModDataComponents.TECPATL_DEPLOYMENT.get());
        int launchedShards = itemStack.getOrDefault(
                ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), 0);
        int latestPiece = getLatestPieceIndex(launchedShards);
        if (batch == null || latestPiece < 0) {
            return false;
        }

        for (TecpatlShardEntity shard : level.getEntitiesOfClass(TecpatlShardEntity.class,
                player.getBoundingBox().inflate(256.0),
                shard -> shard.getOwner() == player
                        && batch.equals(shard.getBatchId())
                        && shard.getPieceIndex() == latestPiece)) {
            player.getCooldowns().addCooldown(itemStack, RECALL_COOLDOWN_TICKS);
            refreshDeployment(player, batch, level.getGameTime());
            shard.markLastShard();
            return true;
        }
        return false;
    }

    private static int getNextPieceIndex(int launchedShards) {
        int nextPiece = -1;
        double bestOrder = Double.MAX_VALUE;
        for (int index = 0; index < SHARD_COUNT; index++) {
            if ((launchedShards & 1 << index) != 0) {
                continue;
            }
            double order = getPieceBreakOrder(index);
            if (order < bestOrder) {
                bestOrder = order;
                nextPiece = index;
            }
        }
        return nextPiece;
    }

    private static int getLatestPieceIndex(int launchedShards) {
        int latestPiece = -1;
        double latestOrder = -Double.MAX_VALUE;
        for (int index = 0; index < SHARD_COUNT; index++) {
            if ((launchedShards & 1 << index) == 0) {
                continue;
            }
            double order = getPieceBreakOrder(index);
            if (order > latestOrder || order == latestOrder && index > latestPiece) {
                latestOrder = order;
                latestPiece = index;
            }
        }
        return latestPiece;
    }

    public static double getPieceBreakOrder(int pieceIndex) {
        RelicFragmenter.Layout layout = RelicFragmenter.createExact(
                ITEM_ID, SHATTER_SEED, SHARD_COUNT);
        if (layout == null || pieceIndex < 0 || pieceIndex >= layout.pieceCount()) {
            return pieceIndex / (double) SHARD_COUNT;
        }
        RelicFragmenter.Piece piece = layout.pieces().get(pieceIndex);
        double centerX = piece.pixels().stream().mapToInt(RelicFragmenter.Pixel::x)
                .average().orElse(0.0) / layout.width();
        double centerY = piece.pixels().stream().mapToInt(RelicFragmenter.Pixel::y)
                .average().orElse(0.0) / layout.height();
        return centerY - centerX;
    }

    public static boolean recallShards(ServerLevel level, Player player, UUID batch) {
        List<TecpatlShardEntity> shards = level.getEntitiesOfClass(
                TecpatlShardEntity.class,
                player.getBoundingBox().inflate(256.0),
                shard -> shard.getOwner() == player
                        && batch.equals(shard.getBatchId()));
        if (shards.isEmpty()) {
            return false;
        }

        Comparator<TecpatlShardEntity> returnOrder = Comparator
                .comparingDouble((TecpatlShardEntity shard) ->
                        getPieceBreakOrder(shard.getPieceIndex()))
                .thenComparingInt(TecpatlShardEntity::getPieceIndex)
                .reversed();
        shards.sort(returnOrder);
        for (int index = 0; index < shards.size(); index++) {
            shards.get(index).beginRecall(index * RECALL_STAGGER_TICKS);
        }
        player.stopUsingItem();
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                player.getSoundSource(), 0.65F, 1.25F);
        return true;
    }

    private static void interruptReconstruction(ServerLevel level, Player player,
                                                ItemStack itemStack, InteractionHand hand,
                                                UUID batch) {
        for (TecpatlShardEntity shard : level.getEntitiesOfClass(TecpatlShardEntity.class,
                player.getBoundingBox().inflate(256.0),
                shard -> batch.equals(shard.getBatchId()))) {
            shard.dropAndWait();
        }

        int launchedShards = itemStack.getOrDefault(
                ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), 0);
        int attachedShards = itemStack.getOrDefault(
                ModDataComponents.TECPATL_RETURNED_SHARDS.get(), COMPLETE_SHARD_MASK);
        int reconstructedShards = launchedShards & attachedShards;
        if (reconstructedShards == 0) {
            return;
        }

        ItemStack dagger = itemStack.copyWithCount(1);
        finishRebuilding(dagger);
        for (int pieceIndex = 0; pieceIndex < SHARD_COUNT; pieceIndex++) {
            if ((reconstructedShards & 1 << pieceIndex) == 0) {
                continue;
            }
            double angle = pieceIndex * 2.399963229728653;
            TecpatlShardEntity shard = new TecpatlShardEntity(
                    level, player, batch, pieceIndex, SHARD_DAMAGE, dagger, hand);
            shard.setPos(player.getX() + Math.cos(angle) * 0.3,
                    player.getEyeY() - 0.35,
                    player.getZ() + Math.sin(angle) * 0.3);
            shard.beginReconstructionWait();
            level.addFreshEntity(shard);
        }
        attachedShards &= ~reconstructedShards;
        itemStack.set(ModDataComponents.TECPATL_RETURNED_SHARDS.get(), attachedShards);
        updateAttackDamage(itemStack, attachedShards);
    }

    public static boolean returnShard(Player player, UUID batch, int pieceIndex,
                                      InteractionHand hand, ItemStack original) {
        int shard = 1 << Math.clamp(pieceIndex, 0, SHARD_COUNT - 1);
        ItemStack rebuilding = findRebuildingDagger(player, batch);
        int returnedShards;
        if (rebuilding.isEmpty()) {
            rebuilding = createRebuildingDagger(original, batch, shard);
            returnedShards = shard;
            giveDagger(player, hand, rebuilding);
        } else {
            returnedShards = rebuilding.getOrDefault(
                    ModDataComponents.TECPATL_RETURNED_SHARDS.get(), 0) | shard;
            rebuilding.set(ModDataComponents.TECPATL_RETURNED_SHARDS.get(), returnedShards);
            updateAttackDamage(rebuilding, returnedShards);
        }

        if (returnedShards == COMPLETE_SHARD_MASK) {
            clearDeployment(player, batch);
            rebuilding = findRebuildingDagger(player, batch);
            if (!rebuilding.isEmpty()) {
                finishRebuilding(rebuilding);
                player.getCooldowns().removeCooldown(
                        player.getCooldowns().getCooldownGroup(rebuilding));
            }
        } else {
            refreshDeployment(player, batch, player.level().getGameTime());
        }
        player.getInventory().setChanged();
        return returnedShards == COMPLETE_SHARD_MASK;
    }

    public static boolean recoverDagger(Player player, UUID batch, InteractionHand hand,
                                        ItemStack original, boolean createIfMissing) {
        ItemStack rebuilding = findRebuildingDagger(player, batch);
        if (rebuilding.isEmpty()) {
            if (!createIfMissing) {
                return false;
            }
            rebuilding = original.isEmpty()
                    ? new ItemStack(ModItems.TECPATL_OF_THE_FIFTH_SUN.get())
                    : original.copyWithCount(1);
            finishRebuilding(rebuilding);
            giveDagger(player, hand, rebuilding);
        } else {
            finishRebuilding(rebuilding);
        }
        player.getCooldowns().removeCooldown(
                player.getCooldowns().getCooldownGroup(rebuilding));
        clearDeployment(player, batch);
        player.getInventory().setChanged();
        return true;
    }

    public static void handlePlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.tickCount % 20 != 0) {
            return;
        }

        TecpatlDeploymentState state = player.getData(ModAttachments.TECPATL_DEPLOYMENT);
        if (state.batchId().isEmpty()) {
            recoverOrphanedDaggers(serverPlayer);
            return;
        }
        if (player.level().getGameTime() < state.recoverAt()) {
            return;
        }
        recoverDeployment(serverPlayer, state);
    }

    public static void handlePlayerDeath(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        TecpatlDeploymentState state = player.getData(ModAttachments.TECPATL_DEPLOYMENT);
        if (state.batchId().isPresent()) {
            recoverDeployment(serverPlayer, state);
        } else {
            recoverOrphanedDaggers(serverPlayer);
        }
    }

    public static boolean isDeploymentActive(Player player, UUID batch) {
        return player.getData(ModAttachments.TECPATL_DEPLOYMENT).matches(batch);
    }

    private static void beginDeployment(Player player, ItemStack dagger, InteractionHand hand,
                                        UUID batch, long gameTime) {
        ItemStack backup = dagger.copyWithCount(1);
        finishRebuilding(backup);
        player.setData(ModAttachments.TECPATL_DEPLOYMENT, new TecpatlDeploymentState(
                Optional.of(batch), backup, hand == InteractionHand.MAIN_HAND,
                gameTime + RECOVERY_TIMEOUT_TICKS));
    }

    private static void refreshDeployment(Player player, UUID batch, long gameTime) {
        TecpatlDeploymentState state = player.getData(ModAttachments.TECPATL_DEPLOYMENT);
        if (state.matches(batch)) {
            player.setData(ModAttachments.TECPATL_DEPLOYMENT,
                    state.refresh(gameTime + RECOVERY_TIMEOUT_TICKS));
        }
    }

    private static void clearDeployment(Player player, UUID batch) {
        if (player.getData(ModAttachments.TECPATL_DEPLOYMENT).matches(batch)) {
            player.setData(ModAttachments.TECPATL_DEPLOYMENT, TecpatlDeploymentState.EMPTY);
        }
    }

    private static void recoverDeployment(ServerPlayer player, TecpatlDeploymentState state) {
        UUID batch = state.batchId().orElse(null);
        if (batch == null) {
            return;
        }

        InteractionHand hand = state.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        recoverDagger(player, batch, hand, state.dagger(), true);
        discardDeploymentShards(player, batch);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                player.getSoundSource(), 0.8F, 0.75F);
    }

    private static void recoverOrphanedDaggers(ServerPlayer player) {
        boolean recovered = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            UUID batch = stack.get(ModDataComponents.TECPATL_DEPLOYMENT.get());
            if (stack.is(ModItems.TECPATL_OF_THE_FIFTH_SUN.get()) && batch != null) {
                finishRebuilding(stack);
                discardDeploymentShards(player, batch);
                recovered = true;
            }
        }
        if (recovered) {
            player.getInventory().setChanged();
        }
        ItemStack dagger = new ItemStack(ModItems.TECPATL_OF_THE_FIFTH_SUN.get());
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(dagger));
    }

    private static void discardDeploymentShards(ServerPlayer player, UUID batch) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (TecpatlShardEntity shard : level.getEntities(ModEntities.TECPATL_SHARD.get(),
                    shard -> batch.equals(shard.getBatchId()))) {
                shard.discard();
            }
        }
    }

    private static ItemStack createRebuildingDagger(ItemStack original, UUID batch, int returnedShards) {
        ItemStack rebuilding = original.copyWithCount(1);
        rebuilding.set(ModDataComponents.TECPATL_DEPLOYMENT.get(), batch);
        rebuilding.set(ModDataComponents.TECPATL_RETURNED_SHARDS.get(), returnedShards);
        rebuilding.set(ModDataComponents.TECPATL_LAUNCHED_SHARDS.get(), COMPLETE_SHARD_MASK);
        updateAttackDamage(rebuilding, returnedShards);
        return rebuilding;
    }

    private static ItemStack findRebuildingDagger(Player player, UUID batch) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (batch.equals(stack.get(ModDataComponents.TECPATL_DEPLOYMENT.get()))) {
                return stack;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return batch.equals(offhand.get(ModDataComponents.TECPATL_DEPLOYMENT.get()))
                ? offhand : ItemStack.EMPTY;
    }

    private static void giveDagger(Player player, InteractionHand hand, ItemStack dagger) {
        ItemStack displaced = player.getItemInHand(hand);
        player.setItemInHand(hand, dagger);
        if (!displaced.isEmpty() && !player.getInventory().add(displaced)) {
            player.drop(displaced, false);
        }
    }

    private static void finishRebuilding(ItemStack stack) {
        stack.remove(ModDataComponents.TECPATL_DEPLOYMENT.get());
        stack.remove(ModDataComponents.TECPATL_RETURNED_SHARDS.get());
        stack.remove(ModDataComponents.TECPATL_LAUNCHED_SHARDS.get());
        updateAttackDamage(stack, COMPLETE_SHARD_MASK);
    }

    private static void updateAttackDamage(ItemStack stack, int attachedShards) {
        double itemAttackDamage = Integer.bitCount(attachedShards & COMPLETE_SHARD_MASK)
                * ATTACK_DAMAGE_PER_SHARD - 1.0;
        ItemAttributeModifiers attributes = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes.withModifierAdded(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, itemAttackDamage,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND));
    }
}
