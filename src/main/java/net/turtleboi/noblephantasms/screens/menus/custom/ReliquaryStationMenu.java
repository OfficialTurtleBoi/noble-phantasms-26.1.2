package net.turtleboi.noblephantasms.screens.menus.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.block.entity.ReliquaryStationBlockEntity;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.item.custom.MythicalReliquaryItem;
import net.turtleboi.noblephantasms.relic.RelicFragmentArchive;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;
import net.turtleboi.noblephantasms.screens.menus.ModMenus;

public final class ReliquaryStationMenu extends AbstractContainerMenu {
    private static final int INVENTORY_X = 27;
    private static final int INVENTORY_Y = 149;
    private static final int HOTBAR_Y = 207;
    private final Inventory inventory;
    private final Container station;
    private final BlockPos stationPos;
    private PendingForge pendingForge;
    private Identifier selectedRelic;

    public ReliquaryStationMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, new SimpleContainer(ReliquaryStationBlockEntity.SIZE), buffer.readBlockPos());
    }

    public ReliquaryStationMenu(int containerId, Inventory inventory, ReliquaryStationBlockEntity station) {
        this(containerId, inventory, station, station.getBlockPos());
    }

    private ReliquaryStationMenu(int containerId, Inventory inventory, Container station, BlockPos pos) {
        super(ModMenus.RELIQUARY_STATION.get(), containerId);
        this.inventory = inventory;
        this.station = station;
        this.stationPos = pos;
        station.startOpen(inventory.player);
        returnLegacyInputs();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    public ItemStack reliquary() {
        return MythicalReliquaryItem.findInInventory(inventory.player);
    }

    public Identifier relicId() {
        return selectedRelic;
    }

    public RelicFragmentArchive archive() {
        return reliquary().getOrDefault(ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(),
                RelicFragmentArchive.EMPTY);
    }

    public RelicFragmentArchive.RelicSet selectedSet() {
        Identifier relicId = relicId();
        return relicId == null ? null : archive().get(relicId);
    }

    public boolean selectRelic(Player player, Identifier selectedRelic) {
        if (!RelicFragmentDefinitions.supports(selectedRelic)) {
            return false;
        }
        ItemStack book = reliquary();
        if (book.isEmpty()) {
            return false;
        }
        this.selectedRelic = selectedRelic;
        book.set(ModDataComponents.MYTHICAL_RELIQUARY_FOCUS.get(), selectedRelic);
        inventory.setChanged();
        broadcastChanges();
        return true;
    }

    public ForgeStart beginForge(Player player, Identifier submittedRelic, long submittedSeed) {
        if (pendingForge != null || submittedRelic == null || !submittedRelic.equals(relicId())) {
            return null;
        }
        ItemStack book = reliquary();
        RelicFragmentArchive archive = book.getOrDefault(
                ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(), RelicFragmentArchive.EMPTY);
        RelicFragmentArchive.RelicSet set = archive.get(submittedRelic);
        if (set == null || set.seed() != submittedSeed || !set.complete()) {
            return null;
        }
        ItemStack output = new ItemStack(BuiltInRegistries.ITEM.getValue(submittedRelic));
        int inventorySlot = inventory.getFreeSlot();
        int menuSlot = inventorySlot < 0 ? -1 : inventorySlot < 9
                ? 27 + inventorySlot : inventorySlot - 9;
        book.set(ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(),
                archive.consume(submittedRelic, submittedSeed));
        inventory.setChanged();
        pendingForge = new PendingForge(submittedRelic, submittedSeed, output, inventorySlot);
        broadcastChanges();
        return new ForgeStart(submittedRelic, submittedSeed, menuSlot, set.pieceCount());
    }

    public boolean finishForge(Player player, Identifier submittedRelic, long submittedSeed) {
        if (pendingForge == null || !pendingForge.relicId().equals(submittedRelic)
                || pendingForge.seed() != submittedSeed) {
            return false;
        }
        PendingForge forge = pendingForge;
        pendingForge = null;
        boolean delivered = forge.inventorySlot() >= 0
                && inventory.getItem(forge.inventorySlot()).isEmpty();
        if (delivered) {
            inventory.setItem(forge.inventorySlot(), forge.output());
            inventory.setChanged();
            broadcastChanges();
            return true;
        }
        dropAtStation(player, forge.output());
        player.closeContainer();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (pendingForge != null) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int mainInventorySize = 27;
        if (slotIndex < mainInventorySize) {
            if (!moveItemStackTo(stack, mainInventorySize, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, mainInventorySize, false)) {
                return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return station.stillValid(player);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (pendingForge == null) {
            super.clicked(slotIndex, buttonNum, input, player);
        }
    }

    @Override
    public void removed(Player player) {
        if (pendingForge != null && !player.level().isClientSide()) {
            PendingForge forge = pendingForge;
            pendingForge = null;
            if (forge.inventorySlot() >= 0 && inventory.getItem(forge.inventorySlot()).isEmpty()) {
                inventory.setItem(forge.inventorySlot(), forge.output());
                inventory.setChanged();
            } else if (!inventory.add(forge.output())) {
                dropAtStation(player, forge.output());
            }
        }
        super.removed(player);
        station.stopOpen(player);
    }

    private void dropAtStation(Player player, ItemStack stack) {
        ItemEntity item = new ItemEntity(player.level(), stationPos.getX() + 0.5,
                stationPos.getY() + 1.1, stationPos.getZ() + 0.5, stack);
        item.setDeltaMovement(0.0, 0.08, 0.0);
        item.setDefaultPickUpDelay();
        player.level().addFreshEntity(item);
    }

    private void returnLegacyInputs() {
        if (inventory.player.level().isClientSide()) {
            return;
        }
        for (int slot = 0; slot < station.getContainerSize(); slot++) {
            ItemStack stack = station.removeItemNoUpdate(slot);
            if (!stack.isEmpty() && !inventory.add(stack)) {
                inventory.player.drop(stack, false);
            }
        }
        station.setChanged();
    }

    public record ForgeStart(Identifier relicId, long seed, int targetMenuSlot,
                             int pieceCount) {
    }

    private record PendingForge(Identifier relicId, long seed, ItemStack output,
                                int inventorySlot) {
    }
}
