package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.turtleboi.noblephantasms.attachment.ModAttachments;

public final class ClydnoHalterItem extends Item {
    public ClydnoHalterItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof AbstractHorse mount) || !mount.isAlive()) {
            return InteractionResult.PASS;
        }
        if (mount.getData(ModAttachments.CLYDNO_HALTERED)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.noblephantasms.clydno_halter.already_bound", mount.getDisplayName()));
            }
            return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        mount.setData(ModAttachments.CLYDNO_HALTERED, true);
        player.setItemInHand(hand, RecallBellItem.createBound(mount));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.noblephantasms.clydno_halter.bound", mount.getDisplayName()));
        }
        player.level().playSound(null, mount.blockPosition(), SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 0.9F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
