package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.SlotContext;

public final class MegingjordItem extends CurioRelicItem {
    private static final CurioAttributeModifiers MODIFIERS = CurioAttributeModifiers.builder()
            .addModifier(Attributes.ATTACK_DAMAGE, modifier("attack_damage", 4.0,
                    AttributeModifier.Operation.ADD_VALUE))
            .addModifier(Attributes.ATTACK_KNOCKBACK, modifier("attack_knockback", 0.75,
                    AttributeModifier.Operation.ADD_VALUE))
            .addModifier(Attributes.BLOCK_BREAK_SPEED, modifier("block_break_speed", 0.5,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
            .addModifier(Attributes.MOVEMENT_EFFICIENCY, modifier("movement_efficiency", 1.0,
                    AttributeModifier.Operation.ADD_VALUE))
            .addModifier(Attributes.WATER_MOVEMENT_EFFICIENCY, modifier("water_movement_efficiency", 1.0,
                    AttributeModifier.Operation.ADD_VALUE))
            .addModifier(Attributes.SNEAKING_SPEED, modifier("sneaking_speed", 0.7,
                    AttributeModifier.Operation.ADD_VALUE))
            .addModifier(Attributes.SUBMERGED_MINING_SPEED, modifier("submerged_mining_speed", 0.8,
                    AttributeModifier.Operation.ADD_VALUE))
            .build();

    public MegingjordItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
        return MODIFIERS;
    }

    @Override
    public boolean canWalkOnPowderedSnow(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    public static void handleDamageComplete(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player
                || !isEquipped(player, ModItems.MEGINGJORD.get())
                || !event.getEntity().isAlive()) {
            return;
        }

        var movement = event.getEntity().getDeltaMovement();
        event.getEntity().setDeltaMovement(movement.x, Math.max(movement.y, 0.45), movement.z);
        event.getEntity().hurtMarked = true;
    }

    private static AttributeModifier modifier(String name, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(
                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "megingjord_" + name),
                amount,
                operation);
    }
}
