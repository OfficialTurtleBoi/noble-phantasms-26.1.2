package net.turtleboi.noblephantasms.entity.custom;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.MythicalReliquaryItem;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;
import net.turtleboi.noblephantasms.network.RelicFragmentRevealPayload;
import net.turtleboi.noblephantasms.relic.RelicFragmentArchive;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public final class RelicFragmentEntity extends ItemEntity {
    private static final int ABSORB_DURATION_TICKS = 14;
    private static final EntityDataAccessor<Float> ABSORB_PROGRESS = SynchedEntityData.defineId(
            RelicFragmentEntity.class, EntityDataSerializers.FLOAT);
    private UUID absorbingPlayer;
    private int absorbTicks;

    public RelicFragmentEntity(EntityType<? extends RelicFragmentEntity> type, Level level) {
        super(type, level);
    }

    public RelicFragmentEntity(ServerLevel level, double x, double y, double z, UUID owner,
                               RelicFragmentData fragment) {
        this(ModEntities.RELIC_FRAGMENT.get(), level);
        setPos(x, y, z);
        setItem(RelicFragmentItem.create(ModItems.RELIC_FRAGMENT.get(), fragment, 1));
        setTarget(owner);
        absorbingPlayer = owner;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setNeverPickUp();
        setUnlimitedLifetime();
    }

    public RelicFragmentData getFragment() {
        return getItem().get(ModDataComponents.RELIC_FRAGMENT.get());
    }

    public float getAbsorbProgress() {
        return entityData.get(ABSORB_PROGRESS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ABSORB_PROGRESS, 0.0F);
    }

    @Override
    public void tick() {
        if (absorbingPlayer == null || level().isClientSide()) {
            super.tick();
            return;
        }
        Player found = ((ServerLevel) level()).getPlayerByUUID(absorbingPlayer);
        if (!(found instanceof ServerPlayer player)
                || !player.isAlive() || player.level() != level()) {
            absorbingPlayer = null;
            absorbTicks = 0;
            entityData.set(ABSORB_PROGRESS, 0.0F);
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            super.tick();
            return;
        }
        setNoGravity(true);
        Vec3 destination = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.85));
        Vec3 movement = destination.subtract(position()).scale(0.38);
        setDeltaMovement(movement);
        super.tick();
        absorbTicks++;
        entityData.set(ABSORB_PROGRESS,
                Math.clamp(absorbTicks / (float) ABSORB_DURATION_TICKS, 0.0F, 1.0F));
        if (absorbTicks >= ABSORB_DURATION_TICKS) {
            finishAbsorption(player);
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide()
                || !(player instanceof ServerPlayer serverPlayer)
                || absorbingPlayer != null
                || getTarget() != null && !getTarget().equals(player.getUUID())) {
            return;
        }
        ItemStack book = MythicalReliquaryItem.findInInventory(serverPlayer);
        if (book.isEmpty()) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.reliquary_station.no_book"));
            return;
        }
        absorbingPlayer = serverPlayer.getUUID();
        absorbTicks = 0;
        entityData.set(ABSORB_PROGRESS, 0.0F);
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    private void finishAbsorption(ServerPlayer player) {
        RelicFragmentData fragment = getFragment();
        ItemStack book = MythicalReliquaryItem.findInInventory(player);
        if (fragment == null || book.isEmpty()) {
            absorbingPlayer = null;
            absorbTicks = 0;
            entityData.set(ABSORB_PROGRESS, 0.0F);
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        RelicFragmentArchive archive = book.getOrDefault(
                ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(), RelicFragmentArchive.EMPTY);
        RelicFragmentArchive updated = archive.store(fragment);
        if (updated == null) {
            absorbingPlayer = null;
            absorbTicks = 0;
            entityData.set(ABSORB_PROGRESS, 0.0F);
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            player.sendOverlayMessage(Component.translatable(
                    "message.noblephantasms.reliquary_station.archive_full"));
            return;
        }
        book.set(ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(), updated);
        player.getInventory().setChanged();
        PacketDistributor.sendToPlayer(player, new RelicFragmentRevealPayload(fragment));
        player.sendOverlayMessage(Component.translatable(
                "message.noblephantasms.reliquary_station.discovered", getItem().getHoverName()));
        level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.85F, 1.45F);
        discard();
    }
}
