package net.turtleboi.noblephantasms.item.custom;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.turtleboi.noblephantasms.item.ModRarities;
import net.turtleboi.noblephantasms.component.ModDataComponents;

public final class TyrfingItem extends Item {
    private static final int SHEATHE_WINDOW = 20 * 4;
    private static final int GLINT_TRANSITION_TICKS = 10;
    private static final Map<Player, DrawState> DRAWN = new WeakHashMap<>();

    public TyrfingItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 7.0F, -2.0F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    public static void handlePlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DrawState state = DRAWN.get(player);
        if (!player.isAlive()) {
            DRAWN.remove(player);
            return;
        }
        if (state == null) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof TyrfingItem) {
                DRAWN.put(player, new DrawState(held, true));
                setCurseActive(held, true, player.level().getGameTime());
                syncCurseState(serverPlayer);
            }
            return;
        }
        if (!state.active) {
            if (player.getMainHandItem() != state.stack) {
                DRAWN.remove(player);
            } else if (--state.sheatheTicks <= 0) {
                state.active = true;
                setCurseActive(state.stack, true, player.level().getGameTime());
                syncCurseState(serverPlayer);
            }
            return;
        }
        Inventory inventory = player.getInventory();
        int slot = findStack(inventory, state.stack);
        if (slot < 0) {
            DRAWN.remove(player);
        } else if (slot < Inventory.getSelectionSize()) {
            forceSelectedSlot(serverPlayer, slot);
        } else {
            inventory.pickSlot(slot);
            inventory.setChanged();
            serverPlayer.connection.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
        }
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player deadPlayer) {
            DrawState state = DRAWN.remove(deadPlayer);
            if (state != null) {
                setCurseActive(state.stack, false, deadPlayer.level().getGameTime());
                if (deadPlayer instanceof ServerPlayer serverPlayer) {
                    syncCurseState(serverPlayer);
                }
            }
        }
        if (event.getSource().getEntity() instanceof Player killer) {
            DrawState state = DRAWN.get(killer);
            if (state != null && state.active) {
                state.active = false;
                state.sheatheTicks = SHEATHE_WINDOW;
                setCurseActive(state.stack, false, killer.level().getGameTime());
                if (killer instanceof ServerPlayer serverPlayer) {
                    syncCurseState(serverPlayer);
                }
            }
        }
    }

    public static void handleToss(ItemTossEvent event) {
        DrawState state = DRAWN.get(event.getPlayer());
        ItemStack tossed = event.getEntity().getItem();
        if (state == null || !state.active || !isCurseActive(tossed)) {
            return;
        }
        event.setCanceled(true);
        Inventory inventory = event.getPlayer().getInventory();
        int slot = inventory.getSelectedSlot();
        if (inventory.getItem(slot).isEmpty()) {
            inventory.setItem(slot, tossed);
            state.stack = tossed;
        } else if (inventory.add(tossed)) {
            state.stack = tossed;
            slot = findStack(inventory, tossed);
        }
        inventory.setChanged();
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            if (slot >= 0 && slot < Inventory.getSelectionSize()) {
                forceSelectedSlot(serverPlayer, slot);
            }
            syncCurseState(serverPlayer);
        }
    }

    public static void handleHandSwap(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player player && isDrawn(player)) {
            event.setCanceled(true);
        }
    }

    private static boolean isDrawn(Player player) {
        DrawState state = DRAWN.get(player);
        return state != null && state.active;
    }

    public static boolean isCurseActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TYRFING_CURSE_ACTIVE.get(), false);
    }

    public static double getCurseGlintTint(ItemStack stack, double gameTime) {
        Long changedAt = stack.get(ModDataComponents.TYRFING_CURSE_CHANGED_AT.get());
        boolean active = isCurseActive(stack);
        if (changedAt == null) {
            return active ? 1.0 : -1.0;
        }
        double progress = Math.clamp((gameTime - changedAt) / GLINT_TRANSITION_TICKS, 0.0, 1.0);
        if (active) {
            return progress;
        }
        return progress < 1.0 ? 1.0 - progress : -1.0;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    private static void setCurseActive(ItemStack stack, boolean active, long gameTime) {
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        stack.set(ModDataComponents.TYRFING_CURSE_CHANGED_AT.get(), gameTime);
        if (active) {
            stack.set(ModDataComponents.TYRFING_CURSE_ACTIVE.get(), true);
        } else {
            stack.remove(ModDataComponents.TYRFING_CURSE_ACTIVE.get());
        }
    }

    private static void syncCurseState(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastFullState();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastFullState();
        }
    }

    private static void forceSelectedSlot(ServerPlayer player, int slot) {
        Inventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() != slot) {
            inventory.setSelectedSlot(slot);
            player.connection.send(new ClientboundSetHeldSlotPacket(slot));
        }
    }

    private static int findStack(Inventory inventory, ItemStack target) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == target) {
                return slot;
            }
        }
        return -1;
    }

    private static final class DrawState {
        private ItemStack stack;
        private boolean active;
        private int sheatheTicks;

        private DrawState(ItemStack stack, boolean active) {
            this.stack = stack;
            this.active = active;
        }
    }
}
