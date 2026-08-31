package net.turtleboi.noblephantasms.item.custom;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.item.ModRarities;

public final class TyrfingItem extends Item {
    private static final int BLOODLUST_DURATION = 20 * 12;
    private static final int MAX_BLOODLUST_STACKS = 5;
    private static final int GLINT_TRANSITION_TICKS = 10;
    private static final Identifier NEGLECT_DAMAGE_ID = id("tyrfing_neglect_damage");
    private static final Identifier NEGLECT_ATTACK_SPEED_ID = id("tyrfing_neglect_attack_speed");
    private static final Identifier NEGLECT_MOVEMENT_SPEED_ID = id("tyrfing_neglect_movement_speed");
    private static final Identifier BLOODLUST_DAMAGE_ID = id("tyrfing_bloodlust_damage");
    private static final Identifier BLOODLUST_ATTACK_SPEED_ID = id("tyrfing_bloodlust_attack_speed");
    private static final Identifier BLOODLUST_MOVEMENT_SPEED_ID = id("tyrfing_bloodlust_movement_speed");
    private static final AttributeModifier NEGLECT_DAMAGE = new AttributeModifier(
            NEGLECT_DAMAGE_ID, -2.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier NEGLECT_ATTACK_SPEED = new AttributeModifier(
            NEGLECT_ATTACK_SPEED_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier NEGLECT_MOVEMENT_SPEED = new AttributeModifier(
            NEGLECT_MOVEMENT_SPEED_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final Map<Player, BloodlustState> BLOODLUST = new WeakHashMap<>();

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
        if (!player.isAlive()) {
            clearModifiers(player);
            BLOODLUST.remove(player);
            return;
        }

        Inventory inventory = player.getInventory();
        boolean hasTyrfing = false;
        boolean held = player.getMainHandItem().getItem() instanceof TyrfingItem;
        boolean curseChanged = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof TyrfingItem)) {
                continue;
            }
            hasTyrfing = true;
            boolean neglected = !held;
            if (isCurseActive(stack) != neglected) {
                setCurseActive(stack, neglected, player.level().getGameTime());
                curseChanged = true;
            }
        }

        updateNeglectPenalty(player, hasTyrfing && !held);
        updateBloodlust(player, held && hasTyrfing);
        if (!hasTyrfing) {
            BLOODLUST.remove(player);
        }
        if (curseChanged) {
            syncCurseState(serverPlayer);
        }
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player deadPlayer) {
            clearModifiers(deadPlayer);
            BLOODLUST.remove(deadPlayer);
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || event.getSource().getDirectEntity() != killer
                || !(killer.getMainHandItem().getItem() instanceof TyrfingItem)) {
            return;
        }

        BloodlustState state = BLOODLUST.computeIfAbsent(killer, ignored -> new BloodlustState());
        state.stacks = Math.min(MAX_BLOODLUST_STACKS, state.stacks + 1);
        state.expiresAt = killer.level().getGameTime() + BLOODLUST_DURATION;
        updateBloodlustModifiers(killer, state.stacks);
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

    private static void updateNeglectPenalty(Player player, boolean active) {
        updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), NEGLECT_DAMAGE, active);
        updateModifier(player.getAttribute(Attributes.ATTACK_SPEED), NEGLECT_ATTACK_SPEED, active);
        updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), NEGLECT_MOVEMENT_SPEED, active);
    }

    private static void updateBloodlust(Player player, boolean held) {
        BloodlustState state = BLOODLUST.get(player);
        if (state != null && player.level().getGameTime() >= state.expiresAt) {
            BLOODLUST.remove(player);
            state = null;
        }
        updateBloodlustModifiers(player, held && state != null ? state.stacks : 0);
    }

    private static void updateBloodlustModifiers(Player player, int stacks) {
        updateScaledModifier(player.getAttribute(Attributes.ATTACK_DAMAGE),
                BLOODLUST_DAMAGE_ID, stacks * 0.75, AttributeModifier.Operation.ADD_VALUE);
        updateScaledModifier(player.getAttribute(Attributes.ATTACK_SPEED),
                BLOODLUST_ATTACK_SPEED_ID, stacks * 0.06,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateScaledModifier(player.getAttribute(Attributes.MOVEMENT_SPEED),
                BLOODLUST_MOVEMENT_SPEED_ID, stacks * 0.04,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void updateModifier(AttributeInstance attribute, AttributeModifier modifier, boolean active) {
        if (attribute == null) {
            return;
        }
        if (active) {
            attribute.addOrUpdateTransientModifier(modifier);
        } else {
            attribute.removeModifier(modifier.id());
        }
    }

    private static void updateScaledModifier(AttributeInstance attribute, Identifier id, double amount,
                                             AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        if (amount == 0.0) {
            attribute.removeModifier(id);
        } else {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void clearModifiers(Player player) {
        updateNeglectPenalty(player, false);
        updateBloodlustModifiers(player, 0);
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, path);
    }

    private static final class BloodlustState {
        private int stacks;
        private long expiresAt;
    }
}
