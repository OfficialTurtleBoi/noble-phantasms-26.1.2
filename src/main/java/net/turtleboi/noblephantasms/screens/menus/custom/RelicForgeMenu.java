package net.turtleboi.noblephantasms.screens.menus.custom;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;
import net.turtleboi.noblephantasms.screens.menus.ModMenus;

public final class RelicForgeMenu extends AbstractContainerMenu {
    private final Identifier relicId;
    private final long seed;
    private final int pieceCount;
    private boolean completed;

    public RelicForgeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, Identifier.STREAM_CODEC.decode(buffer), buffer.readLong());
    }

    public RelicForgeMenu(int containerId, Inventory inventory, Identifier relicId, long seed) {
        super(ModMenus.RELIC_FORGE.get(), containerId);
        this.relicId = relicId;
        this.seed = seed;
        RelicFragmenter.Layout layout = RelicFragmenter.create(relicId, seed);
        this.pieceCount = layout == null ? 0 : layout.pieceCount();
    }

    public Identifier relicId() {
        return relicId;
    }

    public long seed() {
        return seed;
    }

    public int pieceCount() {
        return pieceCount;
    }

    public boolean complete(Player player, long submittedSeed) {
        if (completed || submittedSeed != seed || pieceCount == 0 || !hasFragments(player, pieceCount)) {
            return false;
        }
        consumeFragments(player, pieceCount);
        completed = true;
        return true;
    }

    public boolean hasFragments(Player player, int required) {
        int found = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            RelicFragmentData data = stack.get(ModDataComponents.RELIC_FRAGMENT.get());
            if (data != null && data.relicId().equals(relicId) && data.pieceIndex() < 0) {
                found += stack.getCount();
                if (found >= required) {
                    return true;
                }
            }
        }
        return player.isCreative();
    }

    private void consumeFragments(Player player, int required) {
        if (player.isCreative()) {
            return;
        }
        Inventory inventory = player.getInventory();
        int remaining = required;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            RelicFragmentData data = stack.get(ModDataComponents.RELIC_FRAGMENT.get());
            if (data == null || !data.relicId().equals(relicId) || data.pieceIndex() >= 0) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !completed;
    }
}
