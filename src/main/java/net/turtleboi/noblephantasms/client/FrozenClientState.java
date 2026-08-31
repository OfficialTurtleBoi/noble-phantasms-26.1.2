package net.turtleboi.noblephantasms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.effect.ModEffects;
import net.turtleboi.noblephantasms.mixin.LivingEntityAccessor;

public final class FrozenClientState {
    private static boolean wasFrozen;
    private static Integer selectedSlot;
    private static float yaw;
    private static float pitch;
    private static InteractionHand usedHand;
    private static ItemStack usedItem = ItemStack.EMPTY;
    private static int useRemaining;

    private FrozenClientState() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            clear();
            return;
        }
        boolean frozen = player.hasEffect(ModEffects.FROZEN);
        if (frozen) {
            if (!wasFrozen) {
                yaw = player.getYRot();
                pitch = player.getXRot();
                selectedSlot = player.getInventory().getSelectedSlot();
                captureUse(player);
            }
            enforce(player);
            preserveUse(player);
        } else if (wasFrozen) {
            enforceRotation(player);
            releaseUse(player);
            minecraft.mouseHandler.setIgnoreFirstMove();
            ((FrozenMouseHandlerAccess) minecraft.mouseHandler).noblePhantasms$resetLookState();
            selectedSlot = null;
        }
        wasFrozen = frozen;
    }

    public static void clear() {
        wasFrozen = false;
        selectedSlot = null;
        usedHand = null;
        usedItem = ItemStack.EMPTY;
        useRemaining = 0;
    }

    private static void captureUse(LocalPlayer player) {
        if (!player.isUsingItem()) {
            usedHand = null;
            usedItem = ItemStack.EMPTY;
            useRemaining = 0;
            return;
        }
        usedHand = player.getUsedItemHand();
        usedItem = player.getUseItem().copy();
        useRemaining = player.getUseItemRemainingTicks();
    }

    private static void enforce(LocalPlayer player) {
        if (selectedSlot != null) {
            player.getInventory().setSelectedSlot(selectedSlot);
        }
        enforceRotation(player);
    }

    private static void enforceRotation(LocalPlayer player) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRotO = yaw;
        player.yBodyRotO = yaw;
    }

    private static void preserveUse(LocalPlayer player) {
        if (!validUse(player)) {
            return;
        }
        if (!player.isUsingItem()) {
            player.startUsingItem(usedHand);
        }
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        accessor.noblePhantasms$setUseItem(player.getItemInHand(usedHand));
        accessor.noblePhantasms$setUseItemRemaining(useRemaining);
    }

    private static void releaseUse(LocalPlayer player) {
        if (!validUse(player)) {
            clearUse();
            return;
        }
        player.startUsingItem(usedHand);
        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
        accessor.noblePhantasms$setUseItem(player.getItemInHand(usedHand));
        accessor.noblePhantasms$setUseItemRemaining(useRemaining);
        player.releaseUsingItem();
        clearUse();
    }

    private static boolean validUse(LocalPlayer player) {
        return usedHand != null && useRemaining > 0 && !usedItem.isEmpty()
                && ItemStack.matches(player.getItemInHand(usedHand), usedItem);
    }

    private static void clearUse() {
        usedHand = null;
        usedItem = ItemStack.EMPTY;
        useRemaining = 0;
    }
}
