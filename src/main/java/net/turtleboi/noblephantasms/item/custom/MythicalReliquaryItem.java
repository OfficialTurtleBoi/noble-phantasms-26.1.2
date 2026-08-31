package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.screens.menus.custom.MythicalReliquaryMenu;

public final class MythicalReliquaryItem extends Item {
    public MythicalReliquaryItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        Identifier focusedRelic = stack.get(ModDataComponents.MYTHICAL_RELIQUARY_FOCUS.get());
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) ->
                        new MythicalReliquaryMenu(containerId, inventory, hand, focusedRelic),
                Component.translatable("menu.noblephantasms.mythical_reliquary")), buffer -> {
            buffer.writeEnum(hand);
            buffer.writeBoolean(focusedRelic != null);
            if (focusedRelic != null) {
                Identifier.STREAM_CODEC.encode(buffer, focusedRelic);
            }
        });
        return InteractionResult.SUCCESS_SERVER;
    }
}
