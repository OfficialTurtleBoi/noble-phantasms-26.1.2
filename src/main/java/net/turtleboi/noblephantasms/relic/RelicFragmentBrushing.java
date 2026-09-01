package net.turtleboi.noblephantasms.relic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.custom.RelicFragmentEntity;
import net.turtleboi.noblephantasms.item.custom.MythicalReliquaryItem;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;

public final class RelicFragmentBrushing {
    private static final int BRUSH_DURATION_TICKS = 32;
    private static final double BRUSH_REACH = 5.0;
    private static final double TARGET_WIDTH = 0.7;
    private static final double TARGET_HEIGHT = 0.65;
    private static final Map<UUID, BrushProgress> PROGRESS = new HashMap<>();

    private RelicFragmentBrushing() {
    }

    public static boolean startServer(ServerPlayer player, InteractionHand brushHand, int targetId) {
        ItemStack brush = player.getItemInHand(brushHand);
        if (!(brush.getItem() instanceof BrushItem)) {
            return false;
        }
        Entity entity = player.level().getEntity(targetId);
        if (!(entity instanceof ItemEntity target)
                || target instanceof RelicFragmentEntity
                || !isUnidentified(target.getItem())
                || findTarget(player) != target) {
            return false;
        }
        player.startUsingItem(brushHand);
        ItemStack book = MythicalReliquaryItem.findInInventory(player);
        if (book.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.reliquary_station.no_book"));
            player.stopUsingItem();
            return true;
        }
        if (hasPendingFragment(player)) {
            player.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.reliquary_station.collect_fragment"));
            player.stopUsingItem();
            return true;
        }
        if (PROGRESS.values().stream().anyMatch(progress -> progress.target() == target)) {
            player.stopUsingItem();
            return true;
        }
        clear(player.getUUID());
        target.setNeverPickUp();
        target.setDeltaMovement(Vec3.ZERO);
        PROGRESS.put(player.getUUID(), new BrushProgress(target, brushHand,
                player.level().getGameTime()));
        emitEffects((ServerLevel) player.level(), target.position(), false);
        return true;
    }

