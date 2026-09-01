package net.turtleboi.noblephantasms.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;

public final class ReliquaryStationBlockEntity extends BlockEntity implements Container {
    public static final int RAW_FRAGMENT_SLOT = 0;
    public static final int BRUSH_SLOT = 1;
    public static final int SIZE = 2;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public ReliquaryStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELIQUARY_STATION.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!items.get(RAW_FRAGMENT_SLOT).isEmpty()) {
            output.store("raw_fragments", ItemStack.CODEC, items.get(RAW_FRAGMENT_SLOT));
        }
        if (!items.get(BRUSH_SLOT).isEmpty()) {
            output.store("brush", ItemStack.CODEC, items.get(BRUSH_SLOT));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.set(RAW_FRAGMENT_SLOT, input.read("raw_fragments", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        items.set(BRUSH_SLOT, input.read("brush", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == RAW_FRAGMENT_SLOT
                ? stack.getItem() instanceof RelicFragmentItem fragment && fragment.isUnidentified()
                : slot == BRUSH_SLOT && stack.getItem() instanceof BrushItem;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, this);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
