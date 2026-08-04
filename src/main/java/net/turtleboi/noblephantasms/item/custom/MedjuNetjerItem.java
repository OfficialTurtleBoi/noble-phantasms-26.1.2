package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.item.ModItems;

public class MedjuNetjerItem extends Item {
    public interface MenuAccess {
        boolean noblePhantasms$hasMedjuNetjer();
    }

    public MedjuNetjerItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static void handleTableInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) {
            return;
        }

        BlockEntity table = event.getLevel().getBlockEntity(event.getPos());
        if (table == null) {
            return;
        }

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        boolean installed = table.getData(ModAttachments.MEDJU_NETJER_INSTALLED.get());
        if (heldItem.is(ModItems.MEDJU_NETJER) && !installed) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!event.getLevel().isClientSide()) {
                installBook(table, player, heldItem);
            }
        } else if (heldItem.isEmpty() && player.isShiftKeyDown() && installed) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!event.getLevel().isClientSide()) {
                table.setData(ModAttachments.MEDJU_NETJER_INSTALLED.get(), false);
                table.syncData(ModAttachments.MEDJU_NETJER_INSTALLED.get());
                ItemStack book = new ItemStack(ModItems.MEDJU_NETJER.get());
                if (!player.getInventory().add(book)) {
                    player.drop(book, false);
                }
                event.getLevel().playSound(null, event.getPos(), SoundEvents.BOOK_PAGE_TURN,
                        SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
    }

    public static void handleBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(Blocks.ENCHANTING_TABLE)) {
            return;
        }

        BlockEntity table = level.getBlockEntity(event.getPos());
        if (table != null && table.getData(ModAttachments.MEDJU_NETJER_INSTALLED.get())) {
            table.setData(ModAttachments.MEDJU_NETJER_INSTALLED.get(), false);
            Block.popResource(level, event.getPos(), new ItemStack(ModItems.MEDJU_NETJER.get()));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.ENCHANTING_TABLE)) {
            return InteractionResult.PASS;
        }

        BlockEntity table = context.getLevel().getBlockEntity(context.getClickedPos());
        if (table == null || table.getData(ModAttachments.MEDJU_NETJER_INSTALLED.get())) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        installBook(table, context.getPlayer(), context.getItemInHand());
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void installBook(BlockEntity table, Player player, ItemStack heldItem) {
        table.setData(ModAttachments.MEDJU_NETJER_INSTALLED.get(), true);
        table.syncData(ModAttachments.MEDJU_NETJER_INSTALLED.get());
        heldItem.consume(1, player);
        table.getLevel().playSound(null, table.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 0.75F);
    }
}