    public static void handlePlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BrushProgress progress = PROGRESS.get(serverPlayer.getUUID());
        if (progress == null) {
            return;
        }
        ItemEntity target = progress.target();
        if (!validProgress(serverPlayer, progress)) {
            cancel(serverPlayer.getUUID(), false);
            return;
        }
        long elapsed = serverPlayer.level().getGameTime() - progress.started();
        target.setNeverPickUp();
        target.setDeltaMovement(Vec3.ZERO);
        if (elapsed > 0L && elapsed % 6L == 0L) {
            emitEffects((ServerLevel) serverPlayer.level(), target.position(), false);
        }
        if (elapsed >= BRUSH_DURATION_TICKS) {
            complete(serverPlayer, progress);
        }
    }

    public static void clear(UUID playerId) {
        cancel(playerId, false);
    }

    public static boolean blocksPickup(Player player, ItemStack stack) {
        return isUnidentified(stack)
                && (player.getMainHandItem().getItem() instanceof BrushItem
                || player.getOffhandItem().getItem() instanceof BrushItem);
    }

    private static boolean validProgress(ServerPlayer player, BrushProgress progress) {
        ItemEntity target = progress.target();
        return target.isAlive()
                && target.level() == player.level()
                && isUnidentified(target.getItem())
                && player.isUsingItem()
                && player.getUsedItemHand() == progress.brushHand()
                && player.getItemInHand(progress.brushHand()).getItem() instanceof BrushItem
                && player.distanceToSqr(target) <= BRUSH_REACH * BRUSH_REACH;
    }

    private static void complete(ServerPlayer player, BrushProgress progress) {
        ItemEntity target = progress.target();
        ItemStack sourceStack = target.getItem();
        RelicFragmentItem source = (RelicFragmentItem) sourceStack.getItem();
        ItemStack book = MythicalReliquaryItem.findInInventory(player);
        if (book.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.reliquary_station.no_book"));
            cancel(player.getUUID(), true);
            return;
        }
        RelicFragmentArchive archive = book.getOrDefault(
                ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(), RelicFragmentArchive.EMPTY);
        RelicFragmentArchive.Reveal reveal = archive.reveal(source.origin(), player.getRandom());
        if (reveal == null) {
            player.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.reliquary_station.archive_full"));
            cancel(player.getUUID(), true);
            return;
        }
        PROGRESS.remove(player.getUUID());
        Vec3 position = target.position();
        sourceStack.shrink(1);
        if (sourceStack.isEmpty()) {
            target.discard();
        } else {
            target.setItem(sourceStack);
            target.setPickUpDelay(10);
        }
        ItemStack brush = player.getItemInHand(progress.brushHand());
        ServerLevel level = (ServerLevel) player.level();
        brush.hurtAndBreak(1, level, player,
                ignored -> player.setItemInHand(progress.brushHand(), ItemStack.EMPTY));
        player.stopUsingItem();
        player.getInventory().setChanged();
        emitEffects(level, position, true);
        RelicFragmentEntity fragment = new RelicFragmentEntity(level,
                position.x, position.y, position.z, player.getUUID(), reveal.fragment());
        level.addFreshEntity(fragment);
    }

    private static void cancel(UUID playerId, boolean stopUsing) {
        BrushProgress progress = PROGRESS.remove(playerId);
        if (progress == null) {
            return;
        }
        ItemEntity target = progress.target();
        if (target.isAlive()) {
            target.setPickUpDelay(10);
        }
        if (stopUsing && target.level() instanceof ServerLevel level) {
            Player found = level.getPlayerByUUID(playerId);
            if (found instanceof ServerPlayer player) {
                player.stopUsingItem();
            }
        }
    }

    private static boolean hasPendingFragment(ServerPlayer player) {
        for (Entity entity : ((ServerLevel) player.level()).getAllEntities()) {
            if (entity instanceof RelicFragmentEntity fragment
                    && fragment.isAlive()
                    && player.getUUID().equals(fragment.getTarget())) {
                return true;
            }
        }
        return false;
    }

    public static ItemEntity findTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = start.add(view.scale(BRUSH_REACH));
        HitResult blockHit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double maximumDistance = blockHit.getType() == HitResult.Type.MISS
                ? BRUSH_REACH * BRUSH_REACH : start.distanceToSqr(blockHit.getLocation());
        AABB search = player.getBoundingBox().expandTowards(view.scale(BRUSH_REACH)).inflate(1.0);
        ItemEntity closest = null;
        double closestDistance = maximumDistance;
        for (ItemEntity candidate : player.level().getEntitiesOfClass(ItemEntity.class, search,
                entity -> !(entity instanceof RelicFragmentEntity)
                        && isUnidentified(entity.getItem()))) {
            Optional<Vec3> hit = brushingBounds(candidate).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance <= closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static AABB brushingBounds(ItemEntity entity) {
        double halfWidth = TARGET_WIDTH * 0.5;
        return new AABB(entity.getX() - halfWidth, entity.getY(), entity.getZ() - halfWidth,
                entity.getX() + halfWidth, entity.getY() + TARGET_HEIGHT,
                entity.getZ() + halfWidth);
    }

    private static boolean isUnidentified(ItemStack stack) {
        return stack.getItem() instanceof RelicFragmentItem fragment && fragment.isUnidentified();
    }

    private static void emitEffects(ServerLevel level, Vec3 origin, boolean complete) {
        level.sendParticles(ParticleTypes.DUST_PLUME, origin.x, origin.y + 0.15, origin.z,
                complete ? 28 : 7, 0.22, 0.12, 0.22, complete ? 0.065 : 0.025);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BRUSH_GENERIC,
                SoundSource.PLAYERS, complete ? 1.0F : 0.65F, complete ? 1.3F : 0.95F);
    }

    private record BrushProgress(ItemEntity target, InteractionHand brushHand, long started) {
    }
}
