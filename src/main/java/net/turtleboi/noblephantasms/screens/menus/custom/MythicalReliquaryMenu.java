package net.turtleboi.noblephantasms.screens.menus.custom;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;
import net.turtleboi.noblephantasms.screens.menus.ModMenus;

public final class MythicalReliquaryMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private Identifier focusedRelic;

    public MythicalReliquaryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readEnum(InteractionHand.class), readFocusedRelic(buffer));
    }

    public MythicalReliquaryMenu(int containerId, Inventory inventory, InteractionHand hand,
                                 Identifier focusedRelic) {
        super(ModMenus.MYTHICAL_RELIQUARY.get(), containerId);
        this.hand = hand;
        this.focusedRelic = focusedRelic;
    }

    private static Identifier readFocusedRelic(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Identifier.STREAM_CODEC.decode(buffer) : null;
    }

    public Identifier focusedRelic() {
        return focusedRelic;
    }

    public boolean selectRelic(Player player, Identifier relicId) {
        if (!RelicFragmentDefinitions.supports(relicId)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.MYTHICAL_RELIQUARY.get())) {
            return false;
        }
        stack.set(ModDataComponents.MYTHICAL_RELIQUARY_FOCUS.get(), relicId);
        focusedRelic = relicId;
        player.getInventory().setChanged();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).is(ModItems.MYTHICAL_RELIQUARY.get());
    }
}
